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
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PoaNewContractTx extends NewContractTx implements HasProtocol, NewContractInterface {


  private static final TxType TXT = TxType.POA_NEW_CONTRACT;

  private static final Logger logger = LoggerFactory.getLogger(PoaNewContractTx.class);


  /**
   * Accept an Object[] and return a NewContractTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned NewContractTx object
   */
  public static PoaNewContractTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a PoaNewContractTx.
   *
   * @param txData : The input MPWrappedArray object
   *
   * @return : The constructed PoaNewContractTx object
   */
  public static PoaNewContractTx decodeTX(MPWrappedArray txData) {

    logger.info("New Contract:{}", txData);

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

    String poaAddress = txData.asString(8);
    String poaReference = txData.asString(9);
    long timestamp = txData.asLong(10);
    String poa = txData.asString(11);
    String toAddress = txData.asString(12);

    MPWrappedMap<String, Object> dictionary = txData.asWrappedMap(13);

    String signature = txData.asString(14);
    String protocol = txData.asString(15);
    String metadata = txData.asString(16);

    int height = txData.asInt(17);

    return new PoaNewContractTx(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        toAddress,
        dictionary,
        signature,
        protocol,
        metadata,
        height,
        poa,
        timestamp);

  }

  private String metadata;

  private String poaAddress;

  private String poaReference;

  private String protocol;

  /**
   * Public Constructor.
   * <p>Return new PoaNewContractTx object.
   * Contract Address is calculated.</p>
   *
   * @param chainId            : Txn blockchain chainID
   * @param hash               : Txn hash
   * @param nonce              : Txn nonce
   * @param updated            : Txn updated flag (Sucessfull application to state).
   * @param fromPubKey         : Txn Authoring Public Key
   * @param fromAddress        : Txn Authoring address
   * @param poaAddress         :
   * @param poaReference       :
   * @param contractDictionary : Txn contractDictionary
   * @param signature          : Txn signature
   * @param protocol           :
   * @param metadata           :
   * @param height             : Txn height
   * @param poa                : Txn Power of Attorney
   * @param timestamp          : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaNewContractTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      MPWrappedMap<String, Object> contractDictionary,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp) {

    this(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        AddressUtil.publicKeyToAddress(fromPubKey, AddressType.CONTRACT, nonce),
        contractDictionary,
        signature,
        protocol,
        metadata,
        height,
        poa,
        timestamp
    );


  }


  /**
   * Private Constructor.
   * <p>Return new PoaNewContractTx object.
   * Contract Address is provided.</p>
   *
   * @param chainId            : Blockchain chainID
   * @param hash               : hash
   * @param nonce              : nonce
   * @param updated            : updated flag (Sucessfull application to state).
   * @param fromPubKey         : Authoring Public Key
   * @param fromAddress        : Authoring address
   * @param poaAddress         :
   * @param poaReference       :
   * @param contractAddress    : Contract Address.
   * @param contractDictionary : contractDictionary
   * @param signature          : signature
   * @param protocol           :
   * @param metadata           :
   * @param height             : height
   * @param poa                : Power of Attorney
   * @param timestamp          : timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  private PoaNewContractTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String contractAddress,
      MPWrappedMap<String, Object> contractDictionary,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp) {

    //  ensure super class is invoked
    //
    super(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        contractDictionary,
        signature,
        height,
        poa,
        timestamp
    );

    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
  }


  /**
   * PoaNewContractTx Copy Constructor.
   *
   * @param toCopy : PoaNewContractTx top copy.
   */
  public PoaNewContractTx(PoaNewContractTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.poaAddress = toCopy.getPoaAddress();
    this.poaReference = toCopy.getPoaReference();
    this.metadata = toCopy.getMetadata();
    this.protocol = toCopy.getProtocol();

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
    rVal.add(this.poaAddress);

    rVal.addAll(this.getContractData().addresses());

    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {

    hashList.addAll(new Object[]{
        chainId,
        TXT.getId(),
        nonce,
        fromPubKey,
        poaAddress,
        poaReference,
        timestamp,
        powerOfAttorney,
        protocol,
        metadata,
        this.getContractData().encodeToMapForTxParameter()
    });

    return hashList;

  }


  /**
   * Return this transaction as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {

    PoaNewContractTx tx = this;

    return new Object[]{tx.chainId,
        tx.int1,
        TXT.getId(),
        tx.hash,
        tx.nonce,
        tx.updated,
        tx.fromPubKey,
        tx.fromAddress,
        tx.poaAddress,
        tx.poaReference,
        tx.timestamp,
        tx.powerOfAttorney,
        tx.contractAddress,
        tx.contractDictionary,
        tx.signature,
        tx.protocol,
        tx.metadata,
        tx.height};
  }


  public String getAuthoringAddress() {
    return this.fromAddress;
  }


  public String getAuthoringPublicKey() {
    return this.fromPubKey;
  }


  public String getEffectiveAddress() {
    return this.poaAddress;
  }


  @Override
  public String getMetadata() {

    return metadata;
  }


  public String getPoaAddress() {

    return poaAddress;
  }


  public String getPoaReference() {

    return poaReference;
  }


  @Override
  public int getPriority() {

    return TXT.getPriority();

  }


  public String getProtocol() {

    return protocol;
  }



  @Override
  public TxType getTxType() {

    return TXT;
  }


  public boolean isPOA() {
    return true;
  }

}
