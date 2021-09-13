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

public class AssetTransferTx extends AbstractTx implements HasNamespace, HasClassId, HasToAddress, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.TRANSFER_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(AssetTransferTx.class);


  /**
   * Take AssetIssueTx and copy construct with a null signature.
   *
   * @param txn : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static AssetTransferTx cloneWithoutSignature(AssetTransferTx txn) {
    return new AssetTransferTx(
        txn.chainId,
        txn.int1,
        txn.getHash(),
        txn.nonce,
        txn.updated,
        txn.fromPubKey,
        txn.fromAddress,
        txn.nameSpace,
        txn.classId,
        txn.toAddress,
        txn.amount,
        null,
        txn.protocol,
        txn.metadata,
        txn.height,
        txn.powerOfAttorney,
        txn.timestamp
    );
  }


  /**
   * Accept an Object[] and return a AssetIssueTx.
   *
   * @param encodedTx : The input MPWrappedArray object
   *
   * @return : The returned AssetTransferTx object
   */
  public static AssetTransferTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create AssetTranserTx from it.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The constructed AssetTranserTx object
   */
  public static AssetTransferTx decodeTX(MPWrappedArray vat) {
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = vat.asInt(1);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
      // /NAMESPACE_REGISTER
      // ASSET_CLASS_REGISTER
      // ASSET_ISSUE
      return null;
    }
    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    String nameSpace = vat.asString(10);
    String classId = vat.asString(11);
    String toAddress = vat.asString(12);
    Number amount = (new Balance(vat.get(13))).getValue();
    String signature = vat.asString(14);
    String protocol = vat.asString(15);
    String metadata = vat.asString(16);
    int height = vat.size() > 17 ? vat.asInt(17) : -1;
    return new AssetTransferTx(
        chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
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

  protected String toAddress;


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain ID
   * @param int1        : Txn int1
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true/false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from Address
   * @param nameSpace   : Txn namespace
   * @param classId     : Txn classID
   * @param toAddress   : Txn to address
   * @param amount      : Txn amount
   * @param signature   : Txn signature
   * @param protocol    : Txn protocol
   * @param metadata    : Txn metadata
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn Timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public AssetTransferTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String toAddress,
      Number amount,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure object super is invoked
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.classId = (classId == null ? "" : classId);
    this.toAddress = toAddress;
    this.amount = (new Balance(amount)).getValue();
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public AssetTransferTx(AssetTransferTx toCopy) {
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
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.add(chainId);
    hashList.add(TXT.getId());
    hashList.add(nonce);
    hashList.add(fromPubKey);
    hashList.add(fromAddress);
    hashList.add(timestamp);
    hashList.add(powerOfAttorney);
    hashList.add(toAddress);
    hashList.add(nameSpace);
    hashList.add(classId);
    hashList.add(amount);
    hashList.add(protocol);
    hashList.add(metadata);
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
        this.toAddress,
        this.amount,
        this.signature,
        this.protocol,
        this.metadata,
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


  public String getToAddress() {
    return toAddress;
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
    return String.format("%s nameSpace:%s classId:%s from:%s to:%s %s", super.toString(), nameSpace, classId, fromAddress, toAddress, amount.toString());
  }
}
