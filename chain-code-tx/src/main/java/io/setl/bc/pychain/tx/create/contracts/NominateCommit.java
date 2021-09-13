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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_NOMINATE;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;


import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData.AssetIn;

/**
 * @author Simon Greatrix on 31/08/2020.
 */
public class NominateCommit implements CommitData {

  @Schema(description = "The ID of this asset")
  private String assetId;

  @Schema(description = "Assets added to the contract")
  private List<AssetIn> assetsIn;

  @Schema(description = "The contract's address")
  private String contractAddress;

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "Additional data associated with this commitment")
  private String metadata;

  @Schema(description = "The ID of the namespace that contains the asset")
  private String namespace;

  @JsonInclude(Include.NON_EMPTY)
  @Schema(description = "Protocol to be used")
  private String protocol;


  public NominateCommit() {
    // do nothing
  }


  public NominateCommit(NominateCommitData data) {
    assetId = data.getAssetclass();
    contractAddress = data.contractAddress;
    metadata = data.metadata;
    namespace = data.getNamespace();
    protocol = data.protocol;
    assetsIn = data.getAssetsIn();
  }


  @Override
  public NominateCommitData asInternalData() {
    NominateCommitData data = new NominateCommitData(
        namespace,
        assetId,
        protocol,
        metadata,
        contractAddress
    );
    if (assetsIn != null) {
      assetsIn.forEach(data::setAssetIn);
    }
    return data;
  }


  public String getAssetId() {
    return assetId;
  }


  public List<AssetIn> getAssetsIn() {
    return assetsIn;
  }


  public String getContractAddress() {
    return contractAddress;
  }


  @Override
  public String getContractType() {
    return CONTRACT_NAME_NOMINATE;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNamespace() {
    return namespace;
  }


  public String getProtocol() {
    return protocol;
  }


  public void setAssetId(String assetId) {
    this.assetId = assetId;
  }


  public void setAssetsIn(List<AssetIn> assetsIn) {
    this.assetsIn = assetsIn;
  }


  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

}
