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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;

@Schema(name = TxExternalNames.NEW_CONTRACT, allOf = BaseTransaction.class)
@JsonDeserialize
public class NewContract extends BaseNewContract {

  /**
   * newContractUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         : Authoring Public key.
   * @param fromAddress        : Authoring Address (MPWrappedMap)
   * @param contractDictionary : Contract terms.
   * @param poa                : POA Data.
   *
   * @return : NewContractTx
   */
  public static NewContractTx newContractUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      MPWrappedMap<String, Object> contractDictionary,
      String poa
  ) {

    NewContractTx rVal = new NewContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
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
   * newContractUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         :  Authoring Public key.
   * @param fromAddress        : Authoring Address
   * @param contractDictionary : Contract terms (Map).
   * @param poa                : POA Data.
   *
   * @return : NewContractTx
   */
  public static NewContractTx newContractUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      Map<String, Object> contractDictionary,
      String poa
  ) {

    NewContractTx rVal = new NewContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        new MPWrappedMap<String, Object>(contractDictionary),
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  /**
   * newContractUnsigned.
   * <p>Create a new contract using the given parameters and contract data.</p>
   * <p>Note that the contract type must be encoded in the contractDictionary under the
   * '__function' or 'contractfunction' key. '__function' would take priority.</p>
   *
   * @param chainID            : Blockchain ID.
   * @param nonce              : Authoring address Tx nonce.
   * @param fromPubKey         :  Authoring Public key.
   * @param fromAddress        : Authoring Address
   * @param contractDictionary : Contract terms (IContractData).
   * @param poa                : POA Data.
   *
   * @return : NewContractTx
   */
  public static NewContractTx newContractUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      IContractData contractDictionary,
      String poa
  ) {

    NewContractTx rVal = new NewContractTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        contractDictionary.encode(),
        "",
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


  public NewContract() {
    // do nothing
  }


  public NewContract(NewContractTx tx) {
    super(tx);
    setContractData(tx.getContractDictionary().toMap());
  }


  @Override
  public NewContractTx create() {
    NewContractTx rVal = new NewContractTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        new MPWrappedMap<>(getContractData()),
        getSignature(),
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


  @Override
  public TxType getTxType() {
    return TxType.NEW_CONTRACT;
  }


  public void setContractData(Map<String, Object> contractData) {
    this.contractData = contractData;
  }

}
