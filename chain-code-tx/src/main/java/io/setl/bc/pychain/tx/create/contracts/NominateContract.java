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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import io.swagger.v3.oas.annotations.media.Schema;


import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.common.Balance;

/**
 * @author Simon Greatrix on 31/08/2020.
 */
@Schema(description = "Tokens nominate contract data")
public class NominateContract implements ContractData {

  private Balance blockSizeIn;

  private Balance blockSizeOut;

  private String contractAddress;

  private String contractAuthor;

  private long eventTime;

  private long expiry;

  private String inputTokenAssetId;

  private String metadata;

  private String namespace;

  private String outputTokenAssetId;

  private String protocol;

  private String status;


  public NominateContract() {
    // do nothing
  }


  public NominateContract(TokensNominateContractData data) {
    blockSizeIn = data.blocksizein;
    blockSizeOut = data.blocksizeout;
    contractAddress = data.get__address();
    eventTime = data.get__timeevent();
    expiry = data.expiry;
    inputTokenAssetId = data.getInputtokenclass();
    contractAuthor = data.issuingaddress;
    metadata = data.getMetadata();
    namespace = data.getNamespace();
    outputTokenAssetId = data.getOutputtokenclass();
    protocol = data.getProtocol();
    status = data.status;
  }


  @Override
  public TokensNominateContractData asInternalData() {
    TokensNominateContractData data = new TokensNominateContractData(
        contractAddress,
        getContractType(),
        namespace,
        inputTokenAssetId,
        outputTokenAssetId,
        blockSizeIn,
        blockSizeOut,
        expiry,
        new String[0],
        contractAuthor,
        protocol,
        metadata
    );
    data.set__timeevent(eventTime);
    data.status = status;
    return data;
  }


  public Balance getBlockSizeIn() {
    return blockSizeIn;
  }


  public Balance getBlockSizeOut() {
    return blockSizeOut;
  }


  public String getContractAddress() {
    return contractAddress;
  }


  public String getContractAuthor() {
    return contractAuthor;
  }


  @Override
  public String getContractType() {
    return CONTRACT_NAME_TOKENS_NOMINATE;
  }


  public long getEventTime() {
    return eventTime;
  }


  public long getExpiry() {
    return expiry;
  }


  public String getInputTokenAssetId() {
    return inputTokenAssetId;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNamespace() {
    return namespace;
  }


  public String getOutputTokenAssetId() {
    return outputTokenAssetId;
  }


  public String getProtocol() {
    return protocol;
  }


  public String getStatus() {
    return status;
  }


  public void setBlockSizeIn(Balance blockSizeIn) {
    this.blockSizeIn = blockSizeIn;
  }


  public void setBlockSizeOut(Balance blockSizeOut) {
    this.blockSizeOut = blockSizeOut;
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


  public void setInputTokenAssetId(String inputTokenAssetId) {
    this.inputTokenAssetId = inputTokenAssetId;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }


  public void setOutputTokenAssetId(String outputTokenAssetId) {
    this.outputTokenAssetId = outputTokenAssetId;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setStatus(String status) {
    this.status = status;
  }

}
