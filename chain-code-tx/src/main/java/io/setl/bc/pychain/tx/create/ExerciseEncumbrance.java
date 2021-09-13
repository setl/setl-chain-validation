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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.ExerciseEncumbranceTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = TxExternalNames.EXERCISE_ENCUMBRANCE, allOf = BaseTransaction.class)
@JsonDeserialize
public class ExerciseEncumbrance extends BaseExerciseEncumbrance {

  /**
   * exerciseEncumbranceUnsigned.
   * <p>Return new unsigned instance of an ExerciseEncumbranceTx transaction.</p>
   *
   * @param chainId        : Blockchain ID.
   * @param nonce          : Authoring Address nonce.
   * @param fromPubKey     : Authoring Public key
   * @param fromAddress    : Authoring Address
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
   * @return : ExerciseEncumbranceTx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static ExerciseEncumbranceTx exerciseEncumbranceUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
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

    ExerciseEncumbranceTx tx = new ExerciseEncumbranceTx(chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
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


  public ExerciseEncumbrance() {
    // do nothing
  }


  public ExerciseEncumbrance(ExerciseEncumbranceTx tx) {
    super(tx);
  }


  @Override
  public ExerciseEncumbranceTx create() {
    ExerciseEncumbranceTx rVal = new ExerciseEncumbranceTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
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


  @Override
  public TxType getTxType() {
    return TxType.EXERCISE_ENCUMBRANCE;
  }

}
