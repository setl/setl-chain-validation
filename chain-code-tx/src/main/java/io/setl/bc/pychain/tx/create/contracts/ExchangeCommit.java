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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE_COMMIT;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;


import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;

/**
 * @author Simon Greatrix on 31/08/2020.
 */
@Schema(description = "Commitment to an asset exchange contract")
public class ExchangeCommit implements CommitData {

  @Schema(description = "Assets committed to the exchange")
  private List<NominateAsset> assetsIn = Collections.emptyList();

  @Schema(description = "The contract's address")
  private String contractAddress = "";

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "Additional data associated with this commitment")
  private String metadata = "";

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "Protocol to be used")
  private String protocol = "";

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "Recipient of the asset")
  private String toAddress = "";


  public ExchangeCommit() {
    // do nothing
  }


  public ExchangeCommit(ExchangeCommitData data) {
    assetsIn = data.getAssetsIn();
    contractAddress = data.contractAddress;
    metadata = data.metadata;
    protocol = data.protocol;
    toAddress = data.toaddress;
  }


  @Override
  public ExchangeCommitData asInternalData() {
    return new ExchangeCommitData(contractAddress, assetsIn, toAddress, protocol, metadata);
  }


  public List<NominateAsset> getAssetsIn() {
    return assetsIn;
  }


  public String getContractAddress() {
    return contractAddress;
  }


  @Override
  public String getContractType() {
    return CONTRACT_NAME_EXCHANGE_COMMIT;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getProtocol() {
    return protocol;
  }


  public String getToAddress() {
    return toAddress;
  }


  public void setAssetsIn(List<NominateAsset> assetsIn) {
    this.assetsIn = assetsIn;
  }


  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

}
