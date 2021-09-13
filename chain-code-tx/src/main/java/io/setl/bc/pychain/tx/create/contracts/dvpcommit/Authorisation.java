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
package io.setl.bc.pychain.tx.create.contracts.dvpcommit;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitAuthorise;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(description = "An authorisation of a contract")
public class Authorisation {

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "The address that is authorising the contract")
  private String address;

  @Schema(description = "The ID of this authorisation")
  private String authorisationId;

  @Schema(description = "If true, the authorisation is specific to this contract")
  private boolean isContractSpecific = true;

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "Additional data associated with this authorisation")
  private String metadata;

  @Schema(description = "The public key that signed this authorisation")
  @JsonView({Output.class, Submission.class})
  private String publicKey;

  @Schema(description = "If true, authorisation is refused")
  private boolean refused = false;

  @Schema(description = "The signature on this authorisation")
  @JsonView({Output.class, Submission.class})
  private String signature;


  public Authorisation() {
    // do nothing
  }


  public Authorisation(DvpCommitAuthorise authorise) {
    address = authorise.getAddress();
    authorisationId = authorise.authId;
    isContractSpecific = authorise.specific != null && authorise.specific != 0;
    metadata = authorise.metadata;
    publicKey = authorise.getPublicKey();
    refused = authorise.refused;
    signature = authorise.signature;
  }


  public DvpCommitAuthorise asInternal() throws InvalidTransactionException {
    ContractData.require(authorisationId, "Authorisation ID must be specified");
    String pk = publicKey != null ? publicKey : address;
    ContractData.require(pk, "Either address or public key must be specified");
    return new DvpCommitAuthorise(pk, authorisationId, signature, metadata, refused, isContractSpecific ? 1 : 0);
  }


  @Address
  public String getAddress() {
    return address;
  }


  @NotEmpty
  public String getAuthorisationId() {
    return authorisationId;
  }


  public String getMetadata() {
    return metadata;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  public String getSignature() {
    return signature;
  }


  @Hidden
  @JsonIgnore
  @AssertTrue
  public boolean hasAddressOrPublicKey() {
    return publicKey != null || address != null;
  }


  public boolean isContractSpecific() {
    return isContractSpecific;
  }


  public boolean isRefused() {
    return refused;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAuthorisationId(String authorisationId) {
    this.authorisationId = authorisationId;
  }


  public void setContractSpecific(boolean contractSpecific) {
    isContractSpecific = contractSpecific;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setRefused(boolean refused) {
    this.refused = refused;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
