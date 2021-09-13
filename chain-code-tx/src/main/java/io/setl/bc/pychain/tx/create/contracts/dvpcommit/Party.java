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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(
    description = "The party committing to the contract"
)
@JsonInclude(Include.NON_EMPTY)
public class Party {

  @Schema(
      description = "The party's address"
  )
  @JsonInclude(Include.NON_EMPTY)
  private String address = null;

  @Schema(
      description = "The party's identifier in the contract"
  )
  private String identifier = "";

  @Schema(
      description = "The party's public key"
  )
  @JsonInclude(Include.NON_EMPTY)
  @JsonView({Output.class, Submission.class})
  private String publicKey = null;

  @Schema(
      description = "The party's signature to the contract"
  )
  @JsonInclude(Include.NON_EMPTY)
  @JsonView({Output.class, Submission.class})
  private String signature = "";


  public Party() {
    // do nothing
  }


  public Party(DvpCommitParty party) {
    identifier = party.partyIdentifier;
    signature = party.signature;
    String pk = party.publicKey;
    if (AddressUtil.verifyAddress(pk)) {
      address = pk;
    } else if (AddressUtil.verifyPublicKey(pk)) {
      publicKey = pk;
    }
  }


  public DvpCommitParty asInternal() {
    String pk = address != null ? address : publicKey;
    return new DvpCommitParty(identifier, pk, signature);
  }


  @Address
  public String getAddress() {
    return address;
  }


  @NotNull
  public String getIdentifier() {
    return identifier;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  public String getSignature() {
    return signature;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
