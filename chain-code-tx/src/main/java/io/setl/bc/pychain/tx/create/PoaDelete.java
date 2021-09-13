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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaDeleteTx;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.REVOKE_POA, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaDelete extends BaseTransaction {

  /**
   * Return a new, unsigned PoaAddTx.
   *
   * @param chainID        :
   * @param nonce          :
   * @param fromPubKey     :
   * @param fromAddress    :
   * @param issuingAddress :
   * @param reference      :
   * @param protocol       :
   * @param metadata       :
   * @param poa            :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaDeleteTx poaDeleteUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String issuingAddress,
      String reference,
      String protocol,
      String metadata,
      String poa
  ) {

    if (!AddressUtil.verify(fromAddress, fromPubKey, AddressType.NORMAL)) {
      return null;
    }

    PoaDeleteTx rVal = new PoaDeleteTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        issuingAddress,
        reference,
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

  @JsonProperty("address")
  @Schema(description = "The address submitting the memo. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("metadata")
  @Schema(description = "Additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("poaAddress")
  @Schema(description = "Attorney's address")
  @NotNull
  @Address
  private String poaAddress;

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to delete the attorney.")
  private String protocol;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the submitting address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("reference")
  @Schema(description = "Reference for this transaction.")
  @NotNull
  private String reference;

  public PoaDelete() {
    // do nothing
  }


  /**
   * Construct a representation of the transaction.
   *
   * @param tx the transaction
   */
  public PoaDelete(PoaDeleteTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setMetadata(tx.getMetadata());
    setPoaAddress(tx.getIssuingAddress());
    setReference(tx.getReference());
    setProtocol(tx.getProtocol());
    setPublicKey(tx.getFromPublicKey());
  }


  @Override
  public PoaDeleteTx create() {
    PoaDeleteTx rVal = new PoaDeleteTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getPoaAddress(),
        getReference(),
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


  public String getPoaAddress() {
    return poaAddress;
  }


  public String getProtocol() {
    return protocol;
  }


  public String getPublicKey() {
    return publicKey;
  }


  public String getReference() {
    return reference;
  }


  @Override
  public TxType getTxType() {
    return TxType.REVOKE_POA;
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


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setReference(String reference) {
    this.reference = reference;
  }

}
