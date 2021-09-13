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
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.internal.UpdateDataDocumentDescriptionTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Valerio Trigari, 19/02/2020.
 */
public class UpdateDataDocumentDescription extends JsonUpdateTransaction {

  private SpaceId documentId;


  public UpdateDataDocumentDescription() {
    // do nothing
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public UpdateDataDocumentDescription(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "documentId", required = true) SpaceId documentId,
      @JsonProperty(value = "patch", required = true) JsonPatch patch
  ) {
    super(fromAddress, patch);
    this.documentId = documentId;
  }


  /**
   * New instance from the internal representation of the transaction.
   *
   * @param tx the internal representation
   */
  public UpdateDataDocumentDescription(UpdateDataDocumentDescriptionTx tx) {
    super(tx);
    documentId = tx.getDocumentId();
  }


  @Override
  public UpdateDataDocumentDescriptionTx create() {
    return new UpdateDataDocumentDescriptionTx(this);
  }


  public SpaceId getDocumentId() {
    return documentId;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_DESCRIPTION;
  }


  @Schema(description = "The document's ID", required = true)
  public void setDocumentId(SpaceId documentId) {
    this.documentId = documentId;
  }

}
