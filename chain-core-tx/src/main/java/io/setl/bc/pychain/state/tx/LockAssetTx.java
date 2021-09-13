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
import io.setl.bc.pychain.state.tx.helper.HasClassId;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockAssetTx extends AbstractTx implements HasNamespace, HasClassId {

  public static final TxType TXT = TxType.LOCK_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(LockAssetTx.class);


  /**
   * Accept an Object[] and return a LockAssetTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned LockAssetTx object
   */
  public static LockAssetTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Decode a transaction presented as a MPWrappedArray object.
   *
   * @param encodedTX :   The encodedTX object to be decoded
   *
   * @return :      An LockAssetTx object
   */
  public static LockAssetTx decodeTX(MPWrappedArray encodedTX) {
    int chainId = encodedTX.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(encodedTX.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }
    String hash = encodedTX.asString(TxGeneralFields.TX_HASH);
    long nonce = encodedTX.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = encodedTX.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = encodedTX.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = encodedTX.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = encodedTX.asLong(8);
    String poa = encodedTX.asString(9);
    String nameSpace = encodedTX.asString(10);
    String classId = encodedTX.asString(11);
    String lockdetails = encodedTX.asString(12);
    String metadata = encodedTX.asString(13);
    String signature = encodedTX.asString(14);
    int height = encodedTX.asInt(15);
    return new LockAssetTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        lockdetails,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  protected String classId;

  protected String lockDetails;

  protected String metadata;

  protected String nameSpace;


  /**
   * Constructor.
   *
   * @param chainId     : Txn ID on the block chain
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated
   * @param fromPubKey  : Txn from public key
   * @param fromAddress : Txn from address
   * @param nameSpace   : Txn namespace
   * @param classId     : Txn class ID
   * @param lockDetails : Txn lockDetails
   * @param metadata    : Txn metadata
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public LockAssetTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String lockDetails,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure super class object construction
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    //  load object attributes
    //
    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.classId = (classId == null ? "" : classId);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
    this.lockDetails = lockDetails;
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public LockAssetTx(LockAssetTx toCopy) {
    //  ensure super class object construction
    //
    super(toCopy);
    //  load object attributes
    //
    this.nameSpace = toCopy.getNameSpace();
    this.classId = toCopy.getClassId();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
    this.lockDetails = toCopy.getLockDetails();
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
  @SuppressWarnings("unchecked")
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.addAll(new Object[]{
        this.chainId,
        TXT.getId(),
        this.nonce,
        this.fromPubKey,
        this.fromAddress,
        this.timestamp,
        this.powerOfAttorney,
        this.nameSpace,
        this.classId,
        this.lockDetails,
        this.metadata
    });
    return hashList;
  }


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
        this.nameSpace,
        this.classId,
        this.lockDetails,
        this.metadata,
        this.signature,
        this.height
    };
  }


  public String getClassId() {
    return classId;
  }


  public String getLockDetails() {
    return lockDetails;
  }


  @Override
  public String getMetadata() {
    return metadata;
  }


  public String getNameSpace() {
    return nameSpace;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
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
    return String.format("%s nameSpace:%s classId:%s", super.toString(), nameSpace, classId);
  }
}
