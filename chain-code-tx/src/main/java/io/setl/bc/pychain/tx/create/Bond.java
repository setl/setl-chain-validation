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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.BondTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.GRANT_VOTING_POWER, description = "Bond", allOf = BaseTransaction.class)
@JsonDeserialize
public class Bond extends BaseTransaction {

  /**
   * Return a new, unsigned BondTx.
   *
   * @param chainID         :
   * @param nonce           :
   * @param fromPubKey      :
   * @param fromAddress     :
   * @param toSignodePubKey :
   * @param returnAddress   :
   * @param amount          :
   * @param poa             :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static BondTx bondUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String toSignodePubKey,
      String returnAddress,
      Number amount,
      String poa
  ) {

    BondTx rVal = new BondTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        toSignodePubKey,
        returnAddress,
        amount,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  @JsonProperty("amount")
  @Schema(description = "Amount of asset to bond.", format = "int64")
  @NotNull
  @Min(0)
  private BigInteger amount;

  @JsonProperty("fromAddress")
  @Schema(description = "The issuing address. Must be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String fromAddress;

  @JsonProperty("fromPublicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  @PublicKey
  private String fromPublicKey;

  @JsonProperty("returnAddress")
  @Schema(description = "Address to which bonded assets will be returned.")
  @NotNull
  @Address
  private String returnAddress;

  @JsonProperty("toPublicKey")
  @Schema(description = "Public key which receives the bond.")
  @NotNull
  @PublicKey
  private String toPublicKey;


  public Bond() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public Bond(BondTx tx) {
    super(tx);
    setAmount(toBigInteger(tx.getAmount()));
    setFromAddress(tx.getFromAddress());
    setFromPublicKey(tx.getFromPublicKey());
    setReturnAddress(tx.getReturnAddress());
    setToPublicKey(tx.getToSignodePubKey());
  }


  @Override
  public BondTx create() {
    BondTx rVal = new BondTx(getChainId(), 4, getHash(), getNonce(), isUpdated(), getFromPublicKey(), getFromAddress(), getToPublicKey(), getReturnAddress(),
        getAmount(), getSignature(), getHeight(), getPoa(), getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public BigInteger getAmount() {
    return amount;
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getFromPublicKey() {
    return fromPublicKey;
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
  public @PublicKey String getNoncePublicKey() {
    return getFromPublicKey();
  }


  public String getReturnAddress() {
    return returnAddress;
  }


  public String getToPublicKey() {
    return toPublicKey;
  }


  @Override
  @Nonnull
  public TxType getTxType() {
    return TxType.GRANT_VOTING_POWER;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }


  public void setFromPublicKey(String fromPublicKey) {
    this.fromPublicKey = fromPublicKey;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setFromPublicKey(key);
  }


  public void setReturnAddress(String returnAddress) {
    this.returnAddress = returnAddress;
  }


  public void setToPublicKey(String toPublicKey) {
    this.toPublicKey = toPublicKey;
  }
}
