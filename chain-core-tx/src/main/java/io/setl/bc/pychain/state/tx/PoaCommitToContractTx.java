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
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PoaCommitToContractTx extends CommitToContractTx implements HasProtocol, CommitContractInterface {


  private static final TxType TXT = TxType.POA_COMMIT_TO_CONTRACT;

  private static final Logger logger = LoggerFactory.getLogger(PoaCommitToContractTx.class);


  /**
   * Accept an Object[] and return a PoaCommitToContractTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned PoaCommitToContractTx object
   */
  public static PoaCommitToContractTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a PoaCommitToContractTx.
   *
   * @param txData : The input MPWrappedArray object
   *
   * @return : The constructed PoaCommitToContractTx object
   */
  public static PoaCommitToContractTx decodeTX(MPWrappedArray txData) {

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

    return new PoaCommitToContractTx(chainId,
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
   * Private Constructor.
   * <p>Return new PoaCommitToContractTx object.
   * Contract Address is provided.</p>
   *
   * @param chainId              : Blockchain chainID
   * @param hash                 : hash
   * @param nonce                : nonce
   * @param updated              : updated flag (Sucessfull application to state).
   * @param fromPubKey           : Authoring Public Key
   * @param fromAddress          : Authoring address
   * @param poaAddress           :
   * @param poaReference         :
   * @param contractAddress      : Contract Address(es).
   * @param commitmentDictionary : contractDictionary
   * @param signature            : signature
   * @param protocol             :
   * @param metadata             :
   * @param height               : height
   * @param poa                  : Power of Attorney
   * @param timestamp            : timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaCommitToContractTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      Object contractAddress,
      MPWrappedMap<String, Object> commitmentDictionary,
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
        contractAddress,
        commitmentDictionary,
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
   * PoaCommitToContractTx Copy Constructor.
   *
   * @param toCopy : PoaCommitToContractTx top copy.
   */
  public PoaCommitToContractTx(PoaCommitToContractTx toCopy) {

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

    Set<String> rVal = super.addresses();

    if ((this.poaAddress != null) && (!this.poaAddress.isEmpty())) {
      rVal.add(this.poaAddress);
    }

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
        (contractAddress.size() == 1 ? contractAddress.get(0) : contractAddress.toArray()),
        protocol,
        metadata,
        this.getCommitmentData().encodeToMapForTxParameter()
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

    PoaCommitToContractTx tx = this;

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
        (tx.contractAddress.size() == 1 ? tx.contractAddress.get(0) : tx.contractAddress.toArray()),
        tx.commitmentDictionary,
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


  @Override
  public boolean isPOA() {
    return true;
  }

}
