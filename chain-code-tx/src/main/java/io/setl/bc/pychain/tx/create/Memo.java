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
import io.setl.bc.pychain.state.tx.MemoTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.CREATE_MEMO, allOf = BaseTransaction.class)
@JsonDeserialize
public class Memo extends BaseTransaction {

  /**
   * memoUnsigned().
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  public static MemoTx memoUnsigned(int chainID, long nonce, String fromPubKey, String fromAddress, String metadata, String poa) {
    MemoTx rVal = new MemoTx(chainID, 4, nonce, false, fromPubKey, fromAddress, metadata, poa, Instant.now().getEpochSecond());
    rVal.setHash(Hash.computeHash(rVal));
    return rVal;
  }

  @JsonProperty("address")
  @Schema(description = "The address submitting the memo. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("metadata")
  @Schema(description = "The memo.")
  private String metadata = "";

  @JsonProperty("payload")
  @Schema(description = "Binary data associated with this memo, represented as MIME compliant Base64")
  private byte[] payload;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the submitting address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  public Memo() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public Memo(MemoTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setPublicKey(tx.getFromPublicKey());
    setPayload(tx.getPayload());
    setMetadata(tx.getMetadata());
  }


  @Override
  public MemoTx create() {
    MemoTx rVal = new MemoTx(getChainId(), 4, getHash(), getNonce(), isUpdated(), getPublicKey(), getAddress(), getPayload(), getMetadata(), getSignature(),
        getHeight(), getPoa(), getTimestamp()
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


  public byte[] getPayload() {
    return (payload != null) ? payload.clone() : null;
  }


  public String getPublicKey() {
    return publicKey;
  }


  @Override
  public TxType getTxType() {
    return TxType.CREATE_MEMO;
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


  public void setPayload(byte[] payload) {
    this.payload = (payload != null) ? payload.clone() : null;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

}
