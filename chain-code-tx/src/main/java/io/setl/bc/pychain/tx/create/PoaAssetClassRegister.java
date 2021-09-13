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

import static io.setl.common.StringUtils.cleanString;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAssetClassRegisterTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_REGISTER_ASSET_CLASS, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaAssetClassRegister extends BaseAssetClassRegister {

  /**
   * poaAssetClassRegisterUnsigned().
   * <p>Create an unsigned PoaAssetClassRegisterTx</p>
   *
   * @param chainID      :
   * @param nonce        :
   * @param fromPubKey   :
   * @param fromAddress  :
   * @param poaAddress   : Effective Address
   * @param poaReference : poa Reference
   * @param nameSpace    :
   * @param classId      :
   * @param protocol     : Txn protocol
   * @param metadata     :
   * @param poa          :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaAssetClassRegisterTx poaAssetClassRegisterUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaAssetClassRegisterTx rVal = new PoaAssetClassRegisterTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        cleanString(nameSpace),
        cleanString(classId),
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
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

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to delete the asset.")
  private String protocol;


  public PoaAssetClassRegister() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaAssetClassRegister(PoaAssetClassRegisterTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
    setProtocol(tx.getProtocol());
  }


  @Override
  public PoaAssetClassRegisterTx create() {
    PoaAssetClassRegisterTx rVal = new PoaAssetClassRegisterTx(
        getChainId(),
        4,
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getPoaAddress(),
        getPoaReference(),
        getNameSpace(),
        getClassId(),
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


  public String getProtocol() {
    return protocol;
  }


  @Override
  public TxType getTxType() {
    return TxType.POA_REGISTER_ASSET_CLASS;
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
