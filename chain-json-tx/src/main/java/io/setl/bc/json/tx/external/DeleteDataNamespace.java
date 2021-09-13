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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.internal.DeleteDataNamespaceTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Valerio Trigari 13/02/2020.
 */
public class DeleteDataNamespace extends JsonBaseTransaction {

  private boolean forceDelete = false;
  private String namespaceId;


  public DeleteDataNamespace() {
    // do nothing
  }


  /**
   * Create instance from internal representation.
   *
   * @param tx internal representation
   */
  public DeleteDataNamespace(DeleteDataNamespaceTx tx) {
    super(tx);
    namespaceId = tx.getNamespaceId();
    forceDelete = tx.isForceDelete();
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DeleteDataNamespace(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "id", required = true) String namespaceId,
      @JsonProperty(value = "forceDelete", required = false) Boolean forceDelete
  ) {
    super(fromAddress);
    this.namespaceId = namespaceId;
    this.forceDelete = forceDelete != null && forceDelete.booleanValue();
  }


  @Override
  public DeleteDataNamespaceTx create() {
    return new DeleteDataNamespaceTx(this);
  }


  public String getNamespaceId() {
    return namespaceId;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_DELETE_DATA_NAMESPACE;
  }


  public boolean isForceDelete() {
    return forceDelete;
  }


  @Schema(description = "Should the delete be forced?")
  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }


  public void setNamespaceId(String namespaceId) {
    this.namespaceId = namespaceId;
  }

}
