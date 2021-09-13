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

import static io.setl.common.AddressType.CONTRACT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

public abstract class BaseCommitToContract extends BaseTransaction {

  @JsonProperty("contractAddresses")
  @JsonAlias("contractAddress")
  @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @Schema(description = "The contract's identifying addresses.")
  private List<String> contractAddresses;

  @JsonAlias("fromAddress")
  @JsonProperty("address")
  @Schema(description = "Address which is committing to the contract.")
  private String fromAddress;

  @JsonProperty("publicKey")
  @JsonView({Output.class, Submission.class})
  private String fromPublicKey;


  public BaseCommitToContract() {
    // do nothing
  }


  /**
   * Convert TX to a representation.
   *
   * @param tx the TX
   */
  public BaseCommitToContract(CommitToContractTx tx) throws InvalidTransactionException {
    super(tx);
    setContractAddresses(tx.getContractAddress());
    setFromAddress(tx.getFromAddress());
    setFromPublicKey(tx.getFromPublicKey());
  }


  public abstract CommitToContractTx create();


  public @NotEmpty List<@NotNull @Address(type = CONTRACT) String> getContractAddresses() {
    return contractAddresses;
  }


  @NotNull
  @Address
  public String getFromAddress() {
    return fromAddress;
  }


  @PublicKey
  public String getFromPublicKey() {
    return fromPublicKey;
  }


  @Nonnull
  @NotNull
  @Address
  @JsonIgnore
  @Hidden
  @Override
  public String getNonceAddress() {
    return getFromAddress();
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getFromPublicKey();
  }


  /**
   * setContractAddress. Set Contract (single) that this commit will seek to update.
   *
   * @param contractAddress :
   */
  @JsonSetter("contractAddress")
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setContractAddress(Object contractAddress) throws InvalidTransactionException {
    // Complex setter because :
    //   @JsonFormat(with=Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) does not appear to apply to aliased properties
    //   @JsonSetter("contractaddresses") does not seem to catch aliased properties

    if (contractAddress instanceof String) {
      this.contractAddresses = List.of((String) contractAddress);
    } else if (contractAddress instanceof Collection) {
      this.contractAddresses = new ArrayList<>(((Collection<?>) contractAddress).size());
      ((Collection<?>) contractAddress).stream().map(String::valueOf).forEach(contractAddresses::add);
    } else {
      this.contractAddresses = Collections.emptyList();
    }
  }


  public void setContractAddresses(String contractAddress) throws InvalidTransactionException {
    setContractAddress(contractAddress);
  }


  /**
   * setContractAddresses. Set Contracts that this commit will seek to update.
   *
   * @param contractAddresses :
   */
  @JsonSetter("contractAddresses")
  public void setContractAddresses(List<String> contractAddresses) throws InvalidTransactionException {
    setContractAddress(contractAddresses);
  }


  public void setFromAddress(String fromAddress) throws InvalidTransactionException {
    this.fromAddress = fromAddress;
  }


  public void setFromPublicKey(String fromPublicKey) throws InvalidTransactionException {
    this.fromPublicKey = fromPublicKey;
  }


  @Override
  @Hidden
  @JsonIgnore
  public void setNoncePublicKey(String key) throws InvalidTransactionException {
    setFromPublicKey(key);
  }

}
