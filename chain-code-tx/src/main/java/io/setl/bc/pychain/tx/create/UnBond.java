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

import java.math.BigInteger;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.UnBondTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * UnBond.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.REVOKE_VOTING_POWER, allOf = BaseTransaction.class)
@JsonDeserialize
public class UnBond extends BaseTransaction {

  /**
   * Return a new, unsigned UnBondTx.
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param toAddress   :
   * @param amount      :
   * @param protocol    :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  public static UnBondTx unBondUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa
  ) {
    UnBondTx rVal = new UnBondTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
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

  @JsonProperty("address")
  @Schema(description = "The address performing the un-bonding. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("amount")
  @Schema(description = "Amount of asset to un-bond", format = "int64")
  @Min(0)
  private BigInteger amount = BigInteger.ZERO;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to implement the transfer.")
  private String protocol = "";

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the un-bonding address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("toAddress")
  @Schema(description = "The recipient address for the undonded stake.")
  @NotNull
  @Address
  private String toAddress;

  public UnBond() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public UnBond(UnBondTx tx) {
    super(tx);
    setPublicKey(tx.getFromPublicKey());
    setAddress(tx.getFromAddress());
    setToAddress(tx.getToAddress());
    setAmount(toBigInteger(tx.getAmount()));
    setProtocol(tx.getProtocol());
    setMetadata(tx.getMetadata());
  }


  @Override
  public UnBondTx create() {
    UnBondTx rVal = new UnBondTx(
        getChainId(),
        4,
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
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


  public String getAddress() {
    return address;
  }


  public BigInteger getAmount() {
    return amount;
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


  public String getProtocol() {
    return protocol;
  }


  public String getPublicKey() {
    return publicKey;
  }


  public String getToAddress() {
    return toAddress;
  }


  @Override
  public TxType getTxType() {
    return TxType.REVOKE_VOTING_POWER;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

}
