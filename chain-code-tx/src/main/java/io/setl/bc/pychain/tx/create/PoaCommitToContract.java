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
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaCommitToContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.tx.Views.Input;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_COMMIT_TO_CONTRACT, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaCommitToContract extends BaseCommitToContract {

  /**
   * newCommitmentUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         : Authoring Public key.
   * @param fromAddress        : Authoring Address (MPWrappedMap)
   * @param poaAddress         :
   * @param poaReference       :
   * @param contractAddress    : Authoring Address (MPWrappedMap)
   * @param contractDictionary : Contract terms.
   * @param protocol           :
   * @param metadata           :
   * @param poa                : POA Data.
   *
   * @return : PoaCommitToContractTx
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaCommitToContractTx newCommitmentUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      Object contractAddress,
      MPWrappedMap<String, Object> contractDictionary,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaCommitToContractTx rVal = new PoaCommitToContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
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
   * newCommitmentUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         :  Authoring Public key.
   * @param fromAddress        : Authoring Address
   * @param poaAddress         :
   * @param poaReference       :
   * @param contractAddress    : Authoring Address (MPWrappedMap)
   * @param contractDictionary : Contract terms (Map).
   * @param protocol           :
   * @param metadata           :
   * @param poa                : POA Data.
   *
   * @return : PoaCommitToContractTx
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaCommitToContractTx newCommitmentUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      Object contractAddress,
      Map<String, Object> contractDictionary,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaCommitToContractTx rVal = new PoaCommitToContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
        new MPWrappedMap<>(contractDictionary),
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
   * newCommitmentUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         :  Authoring Public key.
   * @param fromAddress        : Authoring Address
   * @param poaAddress         :
   * @param poaReference       :
   * @param contractAddress    : Authoring Address (MPWrappedMap)
   * @param contractDictionary : Contract terms (IContractData).
   * @param protocol           :
   * @param metadata           :
   * @param poa                : POA Data.
   *
   * @return : PoaCommitToContractTx
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaCommitToContractTx newCommitmentUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      Object contractAddress,
      IContractData contractDictionary,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaCommitToContractTx rVal = new PoaCommitToContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
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

  @JsonIgnore
  private AddressToKeysMapper addressToKeysMapper;

  @JsonProperty("contractData")
  @Schema(description = "The contract's data")
  private Map<String, Object> contractDictionary;

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

  @JsonProperty("signForWallet")
  @Schema(description = "If true, auto-sign the contract for all addresses in the issuing wallet. Otherwise, only auto-sign for the issuing address.")
  @JsonView(Input.class)
  private boolean signForWallet;

  @JsonProperty("walletId")
  @JsonView(Input.class)
  private long walletId;


  public PoaCommitToContract() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaCommitToContract(PoaCommitToContractTx tx) throws InvalidTransactionException {
    super(tx);
    setContractDictionary(tx.getCommitmentDictionary().toMap());
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
    setProtocol(tx.getProtocol());
    setMetadata(tx.getMetadata());
  }


  @Override
  public PoaCommitToContractTx create() {
    PoaCommitToContractTx rVal = new PoaCommitToContractTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getPoaAddress(),
        getPoaReference(),
        getContractAddresses(),
        new MPWrappedMap<>(getContractDictionary()),
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


  public AddressToKeysMapper getAddressToKeysMapper() {
    return addressToKeysMapper;
  }


  public Map<String, Object> getContractDictionary() {
    return contractDictionary;
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
  @Nonnull
  public TxType getTxType() {
    return TxType.POA_COMMIT_TO_CONTRACT;
  }


  public long getWalletId() {
    return walletId;
  }


  public boolean isSignForWallet() {
    return signForWallet;
  }


  public void setAddressToKeysMapper(AddressToKeysMapper addressToKeysMapper) {
    this.addressToKeysMapper = addressToKeysMapper;
  }


  public void setContractDictionary(Map<String, Object> contractDictionary) {
    this.contractDictionary = contractDictionary;
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


  public void setSignForWallet(boolean signForWallet) {
    this.signForWallet = signForWallet;
  }


  public void setWalletId(long walletId) {
    this.walletId = walletId;
  }

}
