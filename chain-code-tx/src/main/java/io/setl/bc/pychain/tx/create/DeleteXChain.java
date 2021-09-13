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
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.DeleteXChainTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.REMOVE_X_CHAIN, allOf = BaseTransaction.class)
@JsonDeserialize
public class DeleteXChain extends BaseTransaction {

  /**
   * deleteXChainUnsigned().
   * <p>Create Unsigned DeleteXChainTx</p>
   *
   * @param baseChainID :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param chainId     :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  public static DeleteXChainTx deleteXChainUnsigned(
      int baseChainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      int chainId,
      String metadata,
      String poa
  ) {

    DeleteXChainTx rVal = new DeleteXChainTx(
        baseChainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        chainId,
        "",
        -1,
        poa,
        metadata,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonAlias("fromaddress")
  @JsonProperty("address")
  @Schema(description = "The issuing address. Must be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonAlias("frompubkey")
  @JsonProperty("publicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("xChainId")
  @Schema(description = "The ID of the remote chain.")
  private int xChainId;

  public DeleteXChain() {
    // do nothing
  }


  /**
   * Construct a representation of the transaction.
   *
   * @param tx the transaction
   */
  public DeleteXChain(DeleteXChainTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setPublicKey(tx.getFromPublicKey());
    setMetadata(tx.getMetadata());
    setXChainId(tx.getXChainId());
  }


  @Override
  public DeleteXChainTx create() {
    DeleteXChainTx rVal = new DeleteXChainTx(getChainId(), getHash(), getNonce(), isUpdated(), getPublicKey(), getAddress(), getXChainId(), getSignature(),
        getHeight(), getPoa(), getMetadata(), getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public String getAddress() {
    return address;
  }


  public String getMetadata() {
    return metadata;
  }


  @Nonnull
  @JsonIgnore
  @Hidden
  @Override
  public String getNonceAddress() {
    return address;
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getPublicKey();
  }


  public String getPublicKey() {
    return publicKey;
  }


  @Override
  public TxType getTxType() {
    return TxType.REMOVE_X_CHAIN;
  }


  public int getXChainId() {
    return xChainId;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setXChainId(int xChainId) {
    this.xChainId = xChainId;
  }

}
