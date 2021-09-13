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
import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * NullTX.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.DO_NOTHING, allOf = BaseTransaction.class)
@JsonDeserialize
public class NullTX extends BaseTransaction {

  /**
   * memoUnsigned().
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param poa         :
   *
   * @return :
   */
  public static NullTx nullUnsigned(int chainID, long nonce, String fromPubKey, String fromAddress, String poa) {
    NullTx rVal = new NullTx(chainID, 4, "", nonce, false, fromPubKey, fromAddress, "", -1, poa, Instant.now().getEpochSecond());
    rVal.setHash(Hash.computeHash(rVal));
    return rVal;
  }

  @JsonProperty("address")
  @Schema(description = "The address submitting the memo. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the submitting address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  public NullTX() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public NullTX(NullTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setPublicKey(tx.getFromPublicKey());
  }


  @Override
  public NullTx create() {
    NullTx rVal = new NullTx(getChainId(), 4, getHash(), getNonce(), isUpdated(), getPublicKey(), getAddress(), getSignature(), getHeight(), getPoa(),
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
    return TxType.DO_NOTHING;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

}