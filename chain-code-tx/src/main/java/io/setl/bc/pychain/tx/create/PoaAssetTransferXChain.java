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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAssetTransferXChainTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@JsonClassDescription("Transfer an asset to another block chain under the power of an attorney.")
@Schema(name = TxExternalNames.POA_TRANSFER_ASSET_X_CHAIN,
    description = "Transfer an asset to another block chain under the power of an attorney.",
    allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaAssetTransferXChain extends BaseAssetTransfer {

  /**
   * poaAssetTransferXChainUnsigned().
   * <p>Create an unsigned XChain Asset Transfer transaction</p>
   *
   * @param chainID      :
   * @param nonce        :
   * @param fromPubKey   :
   * @param fromAddress  :
   * @param poaAddress   : Effective Address
   * @param poaReference : poa Reference
   * @param nameSpace    :
   * @param classId      :
   * @param toChainId    :
   * @param toAddress    :
   * @param amount       :
   * @param protocol     :
   * @param metadata     :
   * @param poa          :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaAssetTransferXChainTx poaAssetTransferXChainUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      int toChainId,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaAssetTransferXChainTx rVal = new PoaAssetTransferXChainTx(
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

  @JsonProperty("tochainid")
  @Schema(description = "ID of the destination chain.")
  private int toChainId = -1;


  public PoaAssetTransferXChain() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaAssetTransferXChain(PoaAssetTransferXChainTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
    setToChainId(tx.getToChainId());
  }


  @Override
  public PoaAssetTransferXChainTx create() {
    int actualToChain = (toChainId != -1) ? toChainId : getChainId();
    PoaAssetTransferXChainTx rVal = new PoaAssetTransferXChainTx(
        getChainId(),
        4,
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getPoaAddress(),
        getPoaReference(),
        getNameSpace(),
        getClassId(),
        actualToChain,
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


  public int getToChainId() {
    return toChainId;
  }


  @Override
  public TxType getTxType() {
    return TxType.POA_TRANSFER_ASSET_X_CHAIN;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }


  public void setToChainId(int toChainId) {
    this.toChainId = toChainId;
  }

}
