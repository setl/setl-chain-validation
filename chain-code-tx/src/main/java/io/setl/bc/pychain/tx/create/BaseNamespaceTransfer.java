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

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.NamespaceTransferTx;
import io.setl.validation.annotations.Address;

/**
 * @author Catherine Cooper on 01/09/2020.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
public abstract class BaseNamespaceTransfer extends BaseTransaction {

  @JsonProperty("address")
  @Schema(description = "The from address. Will be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String fromAddress;

  @JsonProperty("namespace")
  @Schema(description = "The name of the namespace to transfer.")
  @NotNull
  private String nameSpace;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the issuing address. Normally derived from the wallet.")
  private String publicKey;

  @JsonProperty("toAddress")
  @Schema(description = "The from address. Will be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String toAddress;

  protected BaseNamespaceTransfer() {
    // do nothing
  }


  protected BaseNamespaceTransfer(NamespaceTransferTx tx) {
    super(tx);
    setFromAddress(tx.getFromAddress());
    setToAddress(tx.getToAddress());
    setNameSpace(tx.getNameSpace());
    setPublicKey(tx.getFromPublicKey());
  }


  public String getFromAddress() {
    return fromAddress;
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
    return getPublicKey();
  }


  public String getPublicKey() {
    return publicKey;
  }


  public String getToAddress() {
    return toAddress;
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }


  public void setNameSpace(String nameSpace) {
    this.nameSpace = cleanString(nameSpace);
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

}
