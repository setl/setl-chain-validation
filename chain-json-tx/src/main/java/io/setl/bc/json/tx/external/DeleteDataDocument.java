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

import io.setl.bc.json.tx.internal.DeleteDataDocumentTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Valerio Trigari 13/02/2020.
 */
public class DeleteDataDocument extends JsonBaseTransaction {

  private SpaceId documentId;


  public DeleteDataDocument() {
    // do nothing
  }


  public DeleteDataDocument(DeleteDataDocumentTx tx) {
    super(tx);
    documentId = tx.getDocumentId();
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DeleteDataDocument(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "documentId", required = true) SpaceId documentId
  ) {
    super(fromAddress);
    this.documentId = documentId;
  }


  @Override
  public DeleteDataDocumentTx create() {
    return new DeleteDataDocumentTx(this);
  }


  public SpaceId getDocumentId() {
    return documentId;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_DELETE_DATA_DOCUMENT;
  }


  public void setDocumentId(SpaceId documentId) {
    this.documentId = SpaceId.notNull(documentId);
  }

}
