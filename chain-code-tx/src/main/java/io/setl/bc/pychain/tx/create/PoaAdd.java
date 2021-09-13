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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.GRANT_POA, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaAdd extends BaseTransaction {

  @JsonClassDescription("A permission granted to an attorney.")
  public static class PoaPermission {

    @JsonProperty("amount")
    @Schema(description = "The amount of asset the attorney may manipulate with this permission.", format = "int64")
    @Min(0)
    private BigInteger amount;

    @JsonProperty("assets")
    @Schema(description = "The assets the attorney may manipulate.")
    private Set<String> assets;

    @JsonProperty("txType")
    @Schema(description = "The transactions type the attorney may use.")
    private TxType txType;


    public PoaPermission() {
      // do nothing
    }


    /**
     * Recreate the permission from its encoded form
     *
     * @param encoded the encoded form.
     */
    public PoaPermission(PoaItem encoded) {
      txType = encoded.getTxType();
      setAmount(toBigInteger(encoded.getAmount()));
      setAssets(encoded.getAssets());
    }


    Object[] encode() {
      return new Object[]{txType.getId(), (new Balance(amount)).getValue(), assets.toArray(new String[assets.size()])};
    }


    public BigInteger getAmount() {
      return amount;
    }


    public Set<String> getAssets() {
      return Collections.unmodifiableSet(assets);
    }


    public TxType getTxType() {
      return txType;
    }


    public void setAmount(BigInteger amount) {
      this.amount = amount;
    }


    public void setAssets(Set<String> assets) {
      this.assets = assets;
    }


    /**
     * Set the transaction type.
     *
     * @param txType the type
     */
    public void setTxType(TxType txType) {
      this.txType = txType;
    }

  }


  /**
   * Return a new, unsigned PoaAddTx.
   *
   * @param chainID         :
   * @param nonce           :
   * @param fromPubKey      :
   * @param fromAddress     :
   * @param reference       :
   * @param attorneyAddress :
   * @param startDate       :
   * @param endDate         :
   * @param permissionData  :
   * @param protocol        :
   * @param metadata        :
   * @param poa             :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaAddTx poaUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String reference,
      String attorneyAddress,
      long startDate,
      long endDate,
      Object[] permissionData,
      String protocol,
      String metadata,
      String poa
  ) {

    if (!PoaAddTx.verifyItemData(permissionData)) {
      return null;
    }

    if (!AddressUtil.verifyAddress(attorneyAddress)) {
      return null;
    }

    if (!AddressUtil.verify(fromAddress, fromPubKey, AddressType.NORMAL)) {
      return null;
    }

    PoaAddTx rVal = new PoaAddTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        reference,
        attorneyAddress,
        startDate,
        endDate,
        permissionData,
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
  @Schema(description = "The authorising address. Must be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("attorneyAddress")
  @Schema(description = "The attorney's address.")
  @NotNull
  @Address
  private String attorneyAddress;

  @JsonProperty("endTime")
  @Schema(description = "The time at which the attorney's authority finishes. Specify zero for no end time.")
  private long endDate = 0;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("permissions")
  @Schema(description = "Which assets the attorney is allowed to manipulate.")
  private List<@Valid PoaPermission> permissions;

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to implement the addition of an attorney.")
  private String protocol = "";

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the authorising address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("reference")
  @Schema(description = "Reference associated with this granting of attorney permissions.")
  private String reference;

  @JsonProperty("startTime")
  @Schema(description = "The time at which the attorney's authority starts. Specify zero for starting immediately.")
  private long startDate = 0;

  public PoaAdd() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaAdd(PoaAddTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setAttorneyAddress(tx.getAttorneyAddress());
    setEndDate(tx.getEndDate());
    setMetadata(tx.getMetadata());
    setPermissions(tx.getPoaItems().stream().map(poaItem -> new PoaPermission(poaItem)).collect(Collectors.toList()));
    setProtocol(tx.getProtocol());
    setPublicKey(tx.getFromPublicKey());
    setReference(tx.getReference());
    setStartDate(tx.getStartDate());
  }


  @Override
  public PoaAddTx create() {
    Object[] permissionData = permissions.stream().map(poaPermission -> poaPermission.encode()).toArray();
    long endDate = getEndDate();
    if (endDate == 0) {
      endDate = Long.MAX_VALUE;
    }
    PoaAddTx rVal = new PoaAddTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getReference(),
        getAttorneyAddress(),
        getStartDate(),
        endDate,
        permissionData,
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


  public String getAttorneyAddress() {
    return attorneyAddress;
  }


  public long getEndDate() {
    return endDate;
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


  public List<PoaPermission> getPermissions() {
    return permissions;
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


  public long getStartDate() {
    return startDate;
  }


  @Override
  public TxType getTxType() {
    return TxType.GRANT_POA;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAttorneyAddress(String attorneyAddress) {
    this.attorneyAddress = attorneyAddress;
  }


  public void setEndDate(long endDate) {
    this.endDate = endDate;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setPermissions(List<PoaPermission> permissions) {
    this.permissions = permissions;
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


  public void setStartDate(long startDate) {
    this.startDate = startDate;
  }

}
