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
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.UnlockHoldingTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = TxExternalNames.UNLOCK_ASSET_HOLDING, allOf = BaseTransaction.class)
@JsonDeserialize
public class UnlockHolding extends BaseUnlockHolding {

  /**
   * UnEncumberTx.
   * <p>Return unsigned UnEncumberTx transaction.</p>
   *
   * @param chainId        : Chain to apply Tx to.
   * @param nonce          : Tx nonce to use.
   * @param fromPubKey     : Authoring Public Key.
   * @param fromAddress    : Authoring Address.
   * @param nameSpace      : Namespace of asset to unencumber.
   * @param classId        : Class of asset to unencumber.
   * @param subjectAddress : Addreess against which encumbrance is held.
   * @param amount         : Amount to Un-Encumber.
   * @param protocol       : Info only.
   * @param metadata       : Info only.
   * @param poa            : POA data.
   *
   * @return : UnEncumberTx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static UnlockHoldingTx unlockHoldingUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa

  ) {

    UnlockHoldingTx tx = new UnlockHoldingTx(chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    tx.setHash(Hash.computeHash(tx));
    return tx;
  }


  public UnlockHolding() {
    // do nothing
  }


  public UnlockHolding(UnlockHoldingTx tx) {
    super(tx);
  }


  @Override
  public UnlockHoldingTx create() {
    UnlockHoldingTx rVal = new UnlockHoldingTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getSubjectAddress(),
        getNameSpace(),
        getClassId(),
        getAmount(),
        getProtocol(),
        getMetadata(),
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


  @Override
  public TxType getTxType() {
    return TxType.UNLOCK_ASSET_HOLDING;
  }
}
