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
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PoaEncumberTx extends EncumberTx implements HasNamespace, HasClassId, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.POA_ENCUMBER_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(PoaEncumberTx.class);


  /**
   * Accept an Object[] and return a PoaEncumberTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned PoaEncumberTx object
   */
  public static PoaEncumberTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Decode a transaction presented as a MPWrappedArray object.
   *
   * @param encodedTX :   The encodedTX object to be decoded
   *
   * @return :      An PoaEncumberTx object
   */
  public static PoaEncumberTx decodeTX(MPWrappedArray encodedTX) {

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

    String poaAddress = encodedTX.asString(8);
    String poaReference = encodedTX.asString(9);

    long timestamp = encodedTX.asLong(10);
    String poa = encodedTX.asString(11);
    String subjectAddress = encodedTX.asString(12);
    String nameSpace = encodedTX.asString(13);
    String classId = encodedTX.asString(14);
    Long amount = encodedTX.asLong(15);
    MPWrappedMap<String, Object> txDictData = encodedTX.asWrappedMap(16);
    String protocol = encodedTX.asString(17);
    String metadata = encodedTX.asString(18);
    String signature = encodedTX.asString(19);
    int height = encodedTX.asInt(20);

    return new PoaEncumberTx(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
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

  }


  private String poaAddress;

  private String poaReference;


  /**
   * PoaEncumberTx Constructor.
   *
   * @param chainId        : Blockchain that this TX is intended to operate on.
   * @param hash           : TX Hash
   * @param nonce          : TX Nonce (FromAddress)
   * @param updated        : Internal 'Success' flag.
   * @param fromPubKey     : Authoring Public Key
   * @param fromAddress    : Authoring (and Attorney) Address
   * @param poaAddress     : Address whose POA is being deployed
   * @param poaReference   : POA Reference
   * @param subjectaddress : (For future use)
   * @param nameSpace      : Asset Namespace
   * @param classId        : Asset Class
   * @param amount         : Quantity to encumber
   * @param txData         : Object[] conveying Encumbrance details.
   * @param protocol       : Info Only (string)
   * @param metadata       : Info Only (string)
   * @param signature      :
   * @param height         :
   * @param poa            : Not Used
   * @param timestamp      : TX Timestamp.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaEncumberTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
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
      long timestamp) {

    this(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        subjectaddress,
        nameSpace,
        classId,
        amount,
        new MPWrappedMap<>(txData),
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp);
  }


  /**
   * PoaEncumberTx Constructor.
   *
   * @param chainId        : Blockchain that this TX is intended to operate on.
   * @param hash           : TX Hash
   * @param nonce          : TX Nonce (FromAddress)
   * @param updated        : Internal 'Success' flag.
   * @param fromPubKey     : Authoring Public Key
   * @param fromAddress    : Authoring (and Attorney) Address
   * @param poaAddress     : Address whose POA is being deployed
   * @param poaReference   : POA Reference
   * @param subjectaddress : (For future use)
   * @param nameSpace      : Asset Namespace
   * @param classId        : Asset Class
   * @param amount         : Quantity to encumber
   * @param txData         : Wrapped Map conveying Encumbrance details.
   * @param protocol       : Info Only (string)
   * @param metadata       : Info Only (string)
   * @param signature      :
   * @param height         :
   * @param poa            : Not Used
   * @param timestamp      : TX Timestamp.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaEncumberTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
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
      long timestamp) {

    //  ensure super class object construction
    //
    super(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectaddress,
        nameSpace,
        classId,
        amount,
        txData,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );

    //  load object attributes
    //
    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);

  }


  /**
   * PoaEncumberTx.
   *
   * @param chainId        :
   * @param hash           :
   * @param nonce          :
   * @param updated        :
   * @param fromPubKey     :
   * @param fromAddress    : Address to which the encumbrance refers
   * @param poaAddress     : Effective Address
   * @param poaReference   : poa Reference
   * @param subjectaddress : (For future use)
   * @param reference      : Encumbrance Reference
   * @param isCumulative   : the new encumbrance is cumulative
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
  public PoaEncumberTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String subjectaddress,
      String reference,
      boolean isCumulative,
      String nameSpace,
      String classId,
      Number amount,
      Object[] beneficiaries,  // [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
      Object[] administrators, // [[Address, StartTime, EndTime], ...] (See EncumbranceDetail Class)
      String protocol,
      String metadata,
      String signature,
      String poa,
      long timestamp) {

    //  ensure super class object construction
    //
    super(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectaddress,
        reference,
        isCumulative,
        nameSpace,
        classId,
        amount,
        beneficiaries,
        administrators,
        protocol,
        metadata,
        signature,
        poa,
        timestamp
    );

    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);

  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public PoaEncumberTx(PoaEncumberTx toCopy) {
    //  ensure super class object construction
    //
    super(toCopy);

    //  load object attributes
    //
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
    txData.iterate(txDataMap::put);

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

    return new Object[]{this.chainId,
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
        this.subjectaddress,
        this.nameSpace,
        this.classId,
        this.amount,
        this.txData,
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


}
