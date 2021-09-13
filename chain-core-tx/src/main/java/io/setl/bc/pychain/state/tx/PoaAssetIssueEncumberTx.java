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
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.helper.HasAmount;
import io.setl.bc.pychain.state.tx.helper.HasClassId;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoaAssetIssueEncumberTx extends AssetIssueEncumberTx implements HasNamespace, HasClassId, HasToAddress, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.POA_ISSUE_AND_ENCUMBER_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(PoaAssetIssueEncumberTx.class);


  /**
   * Take PoaAssetIssueEncumberTx and copy costruct with a null signature.
   *
   * @param txn : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static PoaAssetIssueEncumberTx cloneWithoutSignature(PoaAssetIssueEncumberTx txn) {
    return new PoaAssetIssueEncumberTx(
        txn.chainId,
        txn.getHash(),
        txn.nonce,
        txn.updated,
        txn.fromPubKey,
        txn.fromAddress,
        txn.poaAddress,
        txn.poaReference,
        txn.nameSpace,
        txn.classId,
        txn.toAddress,
        txn.amount,
        EncumberTx.txDictDataCopy(txn.encumbranceData),
        null,
        txn.height,
        txn.powerOfAttorney,
        txn.timestamp,
        txn.protocol,
        txn.metadata
    );
  }


  /**
   * Accept an Object[] and return a PoaAssetIssueEncumberTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned PoaAssetIssueEncumberTx object
   */
  public static PoaAssetIssueEncumberTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create PoaAssetIssueEncumberTx from it.
   *
   * @param encodedTX : The input MPWrappedArray object
   *
   * @return : The constructed PoaAssetIssueEncumberTx object
   */
  public static PoaAssetIssueEncumberTx decodeTX(MPWrappedArray encodedTX) {
    int chainId = encodedTX.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(encodedTX.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.info("Unsupported:{}", txt);
      // /NAMESPACE_REGISTER
      // ASSET_CLASS_REGISTER
      // ASSET_ISSUE
      return null;
    }
    String hash = encodedTX.asString(TxGeneralFields.TX_HASH);
    long nonce = encodedTX.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = encodedTX.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = encodedTX.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = encodedTX.asString(TxGeneralFields.TX_FROM_ADDR);
    String poaAddress = encodedTX.asString(8);
    String poaReference = encodedTX.asString(9);
    long timestamp = encodedTX.asLong(10);
    String poa = encodedTX.asString(11);
    String nameSpace = encodedTX.asString(12);
    String classId = encodedTX.asString(13);
    String toAddress = encodedTX.asString(14);
    Number amount = (new Balance(encodedTX.get(15))).getValue();
    MPWrappedMap<String, Object> txEncumbranceData = encodedTX.asWrappedMap(16);
    String protocol = encodedTX.asString(17);
    String metadata = encodedTX.asString(18);
    String signature = encodedTX.asString(19);
    int height = encodedTX.asInt(20);
    return new PoaAssetIssueEncumberTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        classId,
        toAddress,
        amount,
        txEncumbranceData,
        signature,
        height,
        poa,
        timestamp,
        protocol,
        metadata
    );
  }


  private String poaAddress;

  private String poaReference;


  /**
   * Constructor.
   *
   * @param chainId      : Txn ID on the blockchain
   * @param hash         : Txn hash
   * @param nonce        : Txn nonce
   * @param updated      : Txn updated true or false
   * @param fromPubKey   : Txn from Public Key
   * @param fromAddress  : Txn from address
   * @param poaAddress   :
   * @param poaReference :
   * @param nameSpace    : Txn name space
   * @param classId      : Txn classID
   * @param toAddress    : Txn to address
   * @param amount       : Txn amount
   * @param signature    : Txn signature
   * @param height       : Txn height
   * @param poa          : Txn Power of Attorney
   * @param timestamp    : Txn timestamp
   * @param protocol     : Txn protocol
   * @param metadata     : Txn metadata
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaAssetIssueEncumberTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String toAddress,
      Number amount,
      MPWrappedMap<String, Object> encumbranceData,
      String signature,
      int height,
      String poa,
      long timestamp,
      String protocol,
      String metadata
  ) {
    //  ensure super object constructor is called
    //
    super(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        toAddress,
        amount,
        EncumberTx.txDictDataCopy(encumbranceData),
        signature,
        height,
        poa,
        timestamp,
        protocol,
        metadata
    );
    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);
  }


  /**
   * Constructor.
   *
   * @param chainId      : Txn ID on the blockchain
   * @param hash         : Txn hash
   * @param nonce        : Txn nonce
   * @param updated      : Txn updated true or false
   * @param fromPubKey   : Txn from Public Key
   * @param fromAddress  : Txn from address
   * @param poaAddress   :
   * @param poaReference :
   * @param nameSpace    : Txn name space
   * @param classId      : Txn classID
   * @param toAddress    : Txn to address
   * @param amount       : Txn amount
   * @param signature    : Txn signature
   * @param height       : Txn height
   * @param poa          : Txn Power of Attorney
   * @param timestamp    : Txn timestamp
   * @param protocol     : Txn protocol
   * @param metadata     : Txn metadata
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaAssetIssueEncumberTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String toAddress,
      Number amount,
      String reference,
      Object[] beneficiaries,
      // [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
      Object[] administrators,
      // [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
      String signature,
      int height,
      String poa,
      long timestamp,
      String protocol,
      String metadata
  ) {
    //  ensure super object constructor is called
    //
    super(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        toAddress,
        amount,
        reference,
        beneficiaries,
        administrators,
        signature,
        height,
        poa,
        timestamp,
        protocol,
        metadata
    );
    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public PoaAssetIssueEncumberTx(PoaAssetIssueEncumberTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.poaAddress = toCopy.getPoaAddress();
    this.poaReference = toCopy.getPoaReference();
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {
    Set<String> rVal = super.addresses();
    if ((this.poaAddress != null) && (!this.poaAddress.isEmpty())) {
      rVal.add(this.poaAddress);
    }
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    TreeMap<String, Object> txDataMap = new TreeMap<>();
    encumbranceData.iterate(txDataMap::put);
    hashList.addAll(new Object[]{
        this.chainId,
        TXT.getId(),
        this.nonce,
        this.fromPubKey,
        this.fromAddress,
        this.poaAddress,
        this.poaReference,
        this.timestamp,
        this.powerOfAttorney,
        this.toAddress,
        this.nameSpace,
        this.classId,
        this.amount,
        txDataMap,
        this.protocol,
        this.metadata,
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
        this.poaAddress,
        this.poaReference,
        this.timestamp,
        this.powerOfAttorney,
        this.nameSpace,
        this.classId,
        this.toAddress,
        this.amount,
        this.encumbranceData,
        this.protocol,
        this.metadata,
        this.signature,
        this.height
    };
  }


  @Override
  public String getPoaAddress() {
    return poaAddress;
  }


  @Override
  public String getPoaReference() {
    return poaReference;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public boolean isPOA() {
    return true;
  }


  @Override
  public String toString() {
    return String.format("%s nameSpace:%s classId:%s from:%s to:%s %s", super.toString(), nameSpace, classId, fromAddress, toAddress, amount.toString());
  }
}
