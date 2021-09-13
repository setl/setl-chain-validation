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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.AddressPermissionsTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.AuthorisedAddressConstants;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings({"squid:S2637", "squid:S00107"}) // "@NonNull" values should not be set to null. Default constructor is required for json,  // Params > 7
@JsonClassDescription("Set which transactions an address can use and how they may be used.")
@Schema(name = TxExternalNames.UPDATE_ADDRESS_PERMISSIONS,
    description = "Set which transactions an address can use and how they may be used.",
    allOf = BaseTransaction.class)
@JsonDeserialize
public class AddressPermissions extends BaseTransaction {

  /**
   * registerAddressUnsigned().
   * <p>Create new unsigned RegisterAddressTx
   * </p>
   *
   * @param chainID            :
   * @param nonce              :
   * @param fromPubKey         :
   * @param fromAddress        :
   * @param toAddress          :
   * @param addressPermissions :
   * @param metadata           :
   * @param poa                :
   *
   * @return :
   */
  public static AddressPermissionsTx addressPermissionsUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String toAddress,
      long addressPermissions,
      String metadata,
      String poa
  ) {
    AddressPermissionsTx rVal = new AddressPermissionsTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        toAddress,
        addressPermissions,
        null,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  /**
   * registerAddressUnsigned().
   * <p>Create new unsigned RegisterAddressTx
   * </p>
   *
   * @param chainID             :
   * @param nonce               :
   * @param fromPubKey          :
   * @param fromAddress         :
   * @param toAddress           :
   * @param addressPermissions  :
   * @param allowedTransactions :
   * @param poa                 :
   *
   * @return :
   */
  public static AddressPermissionsTx addressPermissionsUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String toAddress,
      long addressPermissions,
      Set<TxType> allowedTransactions,
      String metadata,
      String poa
  ) {
    AddressPermissionsTx rVal = new AddressPermissionsTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        toAddress,
        addressPermissions,
        ((addressPermissions & AP_TX_LIST) == 0) ? null : allowedTransactions,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonProperty("fromAddress")
  @Schema(description = "The address issuing the permissions. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String fromAddress;

  @JsonProperty("fromPublicKey")
  @Schema(description = "Public key of the permission issuing address. Normally derived from the wallet.")
  @PublicKey
  private String fromPublicKey;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("permissions")
  @Schema(description = "Names of permissions issued to the address.")
  private List<String> permissions;

  @JsonProperty("toAddress")
  @Schema(description = "The address whose permissions are being set.")
  @NotNull
  @Address
  private String toAddress;

  @JsonProperty("transactions")
  @Schema(description = "The transactions which the address can use.")
  private Set<TxType> transactions;

  public AddressPermissions() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public AddressPermissions(AddressPermissionsTx tx) {
    super(tx);
    setFromAddress(tx.getFromAddress());
    setFromPublicKey(tx.getFromPublicKey());
    setMetadata(tx.getMetadata());
    setToAddress(tx.getToAddress());

    setPermissions(new ArrayList<>(AuthorisedAddressConstants.forCode(tx.getAddressPermissions())));

    Set<TxType> addressTransactions = tx.getAddressTransactions();
    if (addressTransactions != null) {
      transactions = EnumSet.copyOf(addressTransactions);
    } else {
      transactions = null;
    }
  }


  @Override
  public AddressPermissionsTx create() {
    long addressPermissions = AuthorisedAddressConstants.forNames(permissions);

    AddressPermissionsTx rVal = new AddressPermissionsTx(getChainId(), 4, getHash(), getNonce(), isUpdated(), getFromPublicKey(), getFromAddress(),
        getToAddress(), addressPermissions, transactions, getMetadata(), getSignature(), getHeight(), getPoa(), getTimestamp()
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


  public List<String> getPermissions() {
    return permissions;
  }


  public String getToAddress() {
    return toAddress;
  }


  /**
   * Get the transactions.
   *
   * @return the transactions
   */
  public Set<String> getTransactions() {
    return transactions != null ? transactions.stream().map(tx -> tx.getExternalName()).collect(Collectors.toSet()) : null;
  }


  @Override
  public TxType getTxType() {
    return TxType.UPDATE_ADDRESS_PERMISSIONS;
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


  @Override
  public void setNoncePublicKey(String key) {
    setFromPublicKey(key);
  }


  /**
   * Set the permissions.
   *
   * @param permissions the permissions
   */
  public void setPermissions(List<String> permissions) {
    if (permissions != null) {
      this.permissions = List.copyOf(permissions);
    } else {
      this.permissions = Collections.emptyList();
    }
  }


  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }


  /**
   * Set the transactions.
   *
   * @param transactions the transactions
   */
  public void setTransactions(Collection<String> transactions) {
    if (transactions != null) {
      this.transactions = EnumSet.noneOf(TxType.class);
      for (String name : transactions) {
        TxType type = TxType.get(name);
        if (type == null) {
          throw new IllegalArgumentException("Unrecognized transaction type: " + name);
        }
        this.transactions.add(type);
      }
    } else {
      this.transactions = null;
    }
  }

}
