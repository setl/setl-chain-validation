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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashableObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.Pair;
import io.setl.util.PairSerializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Version history:
 * <p>1 : Historic</p>
 * <p>2 : Historic</p>
 * <p>2x : Historic, should have been version 3</p>
 * <p>3 : Version at go-live. Adds effective TX list</p>
 * <p>4 : Concurrent validation.</p>
 */
public class Block implements HashableObjectArray {

  public static final int CURRENT_VERSION = 4;

  private static final MPWrappedArray emptyObjectArray = new MPWrappedArrayImpl(new Object[0]);



  /**
   * Helper class which calculates hash of the cross chain information.
   */

  public class XChainHashableObject implements HashableObjectArray {

    @Override
    public Object[] getHashableObject() {
      return new Object[]{getChainId(), getVersion(), getHeight(), getBaseStateHash(), PairSerializer.serialize(xChainTxs), sigNodes.unwrap()};
    }
  }



  private final int chainId;

  private final Object effectiveTxLock = new Object();

  private final Object txLock = new Object();

  private final int version;

  private final HashableObjectArray xChainHashableObject = new XChainHashableObject();

  private Hash baseStateHash;

  private Hash blockHash;

  private MPWrappedArray contractEvents;

  private Txi[] effectiveTxArray = null;

  private MPWrappedArray effectiveTxList;

  private boolean extendedHack = false;

  private MPWrappedArray forFutureUse;

  private int height;

  private String nodeName;

  private Hash previousBlockHash;

  private MPWrappedArray sigList;

  private MPWrappedArray sigListXChain;

  private MPWrappedArray sigNodes;

  private MPWrappedArray timeEvents;

  private long timeStamp;

  /**
   * The TXs as represented in the txList.
   */
  private Txi[] txArray = null;

  /** The specification of the TXs. */
  private List<MPWrappedArray> txList;

  private List<Pair<String, Integer>> xChainTxs;

  private MPWrappedArray xcHeights;


  /**
   * Block.
   *
   * @param chainId           : Int,      Unique chain identifier.
   * @param height            : Int,      Block height, same as state height upon which it is built.
   * @param baseStateHash     : String,   State hash of state upon which this Block is built.
   * @param previousBlockHash : String,   Block hash of preceding block. Having this allows sequential block verification without building state.
   * @param txList            : Object[], List of TX details.
   * @param sigList           : Object[], List of signatures relating to this block. This may not be a complete set of signatures, but MUST contain at least a
   *                          threshold quantity.
   * @param sigListXChain     : Object[], List of signatures for xChain verification.
   * @param timeStamp         : long,     Time of block creation (UTC Seconds)
   * @param nodeName          : String,   Name of proposing node.
   * @param xChainTxs         : Pair(String, Integer)[],
   * @param signNodes         : Object[], List of signing nodes (signing power) distribution. Not definitive, but used by the witness nodes.
   * @param xcHeights         : Object[],
   * @param timeEvents        : Object[],
   * @param contractEvents    : Object[],
   * @param effectiveTxList   : Object[], List of 'effective' transactions executed by contracts in this block.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public Block(
      int chainId, int height, Hash baseStateHash, Hash previousBlockHash, Object[] txList, Object[] sigList, Object[] sigListXChain,
      long timeStamp, String nodeName, Pair<String, Integer>[] xChainTxs, Object[] signNodes, Object[] xcHeights, Object[] timeEvents, Object[] contractEvents,
      Object[] effectiveTxList
  ) {

    this.chainId = chainId;
    this.version = 4;
    this.height = height;
    this.baseStateHash = baseStateHash;
    this.previousBlockHash = previousBlockHash;
    this.setTxList(Arrays.stream(txList).map(o -> new MPWrappedArrayImpl((Object[]) o)).collect(Collectors.toCollection(ArrayList::new)));
    this.sigList = new MPWrappedArrayImpl(sigList);
    this.sigListXChain = new MPWrappedArrayImpl(sigListXChain);
    this.timeStamp = timeStamp;
    this.nodeName = nodeName;
    this.xChainTxs = xChainTxs != null ? Arrays.asList(xChainTxs.clone()) : null;
    this.sigNodes = new MPWrappedArrayImpl(signNodes);
    this.xcHeights = new MPWrappedArrayImpl(xcHeights);
    this.timeEvents = new MPWrappedArrayImpl(timeEvents);
    this.contractEvents = contractEvents == null ? emptyObjectArray : new MPWrappedArrayImpl(contractEvents);
    this.effectiveTxList = effectiveTxList == null ? emptyObjectArray : new MPWrappedArrayImpl(effectiveTxList);
    this.forFutureUse = emptyObjectArray;
    extendedHack = true;
  }


  /**
   * Block.
   *
   * @param chainId           : Int,      Unique chain identifier.
   * @param height            : Int,      Block height, same as state height upon which it is built.
   * @param baseStateHash     : String,   State hash of state upon which this Block is built.
   * @param previousBlockHash : String,   Block hash of preceding block. Having this allows sequential block verification without building state.
   * @param encodedTxList     : List,     List of TX details.
   * @param timeStamp         : long,     Time of block creation (UTC Seconds)
   * @param nodeName          : String,   Name of proposing node.
   * @param xChainTxs         : Pair(String, Integer)[],
   * @param timeEvents        : Object[],
   * @param contractEvents    : Object[],
   * @param effectiveTxList   : Object[], List of 'effective' transactions executed by contracts in this block.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public Block(
      int chainId, int height, Hash baseStateHash, Hash previousBlockHash, List<MPWrappedArray> encodedTxList,
      long timeStamp, String nodeName, List<Pair<String, Integer>> xChainTxs, Object[] timeEvents, Object[] contractEvents,
      Object[] effectiveTxList
  ) {

    this.chainId = chainId;
    this.version = 4;
    this.height = height;
    this.baseStateHash = baseStateHash;
    this.previousBlockHash = previousBlockHash;
    this.txList = encodedTxList;
    this.timeStamp = timeStamp;
    this.nodeName = nodeName;
    this.xChainTxs = xChainTxs;
    this.sigNodes = new MPWrappedArrayImpl(new Object[0]);
    this.xcHeights = new MPWrappedArrayImpl(new Object[0]);
    this.timeEvents = new MPWrappedArrayImpl(timeEvents);
    this.contractEvents = contractEvents == null ? emptyObjectArray : new MPWrappedArrayImpl(contractEvents);
    this.effectiveTxList = effectiveTxList == null ? emptyObjectArray : new MPWrappedArrayImpl(effectiveTxList);
    this.forFutureUse = emptyObjectArray;

    // These will be set properly later
    this.sigList = emptyObjectArray;
    this.sigListXChain = emptyObjectArray;

    extendedHack = true;
  }


  /**
   * Construct block from message pack wrapper array.
   *
   * @param blockDataArray : Data
   */
  public Block(MPWrappedArray blockDataArray) {

    chainId = blockDataArray.asInt(0);
    version = blockDataArray.asInt(1);
    // TODO:A nasty hack. Seems version number was not updated!!
    if (blockDataArray.size() == 17) {
      extendedHack = true;
    }
    if (version > 3 || extendedHack) {
      assert (blockDataArray.size() == 17);
    } else {
      assert (blockDataArray.size() == 15);
    }
    height = blockDataArray.asInt(2);
    baseStateHash = Hash.fromHex(blockDataArray.asString(3));
    previousBlockHash = Hash.fromHex(blockDataArray.asString(4));
    setTxList(blockDataArray.asWrapped(5).stream().map(o -> new MPWrappedArrayImpl((Object[]) o)).collect(Collectors.toCollection(ArrayList::new)));
    setSigList(blockDataArray.asWrapped(6));
    sigListXChain = blockDataArray.asWrapped(7);
    timeStamp = blockDataArray.asInt(8);
    nodeName = blockDataArray.asString(9);

    // Python chain defaults to "" instead of empty/null.
    Object tmp = blockDataArray.get(10);
    if (tmp == null || "".equals(tmp)) {
      xChainTxs = null;
    } else {
      Object[] array = blockDataArray.asObjectArray(10);
      xChainTxs = Stream.of(array).map(o -> (Object[]) o).map(o -> new Pair<>(o[0].toString(), ((Number) o[1]).intValue())).collect(Collectors.toList());
    }
    sigNodes = blockDataArray.asWrapped(11);
    xcHeights = blockDataArray.asWrapped(12);
    timeEvents = blockDataArray.asWrapped(13);
    if (timeEvents == null) {
      timeEvents = new MPWrappedArrayImpl(emptyObjectArray);
    }

    contractEvents = blockDataArray.asWrapped(14);
    if (contractEvents == null) {
      contractEvents = new MPWrappedArrayImpl(emptyObjectArray);
    }

    if (version > 3 || extendedHack) {
      effectiveTxList = blockDataArray.asWrapped(15);
      forFutureUse = blockDataArray.asWrapped(16);
    }
  }


  /**
   * encode : Returns Block data as list.
   *
   * @return : returns array which represents block data.
   */
  public Object[] encode() {
    // TODO: This only partially encodes to object - some are left as mp-wrapped!!

    return new Object[]{
        chainId, version, height, baseStateHash.toHexString(), previousBlockHash.toHexString(),
        getTxList(), getSigList(), sigListXChain, timeStamp, nodeName, PairSerializer.serialize(xChainTxs), sigNodes,
        xcHeights, timeEvents, contractEvents, effectiveTxList, forFutureUse
    };
  }


  /**
   * Gets hash of base state.
   *
   * @return hash of state
   */
  public Hash getBaseStateHash() {
    return baseStateHash;
  }


  /**
   * Get hash of current block.
   *
   * @return block hash
   */
  public Hash getBlockHash() {
    return blockHash;
  }


  /**
   * Gets chain ID.
   *
   * @return chain id
   */
  public int getChainId() {
    return chainId;
  }


  /**
   * Returns Message pack array which contains time events triggered by contracts.
   *
   * @return contract events
   */
  public MPWrappedArray getContractEvents() {
    return contractEvents;
  }


  /**
   * Get effective transactions.
   *
   * @return : List of effective transactions in this Block.
   */
  public Txi[] getEffectiveTransactions() {
    synchronized (effectiveTxLock) {
      if (effectiveTxArray != null) {
        return effectiveTxArray.clone();
      }

      effectiveTxArray = new Txi[effectiveTxList.size()];

      for (int i = 0, l = effectiveTxList.size(); i < l; i++) {
        MPWrappedArray vat = effectiveTxList.asWrapped(i);
        effectiveTxArray[i] = TxFromList.txFromList(vat);
      }

      return effectiveTxArray.clone();
    }
  }


  /**
   * Get list of fields used to calculate hash.
   *
   * @return hashable object
   */
  public Object[] getHashableObject() {
    Object[][] hashList = new Object[txList.size()][];
    for (int txIndex = 0, l = txList.size(); txIndex < l; txIndex++) {
      MPWrappedArray wrappedTx = txList.get(txIndex);
      hashList[txIndex] = new Object[]{wrappedTx.get(3), wrappedTx.get(5)};
    }

    Object[] oo;
    if (version > 3 || extendedHack) {
      oo = new Object[]{
          this.chainId, this.version, this.timeStamp, this.height,
          this.baseStateHash.toHexString(), hashList, this.xcHeights, this.timeEvents, this.contractEvents,
          forFutureUse, forFutureUse
      };
    } else {
      oo = new Object[]{
          this.chainId, this.version, this.timeStamp, this.height,
          this.baseStateHash, hashList, this.xcHeights, this.timeEvents, this.contractEvents
      };
    }
    return oo;
  }


  /**
   * Return block height.
   *
   * @return height
   */
  public int getHeight() {
    return height;
  }


  /**
   * Return block identifier which is combined chain id and height.
   *
   * @return Block id
   */
  public long getId() {
    // The combination of chain ID and height will be unique.
    return (((long) chainId) << 32) | height;
  }


  public String getNodeName() {
    return nodeName;
  }


  /**
   * Returns hash of previous block.
   *
   * @return previous block hash
   */
  public Hash getPreviousBlockHash() {
    return previousBlockHash;
  }


  /**
   * Returns message pack wrapped array which elements holds block signatures and verification keys.
   *
   * @return list of signatures
   */
  public MPWrappedArray getSigList() {
    return sigList;
  }


  /**
   * Returns message pack wrapped array which elements signatures of cross chain data together with  verification keys
   *
   * @return List of signatures.
   */
  public MPWrappedArray getSigListXChain() {
    return sigListXChain;
  }


  /**
   * Returns list of signing nodes (signing power) distribution.
   *
   * @return List of signing nodes
   */
  public MPWrappedArray getSigNodes() {
    return sigNodes;
  }


  /**
   * Gets list of time events which should take place at time slot represented by this block.
   *
   * @return tie events
   */
  public MPWrappedArray getTimeEvents() {
    return timeEvents;
  }


  /**
   * Returns timestamp when block was created.
   *
   * @return timestamp as linux epoch time
   */
  public long getTimeStamp() {
    return timeStamp;
  }


  /**
   * Get the number of transactions in this block.
   *
   * @return the number of transactions
   */
  public int getTransactionCount() {
    synchronized (txLock) {
      if (txList == null) {
        return txArray == null ? 0 : txArray.length;
      }
      return txList.size();
    }
  }


  /**
   * Returns all valid transactions for given block.
   * Note that some of transactions might have valid signature, but rejected because ie insufficient balance.
   * As nonce counter for addresses which issued that kind of transactions will increase, those transactions will be recorded in the block.
   *
   * @return : List of transactions in this Block.
   */
  public Txi[] getTransactions() {
    synchronized (txLock) {
      if (txArray != null) {
        return txArray.clone();
      }

      List<MPWrappedArray> myTxList = getTxList();
      txArray = new Txi[myTxList.size()];

      for (int i = 0, l = myTxList.size(); i < l; i++) {
        MPWrappedArray vat = myTxList.get(i);
        Txi txi = TxFromList.txFromList(vat);
        txArray[i] = txi;
      }

      return txArray.clone();
    }
  }


  /**
   * Get the hashes of the transactions in this hash.
   *
   * @return the transactions' hashes
   */
  public Set<String> getTxHashes() {
    Set<String> hashes = new HashSet<>();
    synchronized (txLock) {
      if (txList != null) {
        for (MPWrappedArray tx : txList) {
          hashes.add(tx.asString(TxGeneralFields.TX_HASH));
        }
      } else {
        // Use tx array
        for (Txi tx : txArray) {
          hashes.add(tx.getHash());
        }
      }
    }
    return hashes;
  }


  /**
   * Get the list of transactions in this block in encoded form.
   *
   * @return the encoded form
   */
  public List<MPWrappedArray> getTxList() {
    synchronized (txLock) {
      // If this block was dehydrated, we may not have this.
      if (txList == null) {
        int l = txArray.length;
        ArrayList<MPWrappedArray> encoded = new ArrayList<>(l);
        for (Txi tx : txArray) {
          if (tx != null) {
            encoded.add(new MPWrappedArrayImpl(tx.encodeTx()));
          }
        }
        txList = encoded;
      }
      return txList;
    }
  }


  /**
   * Converts block to a transaction package, which contains all transactions from that block.
   *
   * @return transaction package
   */
  public TxPackage getTxPackage() {
    return new TxPackage(P2PType.TX_PACKAGE_ORIGINAL, getChainId(), getTxList());
  }


  /**
   * Returns block version.
   *
   * @return version
   */
  public int getVersion() {
    return version;
  }


  /**
   * Returns array that contains list of fields required to calculate cross-chain data hash.
   *
   * @return Hashable object array
   */
  public HashableObjectArray getXChainHashableObject() {
    return xChainHashableObject;
  }


  /**
   * Returns information about height of registered cross chains.
   *
   * @return cross-chain heights
   */
  public MPWrappedArray getXChainHeights() {
    return xcHeights;
  }


  public List<Pair<String, Integer>> getXChainTxs() {
    return xChainTxs != null ? Collections.unmodifiableList(xChainTxs) : null;
  }


  /**
   * Sets base state hash.
   *
   * @param hash the base state hash of this block
   */
  public void setBaseStateHash(Hash hash) {
    baseStateHash = hash;
  }


  /**
   * Sets block hash.
   *
   * @param hash the hash of this block
   */
  public void setBlockHash(Hash hash) {
    blockHash = hash;
  }


  /**
   * Sets block hash.
   */
  public void setBlockHash() {
    blockHash = new BlockVerifier().computeHash(this);
  }


  /**
   * Set the list of effective transactions that resulted from the actual transactions in this block.
   *
   * @param effectiveTxList the effective transactions
   */
  public void setEffectiveTxList(List<? extends Txi> effectiveTxList) {
    // Convert list to array
    int length = effectiveTxList.size();
    Txi[] newEffectiveArray = new Txi[length];
    newEffectiveArray = effectiveTxList.toArray(newEffectiveArray);

    // Convert the TXs to their encoded form.
    Object[] newEncodedTx = new Object[length];
    for (int i = 0; i < length; i++) {
      newEncodedTx[i] = newEffectiveArray[i].encodeTx();
    }

    synchronized (effectiveTxLock) {
      this.effectiveTxList = new MPWrappedArrayImpl(newEncodedTx);
      this.effectiveTxArray = newEffectiveArray;
    }
  }


  /**
   * Sets list of signatures related to this block.
   *
   * @param sigList the signatures which signed this block
   */
  public void setSigList(MPWrappedArray sigList) {
    this.sigList = sigList;
  }


  /**
   * Sets list of cross-chain signatures.
   *
   * @param sigListXChain the cross-chain signatures for this block.
   */
  public void setSigListXChain(MPWrappedArray sigListXChain) {
    this.sigListXChain = sigListXChain;
  }


  /**
   * Sets transaction list.
   *
   * @param txList the list of transactions in this block
   */
  public void setTxList(List<MPWrappedArray> txList) {
    this.txList = txList;
  }
}
