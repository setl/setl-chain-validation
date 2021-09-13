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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_NOMINATE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.time.Instant;
import java.util.Map;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.bc.pychain.tx.Views.Input;
import io.setl.bc.pychain.tx.contractsign.DvpCommit;
import io.setl.bc.pychain.tx.contractsign.ExchangeCommit;
import io.setl.bc.pychain.tx.contractsign.Nominate;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.common.TypeSafeMap;

@Schema(name = TxExternalNames.COMMIT_TO_CONTRACT, allOf = BaseTransaction.class)
@JsonDeserialize
public class CommitToContract extends BaseCommitToContract {

  public static final String AUTO_SIGN = "autosign";


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
   * @param contractAddress    :
   * @param contractDictionary : Contract terms.
   * @param poa                : POA Data.
   *
   * @return : CommitToContractTx
   */
  public static CommitToContractTx newCommitmentUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      Object contractAddress,
      MPWrappedMap<String, Object> contractDictionary,
      String poa
  ) {

    CommitToContractTx rVal = new CommitToContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        contractAddress,
        contractDictionary,
        "",
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
   * @param contractAddress    : Authoring Address (MPWrappedMap)
   * @param contractDictionary : Contract terms (Map).
   * @param poa                : POA Data.
   *
   * @return : CommitToContractTx
   */
  public static CommitToContractTx newCommitmentUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      Object contractAddress,
      Map<String, Object> contractDictionary,
      String poa
  ) {

    CommitToContractTx rVal = new CommitToContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        contractAddress,
        new MPWrappedMap<>(contractDictionary),
        "",
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
   * @param contractAddress    :
   * @param contractDictionary : Contract terms (IContractData).
   * @param poa                : POA Data.
   *
   * @return : CommitToContractTx
   */
  public static CommitToContractTx newCommitmentUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      Object contractAddress,
      IContractData contractDictionary,
      String poa
  ) {

    CommitToContractTx rVal = new CommitToContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        contractAddress,
        contractDictionary.encode(),
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  // FIXME: duplication of code as in CommitToContractCreator... find way of avoiding duplication if possible
  public static void signCommitment(CommitToContractTx tx, WalletAddress walletAddress, AddressToKeysMapper mapper) throws InvalidTransactionException {
    IContractData contractData = tx.getCommitmentData();

    switch (contractData.getContractType()) {
      case CONTRACT_NAME_DVP_UK_COMMIT:
        DvpCommit.sign(
            mapper,
            (DvpUKCommitData) contractData,
            !tx.getContractAddress().isEmpty() ? tx.getContractAddress().get(0) : "",
            walletAddress,
            tx.isPOA()
        );
        break;
      case CONTRACT_NAME_DVP_UK:
        throw new InvalidTransactionException("DVP Contract data submitted when commitment data was required.");
      case CONTRACT_NAME_TOKENS_NOMINATE:
        throw new InvalidTransactionException("Nominate Contract data submitted when commitment data was required.");
      case CONTRACT_NAME_NOMINATE:
        Nominate.sign(tx, (NominateCommitData) contractData, mapper);
        break;
      case CONTRACT_NAME_EXCHANGE:
        throw new InvalidTransactionException("Exchange Contract data submitted when commitment data was required.");
      case CONTRACT_NAME_EXCHANGE_COMMIT:
        ExchangeCommit.sign(
            mapper,
            (ExchangeCommitData) contractData,
            (!tx.getContractAddress().isEmpty() ? tx.getContractAddress().get(0) : ""),
            tx.isPOA(),
            tx.getAuthoringAddress()
        );
        break;
      default:
        throw new InvalidTransactionException("Contract Entry : Unsupported Contract type  : " + contractData.getContractType());
    }

    tx.setCommitmentDictionary(contractData.encode());
    tx.setHash(Hash.computeHash(tx));
  }


  @JsonIgnore
  private AddressToKeysMapper addressToKeysMapper;

  @JsonProperty("contractData")
  @Schema(description = "The contract's data")
  private Map<String, Object> contractDictionary;

  @JsonProperty("signForWallet")
  @Schema(description = "If true, auto-sign the contract for all addresses in the issuing wallet. Otherwise, only auto-sign for the issuing address.")
  @JsonView(Input.class)
  private boolean signForWallet;

  @JsonIgnore
  private WalletAddress walletAddress;

  @JsonProperty("walletId")
  @JsonView(Input.class)
  private long walletId;


  public CommitToContract() {
    // do nothing
  }


  public CommitToContract(CommitToContractTx tx) throws InvalidTransactionException {
    super(tx);
    setContractDictionary(tx.getCommitmentDictionary().toMap());
  }


  @Override
  public CommitToContractTx create() {
    CommitToContractTx rVal = new CommitToContractTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getContractAddresses(),
        new MPWrappedMap<>(getContractDictionary()),
        getSignature(),
        getHeight(),
        getPoa(),
        getTimestamp()
    );

    TypeSafeMap dictionaryMap = new TypeSafeMap(getContractDictionary());
    boolean autoSign = dictionaryMap.getBoolean(AUTO_SIGN, true);
    WalletAddress myWalletAddress = getWalletAddress();

    if (autoSign && myWalletAddress != null) {
      try {
        AddressToKeysMapper mapper = getAddressToKeysMapper();
        signCommitment(rVal, myWalletAddress, mapper);
      } catch (InvalidTransactionException e) {
        throw new RuntimeException(e);
      }
    }

    return rVal;
  }


  public AddressToKeysMapper getAddressToKeysMapper() {
    return addressToKeysMapper;
  }


  public Map<String, Object> getContractDictionary() {
    return contractDictionary;
  }


  @Override
  @Nonnull
  public TxType getTxType() {
    return TxType.COMMIT_TO_CONTRACT;
  }


  public WalletAddress getWalletAddress() {
    return walletAddress;
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


  public void setSignForWallet(boolean signForWallet) {
    this.signForWallet = signForWallet;
  }


  public void setWalletAddress(WalletAddress walletAddress) {
    this.walletAddress = walletAddress;
  }


  public void setWalletId(long walletId) {
    this.walletId = walletId;
  }

}
