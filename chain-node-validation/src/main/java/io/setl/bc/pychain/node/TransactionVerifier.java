/* <notice>
 
    SETL Blockchain
    Copyright (C) 2021 SETL Ltd
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License, version 3, as
    published by the Free Software Foundation.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
 
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 
</notice> */
package io.setl.bc.pychain.node;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.node.TransactionPool.HasResult;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.bc.pychain.tx.verifier.TxVerifier;
import io.setl.common.MutableBoolean;
import io.setl.common.MutableLong;
import io.setl.util.ParallelTask;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;
import io.setl.util.PriorityExecutor.TaskContext;

/**
 * Verify transactions. Created by aanten on 12/07/2017.
 */
@Component
public class TransactionVerifier {

  private static final Logger logger = LoggerFactory.getLogger(TransactionVerifier.class);



  private enum Verify {
    IN_POOL, INVALID, GOOD;
  }



  static class Result {

    final String hash;

    Verify outcome = null;

    AbstractTx tx;


    Result(String hash) {
      this.hash = hash;
    }


    @SuppressWarnings("squid:S2886")
      // 'Getters and setters should be synchronized in pairs' : Looks like it is ?
    Verify getOutcome() throws InterruptedException {
      synchronized (this) {
        while (outcome == null) {
          wait();
        }
      }
      return outcome;
    }


    void setOutcome(Verify outcome) {
      synchronized (this) {
        this.outcome = outcome;
        this.notifyAll();
      }
    }

  }



  @SuppressWarnings("squid:S1068") // 'Unused "private" fields should be removed'
  private final EventBus eventBus;

  private final MutableLong fifoOut = new MutableLong(0);

  private final AtomicLong fifoSource = new AtomicLong(0);

  private final ConcurrentMap<String, Result> inProgress = new ConcurrentHashMap();

  private final PriorityExecutor priorityExecutor;

  private final StateManager stateManager;

  private final TransactionPool transactionPool;

  private final TransactionProcessor transactionProcessor;

  private final Object[] txLocks;

  private final TxVerifier txVerifier;


  /**
   * some javadoc.
   *
   * @param txVerifier      aa
   * @param transactionPool bb
   */
  public TransactionVerifier(
      TxVerifier txVerifier,
      TransactionPool transactionPool,
      PriorityExecutor priorityExecutor,
      StateManager stateManager,
      TransactionProcessor transactionProcessor,
      EventBus eventBus
  ) {
    this.txVerifier = txVerifier;
    this.transactionPool = transactionPool;
    this.priorityExecutor = priorityExecutor;
    this.stateManager = stateManager;
    this.transactionProcessor = transactionProcessor;
    this.eventBus = eventBus;

    txLocks = new Object[256];
    for (int i = txLocks.length - 1; i >= 0; i--) {
      txLocks[i] = new Object();
    }
  }


  private Object lock(String hash) {
    return txLocks[hash.hashCode() & 0xff];
  }


  /**
   * Process the transactions and add them to the pool. Report if any are found to be "bad".
   *
   * @param source       the source of the transactions
   * @param transactions the transactions
   * @param proposalId   associated proposal if any
   *
   * @return true if all the transactions are good
   */
  private boolean processMultipleTransactions(TaskContext taskContext, PeerAddress source, List<MPWrappedArray> transactions, UUID proposalId) {

    int length = transactions.size();
    if (length == 0) {
      return true;
    }
    boolean isForProposal = proposalId != null;
    long fifo = isForProposal ? 0 : fifoSource.getAndIncrement();
    long startTime = System.currentTimeMillis();
    Result[] results = new Result[length];

    int badCount = 0;
    try {
      ParallelTask.process(taskContext, length, i -> {
        MPWrappedArray def = transactions.get(i);
        if (def != null) {
          results[i] = verifyTx(source, def);
        } else {
          logger.error("Missing transaction {}", i);
        }
      });

      if (!isForProposal) {
        // wait for us to reach the head of the FIFO queue
        synchronized (fifoOut) {
          while (fifoOut.longValue() < fifo) {
            fifoOut.wait();
          }
        }
      }

      // process the results
      for (Result result : results) {
        // Synchronize so we can update in-progress and the tx-pool atomically.
        Verify outcome = result.getOutcome();
        synchronized (lock(result.hash)) {
          switch (outcome) {
            case IN_POOL:
              // do nothing
              break;

            case INVALID:
              badCount++;
              break;

            case GOOD:
              // add to pool
              transactionPool.addTx(result.tx);
              break;

            default:
              break;
          }

          inProgress.remove(result.hash);
        }
      }

    } catch (InterruptedException ie) {
      logger.error("Transaction processing was interrupted.", ie);
      Thread.currentThread().interrupt();
    } finally {
      if (!isForProposal) {
        // Allow next FIFO element out
        synchronized (fifoOut) {
          fifoOut.increment();
          fifoOut.notifyAll();
        }
      }
    }

    if (logger.isInfoEnabled()) {
      long finishTime = System.currentTimeMillis();
      long elapsed = finishTime - startTime;
      long avg = 1000 * length / (elapsed != 0 ? elapsed : 1);
      if (isForProposal) {
        logger.info("Verified {} transactions ({} bad) in {}ms ({} tx/s) for batch {} for proposal {}", length, badCount, elapsed, avg, fifo, proposalId);
      } else {
        logger.info("Verified {} transactions ({} bad) in {}ms ({} tx/s) for batch {}", length, badCount, elapsed, avg, fifo);
      }
    }

    return badCount == 0;
  }


  private Result verifyTx(PeerAddress addr, MPWrappedArray txMessage) {
    try {
      AbstractTx tx = TxFromList.txFromList(txMessage);
      String hash = tx.getHash();

      Result result;

      // TODO - there is a vulnerability here for hash stealing. If a TX with a valid hash has been validated and is in the pool, or in-progress, then a
      // second transaction which has copied its hash will be assumed to be the same transaction and also pass. To fix this we need to check that the TX in
      // the pool is equal to the TX which we have been asked to verify. That requires implementing equals() on all the TXIs.
      //
      // However : When transactions are classed as 'IN_POOL', they are not added again to the TxPool and so are effectively dropped, new Txs have their
      // Hash checked properly a little later on.

      synchronized (lock(hash)) {
        //Is transaction already in the pool?
        HasResult has = transactionPool.hasTx(tx.getNonceAddress(), tx.getNonce(), Hash.fromHex(hash));
        if (has == HasResult.PRESENT) {
          // In pool, so skip.
          result = new Result(hash);
          result.setOutcome(Verify.IN_POOL);
          return result;
        }
        if (has == HasResult.REPLAY) {
          // Replays are expected every now and again. A node may receive a transaction that requires a TX it does not know yet and request it. It will then
          // receive the TX again via the normal process.
          logger.warn("Transaction for pool is replay:{}", tx.getHash());
          result = new Result(hash);
          result.setOutcome(Verify.INVALID);
          return result;
        }

        // Is transaction in progress?
        MutableBoolean didCreate = new MutableBoolean(false);
        result = inProgress.computeIfAbsent(hash, h -> {
          didCreate.set(true);
          return new Result(h);
        });
        if (!didCreate.get()) {
          // Did not create it, so in-progress elsewhere.
          return result;
        }
      }

      result.tx = tx;

      if (!txVerifier.verifyCurrentHash(tx)) {
        //TODO - progress any transactions with same hash
        logger.warn("Transaction hash invalid:{}", tx.getHash());
        result.setOutcome(Verify.INVALID);
        return result;
      }
      if (!txVerifier.verifySignature(tx)) {
        //TODO - Potentially record hash, pubkey and signature to avoid re-verification
        //TODO - progress any transactions with same hash
        logger.warn("Transaction signature invalid:{}", tx.getHash());
        result.setOutcome(Verify.INVALID);
        return result;
      }

      if (!transactionProcessor.checkValidatedTransactionForPool(tx, stateManager.getState())) {
        logger.warn("Transaction for pool failed (replay?):{}", tx.getHash());
        result.setOutcome(Verify.INVALID);
        return result;
      }

      //TODO - remove any transactions with same hash
      if (logger.isTraceEnabled()) {
        logger.trace("Transaction valid:{}", tx.getHash());
      }

      result.setOutcome(Verify.GOOD);
      return result;
    } catch (RuntimeException e) {
      logger.error("Transaction from {} caused internal error:{}", addr.getAddressString(), Arrays.deepToString(txMessage.unwrap()), e);
      throw new IllegalArgumentException("Transaction caused internal error", e);
    }
  }


  /**
   * Verify the transactions and add them to the pool. This method proceeds asynchronously.
   *
   * @param addr       the source of the transactions
   * @param txPackage  the transactions
   * @param proposalId the associated proposal, if any
   */
  public CompletableFuture<Boolean> verifyTxsAndAddToPool(PeerAddress addr, TxPackage txPackage, UUID proposalId) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    priorityExecutor.submit(
        proposalId != null ? Priorities.PROPOSAL : Priorities.TX_VERIFY,
        ctxt -> future.complete(Boolean.valueOf(
            processMultipleTransactions(ctxt, addr, txPackage.getEncodedTxs(), proposalId)
        ))
    );
    return future;
  }

}
