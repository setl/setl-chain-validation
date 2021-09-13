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
package io.setl.bc.pychain.tx.create;

import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaNewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_NEW_CONTRACT, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaNewContract extends BaseNewContract {

  /**
   * poaNewContractUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         : Authoring Public key.
   * @param fromAddress        : Authoring Address (MPWrappedMap)
   * @param poaAddress         : Effective Address
   * @param poaReference       : poa Reference
   * @param contractDictionary : Contract terms.
   * @param protocol           : Txn protocol
   * @param metadata           :
   * @param poa                : POA Data.
   *
   * @return : PoaNewContractTx
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaNewContractTx poaNewContractUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      MPWrappedMap<String, Object> contractDictionary,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaNewContractTx rVal = new PoaNewContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractDictionary,
        "",
        protocol,
        metadata,
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  /**
   * poaNewContractUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         :  Authoring Public key.
   * @param fromAddress        : Authoring Address
   * @param poaAddress         : Effective Address
   * @param poaReference       : poa Reference
   * @param contractDictionary : Contract terms (Map).
   * @param protocol           : Txn protocol
   * @param metadata           :
   * @param poa                : POA Data.
   *
   * @return : PoaNewContractTx
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaNewContractTx poaNewContractUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      Map<String, Object> contractDictionary,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaNewContractTx rVal = new PoaNewContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        new MPWrappedMap<String, Object>(contractDictionary),
        "",
        protocol,
        metadata,
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  /**
   * poaNewContractUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         :  Authoring Public key.
   * @param fromAddress        : Authoring Address
   * @param poaAddress         : Effective Address
   * @param poaReference       : poa Reference
   * @param contractDictionary : Contract terms (IContractData).
   * @param protocol           : Txn protocol
   * @param metadata           :
   * @param poa                : POA Data.
   *
   * @return : PoaNewContractTx
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaNewContractTx poaNewContractUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      IContractData contractDictionary,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaNewContractTx rVal = new PoaNewContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractDictionary.encode(),
        "",
        protocol,
        metadata,
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonProperty("contractData")
  @Schema(description = "The contract details.")
  private Map<String, Object> contractData;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("poaAddress")
  @Schema(description = "Attorney's address")
  @NotNull
  @Address
  private String poaAddress;

  @JsonProperty("poaReference")
  @Schema(description = "Attorney's reference for this transaction.")
  @NotNull
  private String poaReference;

  @JsonProperty("protocol")
  @Schema(description = "Protocol associated with this commitment.")
  private String protocol;

  public PoaNewContract() {
    // do nothing
  }

  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaNewContract(PoaNewContractTx tx) {
    super(tx);
    setContractData(tx.getContractDictionary().toMap());
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
    setProtocol(tx.getProtocol());
    setMetadata(tx.getMetadata());
  }


  @Override
  public PoaNewContractTx create() {
    PoaNewContractTx rVal = new PoaNewContractTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getPoaAddress(),
        getPoaReference(),
        new MPWrappedMap<String, Object>(getContractData()),
        getSignature(),
        getProtocol(),
        getMetadata(),
        getHeight(),
        getPoa(),
        getTimestamp()
    );
    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public Map<String, Object> getContractData() {
    return contractData;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getPoaAddress() {
    return poaAddress;
  }


  public String getPoaReference() {
    return poaReference;
  }


  public String getProtocol() {
    return protocol;
  }


  @Override
  public TxType getTxType() {
    return TxType.POA_NEW_CONTRACT;
  }


  public void setContractData(Map<String, Object> contractData) {
    this.contractData = contractData;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

}
