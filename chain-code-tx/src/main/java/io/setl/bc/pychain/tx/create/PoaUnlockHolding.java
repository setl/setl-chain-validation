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
import io.setl.bc.pychain.state.tx.PoaUnlockHoldingTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import javax.validation.constraints.NotNull;

/**
 * PoaUnlockHolding.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_UNLOCK_ASSET_HOLDING, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaUnlockHolding extends BaseUnlockHolding {

  /**
   * UnEncumberTx.
   * <p>Return unsigned UnEncumberTx transaction.</p>
   *
   * @param chainId        : Chain to apply Tx to.
   * @param nonce          : Tx nonce to use.
   * @param fromPubKey     : Authoring Public Key.
   * @param fromAddress    : Authoring Address.
   * @param poaAddress     : Effective Address
   * @param poaReference   : poa Reference
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
  public static PoaUnlockHoldingTx poaUnlockHoldingUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa

  ) {

    PoaUnlockHoldingTx tx = new PoaUnlockHoldingTx(chainId,
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


  @JsonProperty("poaAddress")
  @Schema(description = "Attorney's address")
  @NotNull
  @Address
  private String poaAddress;

  @JsonProperty("poaReference")
  @Schema(description = "Attorney's reference for this transaction.")
  @NotNull
  private String poaReference;


  public PoaUnlockHolding() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaUnlockHolding(PoaUnlockHoldingTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
  }


  @Override
  public PoaUnlockHoldingTx create() {
    PoaUnlockHoldingTx rVal = new PoaUnlockHoldingTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getPoaAddress(),
        getPoaReference(),
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


  public String getPoaAddress() {
    return poaAddress;
  }


  public String getPoaReference() {
    return poaReference;
  }


  @Override
  public TxType getTxType() {
    return TxType.POA_UNLOCK_ASSET_HOLDING;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }


}
