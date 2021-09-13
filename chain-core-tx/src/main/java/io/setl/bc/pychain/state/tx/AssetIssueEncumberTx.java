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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADMINISTRATORS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.BENEFICIARIES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;

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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetIssueEncumberTx extends AssetIssueTx implements HasNamespace, HasClassId, HasToAddress, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.ISSUE_AND_ENCUMBER_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(AssetIssueEncumberTx.class);


  /**
   * Take AssetIssueEncumberTx and copy costruct with a null signature.
   *
   * @param txn : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static AssetIssueEncumberTx cloneWithoutSignature(AssetIssueEncumberTx txn) {
    return new AssetIssueEncumberTx(
        txn.chainId,
        txn.getHash(),
        txn.nonce,
        txn.updated,
        txn.fromPubKey,
        txn.fromAddress,
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
   * Accept an Object[] and return a AssetIssueEncumberTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned AssetIssueEncumberTx object
   */
  public static AssetIssueEncumberTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create AssetIssueEncumberTx from it.
   *
   * @param encodedTX : The input MPWrappedArray object
   *
   * @return : The constructed AssetIssueEncumberTx object
   */
  public static AssetIssueEncumberTx decodeTX(MPWrappedArray encodedTX) {
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
    long timestamp = encodedTX.asLong(8);
    String poa = encodedTX.asString(9);
    String nameSpace = encodedTX.asString(10);
    String classId = encodedTX.asString(11);
    String toAddress = encodedTX.asString(12);
    Number amount = (new Balance(encodedTX.get(13))).getValue();
    MPWrappedMap<String, Object> txEncumbranceData = encodedTX.asWrappedMap(14);
    String protocol = encodedTX.asString(15);
    String metadata = encodedTX.asString(16);
    String signature = encodedTX.asString(17);
    int height = encodedTX.asInt(18);
    return new AssetIssueEncumberTx(
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
        txEncumbranceData,
        signature,
        height,
        poa,
        timestamp,
        protocol,
        metadata
    );
  }


  protected MPWrappedMap<String, Object> encumbranceData;


  /**
   * Constructor.
   *
   * @param chainId     : Txn ID on the blockchain
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true or false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param nameSpace   : Txn name space
   * @param classId     : Txn classID
   * @param toAddress   : Txn to address
   * @param amount      : Txn amount
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   * @param protocol    : Txn protocol
   * @param metadata    : Txn metadata
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public AssetIssueEncumberTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
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
    super(chainId, hash, nonce, updated, fromPubKey, fromAddress, nameSpace, classId, toAddress, amount, signature, height, poa, timestamp, protocol, metadata);
    this.encumbranceData = EncumberTx.txDictDataCopy(encumbranceData);
  }


  /**
   * Constructor.
   *
   * @param chainId     : Txn ID on the blockchain
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true or false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param nameSpace   : Txn name space
   * @param classId     : Txn classID
   * @param toAddress   : Txn to address
   * @param amount      : Txn amount
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   * @param protocol    : Txn protocol
   * @param metadata    : Txn metadata
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public AssetIssueEncumberTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
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
    super(chainId, hash, nonce, updated, fromPubKey, fromAddress, nameSpace, classId, toAddress, amount, signature, height, poa, timestamp, protocol, metadata);
    TreeMap<String, Object> tempMap = new TreeMap<>();
    if ((reference != null) && (!reference.isEmpty())) {
      tempMap.put(REFERENCE, reference);
    }
    tempMap.put(BENEFICIARIES, beneficiaries);
    tempMap.put(ADMINISTRATORS, administrators);
    this.encumbranceData = new MPWrappedMap<>(tempMap);
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public AssetIssueEncumberTx(AssetIssueEncumberTx toCopy) {
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
    this.encumbranceData = EncumberTx.txDictDataCopy(toCopy.getEncumbrance());
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {
    return super.addresses();
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


  public MPWrappedMap<String, Object> getEncumbrance() {
    return this.encumbranceData;
  }


  public Map<String, Object> getEncumbrancesAsMap() {
    return EncumberTx.txDictToMap(this.encumbranceData);
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
  public String toString() {
    return String.format("%s nameSpace:%s classId:%s from:%s to:%s %s", super.toString(), nameSpace, classId, fromAddress, toAddress, amount.toString());
  }
}
