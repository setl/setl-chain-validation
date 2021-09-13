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

import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.bc.pychain.tx.create.contracts.DvpContractData;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
@Schema(
    description = "A party to a contract"
)
public class Party {

  @Schema(
      description = "The party's address", required = true
  )
  private String address;

  @Schema(
      description = "An identifier for this party in this contract", required = true
  )
  private String identifier = "";

  @Schema(
      description = "If true, the party must sign the contract even if they have an encumbrance that could fulfill it automatically."
  )
  private boolean mustSign = true;

  @Schema(
      description = "The inward payments to this address's balance"
  )
  @JsonInclude(Include.NON_EMPTY)
  private List<PayIn> payIn = Collections.emptyList();

  @Schema(
      description = "The outward payments from this address's balance"
  )
  @JsonInclude(Include.NON_EMPTY)
  private List<PayOut> payOut = Collections.emptyList();

  @Schema(
      description = "The public key used to sign for this party"
  )
  @JsonInclude(Include.NON_EMPTY)
  @JsonView(Output.class)
  private String publicKey;

  @Schema(
      description = "The party's signature"
  )
  @JsonInclude(Include.NON_NULL)
  @JsonView(Output.class)
  private String signature;


  public Party() {
    // do nothing
  }


  public Party(DvpParty dvpParty) throws InvalidTransactionException {
    address = dvpParty.sigAddress;
    mustSign = dvpParty.mustSign;
    identifier = dvpParty.partyIdentifier;
    payIn = DvpContractData.convert(dvpParty.receiveList, PayIn::new);
    payOut = DvpContractData.convert(dvpParty.payList, PayOut::new);
    publicKey = dvpParty.publicKey;
    signature = dvpParty.signature;
  }


  public DvpParty asInternal() throws InvalidTransactionException {
    ContractData.require(address, "Party address must be specified");
    DvpParty dvpParty = new DvpParty(identifier, address, publicKey, signature, mustSign);
    for (PayIn in : payIn) {
      if (in.getAddress() == null) {
        in.setAddress(address);
      }
      dvpParty.receiveList.add(in.asInternal());
    }
    for (PayOut out : payOut) {
      if (out.getAddress() == null) {
        out.setAddress(address);
      }
      dvpParty.payList.add(out.asInternal());
    }
    return dvpParty;
  }


  @NotNull
  @Address
  public String getAddress() {
    return address;
  }


  @NotNull
  public String getIdentifier() {
    return identifier;
  }


  public List<@Valid PayIn> getPayIn() {
    return payIn;
  }


  public List<@Valid PayOut> getPayOut() {
    return payOut;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  public String getSignature() {
    return signature;
  }


  public boolean isMustSign() {
    return mustSign;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setIdentifier(String identifier) {
    this.identifier = notNull(identifier);
  }


  public void setMustSign(boolean mustSign) {
    this.mustSign = mustSign;
  }


  public void setPayIn(List<PayIn> payIn) {
    this.payIn = payIn != null ? payIn : Collections.emptyList();
  }


  public void setPayOut(List<PayOut> payOut) {
    this.payOut = payOut != null ? payOut : Collections.emptyList();
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
