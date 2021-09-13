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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.StringUtils.notNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.create.contracts.dvp.AddEncumbrance;
import io.setl.bc.pychain.tx.create.contracts.dvp.Authorisation;
import io.setl.bc.pychain.tx.create.contracts.dvp.Encumbrance;
import io.setl.bc.pychain.tx.create.contracts.dvp.Parameter;
import io.setl.bc.pychain.tx.create.contracts.dvp.Party;
import io.setl.common.AddressType;
import io.setl.common.CommonPy.ContractConstants;
import io.setl.validation.annotations.Address;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
@Schema(
    description = "Specification of a Deliveries Versus Payment (DVP) contract"
)
public class DvpContractData implements ContractData {

  @FunctionalInterface
  public interface TxFunction<X, Y> {

    Y apply(X x) throws InvalidTransactionException;

  }


  /**
   * Convert a list of one type to another type, handling nulls.
   *
   * @param input    the input list
   * @param function the conversion function
   * @param <X>      output type
   * @param <Y>      input type
   *
   * @return list of output types
   */
  public static <X, Y> List<Y> convert(List<X> input, TxFunction<X, Y> function) throws InvalidTransactionException {
    if (input == null || input.isEmpty()) {
      return Collections.emptyList();
    }
    List<Y> output = new ArrayList<>(input.size());
    for (X x : input) {
      if (x != null) {
        output.add(function.apply(x));
      }
    }
    return output;
  }



  @Schema(
      description = "Encumbrances that are to be created on completion of the contract"
  )
  @JsonInclude(Include.NON_EMPTY)
  private List<AddEncumbrance> addEncumbrances;

  @Schema(
      description = "Additional authorisations for the contract. An authorising address does not participate in the exchange of assets, but may prevent the "
          + "exchange from happening."
  )
  @JsonInclude(Include.NON_EMPTY)
  private List<Authorisation> authorisations;

  @Schema(
      description = "The time (in seconds since the epoch) at which the contract was cancelled."
  )
  @JsonView(Output.class)
  private long cancelTime;

  @Schema(
      description = "Is the contract completed?"
  )
  @JsonView(Output.class)
  private boolean completed;

  @Schema(
      description = "The contract's address. This is its unique identifier."
  )
  private String contractAddress;

  @Schema(
      description = "The address which authored the contract."
  )
  @JsonView(Output.class)
  private String contractAuthor;

  @Schema(
      description = "An additional number of seconds which the contract will be retained in state after it is completed or cancelled before it is"
          + "automatically deleted."
  )
  private long deleteDelay;

  @Schema(
      description = "An encumbrance that can be used to fulfill the contract. Parties that have such an encumbrance to cover their outward payments may not "
          + "have to sign the contract."
  )
  @JsonInclude(Include.NON_NULL)
  private Encumbrance encumbrance;

  @Schema(
      description = "The next time (in seconds since the epoch) when this contract will be evaluated."
  )
  @JsonView(Output.class)
  private long eventTime;

  @Schema(
      description = "The next time (in seconds since the epoch) when this contract will expiry. It cannot complete after this time. "
          + "Defaults to 180 days after the start time."
  )
  private long expiry = -1;

  @Schema(
      description = "Any additional data associated with this contract"
  )
  @JsonInclude(Include.NON_NULL)
  private String metadata;

  @Schema(
      description = "Parameters that may affect the outcome of this contract"
  )
  private Map<String, Parameter> parameters;

  @Schema(
      description = "Parties to this contract"
  )
  private List<Party> parties;

  @Schema(
      description = "A protocol associated with this contract"
  )
  @JsonInclude(Include.NON_NULL)
  private String protocol;

  @Schema(
      description = "The next time (in seconds since the epoch) when this contract will first be valid. It cannot complete before this time. "
          + "Defaults to the time the contract was created."
  )
  private long startDate = -1;

  @Schema(
      description = "Progressive status of the last contract evaluation. Last is most recent."
  )
  @JsonView(Output.class)
  private List<String> status;


  public DvpContractData() {
    // do nothing
  }


  /**
   * Create an external data representation from the internal representation.
   *
   * @param iContractData the internal representation
   *
   * @throws InvalidTransactionException if the internal representation violates one of the rules
   */
  public DvpContractData(DvpUkContractData iContractData) throws InvalidTransactionException {
    addEncumbrances = convert(iContractData.getAddencumbrances(), AddEncumbrance::new);
    authorisations = convert(iContractData.getAuthorisations(), Authorisation::new);
    cancelTime = iContractData.get__canceltime();
    completed = iContractData.get__completed() != 0;
    contractAddress = iContractData.get__address();
    deleteDelay = iContractData.getDeleteDelayOnComplete();

    DvpEncumbrance dvpEncumbrance = iContractData.getEncumbrance();
    if (dvpEncumbrance != null) {
      encumbrance = new Encumbrance(dvpEncumbrance);
    }

    eventTime = iContractData.get__timeevent();
    expiry = iContractData.getExpiry();
    contractAuthor = iContractData.getIssuingaddress();
    metadata = iContractData.getMetadata();

    Map<String, DvpParameter> contractParams = iContractData.getParameters();
    if (contractParams != null && !contractParams.isEmpty()) {
      parameters = new TreeMap<>();
      for (Entry<String, DvpParameter> entry : contractParams.entrySet()) {
        String k = entry.getKey();
        DvpParameter v = entry.getValue();
        parameters.put(k, new Parameter(v));
      }
    }
    parties = convert(iContractData.getParties(), Party::new);
    protocol = iContractData.getProtocol();
    startDate = iContractData.getStartdate();
    status = Arrays.asList(notNull(iContractData.get__status()).split("\n"));
  }


  @Override
  public DvpUkContractData asInternalData() throws InvalidTransactionException {
    if (parties == null || parties.isEmpty()) {
      throw new InvalidTransactionException("DVP Contract must have parties.");
    }
    HashSet<String> partyIds = new HashSet<>();
    for (Party p : parties) {
      if (p == null) {
        throw new InvalidTransactionException("DVP Contract parties must be specified, and not null");
      }
      if (!partyIds.add(p.getIdentifier())) {
        throw new InvalidTransactionException("DVP Contract parties must have unique identifiers.");
      }
    }
    if (startDate == -1) {
      startDate = Instant.now().getEpochSecond();
    }
    if (expiry == -1) {
      expiry = startDate + ContractConstants.MAX_DVP_CONTRACTDURATION;
    }

    DvpUkContractData output = new DvpUkContractData(
        contractAddress,
        completed ? 1 : 0,
        getContractType(),
        eventTime,
        startDate,
        expiry,
        contractAuthor,
        protocol,
        metadata,
        new String[]{"commit", "time"},
        null
    );

    if (addEncumbrances != null) {
      output.setAddencumbrances(convert(addEncumbrances, AddEncumbrance::asInternal));
    }
    if (authorisations != null) {
      output.setAuthorisations(convert(authorisations, Authorisation::asInternal));
    }
    if (encumbrance != null) {
      output.setEncumbrance(encumbrance.asInternal());
    }
    if (parameters != null) {
      TreeMap<String, DvpParameter> newMap = new TreeMap<>();
      for (Entry<String, Parameter> entry : parameters.entrySet()) {
        String k = entry.getKey();
        Parameter p = entry.getValue();
        newMap.put(k, p.asInternal());
      }
      output.setParameters(newMap);
    }
    output.setParties(convert(parties, Party::asInternal));

    return output;
  }

  @Valid
  public List<@Valid @NotNull AddEncumbrance> getAddEncumbrances() {
    return addEncumbrances;
  }


  @Valid
  public List<@Valid @NotNull Authorisation> getAuthorisations() {
    return authorisations;
  }


  public long getCancelTime() {
    return cancelTime;
  }


  @Address(type = AddressType.CONTRACT)
  public String getContractAddress() {
    return contractAddress;
  }


  @Address
  public String getContractAuthor() {
    return contractAuthor;
  }


  @Nonnull
  @Override
  public String getContractType() {
    return CONTRACT_NAME_DVP_UK;
  }


  @Min(0)
  public long getDeleteDelay() {
    return deleteDelay;
  }


  public Encumbrance getEncumbrance() {
    return encumbrance;
  }


  public long getEventTime() {
    return eventTime;
  }


  public long getExpiry() {
    return expiry;
  }


  public String getMetadata() {
    return metadata;
  }


  public Map<String, Parameter> getParameters() {
    return parameters;
  }


  @NotEmpty
  public List<Party> getParties() {
    return parties;
  }


  public String getProtocol() {
    return protocol;
  }


  public long getStartDate() {
    return startDate;
  }


  public List<String> getStatus() {
    return status;
  }


  public boolean isCompleted() {
    return completed;
  }


  /**
   * Set the encumbrances to be added by this contract.
   *
   * @param addEncumbrances the new encumbrances
   *
   * @throws InvalidTransactionException if any new encumbrance is null
   */
  public void setAddEncumbrances(List<AddEncumbrance> addEncumbrances) throws InvalidTransactionException {
    if (addEncumbrances != null) {
      for (AddEncumbrance v : addEncumbrances) {
        ContractData.require(v, "Add Encumbrance must not be null");
      }
    }
    this.addEncumbrances = addEncumbrances;
  }


  /**
   * Set the authorisations to be required by this contract.
   *
   * @param authorisations the authorisations
   *
   * @throws InvalidTransactionException if any authorisation is null
   */
  public void setAuthorisations(List<Authorisation> authorisations) throws InvalidTransactionException {
    if (authorisations != null) {
      for (Authorisation v : authorisations) {
        ContractData.require(v, "Authorisation must not be null");
      }
    }
    this.authorisations = authorisations;
  }


  public void setCancelTime(long cancelTime) {
    this.cancelTime = cancelTime;
  }


  public void setCompleted(boolean completed) {
    this.completed = completed;
  }


  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }


  public void setContractAuthor(String contractAuthor) {
    this.contractAuthor = contractAuthor;
  }


  /**
   * Set the delete delay. The delay must be non-negative.
   *
   * @param deleteDelay the delay
   *
   * @throws InvalidTransactionException if the delay is negative
   */
  public void setDeleteDelay(long deleteDelay) throws InvalidTransactionException {
    if (deleteDelay < 0) {
      throw new InvalidTransactionException("Delete delay must be non-negative, not " + deleteDelay);
    }
    this.deleteDelay = deleteDelay;
  }


  public void setEncumbrance(Encumbrance encumbrance) {
    this.encumbrance = encumbrance;
  }


  public void setEventTime(long eventTime) {
    this.eventTime = eventTime;
  }


  public void setExpiry(long expiry) {
    this.expiry = expiry;
  }


  public void setMetadata(String metadata) {
    this.metadata = notNull(metadata);
  }


  /**
   * Set the list of parameters in this contract.
   *
   * @param parameters the parameters
   *
   * @throws InvalidTransactionException if any parameter is null
   */
  public void setParameters(Map<String, Parameter> parameters) throws InvalidTransactionException {
    if (parameters != null) {
      for (Parameter p : parameters.values()) {
        ContractData.require(p, "Parameters may not be null");
      }
    }
    this.parameters = parameters;
  }


  /**
   * Set the list of parties in this contract. The party list is required and must be non-empty.
   *
   * @param parties the parties
   *
   * @throws InvalidTransactionException if list is null or empty
   */
  public void setParties(List<Party> parties) throws InvalidTransactionException {
    ContractData.require(parties, "Contract party list must not be null");
    if (parties.isEmpty()) {
      throw new InvalidTransactionException("Contract party list must not be empty");
    }
    for (Party v : parties) {
      ContractData.require(v, "Party may not be null");
    }
    this.parties = parties;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setStartDate(long startDate) {
    this.startDate = startDate;
  }


  public void setStatus(List<String> statuses) {
    this.status = statuses;
  }

}
