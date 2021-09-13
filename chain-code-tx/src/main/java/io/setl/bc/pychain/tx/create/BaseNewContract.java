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

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.tx.Views.Input;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.common.AddressType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/05/2018.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
public abstract class BaseNewContract extends BaseTransaction {

  @JsonProperty("address")
  @Schema(description = "The address submitting the new contract. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("contractAddress")
  @Schema(description = "The address of the contract.")
  @JsonView(Output.class)
  @Address(type = AddressType.CONTRACT)
  private String contractAddress;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the submitting address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("signForWallet")
  @Schema(description = "If true, auto-sign the contract for all addresses in the issuing wallet. Otherwise, only auto-sign for the issuing address.")
  @JsonView(Input.class)
  private boolean signForWallet;

  public BaseNewContract() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public BaseNewContract(NewContractTx tx) {
    super(tx);
    setAddress(tx.getAuthoringAddress());
    setContractAddress(tx.getContractAddress());
    setPublicKey(tx.getAuthoringPublicKey());
  }


  @NotNull
  @Address
  public String getAddress() {
    return address;
  }


  @Address(type = AddressType.CONTRACT)
  public String getContractAddress() {
    return contractAddress;
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


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  public boolean isSignForWallet() {
    return signForWallet;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setSignForWallet(boolean signForWallet) {
    this.signForWallet = signForWallet;
  }

}
