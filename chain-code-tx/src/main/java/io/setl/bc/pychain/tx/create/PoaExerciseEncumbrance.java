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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaExerciseEncumbranceTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_EXERCISE_ENCUMBRANCE, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaExerciseEncumbrance extends BaseExerciseEncumbrance {

  /**
   * exerciseEncumbranceUnsigned.
   * <p>Return new unsigned instance of an PoaExerciseEncumbranceTx transaction.</p>
   *
   * @param chainId        : Blockchain ID.
   * @param nonce          : Authoring Address nonce.
   * @param fromPubKey     : Txn from Public Key
   * @param fromAddress    : Address Exercising the POA
   * @param poaAddress     : Effective Address Exercising the Encumbrance
   * @param poaReference   : poa Reference
   * @param nameSpace      : Namespace od asset to encumber.
   * @param classId        : Class of asset to encumber.
   * @param subjectAddress : Address against which the encumbrance is registered.
   * @param reference      : Encumbrance reference.
   * @param toChainId      : Chain to which payment will be made.
   * @param toAddress      : Address to which payment will be made.
   * @param amount         : Amount of encumbrance to be exercised. Note that this may not exceed the encumbrance amount.
   * @param protocol       : Info only .
   * @param metadata       : Info only.
   * @param poa            : POA data.
   *
   * @return : PoaExerciseEncumbranceTx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaExerciseEncumbranceTx exerciseEncumbranceUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String subjectAddress,
      String reference,
      int toChainId,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaExerciseEncumbranceTx tx = new PoaExerciseEncumbranceTx(chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        subjectAddress,
        nameSpace,
        classId,
        reference,
        toChainId,
        toAddress,
        amount,
        "",
        protocol,
        metadata,
        -1,
        poa,
        Instant.now().getEpochSecond()
    );
    tx.setHash(Hash.computeHash(tx));
    return tx;
  }


  @JsonProperty("poaAddress")
  @Schema(description = "Attorney's address")
  @NotNull
  @Address
  private String poaAddress;

  @JsonProperty("poaReference")
  @Schema(description = "Attorney's reference for this transaction.")
  @NotNull
  private String poaReference;


  public PoaExerciseEncumbrance() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaExerciseEncumbrance(PoaExerciseEncumbranceTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
  }


  @Override
  public PoaExerciseEncumbranceTx create() {
    PoaExerciseEncumbranceTx rVal = new PoaExerciseEncumbranceTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getPoaAddress(),
        getPoaReference(),
        getSubjectAddress(),
        getNameSpace(),
        getClassId(),
        getReference(),
        getToChainId(),
        getToAddress(),
        getAmount(),
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


  public String getPoaAddress() {
    return poaAddress;
  }


  public String getPoaReference() {
    return poaReference;
  }


  @Override
  public TxType getTxType() {
    return TxType.POA_EXERCISE_ENCUMBRANCE;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }
}
