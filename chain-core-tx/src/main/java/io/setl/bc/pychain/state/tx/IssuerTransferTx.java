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
import io.setl.bc.pychain.state.tx.helper.HasAmount;
import io.setl.bc.pychain.state.tx.helper.HasClassId;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuerTransferTx extends AbstractTx implements HasNamespace, HasClassId, HasToAddress, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.TRANSFER_ASSET_AS_ISSUER;

  private static final Logger logger = LoggerFactory.getLogger(IssuerTransferTx.class);


  /**
   * Take AssetIssueTx and copy construct with a null signature.
   *
   * @param toCopy : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static IssuerTransferTx cloneWithoutSignature(IssuerTransferTx toCopy) {
    return new IssuerTransferTx(
        toCopy.chainId,
        toCopy.getHash(),
        toCopy.nonce,
        toCopy.updated,
        toCopy.fromPubKey,
        toCopy.fromAddress,
        toCopy.nameSpace,
        toCopy.classId,
        toCopy.sourceAddress,
        toCopy.toChainId,
        toCopy.toAddress,
        toCopy.amount,
        "",
        toCopy.protocol,
        toCopy.metadata,
        toCopy.height,
        toCopy.powerOfAttorney,
        toCopy.timestamp
    );
  }


  /**
   * Accept an Object[] and return a IssuerTransferTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned IssuerTransferTx object
   */
  public static IssuerTransferTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create IssuerTransferTx from it.
   *
   * @param txData : The input MPWrappedArray
   *
   * @return : The constructed IssuerTransferTx object
   */
  public static IssuerTransferTx decodeTX(MPWrappedArray txData) {
    int chainId = txData.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(txData.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }
    String hash = txData.asString(TxGeneralFields.TX_HASH);
    long nonce = txData.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = txData.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = txData.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = txData.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = txData.asLong(8);
    String poa = txData.asString(9);
    String nameSpace = txData.asString(10);
    String classId = txData.asString(11);
    String sourceAddress = txData.asString(12);
    int toChainId = txData.asInt(13);
    String toAddress = txData.asString(14);
    Number amount = (new Balance(txData.get(15))).getValue();
    String protocol = txData.asString(16);
    String metadata = txData.asString(17);
    String signature = txData.asString(18);
    int height = txData.asInt(19);
    return new IssuerTransferTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        sourceAddress,
        toChainId,
        toAddress,
        amount,
        signature,
        protocol,
        metadata,
        height,
        poa,
        timestamp
    );
  }


  protected Number amount;

  protected String classId;

  protected String metadata;

  protected String nameSpace;

  protected String protocol;

  protected String sourceAddress;

  protected String toAddress;

  protected int toChainId;


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain ID
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true/false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param nameSpace   : Txn namespace
   * @param classId     : Txn classID
   * @param toChainId   : Txn target blockchain ID
   * @param toAddress   : Txn to address
   * @param amount      : Txn amount
   * @param signature   : Txn signature
   * @param protocol    : Txn protocol
   * @param metadata    : Txn metadata
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public IssuerTransferTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String sourceAddress,
      int toChainId,
      String toAddress,
      Number amount,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure that super class constructor is called
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.classId = (classId == null ? "" : classId);
    this.toAddress = toAddress;
    this.amount = (new Balance(amount)).getValue();
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
    this.toChainId = toChainId;
    this.sourceAddress = sourceAddress;
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public IssuerTransferTx(IssuerTransferTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.nameSpace = toCopy.getNameSpace();
    this.classId = toCopy.getClassId();
    this.toAddress = toCopy.getToAddress();
    this.amount = toCopy.getAmount();
    this.metadata = toCopy.getMetadata();
    this.protocol = toCopy.getProtocol();
    this.height = toCopy.getHeight();
    this.toChainId = toCopy.getToChainId();
    this.sourceAddress = toCopy.getSourceAddress();
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
    if ((this.toAddress != null) && (this.toAddress.length() > 0)) {
      rVal.add(this.toAddress);
    }
    if ((this.sourceAddress != null) && (this.sourceAddress.length() > 0)) {
      rVal.add(this.sourceAddress);
    }
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.addAll(
        new Object[]{
            chainId,
            TXT.getId(),
            nonce,
            fromPubKey,
            fromAddress,
            timestamp,
            powerOfAttorney,
            nameSpace,
            classId,
            sourceAddress,
            toChainId,
            toAddress,
            amount,
            protocol,
            metadata,
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
        this.nameSpace,
        this.classId,
        this.sourceAddress,
        this.toChainId,
        this.toAddress,
        this.amount,
        this.protocol,
        this.metadata,
        this.signature,
        this.height
    };
  }


  public Number getAmount() {
    return amount;
  }


  public String getClassId() {
    return classId;
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


  public String getProtocol() {
    return protocol;
  }


  public String getSourceAddress() {
    return sourceAddress;
  }


  public String getToAddress() {
    return toAddress;
  }


  @Override
  public int getToChainId() {
    return toChainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public String toString() {
    return String.format("%s nameSpace:%s classId:%s from:%s to:%s %s", super.toString(), nameSpace, classId, fromAddress, toAddress, amount.toString());
  }
}
