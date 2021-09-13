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
package io.setl.bc.pychain.tx.create.contracts.dvp;

import static io.setl.common.StringUtils.notNull;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
@Schema(
    description = "An authorisation of a contract. An authoriser may be otherwise uninvolved in the contract."
)
public class Authorisation {

  @Schema(
      description = "The address that signed this."
  )
  @JsonInclude(Include.NON_NULL)
  @JsonView(Output.class)
  private String address;

  @Schema(description = "The addresses that could authorise this. Cleared once signed.")
  @JsonInclude(Include.NON_NULL)
  private Set<String> addresses;

  @Schema(
      description = "Unique identifier for this authorisation"
  )
  private String authorisationId = "";

  @Schema(
      description = "Is this authorisation specific to this contract, as opposed to being applicable to multiple contracts"
  )
  private boolean isContractSpecific = true;

  @Schema(
      description = "Is authorisation refused?"
  )
  private boolean isRefused = false;

  @Schema(
      description = "Additional data associated with this authorisation"
  )
  @JsonInclude(Include.NON_EMPTY)
  private String metadata = "";

  @Schema(
      description = "The public key of the attorney that signed this."
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_NULL)
  private String poaPublicKey;

  @Schema(
      description = "The public key of the address that signed this."
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_NULL)
  private String publicKey;

  @Schema(
      description = "Signature for this authorisation"
  )
  @JsonInclude(Include.NON_NULL)
  private String signature;


  public Authorisation() {
    // do nothing
  }


  public Authorisation(DvpAuthorisation authorisation) {
    String authAddress = authorisation.getAddress();
    if (AddressUtil.verifyAddress(authAddress)) {
      address = authAddress;
      publicKey = null;
    } else if (authAddress != null) {
      address = AddressUtil.publicKeyToAddress(authAddress, AddressType.NORMAL);
      publicKey = authAddress;
    } else {
      address = null;
      publicKey = null;
    }

    String[] a = authorisation.getAddresses();
    if (a != null) {
      addresses = new TreeSet<>(Arrays.asList(a));
    } else {
      addresses = null;
    }
    authorisationId = authorisation.authorisationID;
    isContractSpecific = authorisation.isContractSpecific();
    isRefused = authorisation.getRefused();
    metadata = authorisation.getMetadata();
    poaPublicKey = authorisation.getPoaPublicKey();
    signature = authorisation.getSignature();
  }


  public DvpAuthorisation asInternal() throws InvalidTransactionException {
    ContractData.require(authorisationId, "Authorisation is required");
    if (address == null && publicKey == null) {
      if (addresses == null || addresses.isEmpty()) {
        throw new InvalidTransactionException("Authorisation must specify addresses that can sign or the address that did sign");
      }
      return new DvpAuthorisation(
          addresses.toArray(new String[0]),
          authorisationId,
          signature,
          metadata,
          isRefused,
          isContractSpecific ? 1 : 0
      );
    } else {
      String id = publicKey != null ? publicKey : address;
      return new DvpAuthorisation(
          id,
          authorisationId,
          signature,
          metadata,
          isRefused,
          isContractSpecific ? 1 : 0
      );
    }
  }


  @Address
  public String getAddress() {
    return address;
  }


  public Set<@Address String> getAddresses() {
    return addresses;
  }


  @NotEmpty
  public String getAuthorisationId() {
    return authorisationId;
  }


  public String getMetadata() {
    return metadata;
  }


  @PublicKey
  public String getPoaPublicKey() {
    return poaPublicKey;
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
  public boolean hasAuthorisers() {
    // Either has an address that has authorised, or a list of potential authorisers.
    return address != null || (addresses != null && !addresses.isEmpty());
  }


  public boolean isContractSpecific() {
    return isContractSpecific;
  }


  public boolean isRefused() {
    return isRefused;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAddresses(Set<String> addresses) {
    if (addresses != null) {
      this.addresses = new TreeSet<>(addresses);
    } else {
      this.addresses = null;
    }
  }


  public void setAuthorisationId(String authorisationId) {
    this.authorisationId = notNull(authorisationId);
  }


  public void setContractSpecific(boolean contractSpecific) {
    isContractSpecific = contractSpecific;
  }


  public void setMetadata(String metadata) {
    this.metadata = notNull(metadata);
  }


  public void setPoaPublicKey(String poaPublicKey) {
    this.poaPublicKey = poaPublicKey;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setRefused(Boolean refused) {
    isRefused = refused != null && refused;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
