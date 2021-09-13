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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_NOMINATE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.NewContractTx.UnsupportedContractException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;

public class CommitToContractTx extends AbstractTx implements CommitContractInterface {

  public static final TxType TXT = TxType.COMMIT_TO_CONTRACT;

  private static final Logger logger = LoggerFactory.getLogger(CommitToContractTx.class);


  public static CommitToContractTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept mpWrappedArray object and use it to create CommitToContractTx.
   *
   * @param vat : The MPWrappedArray input object
   *
   * @return : The CommitToContractTx which is created
   */
  public static CommitToContractTx decodeTX(MPWrappedArray vat) {
    logger.info("CommitToContractTx:{}", vat);
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
      return null;
    }
    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    Object contractAddress = vat.get(10);
    MPWrappedMap<String, Object> newContractData = vat.asWrappedMap(11);
    String signature = vat.asString(12);
    int height = -1;

    if (vat.size() > 13) {
      height = vat.asInt(13);
    }

    return new CommitToContractTx(chainId, hash, nonce, updated, fromPubKey, fromAddress, contractAddress, newContractData, signature, height, poa, timestamp);
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
    return getContractDataFromDictionary(new MPWrappedMap<>(thisDictionary), functionType);
  }


  /**
   * getContractDataFromDictionary().
   *
   * @param thisDictionary :
   *
   * @return :
   */
  public static IContractData getContractDataFromDictionary(MPWrappedMap<String, Object> thisDictionary) {
    return getContractDataFromDictionary(thisDictionary, "NotType");
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
    final String thisFunctionType = NewContractTx.getContractFunction(thisDictionary, functionType);
    switch (thisFunctionType) {
      case CONTRACT_NAME_DVP_UK:
      case CONTRACT_NAME_DVP_UK_COMMIT:
        return new DvpUKCommitData(thisDictionary);
      case CONTRACT_NAME_TOKENS_NOMINATE:
      case CONTRACT_NAME_NOMINATE:
        return new NominateCommitData(thisDictionary);
      case CONTRACT_NAME_EXCHANGE:
      case CONTRACT_NAME_EXCHANGE_COMMIT:
        return new ExchangeCommitData(thisDictionary);
      default:
        throw new UnsupportedContractException("Contract Entry : Unsupported Contract type  : " + thisFunctionType);
    }
  }


  protected MPWrappedMap<String, Object> commitmentDictionary;

  protected List<String> contractAddress = null;

  private IContractData tempCommitmentData;


  /**
   * Constructor.
   *
   * @param chainId         : Txn blockchain ID
   * @param hash            : Txn hash
   * @param nonce           : Txn nonce
   * @param updated         : Txn updated
   * @param fromAddress     : Txn from address
   * @param fromPubKey      : Txn from Public Key
   * @param contractAddress : Txn contract address(es)
   * @param newContractData : Txn new contract data
   * @param signature       : Txn signature
   * @param height          : Txn height
   * @param poa             : Txn Power of Attorney
   * @param timestamp       : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public CommitToContractTx(
      int chainId, String hash, long nonce, boolean updated, String fromPubKey, String fromAddress, Object contractAddress,
      MPWrappedMap<String, Object> newContractData, String signature, int height, String poa, long timestamp
  ) {
    //  ensure parent class constructor is invoked
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    if (contractAddress instanceof String) {
      this.contractAddress = Collections.singletonList((String) contractAddress);
    } else if (contractAddress instanceof List) {
      this.contractAddress = new ArrayList<>(((List) contractAddress).size());
      for (Object thisAddress : (List) contractAddress) {
        if (thisAddress instanceof String) {
          this.contractAddress.add((String) thisAddress);
        }
      }
    } else if (contractAddress instanceof Object[]) {
      this.contractAddress = new ArrayList<>(((Object[]) contractAddress).length);
      for (Object thisAddress : (Object[]) contractAddress) {
        if (thisAddress instanceof String) {
          this.contractAddress.add((String) thisAddress);
        }
      }
    }
    if (this.contractAddress == null) {
      this.contractAddress = new ArrayList<>(0);
    }
    this.commitmentDictionary = newContractData;
    this.height = height;
    this.tempCommitmentData = null;
  }


  /**
   * NewContractTx Copy Constructor.
   *
   * @param toCopy : NewContractTx top copy.
   */
  public CommitToContractTx(CommitToContractTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.contractAddress = new ArrayList<>(toCopy.getContractAddress());
    this.height = toCopy.getHeight();
    this.tempCommitmentData = null;
    this.commitmentDictionary = toCopy.getCommitmentData().encode();
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
    rVal.addAll(this.getCommitmentData().addresses());
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.addAll(new Object[]{
        chainId, TXT.getId(), nonce, fromPubKey, timestamp, powerOfAttorney,
        (contractAddress.size() == 1 ? contractAddress.get(0) : contractAddress.toArray()), this.getCommitmentData().encodeToMapForTxParameter()
    });
    return hashList;
  }


  @Override
  public Object[] encodeTx() {
    CommitToContractTx tx = this;
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
        (tx.contractAddress.size() == 1 ? tx.contractAddress.get(0) : tx.contractAddress.toArray()),
        tx.commitmentDictionary,
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


  /**
   * getCommitmentData().
   *
   * @return :
   */
  public IContractData getCommitmentData() {
    if (this.tempCommitmentData == null) {
      this.tempCommitmentData = CommitToContractTx.getContractDataFromDictionary(this.commitmentDictionary);
    }
    return tempCommitmentData;
  }


  public MPWrappedMap<String, Object> getCommitmentDictionary() {
    return commitmentDictionary;
  }


  public List<String> getContractAddress() {
    return contractAddress;
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


  public void setCommitmentData(IContractData data) {
    tempCommitmentData = data;
    commitmentDictionary = data.encode();
  }


  /**
   * setCommitmentDictionary().
   *
   * @param contractDictionary :
   */
  public void setCommitmentDictionary(MPWrappedMap<String, Object> contractDictionary) {
    this.commitmentDictionary = contractDictionary;
    this.tempCommitmentData = null;
  }

}
