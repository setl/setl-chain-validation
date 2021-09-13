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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitCancel;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(
    description = "Cancellation of the contract"
)
public class Cancellation {

  @Schema(
      description = "The address that signed this cancellation"
  )
  private String address;

  @Schema(
      description = "The public key that signed this cancellation"
  )
  @JsonView({Output.class, Submission.class})
  private String publicKey;

  @Schema(
      description = "The signature"
  )
  @JsonView({Output.class, Submission.class})
  private String signature;


  public Cancellation() {
    // do nothing
  }


  public Cancellation(DvpCommitCancel cancel) {
    // The 'publicKey' field could be a public key or an address.
    String pk = cancel.publicKey;
    if (AddressUtil.verifyAddress(pk)) {
      // verified as an address
      address = pk;
      publicKey = null;
    } else {
      // assume it is a public key
      address = null;
      publicKey = pk;
    }
    signature = cancel.signature;
  }


  public DvpCommitCancel asInternal() {
    String pk = publicKey != null ? publicKey : address;
    return new DvpCommitCancel(pk, signature);
  }


  @Address
  public String getAddress() {
    return address;
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

  public void setAddress(String address) {
    this.address = address;
    if (this.address != null) {
      publicKey = null;
    }
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
    if (publicKey != null) {
      address = null;
    }
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
