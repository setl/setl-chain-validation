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
package io.setl.bc.json.tx.external;

import javax.annotation.Nonnull;
import javax.json.JsonPatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.setl.bc.json.tx.internal.UpdateDataNamespaceTx;
import io.setl.common.CommonPy.TxType;

/**
 * Update a namespace's definition.
 *
 * @author Valerio Trigari
 */
public class UpdateDataNamespace extends JsonUpdateTransaction {

  private String namespaceId;


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public UpdateDataNamespace(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "namespaceId", required = true) String namespaceId,
      @JsonProperty(value = "patch", required = true) JsonPatch patch
  ) {
    super(fromAddress, patch);
    this.namespaceId = namespaceId;
  }


  public UpdateDataNamespace(UpdateDataNamespaceTx tx) {
    super(tx);
    namespaceId = tx.getNamespaceId();
  }


  public UpdateDataNamespace() {
    // do nothing
  }


  @Override
  public UpdateDataNamespaceTx create() {
    return new UpdateDataNamespaceTx(this);
  }


  public String getNamespaceId() {
    return namespaceId;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_DATA_NAMESPACE;
  }


  @JsonProperty(required = true)
  public void setNamespaceId(String namespaceId) {
    this.namespaceId = namespaceId;
  }

}
