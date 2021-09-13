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

import java.util.Map;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PrivilegedOperationTx;
import io.setl.common.AddressType;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 07/07/2020.
 */
public class PrivilegedOperation extends BaseTransaction {

  @JsonProperty("address")
  @Schema(description = "The address submitting this transaction. Can be derived from the associated public key.")
  @NotNull
  @Address(type = AddressType.PRIVILEGED)
  private String address;

  @JsonProperty("operationInput")
  @Schema(description = "Input to the privileged operation")
  private Map<String, Object> operationInput;

  @JsonProperty("operationName")
  @Schema(description = "Name of the privileged operation to perform.")
  private String operationName;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the submitting address. Must be a privileged address.")
  @PublicKey
  private String publicKey;

  public PrivilegedOperation() {
    address = UNSET_VALUE;
  }


  public PrivilegedOperation(PrivilegedOperationTx txi) {
    super(txi);
    address = txi.getFromAddress();
    operationInput = txi.getOperationInput();
    operationName = txi.getOperationName();
    publicKey = txi.getFromPublicKey();
  }


  @Override
  public PrivilegedOperationTx create() {
    PrivilegedOperationTx tx = new PrivilegedOperationTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getAddress(),
        getPublicKey(),
        getSignature(),
        getPoa(),
        getTimestamp(),
        operationName,
        operationInput
    );
    if (Strings.isNullOrEmpty(getHash())) {
      tx.setHash(Hash.computeHash(tx));
    }
    return tx;
  }


  public String getAddress() {
    return address;
  }


  @JsonIgnore
  @Nonnull
  @Override
  public String getNonceAddress() {
    return getAddress();
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getPublicKey();
  }


  public Map<String, Object> getOperationInput() {
    return operationInput;
  }


  public String getOperationName() {
    return operationName;
  }


  public String getPublicKey() {
    return publicKey;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.DO_PRIVILEGED_OPERATION;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setOperationInput(Map<String, Object> operationInput) {
    this.operationInput = operationInput;
  }


  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

}
