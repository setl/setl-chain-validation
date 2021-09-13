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
package io.setl.bc.json.tx.internal;

import io.setl.bc.json.data.JsonSalt;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.external.UpdateDataDocument;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Valerio Trigari, 17/02/2020.
 */
public class UpdateDataDocumentTx extends BaseUpdateTx {

  private SpaceId documentId;

  private String salt;


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public UpdateDataDocumentTx(ObjectArrayReader reader) {
    super(reader);
    documentId = new SpaceId(reader.getReader());
    setSalt(reader.getString());
  }


  /**
   * New instance from the external representation of the transaction.
   *
   * @param updateDataDocument the external representation
   */
  public UpdateDataDocumentTx(UpdateDataDocument updateDataDocument) {
    super(updateDataDocument);
    documentId = updateDataDocument.getDocumentId();
    setSalt(updateDataDocument.getSalt());
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(documentId.encode());
    accumulator.add(getSalt());
    return accumulator;
  }


  public SpaceId getDocumentId() {
    return documentId;
  }


  public String getSalt() {
    return salt;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_DATA_DOCUMENT;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 2;
  }


  public void setDocumentId(SpaceId documentId) {
    this.documentId = documentId;
  }


  public void setSalt(String salt) {
    this.salt = (salt == null || salt.isEmpty()) ? JsonSalt.create() : salt;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = documentId.encode();
    encoded[p++] = salt;
    return p;
  }

}
