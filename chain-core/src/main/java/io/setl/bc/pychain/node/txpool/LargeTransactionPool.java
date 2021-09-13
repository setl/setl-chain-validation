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
package io.setl.bc.pychain.node.txpool;

import static io.setl.bc.logging.LoggingConstants.MARKER_PERFORMANCE;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.setl.bc.performance.PerformanceEventListener;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.block.TxIdList;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.common.MutableLong;
import io.setl.util.ParallelTask;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;
import io.setl.util.PriorityExecutor.TaskContext;

import io.micrometer.core.instrument.MeterRegistry;


/**
 * A transaction pool optimised for a large number of transactions.
 *
 * <p>The pool is composed of 65521 lanes (65521 = 2^16 - 15 is a prime number). An address is assigned to a lane by its Java hash code. As addresses are
 * derived using a SHA, they are expected to be uniformly distributed. Operations within a lane are single-threaded.</p>
 *
 * @author Simon Greatrix on 2019-04-12.
 */
public class LargeTransactionPool implements TransactionPool {

  /** The number of lanes we break addresses into. This is a prime number. */
  private static final int LANE_COUNT = 65521;

  private static final Logger logger = LoggerFactory.getLogger(LargeTransactionPool.class);

  private final Lane[] lanes;

  private final PriorityExecutor priorityExecutor;

  private final SecureRandom random;

  private final AtomicInteger size = new AtomicInteger(0);

  private final StateManager stateManager;

  private final Object txFlowLock = new Object();

  private final List<PerformanceEventListener> txPoolListeners = new CopyOnWriteArrayList<>();

  /** Store cross chain transaction packages by hash only. They all have the same address, and the nonces are not in sequence if from multiple chains. */
  private final ConcurrentHashMap<Hash, Txi> xChainTx = new ConcurrentHashMap<>();

  @Value("${txPool.maxLevel}")
  private int maxLevel;

  @Value("${txPool.minLevel}")
  private int minLevel;

  private boolean notifyAbove;

  private boolean notifyBelow;


  /**
   * New instance.
   *
   * @param stateManager     the state manager
   * @param priorityExecutor the executor
   * @param secureRandom     a PRNG
   */
  @Autowired
  public LargeTransactionPool(StateManager stateManager, PriorityExecutor priorityExecutor, SecureRandom secureRandom, MeterRegistry meterRegistry) {
    this.stateManager = stateManager;
    this.priorityExecutor = priorityExecutor;
    this.random = secureRandom;

    lanes = new Lane[LANE_COUNT];
    for (int i = 0; i < LANE_COUNT; i++) {
      lanes[i] = new Lane();
    }

    meterRegistry.gauge("transactionPool_size", size);
  }


  @Override
  public void addListener(PerformanceEventListener listener) {
    txPoolListeners.add(listener);
  }


  @Override
  public void addTx(Txi va) {
    String nonceAddress = va.getNonceAddress();
    if (nonceAddress.equals("")) {
      Hash hash = Hash.fromHex(va.getHash());
      if (xChainTx.put(hash, va) == null) {
        size.incrementAndGet();
        notifyListeners();
      }

      return;
    }

    int laneChoice = (nonceAddress.hashCode() & 0x7fff_ffff) % LANE_COUNT;
    Lane myLane = lanes[laneChoice];
    Series series = myLane.findOrCreate(stateManager, nonceAddress);
    if (series.addTx(va)) {
      size.incrementAndGet();
      notifyListeners();
    }
  }


  @Override
  public void bulkRemove(Block block) {
    TaskContext taskContext = priorityExecutor.newTaskContext(Priorities.PROPOSAL);

    // Find the maximum nonce for each address in the block.
    Txi[] transactions = block.getTransactions();
    ConcurrentHashMap<String, MutableLong> nonceUsage = new ConcurrentHashMap<>();

    ParallelTask.process(taskContext, transactions.length, i -> {
      Txi txi = transactions[i];
      String a = txi.getNonceAddress();
      if (a.equals("")) {
        xChainTx.remove(Hash.fromHex(txi.getHash()));
        return;
      }

      long n = txi.getNonce();
      MutableLong minMax = nonceUsage.computeIfAbsent(a, x -> new MutableLong());
      synchronized (minMax) {
        minMax.max(n);
      }
    });

    Spliterator<Entry<String, MutableLong>> spliterator = nonceUsage.entrySet().spliterator();
    ParallelTask.process(taskContext, spliterator, e -> {
      String nonceAddress = e.getKey();
      MutableLong nonce = e.getValue();
      int laneChoice = (nonceAddress.hashCode() & 0x7fff_ffff) % LANE_COUNT;
      Lane myLane = lanes[laneChoice];
      logger.debug("Clearing TXs for {} up to {}", nonceAddress, nonce);
      myLane.bulkRemove(nonceAddress, nonce.longValue());
      return false;
    });

    ParallelTask.process(taskContext, LANE_COUNT, (IntConsumer) index -> lanes[index].cleanUp());

    size.addAndGet(-transactions.length);
    notifyListeners();
  }


  @Override
  public void clear() {
    for (Lane l : lanes) {
      l.clear();
    }
    size.set(0);
    notifyListeners();
  }


  @Override
  public ProposedTxList createProposal(int maxTxPerBlock) {
    int startLane = random.nextInt(LANE_COUNT);
    ProposedTxList.Builder builder = ProposedTxList.build();

    xChainTx.values().forEach(builder::add);

    for (int i = LANE_COUNT; i > 0 && maxTxPerBlock > 0; i--) {
      Lane myLane = lanes[(i + startLane) % LANE_COUNT];
      maxTxPerBlock = myLane.propose(builder, maxTxPerBlock);
    }
    return builder.build();
  }


  @Override
  public List<Txi> getAllTx(String nonceAddress) {
    if (nonceAddress.equals("")) {
      return new ArrayList<>(xChainTx.values());
    }

    int laneChoice = (nonceAddress.hashCode() & 0x7fff_ffff) % LANE_COUNT;
    Lane myLane = lanes[laneChoice];
    Series series = myLane.findOrCreate(stateManager, nonceAddress);
    return series.getAllTXs();
  }


  @Override
  public int getAvailableTransactionCount() {
    return size.get();
  }


  @Override
  public Txi getTx(String nonceAddress, long nonce, Hash hash) {
    if (nonceAddress.equals("")) {
      return xChainTx.get(hash);
    }

    int laneChoice = (nonceAddress.hashCode() & 0x7fff_ffff) % LANE_COUNT;
    Lane myLane = lanes[laneChoice];
    Series series = myLane.findOrCreate(stateManager, nonceAddress);
    return series.getTx(nonce, hash);
  }


  @Override
  public boolean hasAvailableTransactions() {
    return size.get() > 0;
  }


  @Override
  public HasResult hasTx(String nonceAddress, long nonce, Hash hash) {
    if (nonceAddress.equals("")) {
      return xChainTx.containsKey(hash) ? HasResult.PRESENT : HasResult.NOT_PRESENT;
    }
    int laneChoice = (nonceAddress.hashCode() & 0x7fff_ffff) % LANE_COUNT;
    Lane myLane = lanes[laneChoice];
    Series series = myLane.findOrCreate(stateManager, nonceAddress);
    return series.hasTx(nonce, hash);
  }


  @Override
  public void matchTxs(List<Txi> transactions, Set<NonceAndHash> unmatched, TxIdList txIds) {
    String nonceAddress = txIds.getAddress();
    if (nonceAddress.equals("")) {
      for (Hash hash : txIds.getHashes()) {
        Txi txi = xChainTx.get(hash);
        if (txi != null) {
          transactions.add(txi);
        } else {
          unmatched.add(new NonceAndHash(0, hash));
        }
      }
      return;
    }

    int laneChoice = (nonceAddress.hashCode() & 0x7fff_ffff) % LANE_COUNT;
    Lane myLane = lanes[laneChoice];
    Series series = myLane.findOrCreate(stateManager, nonceAddress);
    series.matchTxs(transactions, unmatched, txIds);
  }


  @Override
  public void notifyListeners() {
    int currentSize = size.get();

    if (currentSize < minLevel && !notifyBelow) {
      if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
        logger.warn(MARKER_PERFORMANCE, "Tx Pool size is below minimum level: {}", currentSize);
      }

      synchronized (txFlowLock) {
        notifyBelow = true;
        notifyAbove = false;
        txPoolListeners.forEach(PerformanceEventListener::belowMinimumEvent);
      }
    }

    if (currentSize > maxLevel && !notifyAbove) {
      if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
        logger.warn(MARKER_PERFORMANCE, "Tx Pool size is above maximum level: {}", currentSize);
      }

      synchronized (txFlowLock) {
        notifyBelow = false;
        notifyAbove = true;
        txPoolListeners.forEach(PerformanceEventListener::aboveMaximumEvent);
      }
    }
  }


  public void setMaxLevel(int maxLevel) {
    this.maxLevel = maxLevel;
  }


  public void setMinLevel(int minLevel) {
    this.minLevel = minLevel;
  }

}
