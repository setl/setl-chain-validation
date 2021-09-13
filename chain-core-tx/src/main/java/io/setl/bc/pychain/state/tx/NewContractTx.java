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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;

public class NewContractTx extends AbstractTx implements NewContractInterface {

  private static final TxType TXT = TxType.NEW_CONTRACT;

  private static final Logger logger = LoggerFactory.getLogger(NewContractTx.class);



  public static class UnsupportedContractException extends RuntimeException {

    UnsupportedContractException(String e) {
      super(e);
    }

  }


  /**
   * Accept an Object[] and return a NewContractTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned NewContractTx object
   */
  public static NewContractTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a NewConstractTx.
   *
   * @param contractData : The input MPWrappedArray object
   *
   * @return : The constructed NewContractTx object
   */
  public static NewContractTx decodeTX(MPWrappedArray contractData) {
    logger.info("New Contract:{}", contractData);
    int chainId = contractData.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(contractData.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }
    String hash = contractData.asString(TxGeneralFields.TX_HASH);
    long nonce = contractData.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = contractData.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = contractData.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = contractData.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = contractData.asLong(8);
    String poa = contractData.asString(9);
    String toAddress = contractData.asString(10);
    MPWrappedMap<String, Object> dictionary = contractData.asWrappedMap(11);
    String signature = contractData.asString(12);
    int height = -1;
    if (contractData.size() > 13) {
      height = contractData.asInt(13);
    }
    return new NewContractTx(chainId, hash, nonce, updated, fromPubKey, fromAddress, toAddress, dictionary, signature, height, poa, timestamp);
    // thisTX.priority = -20
  }


  public static IContractData getContractDataFromDictionary(Map<String, Object> thisDictionary) {
    return getContractDataFromDictionary(thisDictionary, "NotKnown");
  }


  /**
   * getContractDataFromDictionary().
   * Parse the given dictionary and return the appropriate IContractData implementation.
   *
   * @param thisDictionary :
   * @param functionType   :
   *
   * @return :
   */
  public static IContractData getContractDataFromDictionary(Map<String, Object> thisDictionary, String functionType) {
    String thisFunctionType = (String) thisDictionary.get(__FUNCTION);
    if (thisFunctionType == null) {
      thisFunctionType = (String) thisDictionary.get(CONTRACT_FUNCTION);
      if (thisFunctionType == null) {
        thisFunctionType = functionType;
      }
    }
    switch (thisFunctionType) {
      case CONTRACT_NAME_DVP_UK:
        return new DvpUkContractData(thisDictionary);
      case CONTRACT_NAME_TOKENS_NOMINATE: // "tokens_nominate":
        return new TokensNominateContractData(thisDictionary);
      case CONTRACT_NAME_EXCHANGE:
        return new ExchangeContractData(thisDictionary);
      default:
        throw new UnsupportedContractException("Unsupported Contract type  : " + thisFunctionType);
    }
  }


  /**
   * getContractDataFromDictionary().
   *
   * @param thisDictionary :
   *
   * @return :
   */
  public static IContractData getContractDataFromDictionary(MPWrappedMap<String, Object> thisDictionary) {
    return getContractDataFromDictionary(thisDictionary, "NotKnown");
  }


  /**
   * getContractDataFromDictionary().
   *
   * @param thisDictionary :
   * @param functionType   :
   *
   * @return :
   */
  public static IContractData getContractDataFromDictionary(MPWrappedMap<String, Object> thisDictionary, String functionType) {
    final String thisFunctionType = getContractFunction(thisDictionary, functionType);
    switch (thisFunctionType == null ? "null" : thisFunctionType) {
      case CONTRACT_NAME_DVP_UK:
        return new DvpUkContractData(thisDictionary);
      case CONTRACT_NAME_TOKENS_NOMINATE:
        return new TokensNominateContractData(thisDictionary);
      case CONTRACT_NAME_EXCHANGE:
        return new ExchangeContractData(thisDictionary);
      default:
        throw new UnsupportedContractException("Contract Entry : Unsupported Contract type  : " + thisFunctionType);
    }
  }


  static String getContractFunction(MPWrappedMap<String, Object> thisDictionary, String defaultFunction) {
    final String[] function = {null};
    if (thisDictionary == null) {
      return null;
    }
    thisDictionary.iterate((k, v) -> {
      if ((__FUNCTION.equalsIgnoreCase(k)) || (function[0] == null && CONTRACT_FUNCTION.equalsIgnoreCase(k))) {
        // __function has precedence
        function[0] = (String) v;
      }
    });
    if (function[0] == null) {
      return defaultFunction;
    }
    return function[0];
  }


  protected String contractAddress;

  protected MPWrappedMap<String, Object> contractDictionary;

  protected IContractData tempContractData;


  /**
   * Public Constructor.
   * <p>Return new NewContractTx object.
   * Contract Address is calculated.</p>
   *
   * @param chainId            : Txn blockchain chainID
   * @param hash               : Txn hash
   * @param nonce              : Txn nonce
   * @param updated            : Txn updated flag (Sucessfull application to state).
   * @param fromPubKey         : Txn Authoring Public Key
   * @param fromAddress        : Txn Authoring address
   * @param contractDictionary : Txn contractDictionary
   * @param signature          : Txn signature
   * @param height             : Txn height
   * @param poa                : Txn Power of Attorney
   * @param timestamp          : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public NewContractTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      MPWrappedMap<String, Object> contractDictionary,
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
        AddressUtil.publicKeyToAddress(fromPubKey, AddressType.CONTRACT, nonce),
        contractDictionary,
        signature,
        height,
        poa,
        timestamp
    );
  }


  /**
   * Private Constructor.
   * <p>Return new NewContractTx object.
   * Contract Address is provided.</p>
   *
   * @param chainId            : Blockchain chainID
   * @param hash               : hash
   * @param nonce              : nonce
   * @param updated            : updated flag (Sucessfull application to state).
   * @param fromPubKey         : Authoring Public Key
   * @param fromAddress        : Authoring address
   * @param contractAddress    : Contract Address.
   * @param contractDictionary : contractDictionary
   * @param signature          : signature
   * @param height             : height
   * @param poa                : Power of Attorney
   * @param timestamp          : timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  private NewContractTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String contractAddress,
      MPWrappedMap<String, Object> contractDictionary,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure super class is invoked
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.height = height;
    this.contractAddress = contractAddress;
    this.contractDictionary = contractDictionary;
    this.tempContractData = null;
  }


  /**
   * NewContractTx Copy Constructor.
   *
   * @param toCopy : NewContractTx top copy.
   */
  public NewContractTx(NewContractTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.contractAddress = toCopy.getContractAddress();
    this.height = toCopy.getHeight();
    this.contractDictionary = toCopy.getContractData().encode();
    this.tempContractData = null;
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
    rVal.addAll(this.getContractData().addresses());
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.addAll(new Object[]{chainId, TXT.getId(), nonce, fromPubKey, timestamp, powerOfAttorney, this.getContractData().encodeToMapForTxParameter()});
    return hashList;
  }


  /**
   * Return this transaction as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {
    NewContractTx tx = this;
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
        tx.contractAddress,
        tx.getContractDictionary(),
        tx.signature,
        tx.height
    };
  }


  /**
   * getAuthoringAddress : Return Commitment authoring address.
   *
   * @return :
   */
  public String getAuthoringAddress() {
    return this.getFromAddress();
  }


  /**
   * getAuthoringPublicKey : Return Commitment authoring Public Key.
   *
   * @return :
   */
  public String getAuthoringPublicKey() {
    return this.getFromPublicKey();
  }


  public String getContractAddress() {
    return contractAddress;
  }


  /**
   * getContractData().
   * Return ContractData (IContractData) for this Transaction.
   *
   * @return :
   */
  public IContractData getContractData() {
    if (this.tempContractData == null) {
      this.tempContractData = NewContractTx.getContractDataFromDictionary(this.contractDictionary);
    }
    return tempContractData;
  }


  /**
   * Get the contract dictionary associated with this contract.
   *
   * @return the contract
   */
  public MPWrappedMap<String, Object> getContractDictionary() {
    return contractDictionary;
  }


  /**
   * getEffectiveAddress.
   *
   * @return : Authoring (From) Address.
   */
  public String getEffectiveAddress() {
    return this.getFromAddress();
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


  public void setContractData(IContractData data) {
    tempContractData = data;
    contractDictionary = data.encode();
  }


  /**
   * setContractDictionary().
   * Set ContractData (from Map) for this Transaction.
   *
   * @param contractDictionary :
   */
  public void setContractDictionary(MPWrappedMap<String, Object> contractDictionary) {
    this.contractDictionary = contractDictionary;
    this.tempContractData = null;
  }

}
