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
package io.setl.bc.pychain.tx.create.contracts;

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.AddEncumbrance;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.Authorisation;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.Cancellation;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.Parameter;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.Party;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.PayIn;
import io.setl.bc.pychain.tx.create.contracts.dvpcommit.PayOut;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(
    description = "A commitment to part or all of a DVP contract"
)
public class DvpCommitData implements CommitData {

  @Schema(description = "Commitments to add encumbrances")
  private List<AddEncumbrance> addEncumbrances;

  @Schema(description = "Authorisations of the contract")
  private List<Authorisation> authorisations;

  @Schema(
      description = "A cancellation of the contract"
  )
  private Cancellation cancellation;

  @Schema(
      description = "Commitment to parameters of the contract"
  )
  private Map<String, Parameter> parameters;

  @Schema(
      description = "The party making the commitment. Not required if only authorisations and parameters are committed to."
  )
  private Party party;

  @Schema(
      description = "Commitment to inward payments. Only required to specify receiving address when the initial contract did not specify it."
  )
  private List<PayIn> payIn;

  @Schema(
      description = "Commitment to outward payments."
  )
  private int[] payOutIndices;

  @Schema(
      description = "Detailed commitment to outward payments. Required for PoA commitments and when the contract data is not available to the server."
  )
  private List<PayOut> payOut;


  @Override
  public DvpUKCommitData asInternalData() throws InvalidTransactionException {
    if (party == null) {
      if (addEncumbrances != null && !addEncumbrances.isEmpty()) {
        throw new InvalidTransactionException("Party must be specified if Add Encumbrance is specified");
      }
      if (payIn != null && !payIn.isEmpty()) {
        throw new InvalidTransactionException("Party must be specified if Pay In is specified");
      }
      if (payOut != null && !payOut.isEmpty()) {
        throw new InvalidTransactionException("Party must be specified if Pay Out is specified");
      }
    }

    DvpUKCommitData commitData = new DvpUKCommitData(Collections.emptyMap());
    if (addEncumbrances != null) {
      for (AddEncumbrance v : addEncumbrances) {
        commitData.setEncumbrance(v.asInternal());
      }
    }
    if (authorisations != null) {
      for (Authorisation v : authorisations) {
        commitData.setAuthorise(v.asInternal());
      }
    }
    if (cancellation != null) {
      commitData.setCancel(cancellation.asInternal());
    }
    if (parameters != null) {
      for (Entry<String, Parameter> entry : parameters.entrySet()) {
        String k = entry.getKey();
        Parameter v = entry.getValue();
        commitData.setParameter(v.asInternal(k));
      }
    }
    if (party != null) {
      commitData.setParty(party.asInternal());
    }
    if (payIn != null) {
      payIn.forEach(v -> commitData.setReceive(v.asInternal()));
    }
    if (payOut != null) {
      payOut.forEach(v -> commitData.setCommitment(v.asInternal()));
    }
    return commitData;
  }


  @Valid
  public List<@NotNull @Valid AddEncumbrance> getAddEncumbrances() {
    return addEncumbrances;
  }


  @Valid
  public List<@NotNull @Valid Authorisation> getAuthorisations() {
    return authorisations;
  }


  public @Valid Cancellation getCancellation() {
    return cancellation;
  }


  @NotNull
  @Override
  public String getContractType() {
    return CONTRACT_NAME_DVP_UK_COMMIT;
  }


  @Valid
  public Map<String, @NotNull @Valid Parameter> getParameters() {
    return parameters;
  }


  @Valid
  public Party getParty() {
    return party;
  }


  @Valid
  public List<@NotNull @Valid PayIn> getPayIn() {
    return payIn;
  }


  @Valid
  public List<@NotNull @Valid PayOut> getPayOut() {
    return payOut;
  }


  public int[] getPayOutIndices() {
    return payOutIndices;
  }


  public void setAddEncumbrances(List<AddEncumbrance> addEncumbrances) {
    this.addEncumbrances = addEncumbrances;
  }


  public void setAuthorisations(List<Authorisation> authorisations) {
    this.authorisations = authorisations;
  }


  public void setCancellation(Cancellation cancellation) {
    this.cancellation = cancellation;
  }


  public void setParameters(Map<String, Parameter> parameters) {
    this.parameters = parameters;
  }


  public void setParty(Party party) {
    this.party = party;
  }


  public void setPayIn(List<PayIn> payIn) {
    this.payIn = payIn;
  }


  public void setPayOut(List<PayOut> payOut) {
    this.payOut = payOut;
  }


  public void setPayOutIndices(int[] payOutIndices) {
    this.payOutIndices = payOutIndices;
  }

}
