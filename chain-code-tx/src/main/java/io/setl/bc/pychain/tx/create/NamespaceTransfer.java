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

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.NamespaceTransferTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.TRANSFER_NAMESPACE, allOf = BaseTransaction.class)
@JsonDeserialize
public class NamespaceTransfer extends BaseTransaction {

  /**
   * namespaceTransferUnsigned().
   * <p>Create Unsigned NamespaceTransferTx</p>
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param nameSpace   :
   * @param toAddress   :
   * @param poa         :
   *
   * @return :
   */
  public static NamespaceTransferTx namespaceTransferUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String toAddress,
      String poa
  ) {
    NamespaceTransferTx rVal = new NamespaceTransferTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        toAddress,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonProperty("address")
  @Schema(description = "The current controlling address for the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String fromAddress;

  @JsonProperty("publicKey")
  @JsonAlias("frompublickey")
  @Schema(description = "Public key of the issuing address. Normally derived from the wallet.")
  @PublicKey
  private String fromPublicKey;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("namespace")
  @Schema(description = "The name of the namespace to delete.")
  @NotNull
  private String nameSpace;

  @JsonProperty("toAddress")
  @Schema(description = "The new controller for the namespace.")
  @NotNull
  @Address
  private String toAddress;

  public NamespaceTransfer() {
    // do nothing
  }


  /**
   * Construct a representation of the transaction.
   *
   * @param tx the transaction
   */
  public NamespaceTransfer(NamespaceTransferTx tx) {
    super(tx);
    setFromAddress(tx.getFromAddress());
    setMetadata(tx.getMetadata());
    setNameSpace(tx.getNameSpace());
    setFromPublicKey(tx.getFromPublicKey());
    setToAddress(tx.getToAddress());
  }


  @Override
  public NamespaceTransferTx create() {
    NamespaceTransferTx rVal = new NamespaceTransferTx(getChainId(), 4, getHash(), getNonce(), isUpdated(), getFromPublicKey(), getFromAddress(),
        getNameSpace(), getToAddress(), getSignature(), getHeight(), getPoa(), getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getFromPublicKey() {
    return fromPublicKey;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNameSpace() {
    return nameSpace;
  }


  @Nonnull
  @JsonIgnore
  @Hidden
  @Override
  public String getNonceAddress() {
    return fromAddress;
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getFromPublicKey();
  }


  public String getToAddress() {
    return toAddress;
  }


  @Override
  public TxType getTxType() {
    return TxType.TRANSFER_NAMESPACE;
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }


  public void setFromPublicKey(String fromPublicKey) {
    this.fromPublicKey = fromPublicKey;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setNameSpace(String nameSpace) {
    this.nameSpace = cleanString(nameSpace);
  }


  @Override
  public void setNoncePublicKey(String key) {
    setFromPublicKey(key);
  }


  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

}
