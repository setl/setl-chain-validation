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
import static io.setl.bc.pychain.state.tx.helper.TxParameters.IS_CUMULATIVE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.helper.HasAmount;
import io.setl.bc.pychain.state.tx.helper.HasClassId;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncumberTx extends AbstractTx implements HasNamespace, HasClassId, HasAmount {

  public static final TxType TXT = TxType.ENCUMBER_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(EncumberTx.class);


  /**
   * Accept an Object[] and return a EncumberTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned EncumberTx object
   */
  public static EncumberTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Decode a transaction presented as a MPWrappedArray object.
   *
   * @param encodedTX :   The encodedTX object to be decoded
   *
   * @return :      An EncumberTx object
   */
  public static EncumberTx decodeTX(MPWrappedArray encodedTX) {
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
    String subjectAddress = encodedTX.asString(10);
    String nameSpace = encodedTX.asString(11);
    String classId = encodedTX.asString(12);
    Number amount = (new Balance(encodedTX.get(13)));
    MPWrappedMap<String, Object> txDictData = encodedTX.asWrappedMap(14);
    String protocol = encodedTX.asString(15);
    String metadata = encodedTX.asString(16);
    String signature = encodedTX.asString(17);
    int height = encodedTX.asInt(18);
    return new EncumberTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        txDictDataCopy(txDictData),
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
    // thisTX.priority = -20
  }


  /**
   * txDictDataCopy.
   * <p>Utility function to copy Encumbrance data.
   * {
   * reference      : REFERENCE,
   * beneficiaries  : [['Address', Start, End], ...]
   * administrators : [['Address', Start, End], ...]
   * }
   * </p>
   *
   * @param txDictData : Map containing encumbrance data (as above).
   *
   * @return : TreeMap - Copy of encumbrance.
   */
  public static MPWrappedMap<String, Object> txDictDataCopy(MPWrappedMap<String, Object> txDictData) {
    TreeMap<String, Object> txDataMap = new TreeMap<>();
    txDictData.iterate((key, value) -> {
      switch (key) {
        case REFERENCE:
        case IS_CUMULATIVE:
          txDataMap.put(key, value);
          break;
        case BENEFICIARIES:
        case ADMINISTRATORS:
          if (value instanceof Object[]) {
            Object[] dataArray = (Object[]) value;
            Object[] tempArray = new Object[dataArray.length];
            for (int index = 0, l = dataArray.length; index < l; index++) {
              if (dataArray[index] instanceof Object[]) {
                tempArray[index] = (new EncumbranceDetail((Object[]) dataArray[index])).encode();
              } else if (dataArray[index] instanceof EncumbranceDetail) {
                tempArray[index] = ((EncumbranceDetail) dataArray[index]).encode();
              }
            }
            txDataMap.put(key, tempArray);
          }
          break;
        default:
          break;
      }
    });
    return new MPWrappedMap<>(txDataMap);
  }


  /**
   * txDictToMap.
   * <p>Return EncumberTx as usable map Object :
   * 'reference'      : String
   * 'beneficiaries'  : [EncumbranceDetail, ...]
   * 'administrators' : [EncumbranceDetail, ...]
   * </p>
   *
   * @param txDictData :
   *
   * @return : Map{String, Object}
   */
  public static Map<String, Object> txDictToMap(MPWrappedMap<String, Object> txDictData) {
    TreeMap<String, Object> txDataMap = new TreeMap<>();
    if (txDictData == null) {
      return txDataMap;
    }
    txDictData.iterate((key, value) -> {
      switch (key) {
        case REFERENCE:
        case IS_CUMULATIVE:
          txDataMap.put(key, value);
          break;
        case BENEFICIARIES:
        case ADMINISTRATORS:
          if (value instanceof Object[]) {
            Object[] dataArray = (Object[]) value;
            EncumbranceDetail[] tempArray = new EncumbranceDetail[dataArray.length];
            for (int index = 0, l = dataArray.length; index < l; index++) {
              if (dataArray[index] instanceof Object[]) {
                tempArray[index] = new EncumbranceDetail((Object[]) dataArray[index]);
              }
            }
            txDataMap.put(key, tempArray);
          }
          break;
        default:
          // Checkstyle !!
          break;
      }
    });
    return txDataMap;
  }


  protected Number amount;

  protected String classId;

  protected String metadata;

  protected String nameSpace;

  protected String protocol;

  protected String subjectaddress;

  protected MPWrappedMap<String, Object> txData;


  /**
   * EncumberTx Constructor.
   *
   * @param chainId        : Blockchain that this TX is intended to operate on.
   * @param hash           : TX Hash
   * @param nonce          : TX Nonce (FromAddress)
   * @param updated        : Internal 'Success' flag.
   * @param fromPubKey     : Authoring Public Key
   * @param fromAddress    : Authoring (and Granting) Address
   * @param subjectaddress : (For future use)
   * @param nameSpace      : Asset Namespace
   * @param classId        : Asset Class
   * @param amount         : Quantity to encumber
   * @param txData         : Object Array conveying Encumbrance details.
   * @param protocol       : Info Only (string)
   * @param metadata       : Info Only (string)
   * @param signature      :
   * @param height         :
   * @param poa            :
   * @param timestamp      :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public EncumberTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String subjectaddress,
      String nameSpace,
      String classId,
      Number amount,
      Object[] txData,
      String protocol,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    this(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectaddress,
        nameSpace,
        classId,
        amount,
        new MPWrappedMap<>(txData != null ? txData : new Object[0]),
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  /**
   * EncumberTx Constructor.
   *
   * @param chainId        : Blockchain that this TX is intended to operate on.
   * @param hash           : TX Hash
   * @param nonce          : TX Nonce (FromAddress)
   * @param updated        : Internal 'Success' flag.
   * @param fromPubKey     : Authoring Public Key
   * @param fromAddress    : Authoring (and Granting) Address
   * @param subjectaddress : (For future use)
   * @param nameSpace      : Asset Namespace
   * @param classId        : Asset Class
   * @param amount         : Quantity to encumber
   * @param txData         : Wrapped Map conveying Encumbrance details.
   * @param protocol       : Info Only (string)
   * @param metadata       : Info Only (string)
   * @param signature      :
   * @param height         :
   * @param poa            :
   * @param timestamp      :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public EncumberTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String subjectaddress,
      String nameSpace,
      String classId,
      Number amount,
      MPWrappedMap<String, Object> txData,
      String protocol,
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
    this.subjectaddress = (subjectaddress == null ? "" : subjectaddress);
    this.amount = (new Balance(amount)).getValue();
    this.txData = txData;
    this.protocol = (protocol == null ? "" : protocol);
  }


  /**
   * EncumberTx.
   *
   * @param chainId        :
   * @param hash           :
   * @param nonce          :
   * @param updated        :
   * @param fromPubKey     :
   * @param fromAddress    : Address to which the encumbrance refers
   * @param subjectaddress : (For future use)
   * @param reference      : Encumbrance Reference
   * @param isCumulative   : Is the encumbrance reference cumulative?
   * @param nameSpace      : Asset Namespace
   * @param classId        : Asset Class
   * @param amount         : Quantity to encumber
   * @param beneficiaries  : [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
   * @param administrators : [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
   * @param protocol       : Info Only (string)
   * @param metadata       : Info Only (string)
   * @param signature      :
   * @param poa            :
   * @param timestamp      :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public EncumberTx(
      int chainId, String hash, long nonce, boolean updated, String fromPubKey, String fromAddress, String subjectaddress, String reference,
      boolean isCumulative, String nameSpace, String classId, Number amount, Object[] beneficiaries,
      // [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
      Object[] administrators, // [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
      String protocol, String metadata, String signature, String poa, long timestamp
  ) {
    //  ensure super class object construction
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    TreeMap<String, Object> tempMap = new TreeMap<>();
    if ((reference != null) && (!reference.isEmpty())) {
      tempMap.put(REFERENCE, reference);
      tempMap.put(IS_CUMULATIVE, isCumulative);
    }
    tempMap.put(BENEFICIARIES, beneficiaries);
    tempMap.put(ADMINISTRATORS, administrators);
    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.classId = (classId == null ? "" : classId);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = -1;
    this.subjectaddress = (subjectaddress == null ? "" : subjectaddress);
    this.amount = (new Balance(amount)).getValue();
    this.txData = new MPWrappedMap<>(tempMap);
    this.protocol = (protocol == null ? "" : protocol);
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public EncumberTx(EncumberTx toCopy) {
    //  ensure super class object construction
    //
    super(toCopy);
    //  load object attributes
    //
    this.nameSpace = toCopy.getNameSpace();
    this.classId = toCopy.getClassId();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
    this.subjectaddress = toCopy.getSubjectaddress();
    this.amount = toCopy.getAmount();
    this.protocol = toCopy.getProtocol();
    this.txData = txDictDataCopy(toCopy.getEncumbrances());
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
    if ((this.subjectaddress != null) && (!this.subjectaddress.isEmpty())) {
      rVal.add(this.subjectaddress);
    }
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    TreeMap<String, Object> txDataMap = new TreeMap<>();
    txData.iterate(txDataMap::put);
    hashList.addAll(
        new Object[]{
            this.chainId,
            TXT.getId(),
            this.nonce,
            this.fromPubKey,
            this.fromAddress,
            this.timestamp,
            this.powerOfAttorney,
            this.subjectaddress,
            this.nameSpace,
            this.classId,
            this.amount,
            txDataMap,
            this.protocol,
            this.metadata,
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
        this.subjectaddress,
        this.nameSpace,
        this.classId,
        this.amount,
        this.txData,
        protocol,
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


  public MPWrappedMap<String, Object> getEncumbrances() {
    return this.txData;
  }


  public Map<String, Object> getEncumbrancesAsMap() {
    return txDictToMap(this.txData);
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


  public String getSubjectaddress() {
    return subjectaddress;
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  public boolean isHoldingLockTx() {
    return false;
  }


  @Override
  public String toString() {
    return String.format("%s nameSpace:%s classId:%s", super.toString(), nameSpace, classId);
  }
}
