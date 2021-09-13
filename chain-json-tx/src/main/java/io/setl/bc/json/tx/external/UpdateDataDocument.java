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

import io.setl.bc.json.data.JsonSalt;
import io.setl.bc.json.tx.internal.UpdateDataDocumentTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Valerio Trigari, 17/02/2020.
 * @author Simon Greatrix.
 */
public class UpdateDataDocument extends JsonUpdateTransaction {

  private SpaceId documentId;

  private String salt;


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public UpdateDataDocument(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "documentId", required = true) SpaceId documentId,
      @JsonProperty(value = "patch", required = true) JsonPatch patch,
      @JsonProperty(value = "salt", required = false) String salt
  ) {
    super(fromAddress, patch);
    this.documentId = documentId;
    setSalt(salt);
  }


  /**
   * New instance from the internal form of the transaction.
   *
   * @param tx the internal for
   */
  public UpdateDataDocument(UpdateDataDocumentTx tx) {
    super(tx);
    documentId = tx.getDocumentId();
    salt = tx.getSalt();
  }


  public UpdateDataDocument() {
    // do nothing
  }


  @Override
  public UpdateDataDocumentTx create() {
    return new UpdateDataDocumentTx(this);
  }


  public SpaceId getDocumentId() {
    return documentId;
  }


  public String getSalt() {
    return salt;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_DATA_DOCUMENT;
  }


  @Schema(description = "The document's ID", required = true)
  @JsonProperty(value = "documentId", required = true)
  public void setDocumentId(SpaceId documentId) {
    this.documentId = documentId;
  }


  public void setSalt(String salt) {
    this.salt = (salt == null || salt.isEmpty()) ? JsonSalt.create() : salt;
  }

}
