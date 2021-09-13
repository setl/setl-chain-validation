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

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.tx.helper.HasClassId;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;

/**
 * Basic StockSplit TX.
 * <p>NOT TO BE USED EXCEPT FOR DEMONSTRATION.</p>
 */
public class StockSplitTx extends AbstractTx implements HasNamespace, HasClassId {

  public static final TxType TXT = TxType.SPLIT_STOCK;

  private static final Logger logger = LoggerFactory.getLogger(StockSplitTx.class);


  public static StockSplitTx decodeTX(Object[] dataArray) {
    return decodeTX(new MPWrappedArrayImpl(dataArray));
  }


  /**
   * Accept an MPWrappedArray object and return it as a MemoTx.
   *
   * @param dataArray : The input MPWrappedArray object
   *
   * @return : The returned MemoTx object
   */
  public static StockSplitTx decodeTX(MPWrappedArray dataArray) {

    int chainId = dataArray.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = dataArray.asInt(1);
    TxType txt = TxType.get(dataArray.asInt(TxGeneralFields.TX_TXTYPE));

    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      // /NAMESPACE_REGISTER
      // ASSET_CLASS_REGISTER
      // ASSET_ISSUE

      return null;
    }

    String hash = dataArray.asString(TxGeneralFields.TX_HASH);
    long nonce = dataArray.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = dataArray.asBoolean(TxGeneralFields.TX_UPDATED);

    String fromPubKey = dataArray.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = dataArray.asString(TxGeneralFields.TX_FROM_ADDR);

    long timestamp = dataArray.asLong(8);
    String poa = dataArray.asString(9);

    String namespace = dataArray.asString(10);
    String classId = dataArray.asString(11);
    double ratio = dataArray.asDouble(12);
    String referenceStateHash = dataArray.asString(13);

    String signature = dataArray.asString(14);
    String metadata = dataArray.asString(15);

    int height = -1;

    if (dataArray.size() > 16) {
      height = dataArray.asInt(16);
    }

    return new StockSplitTx(
        chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        namespace,
        classId,
        referenceStateHash,
        ratio,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  private final String classId;

  private final String metadata;

  private final String namespace;

  private final double ratio;

  private final String referenceStateHash;


  /**
   * Constructor.
   *
   * @param chainId            : Txn ID on the blockchain
   * @param int1               : Txn int1 value
   * @param hash               : Txn hash
   * @param nonce              : Txn nonce
   * @param updated            : Txn updated
   * @param fromPubKey         : Txn from public key
   * @param fromAddress        : Txn from address
   * @param namespace          : Txn namespace
   * @param classId            : Txn class ID
   * @param referenceStateHash : Txn string property
   * @param ratio              : Txn ration
   * @param metadata           : Txn metadata
   * @param signature          : Txn signature
   * @param height             : Txn height
   * @param poa                : Txn Power of Attorney
   * @param timestamp          : Txn Timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public StockSplitTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String namespace,
      String classId,
      String referenceStateHash,
      double ratio,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {

    //  ensure super class is invoked
    //
    super(
        chainId,
        hash,
        nonce,
        updated,
        fromAddress,
        fromPubKey,
        signature,
        poa,
        timestamp
    );
    this.metadata = (metadata == null ? "" : metadata);
    this.namespace = (namespace == null ? "" : namespace);
    this.classId = (classId == null ? "" : classId);
    this.referenceStateHash = (referenceStateHash == null ? "" : referenceStateHash);
    this.ratio = ratio;
    this.height = height;
  }


  /**
   * StockSplitTx Copyu Constructor.
   *
   * @param toCopy : StockSplitTx to copy from.
   */
  public StockSplitTx(StockSplitTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.metadata = toCopy.getMetadata();
    this.namespace = toCopy.getNameSpace();
    this.classId = toCopy.getClassId();
    this.referenceStateHash = toCopy.getReferenceStateHash();
    this.ratio = toCopy.getRatio();
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    rVal.add(this.fromAddress);

    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {

    hashList.addAll(new Object[]{
        this.chainId,
        TXT.getId(),
        this.nonce,
        this.fromPubKey,
        this.fromAddress,
        this.timestamp,
        this.powerOfAttorney,
        this.namespace,
        this.classId,
        this.ratio,
        this.referenceStateHash,
        this.metadata
    });

    return hashList;

  }


  /**
   * Return this transation as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {

    return new Object[]{
        this.chainId,
        this.int1,
        TXT.getId(),
        this.hash,
        this.nonce,
        this.updated,
        this.fromPubKey,
        this.fromAddress,
        this.timestamp,
        this.powerOfAttorney,
        this.namespace,
        this.classId,
        this.ratio,
        this.referenceStateHash,
        this.signature,
        this.metadata,
        this.height
    };
  }


  public String getClassId() {
    return classId;
  }


  @Override
  public String getMetadata() {
    return metadata;
  }


  public String getNameSpace() {
    return namespace;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  public double getRatio() {
    return ratio;
  }


  public String getReferenceStateHash() {
    return referenceStateHash;
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public String toString() {
    return String.format("%s nameSpace:%s classId:%s from:%s %f", super.toString(), namespace, classId, fromAddress, ratio);
  }

}
