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
package io.setl.bc.pychain.tx;

import static java.util.function.Predicate.not;

import static io.setl.bc.pychain.block.TransactionForProcessing.SORT_BLOCK_ORDER;
import static io.setl.bc.pychain.block.TransactionForProcessing.SORT_NONCE_CHECK_ORDER;
import static io.setl.bc.pychain.block.TransactionForProcessing.SORT_PROCESSING_ORDER;
import static io.setl.bc.pychain.tx.UpdateEvent.updateEvent;
import static io.setl.common.CommonPy.VersionConstants.VERSION_SET_ADDRESS_TIME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.block.TransactionForProcessing;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.MemoTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.tx.create.Memo;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.common.MutableLong;
import io.setl.common.MutableObject;
import io.setl.util.PriorityExecutor;

/**
 * Created by nicholaspennington on 10/07/2017.
 */
public class DefaultProcessor implements TransactionProcessor {

  private static final Logger logger = LoggerFactory.getLogger(DefaultProcessor.class);

  private static DefaultProcessor theInstance = null;


  /**
   * DefaultProcessor singleton model.
   *
   * @return : DefaultProcessor
   */
  public static synchronized DefaultProcessor getInstance() {
    if (theInstance == null) {
      theInstance = new DefaultProcessor();
    }
    return theInstance;
  }


  /**
   * Check a nonce is not too old, not too new, but just right. Log appropriately and signal processing abort if validating.
   *
   * @param tfp          the transaction
   * @param expected     the expected nonce
   * @param validateMode the mode
   *
   * @return true if processing should abort
   */
  private boolean checkForBadNonce(TransactionForProcessing tfp, MutableLong expected, boolean validateMode) {
    switch (Long.compare(tfp.getNonce(), expected.longValue())) {
      case -1:
        tfp.rejectNonceInPast();  // Will be dropped from the Proposal
        if (validateMode) {
          logger.error("Failed to process transaction - replay tx {}:{} expected nonce {}", tfp.getHash(), tfp.getNonce(), expected.longValue());
          return true;
        }
        break;
      case 1:
        tfp.rejectNonceInFuture();  // Will be dropped from the Proposal
        if (validateMode) {
          logger.error("Failed to process transaction - future tx {}:{} expected nonce {}", tfp.getHash(), tfp.getNonce(), expected.longValue());
          return true;
        }
        break;
      default:
        // Nonce OK. Next one must be in sequence.
        expected.increment();
        break;
    }
    return false;
  }


  /**
   * Validate the nonces on the supplied transactions. The transactions are already sorted into address+nonce order. The first transaction for each address
   * must match the expectations of state and then nonces must increase without gaps. An address not currently present in state must start with nonce zero.
   *
   * @param stateSnapshot the current state
   * @param transactions  the transactions
   * @param validateMode  if true, terminate processing if a problem is detected
   *
   * @return true if processing should be terminated.
   */
  private boolean checkForBadNonces(StateSnapshot stateSnapshot, TransactionForProcessing[] transactions, boolean validateMode) {
    // X-Chain TX Package transactions have a unique nonce system, so we need to separate them out and process them separately.
    ArrayList<TransactionForProcessing> regularTxs = new ArrayList<>(transactions.length);
    ArrayList<TransactionForProcessing> xChainTxs = new ArrayList<>(transactions.length);

    // Copy and sort into nonce check order
    TransactionForProcessing[] nonceOrderedTxs = transactions.clone();
    Arrays.sort(nonceOrderedTxs, SORT_NONCE_CHECK_ORDER);

    for (TransactionForProcessing tfp : nonceOrderedTxs) {
      if (tfp.getWrapped().getTxType() == TxType.X_CHAIN_TX_PACKAGE) {
        xChainTxs.add(tfp);
      } else {
        regularTxs.add(tfp);
      }
    }

    if (checkForBadRegularNonces(stateSnapshot, regularTxs, validateMode)) {
      return true;
    }
    return checkForBadXChainNonces(stateSnapshot, xChainTxs, validateMode);
  }


  private boolean checkForBadRegularNonces(StateSnapshot stateSnapshot, List<TransactionForProcessing> transactions, boolean validateMode) {
    MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
    String currentAddress = null;
    MutableLong addressNonce = new MutableLong(0L);

    for (TransactionForProcessing tfp : transactions) {
      Txi txi = tfp.getWrapped();

      String address = txi.getNonceAddress();
      if (!address.equals(currentAddress)) {
        // new address, get starting nonce from state
        AddressEntry asset = assetBalances.find(address);
        if (asset != null) {
          addressNonce.set(asset.getNonce());
        } else {
          addressNonce.set(0L);
        }
        currentAddress = address;
      }

      if (checkForBadNonce(tfp, addressNonce, validateMode)) {
        return true;
      }
    }

    // Either nonces OK, or not in validate mode. No need to terminate processing
    return false;
  }


  /**
   * Validate the nonces on Cross Chain Transaction Package transactions, which have their own nonce system.
   *
   * @param stateSnapshot the current state
   * @param transactions  the cross chain transaction package transactions
   * @param validateMode  if true, terminate processing if a problem is detected
   *
   * @return true if processing should be terminated.
   */
  private boolean checkForBadXChainNonces(StateSnapshot stateSnapshot, List<TransactionForProcessing> transactions, boolean validateMode) {

    // Cross chain packages use their own nonce track, not the addresses.
    HashMap<Integer, MutableLong> xChainNonces = new HashMap<>();

    for (TransactionForProcessing tfp : transactions) {
      // Get the nonce from the X Chain details
      XChainTxPackageTx xChainTx = (XChainTxPackageTx) tfp.getWrapped();
      Integer chainId = xChainTx.getFromChainID();
      MutableLong expected = xChainNonces.computeIfAbsent(chainId, k -> {
        XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(k);
        return xChainDetails == null
            ? new MutableLong(-1)
            : new MutableLong(xChainDetails.getBlockHeight() + 1);
      });

      if (checkForBadNonce(tfp, expected, validateMode)) {
        return true;
      }
    }

    // Either nonces OK, or not in validate mode. No need to terminate processing
    return false;
  }


  /**
   * Check the nonce for the given address is the next that should be processed.
   *
   * @param xChainDetails cross chain details.
   * @param txNonce       The nonce.
   *
   * @return 0 if the nonce provided is the next that should be processed, otherwise false, 1 if a future nonce, and -1 if a replay nonce.
   */
  private int checkNonce(XChainDetails xChainDetails, long txNonce) {
    if (xChainDetails == null) {
      return -1;
    }

    //nonce is known height of cross chain, txNonce new state this change is here for sake of compatibility with python system
    long nonce = xChainDetails.getBlockHeight() + 1;
    return Long.compare(txNonce, nonce);
  }


  /**
   * Check if the given transaction should be processed.
   *
   * @param tx    The signature and hash checked transaction.
   * @param state The current state.
   *
   * @return true if the transaction should be processed, otherwise false.
   */
  @Override
  public boolean checkValidatedTransactionForPool(AbstractTx tx, State state) {
    //TODO Provide information to storage manager to allow prefetch of required data. This can be achieved by accessing any required merkle entries.
    AddressEntry asset = state.getAssetBalances().find(tx.getNonceAddress());
    long nonce = (asset != null) ? asset.getNonce() : -1L;
    return (tx.getNonce() >= nonce);
  }


  /**
   * Create a memo transaction that contain a summary of contract events and the addresses affected by those events.
   *
   * @param stateSnapshot the snapshot
   */
  private void createContractMemo(StateSnapshot stateSnapshot) {
    // Convert the enum map to a sorted map for state
    TreeMap<String, SortedSet<String>> sortedEvents = new TreeMap<>();
    Set<Entry<ContractLifeCycle, SortedSet<String>>> entrySet = stateSnapshot.getContractLifeCycleEvents().entrySet();
    for (Entry<ContractLifeCycle, SortedSet<String>> e : entrySet) {
      sortedEvents.put(e.getKey().name().toLowerCase(), e.getValue());
    }

    SortedMap<String, SortedSet<String>> users = stateSnapshot.getContractUsers();

    // Only add the effective TX if there was some contract activity.
    if (!(sortedEvents.isEmpty() && users.isEmpty())) {
      // Message pack the data. The first value is a version number.
      Object[] encoded = new Object[]{1, sortedEvents, users};
      Memo memo = new Memo();
      memo.setChainId(stateSnapshot.getChainId());
      memo.setNonce(0);
      memo.setAddress("");
      memo.setPublicKey("");
      memo.setTimestamp(stateSnapshot.getTimestamp());
      memo.setMetadata("Contract Events");
      memo.setPayload(MsgPackUtil.pack(encoded));
      MemoTx memoTx = memo.create();
      stateSnapshot.addEffectiveTX(memoTx);
    }
  }


  /**
   * postProcessTransactions.
   */
  @Override
  public boolean postProcessTransactions(StateSnapshot stateSnapshot, Block block, long updateTime) {

    // Process Contract time events (but only those specified in the block).
    MPWrappedArray blockTimeEvents = block.getTimeEvents();
    if ((blockTimeEvents != null) && !blockTimeEvents.isEmpty()) {
      for (int index = 0, l = blockTimeEvents.size(); index < l; index++) {
        String contractAddress = blockTimeEvents.asString(index);

        // Run contract event code
        ContractEntry contract = stateSnapshot.getContracts().find(contractAddress);

        if (contract != null) {
          EventData event = new EventData(contractAddress, contract.getFunction(), "time", updateTime);
          if (updateEvent(stateSnapshot, contractAddress, event, updateTime, false).success != SuccessType.PASS) {
            logger.warn("Post processing of time event failed : {}, {}, {}, {}, {}",
                event.getEventAddress(), event.getEventFunction(), event.getEventName(), event.getNumericValue(),
                event.getStringValue()
            );
            return false;
          }
        }
      } // For

    } // block time events

    // Process Contract Triggered Events events (Not Time)
    for (EventData event : stateSnapshot.getContractEvents()) {
      if (updateEvent(stateSnapshot, event.getEventAddress(), event, updateTime, false).success != SuccessType.PASS) {
        logger.warn("Post processing of event failed : {}, {}, {}, {}, {}",
            event.getEventAddress(), event.getEventFunction(), event.getEventName(), event.getNumericValue(),
            event.getStringValue()
        );
        return false;
      }
    }

    createContractMemo(stateSnapshot);
    block.setEffectiveTxList(stateSnapshot.getEffectiveTXList());
    return true;
  }


  /**
   * Process a single transaction.
   *
   * @param stateSnapshot the state snapshot
   * @param assetList     asset balances
   * @param tfp           the transaction to process
   * @param updateTime    the time of the block
   * @param validateMode  operation mode
   *
   * @return true if all OK
   */
  private boolean processTransaction(
      StateSnapshot stateSnapshot, MutableMerkle<AddressEntry> assetList, TransactionForProcessing tfp, long updateTime,
      MutableObject<ReturnTuple> result, boolean validateMode
  ) {
    Txi txo = tfp.getWrapped();

    boolean txValid;
    if (tfp.isFlawed()) {
      logger.warn("Rejecting flawed transaction {}:{}/{} of type {}", tfp.getNonceAddress(), tfp.getNonce(), tfp.getHash(), tfp.getTxType());
      txValid = false;
      result.set(new ReturnTuple(SuccessType.FAIL, String.format(
          "Flawed transaction %s:%d/%s of type %s", tfp.getNonceAddress(), tfp.getNonce(), tfp.getHash(), tfp.getTxType()
      )));
    } else {
      try {
        StateSnapshot txSnapshot = stateSnapshot.createSnapshot();
        ReturnTuple txResult = UpdateState.doUpdate(txo, txSnapshot, updateTime, tfp.getPriority(), false);
        txValid = txResult.success == SuccessType.PASS;
        result.set(txResult);
        txSnapshot.commitIfNotCorrupt();
      } catch (RuntimeException failure) {
        // We have to hope that whatever causes this failure will be the same on all nodes.
        txValid = false;
        result.set(new ReturnTuple(SuccessType.FAIL, failure.toString()));
        logger.error(
            "Internal error processing transaction {}:{}/{} of type {}",
            tfp.getNonceAddress(), tfp.getNonce(), tfp.getHash(), tfp.getTxType(), failure
        );
      }
    }

    if (validateMode) {
      // Validating, so a valid TX must be marked as good.
      if (txo.isGood() != txValid) {
        if (txValid) {
          logger.error("Transaction marked as invalid, but has been determined to be good : {}:{}", txo.getHash(), txo.getNonce());
          result.set(new ReturnTuple(SuccessType.FAIL, "Transaction marked as invalid, but has been determined to be good"));
        } else {
          logger.error("Transaction marked as valid, but has been determined to be bad : {}:{}", txo.getHash(), txo.getNonce());
          result.set(new ReturnTuple(SuccessType.FAIL, "Transaction marked as valid, but has been determined to be bad"));
        }

        // Abort processing
        return false;
      }
    } else {
      // Not validating, so mark TX as updated (good) or not
      txo.setUpdated(txValid);
      logger.debug("Transaction {}:{} marked as {}", txo.getHash(), txo.getNonce(), txValid ? "good" : "bad");
    }

    if (txo.getTxType() != TxType.X_CHAIN_TX_PACKAGE) {
      // Update address's nonce. Note we may invoke this out of nonce order, but that is OK.
      updateOrAddNonce(txo, assetList, txo.getPriority(), stateSnapshot.getVersion(), updateTime);

    } else {
      // Update chain's nonce
      XChainTxPackageTx xChainTx = (XChainTxPackageTx) txo;
      XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(xChainTx.getFromChainID());

      if ((xChainDetails != null) && (checkNonce(xChainDetails, xChainTx.getNonce()) == 0)) {

        if (!Long.valueOf(txo.getNonce()).equals(xChainDetails.getBlockHeight())) {
          xChainDetails = xChainDetails.setBlockHeight(txo.getNonce());
          stateSnapshot.setXChainSignNodesValue(xChainTx.getFromChainID(), xChainDetails);
        }
      }
    }

    // All OK if not corrupted, no need to abort processing
    return !stateSnapshot.isCorrupted();
  }


  /**
   * processTransactions() Apply a list of transactions (e.g. from a block) to a given state snapshot.
   *
   * @param stateSnapshot : State to apply changes to.
   * @param txiList       : List of Transactions
   * @param updateTime    : UpdateState Time (Block Time) relevant for certain TX Types (Contracts).
   * @param validateMode  : If true, validation mode is selected. Transactions marked as valid must be valid, and those marked as invalid must be invalid.
   *                      If false, transactions will be marked as valid or invalid depending on ability to be processed.
   *
   * @return : Success / Failure
   */
  public boolean processTransactions(StateSnapshot stateSnapshot, Txi[] txiList, long updateTime, boolean validateMode) {
    return processTransactions(stateSnapshot, TransactionForProcessing.wrap(txiList), null, updateTime, validateMode);
  }


  /**
   * processTransactions() Apply a list of transactions (e.g. from a block) to a given state snapshot.
   *
   * @param stateSnapshot : State to apply changes to.
   * @param transactions  : List of Transactions
   * @param updateTime    : UpdateState Time (Block Time) relevant for certain TX Types (Contracts).
   * @param validateMode  : If true, validation mode is selected. Transactions marked as valid must be valid, and those marked as invalid must be invalid.
   *                      If false, transactions will be marked as valid or invalid depending on ability to be processed.
   *
   * @return : Success / Failure
   */
  private boolean processTransactions(
      StateSnapshot stateSnapshot,
      TransactionForProcessing[] transactions,
      List<ReturnTuple> results,
      long updateTime,
      boolean validateMode
  ) {
    logger.info("Processing {} transactions : ", transactions.length);

    // Ensure sort order : The first one (given) must be in standard application order (Address, nonce, hash). Note that performing this sort is a side
    // effect of invoking this method which has been maintained for backwards compatibility. It is probably un-necessary as we expect the input to be in this
    // order anyway.
    Arrays.sort(transactions, SORT_BLOCK_ORDER);

    if (checkForBadNonces(stateSnapshot, transactions, validateMode)) {
      // Invalid nonce and validating, so failed
      return false;
    }

    // Filter out futures and replays, and sort into correct order
    List<TransactionForProcessing> priorityOrderTransactions = Stream.of(transactions).filter(not(TransactionForProcessing::isFlawed))
        .sorted(SORT_PROCESSING_ORDER).collect(Collectors.toList());

    MutableMerkle<AddressEntry> assetList = stateSnapshot.getAssetBalances();
    MutableObject<ReturnTuple> txResult = new MutableObject<>();
    for (TransactionForProcessing tfp : priorityOrderTransactions) {
      boolean isOk = processTransaction(stateSnapshot, assetList, tfp, updateTime, txResult, validateMode);
      if (results != null) {
        results.add(txResult.get());
      }
      if (!isOk) {
        // Invalid transaction and validating, so failed
        return false;
      }
    }

    // Success
    return true;
  }


  @Override
  public boolean processTransactions(
      int blockVersion, PriorityExecutor priorityExecutor, StateSnapshot stateSnapshot, ProposedTxList transactions,
      final long updateTime
  ) {
    // In theory we could support concurrent transaction processing here. This is a future enhancement.
    return processTransactions(stateSnapshot, transactions.getAllTx(), null, updateTime, false);
  }


  @Override
  public void removeProcessedTimeEvents(StateSnapshot snapshot, Block block, long updateTime) {

    MPWrappedArray blockTimeEvents = block.getTimeEvents();
    HashSet<String> addressSet = new HashSet<>();

    if ((blockTimeEvents != null) && !blockTimeEvents.isEmpty()) {
      for (int index = 0, l = blockTimeEvents.size(); index < l; index++) {
        addressSet.add(blockTimeEvents.asString(index));
      }

      snapshot.removePendingEventTimeAddresses(updateTime, addressSet);
    }
  }


  /**
   * Test the result of processing the transactions.
   *
   * @param stateSnapshot State to apply changes to.
   * @param txiList       List of Transactions
   * @param results       the result of processing each transaction
   * @param updateTime    UpdateState Time (Block Time) relevant for certain TX Types (Contracts).
   *
   * @return : Success / Failure
   */
  public boolean testTransactions(StateSnapshot stateSnapshot, Txi[] txiList, List<ReturnTuple> results, long updateTime) {
    return processTransactions(stateSnapshot, TransactionForProcessing.wrap(txiList), results, updateTime, false);
  }


  private void updateOrAddNonce(Txi txo, MutableMerkle<AddressEntry> assetList, int priority, int version, long updateTime) {
    String nonceAddress = txo.getNonceAddress();
    AddressEntry addressEntry = assetList.findAndMarkUpdated(nonceAddress);

    if (addressEntry == null) {
      // the address might not exists because a delete address transaction has just run.
      return;
    }

    if ((addressEntry.getUpdateTime() == null) && (version >= VERSION_SET_ADDRESS_TIME)) {
      addressEntry.setUpdateTime(updateTime);
    }

    // check a nonce exists
    if (addressEntry.isNonceUnset()) {
      addressEntry.setNonce(0L);
    }

    // Set the nonce. Note that the AddressEntry class will not allow a nonce to be lowered, so even if we call set in the wrong order it will end up with
    // the correct value.
    long newNonce = txo.getNonce() + 1;
    addressEntry.setNonce(newNonce);

    // set high or low priority nonces if appropriate
    if (priority < 0) {
      addressEntry.setHighPriorityNonce(newNonce);
    } else if (priority > 0) {
      addressEntry.setLowPriorityNonce(newNonce);
    }

    // Update Address time if address time is in use (not done always for backwards compatibility)
    if (addressEntry.getUpdateTime() != null) {
      addressEntry.setUpdateTime(updateTime);
    }
  }

}
