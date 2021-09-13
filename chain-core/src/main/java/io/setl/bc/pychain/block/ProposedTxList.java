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
package io.setl.bc.pychain.block;

import static io.setl.common.CommonPy.XChainTypes.XChainTxAllToList;
import static io.setl.common.CommonPy.XChainTypes.XChainTxAssetTypes;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.common.CommonPy.TxType;
import io.setl.common.MutableLong;
import io.setl.common.Pair;
import io.setl.util.ParallelTask;
import io.setl.util.PriorityExecutor.TaskContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A list of transactions that will constitute the next block.
 *
 * @author Simon Greatrix on 2019-04-29.
 */
public class ProposedTxList {

  private static Logger logger = LoggerFactory.getLogger(ProposedTxList.class);



  public static class BlockTx {

    private final List<MPWrappedArray> encodedTx;

    private final List<Pair<String, Integer>> xChainTx;


    BlockTx(List<MPWrappedArray> encodedTx, List<Pair<String, Integer>> xChainTx) {
      this.encodedTx = encodedTx;
      this.xChainTx = xChainTx;
    }


    public List<MPWrappedArray> getEncodedTx() {
      return encodedTx;
    }


    public List<Pair<String, Integer>> getXChainTx() {
      return xChainTx;
    }
  }



  public static class Builder {

    final ProposedTxList list = new ProposedTxList();


    public void add(Txi txi) {
      TxList txList = list.transactions.computeIfAbsent(txi.getNonceAddress(), t -> new TxList(txi));
      txList.txis.add(new TransactionForProcessing(txi));
    }


    public ProposedTxList build() {
      return list;
    }
  }



  static class TxList {

    final String address;

    final long firstNonce;

    final List<TransactionForProcessing> txis;


    TxList(Txi txi) {
      address = txi.getNonceAddress();
      firstNonce = txi.getNonce();
      txis = new ArrayList<>();
    }


    TxList(String address, long firstNonce, List<Txi> txis) {
      this.address = address;
      this.firstNonce = firstNonce;
      this.txis = new ArrayList<>(txis.size());
      for (Txi txi : txis) {
        if (txi != null) {
          this.txis.add(new TransactionForProcessing(txi));
        } else {
          this.txis.add(null);
        }
      }
    }


    synchronized void add(Txi txi) {
      if (address.equals("")) {
        txis.add(new TransactionForProcessing(txi));
        return;
      }
      long nonce = txi.getNonce();
      txis.set((int) (nonce - firstNonce), new TransactionForProcessing(txi));
    }
  }


  /**
   * Create a builder for a proposed TX list.
   *
   * @return the builder
   */
  public static Builder build() {
    return new Builder();
  }


  /**
   * Check a nonce is not too old, not too new, but just right. Log appropriately and signal processing abort if validating.
   *
   * @param tfp      the transaction
   * @param expected the expected nonce
   *
   * @return true if processing should abort
   */
  private static boolean checkForBadNonce(TransactionForProcessing tfp, long expected) {
    switch (Long.compare(tfp.getNonce(), expected)) {
      case -1:
        tfp.rejectNonceInPast();  // Will fail proposal
        logger.error("Failed to process transaction - replay tx {}:{} expected nonce {}", tfp.getHash(), tfp.getNonce(), expected);
        return true;
      case 1:
        tfp.rejectNonceInFuture();  // Will fail proposal
        logger.error("Failed to process transaction - future tx {}:{} expected nonce {}", tfp.getHash(), tfp.getNonce(), expected);
        return true;
      default:
        // Nonce OK.
        return false;
    }
  }


  private static boolean checkForBadRegularNonces(TaskContext taskContext, AddressEntry addressEntry, TxList txList) {
    final long firstNonce;
    if (addressEntry != null) {
      firstNonce = addressEntry.getNonce();
    } else {
      firstNonce = 0;
    }
    List<TransactionForProcessing> tfps = txList.txis;
    ParallelTask.process(taskContext, tfps.size(), index -> !checkForBadNonce(tfps.get(index), firstNonce + index));

    // All OK
    return false;
  }


  private static boolean checkForBadXChainNonces(StateSnapshot snapshot, TxList txList) {
    // Cross chain packages use their own nonce track, not the addresses.
    HashMap<Integer, MutableLong> xChainNonces = new HashMap<>();

    for (TransactionForProcessing tfp : txList.txis) {
      // Get the nonce from the X Chain details
      XChainTxPackageTx xChainTx = (XChainTxPackageTx) tfp.getWrapped();
      Integer chainId = xChainTx.getFromChainID();
      MutableLong expected = xChainNonces.computeIfAbsent(chainId, k -> {
        XChainDetails xChainDetails = snapshot.getXChainSignNodesValue(k);
        return xChainDetails == null
            ? new MutableLong(-1)
            : new MutableLong(xChainDetails.getBlockHeight() + 1);
      });

      if (checkForBadNonce(tfp, expected.longValue())) {
        return true;
      }
      expected.increment();
    }

    // Nonces OK. No need to terminate processing
    return false;
  }


  final Map<String, TxList> transactions = new HashMap<>();

  private String[] nonceAddresses;


  public ProposedTxIds asIdList() {
    return new ProposedTxIds(this);
  }


  /**
   * Verify the nonces are correct.
   *
   * @param taskContext an executor context
   * @param snapshot    the snapshot indicating state prior to applying the transactions
   *
   * @return true if any nonce is bad
   */
  public boolean checkForBadNonces(TaskContext taskContext, StateSnapshot snapshot) {
    String[] addresses = getAddresses();
    MutableMerkle<AddressEntry> assetBalances = snapshot.getAssetBalances();
    AtomicBoolean hasBad = new AtomicBoolean(false);
    ParallelTask.process(taskContext, 1, addresses.length, index -> {
      if (hasBad.get()) {
        return;
      }

      String address = addresses[index];
      TxList txList = transactions.get(address);
      boolean anyBad;
      if (address.equals("")) {
        anyBad = checkForBadXChainNonces(snapshot, txList);
      } else {
        AddressEntry addressEntry = assetBalances.find(address);
        anyBad = checkForBadRegularNonces(taskContext, addressEntry, txList);
      }
      if (anyBad) {
        hasBad.set(true);
      }
    });

    return hasBad.get();
  }


  private String[] getAddresses() {
    if (nonceAddresses == null) {
      nonceAddresses = transactions.keySet().toArray(new String[0]);
      Arrays.sort(nonceAddresses);
    }
    return nonceAddresses;
  }


  /**
   * Get all the transactions in this list.
   *
   * @return all the transactions
   */
  public TransactionForProcessing[] getAllTx() {
    ArrayList<TransactionForProcessing> allTXs = new ArrayList<>();
    String[] addresses = getAddresses();
    for (String address : addresses) {
      TxList list = transactions.get(address);
      allTXs.addAll(list.txis);
    }
    return allTXs.toArray(new TransactionForProcessing[0]);
  }


  /**
   * Create a list of encoded TXs in block order with the height set as specified.
   *
   * @param getXC  if true, extract the cross chain TXs
   * @param height the TX height
   *
   * @return the encoded TXs
   */
  public BlockTx getBlockTransactions(TaskContext context, boolean getXC, int chainId, int height) {
    // Build list of transactions in nonce address, nonce order.
    List<TransactionForProcessing> allTXs = new ArrayList<>();
    String[] addresses = getAddresses();
    for (String address : addresses) {
      TxList list = transactions.get(address);
      allTXs.addAll(list.txis);
    }

    MPWrappedArray[] encoded = new MPWrappedArrayImpl[allTXs.size()];
    boolean[] isXC = new boolean[encoded.length];
    AtomicInteger xcCount = new AtomicInteger(0);

    // Encode transactions, set height and tag XC transactions.
    ParallelTask.process(context, encoded.length, i -> {
      TransactionForProcessing tfp = allTXs.get(i);
      Txi txi = tfp.getWrapped();
      txi.setHeight(height);
      encoded[i] = new MPWrappedArrayImpl(txi.encodeTx());

      TxType txType = txi.getTxType();
      if (getXC && XChainTxAllToList.contains(txType) && !(XChainTxAssetTypes.contains(txType) && txi.getToChainId() == chainId)) {
        isXC[i] = true;
        xcCount.incrementAndGet();
      }
    });

    // Create the pairs for the XC transactions
    int xcMax = xcCount.get();
    List<Pair<String, Integer>> xcTXs = new ArrayList<>(xcMax);
    int i = 0;
    int xcIndex = 0;
    while (xcIndex < xcMax) {
      if (isXC[i]) {
        xcIndex++;
        Txi txi = allTXs.get(i).getWrapped();
        xcTXs.add(new Pair<>(txi.getHash(), txi.getToChainId()));
      }
      i++;
    }

    return new BlockTx(Arrays.asList(encoded), xcTXs);
  }


  /**
   * Is this list empty? Empty includes having only cross chain packages which are themselves empty.
   *
   * @return true if this is empty
   */
  public boolean isEmpty() {
    if (transactions.isEmpty()) {
      return true;
    }

    // Can still be empty if all we have are empty X-Chain transaction packages
    if (transactions.size() != 1 || !transactions.containsKey("")) {
      return false;
    }
    TxList xChains = transactions.get("");
    for (TransactionForProcessing txi : xChains.txis) {
      if (txi.getTxType() != TxType.X_CHAIN_TX_PACKAGE) {
        return false;
      }
      if (((XChainTxPackageTx) txi.getWrapped()).getTxHashList().length != 0) {
        return false;
      }
    }
    return true;
  }


  /**
   * How many transactions are in this list?.
   *
   * @return the number of transactions
   */
  public int size() {
    int s = 0;
    for (TxList txList : transactions.values()) {
      s += txList.txis.size();
    }
    return s;
  }
}
