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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;

import java.math.BigInteger;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.tx.Views.Output;

/**
 * @author Simon Greatrix on 31/08/2020.
 */
@Schema(description = "The definition of an exchange contract")
public class ExchangeContract implements ContractData {

  @Schema(
      description = "Should the contract elements be automatically signed on submission? Not all API endpoints support this feature."
  )
  private boolean autoSign = true;

  @Schema(
      description = "The contract's address. This is its unique identifier."
  )
  private String contractAddress;

  @Schema(description = "The address that issued this contract")
  private String contractAuthor;

  @Schema(
      description = "The next time (in seconds since the epoch) when this contract will be evaluated."
  )
  @JsonView(Output.class)
  private long eventTime;

  @Schema(description = "Time when this contract will expire, in seconds since the epoch.")
  private long expiry;

  @Schema(description = "Inputs to the exchange")
  @JsonInclude(Include.NON_EMPTY)
  private List<NominateAsset> inputs;

  @Schema(description = "The maximum number to exchange")
  private BigInteger maxBlocks = BigInteger.ZERO;

  @Schema(
      description = "Any additional data associated with this contract"
  )
  @JsonInclude(Include.NON_NULL)
  private String metadata;

  @Schema(description = "The minimum number to exchange")
  private BigInteger minBlocks = BigInteger.ZERO;

  @Schema(description = "The outputs from the exchange")
  @JsonInclude(Include.NON_EMPTY)
  private List<NominateAsset> outputs;

  @Schema(
      description = "A protocol associated with this contract"
  )
  @JsonInclude(Include.NON_NULL)
  private String protocol;

  @Schema(
      description = "The next time (in seconds since the epoch) when this contract will first be valid. It cannot complete before this time."
  )
  private long startDate;

  @Schema(description = "Result of the last update of this contract")
  private String status;


  public ExchangeContract() {
    // do nothing
  }


  public ExchangeContract(ExchangeContractData data) {
    contractAddress = data.get__address();
    contractAuthor = data.getIssuingaddress();
    eventTime = data.get__timeevent();
    expiry = data.getExpiry();
    inputs = data.getInputs();
    maxBlocks = data.getMaxblocks().bigintValue();
    metadata = data.getMetadata();
    minBlocks = data.getMinblocks().bigintValue();
    outputs = data.getOutputs();
    protocol = data.getProtocol();
    startDate = data.getStartdate();
    status = data.getStatus();
  }


  @Override
  public ExchangeContractData asInternalData() {
    return new ExchangeContractData(
        contractAddress,
        getContractType(),
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiry,
        new String[0],
        contractAuthor,
        protocol,
        metadata
    );
  }


  public String getContractAddress() {
    return contractAddress;
  }


  public String getContractAuthor() {
    return contractAuthor;
  }


  @Override
  public String getContractType() {
    return CONTRACT_NAME_EXCHANGE;
  }


  public long getEventTime() {
    return eventTime;
  }


  public long getExpiry() {
    return expiry;
  }


  public List<NominateAsset> getInputs() {
    return inputs;
  }


  public BigInteger getMaxBlocks() {
    return maxBlocks;
  }


  public String getMetadata() {
    return metadata;
  }


  public BigInteger getMinBlocks() {
    return minBlocks;
  }


  public List<NominateAsset> getOutputs() {
    return outputs;
  }


  public String getProtocol() {
    return protocol;
  }


  public long getStartDate() {
    return startDate;
  }


  public String getStatus() {
    return status;
  }


  public boolean isAutoSign() {
    return autoSign;
  }


  public void setAutoSign(boolean autoSign) {
    this.autoSign = autoSign;
  }


  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }


  public void setContractAuthor(String contractAuthor) {
    this.contractAuthor = contractAuthor;
  }


  public void setEventTime(long eventTime) {
    this.eventTime = eventTime;
  }


  public void setExpiry(long expiry) {
    this.expiry = expiry;
  }


  public void setInputs(List<NominateAsset> inputs) {
    this.inputs = inputs;
  }


  public void setMaxBlocks(BigInteger maxBlocks) {
    this.maxBlocks = maxBlocks;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setMinBlocks(BigInteger minBlocks) {
    this.minBlocks = minBlocks;
  }


  public void setOutputs(List<NominateAsset> outputs) {
    this.outputs = outputs;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setStartDate(long startDate) {
    this.startDate = startDate;
  }


  public void setStatus(String status) {
    this.status = status;
  }

}
