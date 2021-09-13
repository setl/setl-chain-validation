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
import io.setl.bc.pychain.state.tx.PoaIssuerTransferTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_TRANSFER_ASSET_AS_ISSUER, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaIssuerTransfer extends BaseIssuerTransfer {

  /**
   * Return a new, unsigned PoaIssuerTransferTx.
   *
   * @param chainID       : Chain to operate on.
   * @param nonce         : Tx Nonce.
   * @param fromPubKey    : Authoring (Issuer) Public key.
   * @param fromAddress   : Authoring (Issuer) Address.
   * @param poaAddress    :
   * @param poaReference  :
   * @param nameSpace     : Asset Namespace. (Must be controlled by 'fromPubKey'.
   * @param classId       : Asset Class.
   * @param sourceAddress : Address from which to move assets.
   * @param toChainId     : Destination Chain.
   * @param toAddress     : Destination Address.
   * @param amount        : Amount to move.
   * @param protocol      : Protocol, info only.
   * @param metadata      : metadata, info only.
   * @param poa           : POA.
   *
   * @return : new PoaIssuerTransferTx.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaIssuerTransferTx poaIssuerTransferUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String sourceAddress,
      int toChainId,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa) {

    PoaIssuerTransferTx rVal = new PoaIssuerTransferTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        classId,
        sourceAddress,
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


  public PoaIssuerTransfer() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaIssuerTransfer(PoaIssuerTransferTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
  }


  @Override
  public PoaIssuerTransferTx create() {
    PoaIssuerTransferTx rVal = new PoaIssuerTransferTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getPoaAddress(),
        getPoaReference(),
        getNameSpace(),
        getClassId(),
        getSourceAddress(),
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
    return TxType.POA_TRANSFER_ASSET_AS_ISSUER;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }

}
