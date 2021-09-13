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
package io.setl.bc.pychain.state.tx;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.common.Pair;
import io.setl.util.PairSerializer;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XChainTxPackageTx extends AbstractTx {

  public static final TxType TXT = TxType.X_CHAIN_TX_PACKAGE;

  private static final Logger logger = LoggerFactory.getLogger(XChainTxPackageTx.class);


  /**
   * Accept an Object[] and return a AssetIssueTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned XChainTxPackageTx object
   */
  public static XChainTxPackageTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a XChainTxPackageTx.
   *
   * @param dataArray : The input MPWrappedArray object
   *
   * @return : The constructed XChainTxPackageTx object
   */
  public static XChainTxPackageTx decodeTX(MPWrappedArray dataArray) {
    int toChainId = dataArray.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(dataArray.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
      return null;
    }
    String hash = dataArray.asString(TxGeneralFields.TX_HASH);
    int blockheight = dataArray.asInt(TxGeneralFields.TX_NONCE);
    boolean updated = dataArray.asBoolean(TxGeneralFields.TX_UPDATED);
    long timestamp = dataArray.asLong(8);
    int fromChainId = dataArray.asInt(10);
    String blockVersion = dataArray.asString(11);
    String baseHash = dataArray.asString(12);
    Object[] signodes = dataArray.asObjectArray(13);
    Pair<String, Integer>[] hashList = Arrays.stream(dataArray.asObjectArray(14)).map(t -> (Object[]) t)
        .map(p -> new Pair<>(p[0].toString(), ((Number) p[1]).intValue())).collect(Collectors.toList()).toArray(new Pair[dataArray.asObjectArray(14).length]);
    Object[] xChainSigs = dataArray.asObjectArray(15);
    Object[] txList = dataArray.asObjectArray(16);
    Long blockTime = dataArray.asLong(17);
    int height = dataArray.asInt(18);
    return new XChainTxPackageTx(
        toChainId,
        hash,
        updated,
        blockVersion,
        blockheight,
        fromChainId,
        baseHash,
        signodes,
        hashList,
        xChainSigs,
        txList,
        blockTime,
        height,
        timestamp
    );
  }


  private String basehash;

  private int blockHeight = -1;

  private Long blockTimestamp;

  private int fromChainID = -1;

  private Object[] signodes; //     not Used.

  private Pair<String, Integer>[] txHashList; //   [Hash, ...] List of hashes that should be in the txList.

  private Object[] txList; //       [[Tx Data], ...]

  private String version = "";

  //                                Could just take this from the txList, recalculate the Package hash and the package signatures.
  private Object[] xChainSigs;  //  [[SigStr, PubKey, "XC", ], ...]


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public XChainTxPackageTx(XChainTxPackageTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.height = toCopy.getHeight();
    this.version = toCopy.getVersion();
    this.blockHeight = toCopy.getBlockHeight();
    this.fromChainID = toCopy.getFromChainID();
    this.basehash = toCopy.getBasehash();
    this.signodes = toCopy.getSignodes(); // Not Used, clone should be OK.
    this.txHashList = (txHashList != null ? txHashList.clone() : new Pair[0]);  // Shallow Copy OK.
    this.blockTimestamp = toCopy.getBlockTimestamp();
    Object[] tempItem = toCopy.getXChainSigs();
    if (tempItem != null) {
      xChainSigs = new Object[tempItem.length];
      for (int index = 0, l = tempItem.length; index < l; index++) {
        xChainSigs[index] = (tempItem[index] == null ? null : ((Object[]) tempItem[index]).clone());
      }
    } else {
      xChainSigs = new Object[0];
    }
    tempItem = toCopy.getTxList();
    if (tempItem != null) {
      txList = new Object[tempItem.length];
      for (int index = 0, l = tempItem.length; index < l; index++) {
        txList[index] = (tempItem[index] == null ? null : ((Object[]) tempItem[index]).clone());
      }
    } else {
      txList = new Object[0];
    }
  }


  /**
   * Constructor.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public XChainTxPackageTx(
      int toChainId,
      String hash,
      boolean updated,
      String version,
      int blockHeight,
      int fromChainID,
      String basehash,
      Object[] signodes,
      //     not Used.
      Pair[] txHashList, //   [ <Hash, chainId > , ...] List of hashes that should be in the txList.
      //                        Could just take this from the txList, recalculate the Package hash and the package signatures.
      Object[] xChainSigs,  //  [[SigStr, PubKey, "XC", ], ...]
      Object[] txList, //       [[Tx Data], ...]
      Long blockTimestamp,
      int height,             //     not Used.
      long timestamp
  ) {
    //  ensure superclass constructor is invoked
    //
    super(
        toChainId,
        hash, blockHeight,
        updated, "", // fromAddress,
        "", // fromPubKey,
        "", // signature,
        "", // poa
        timestamp
    );
    this.height = height;
    this.version = version;
    this.blockHeight = blockHeight;
    this.fromChainID = fromChainID;
    this.basehash = basehash;
    this.signodes = (signodes != null ? signodes.clone() : null); // Not Used
    this.txHashList = (txHashList != null ? txHashList.clone() : new Pair[0]);
    this.xChainSigs = (xChainSigs != null ? xChainSigs.clone() : new Object[0]); // Yes, I know, a shallow copy.
    this.txList = (txList != null ? txList.clone() : new Object[0]); // Yes, I know, a shallow copy.
    this.blockTimestamp = blockTimestamp;
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {
    Set<String> rVal = new TreeSet<>();
    for (Object thisList : this.txList) {
      rVal.addAll((TxFromList.txFromList(new MPWrappedArrayImpl((Object[]) thisList))).addresses());
    }
    return rVal;
  }


  @Override
  @SuppressWarnings(value = "unchecked")
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.add(chainId);
    hashList.add(TXT.getId());
    hashList.add(nonce);
    hashList.add(powerOfAttorney);
    hashList.add(fromChainID);
    hashList.add(basehash);
    hashList.add(signodes);
    hashList.add(PairSerializer.serialize(txHashList));
    hashList.add(txList);
    return hashList;
  }


  /**
   * Return this transation as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {
    XChainTxPackageTx tx = this;
    return new Object[]{
        tx.chainId,
        tx.int1,
        TXT.getId(),
        tx.hash,
        tx.nonce,
        tx.updated,
        tx.fromPubKey,
        tx.fromAddress,
        tx.timestamp,
        tx.powerOfAttorney,
        tx.fromChainID,
        tx.version,
        tx.basehash,
        tx.signodes,
        PairSerializer.serialize(tx.txHashList),
        tx.xChainSigs,
        tx.txList,
        tx.blockTimestamp,
        -1
    };
  }


  public String getBasehash() {
    return basehash;
  }


  public int getBlockHeight() {
    return blockHeight;
  }


  public Long getBlockTimestamp() {
    return blockTimestamp;
  }


  /**
   * Cross chain TX packages do not have a from address.
   *
   * @return the empty string, always
   */
  @Override
  public String getFromAddress() {
    // The from-address is explicitly an empty string, and cannot be set to any other value. Have an empty string as the from address could be used to
    // identify a Cross-chain TX package.
    return "";
  }


  public int getFromChainID() {
    return fromChainID;
  }


  /**
   * Cross chain TX packages do not have a nonce address.
   *
   * @return the empty string, always
   */
  @Override
  public String getNonceAddress() {
    // The nonce address is explicitly an empty string and cannot be set to any other value. Having the nonce address as an empty string indicates the nonces
    // come from a source other than the address - in this case it is the block height of the remote chain. Do not change the nonce address to an empty
    // string without checking for code that looks for such an case.
    return "";
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  /**
   * Get the definition of the Sig Nodes.
   *
   * @return the definition
   */
  public Object[] getSignodes() {
    return (signodes == null ? null : signodes.clone());
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  public Pair<String, Integer>[] getTxHashList() {
    return txHashList;
  }


  public Object[] getTxList() {
    return txList == null ? null : txList.clone();
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  public String getVersion() {
    return version;
  }


  public Object[] getXChainSigs() {
    return (xChainSigs == null) ? null : xChainSigs.clone();
  }
}
