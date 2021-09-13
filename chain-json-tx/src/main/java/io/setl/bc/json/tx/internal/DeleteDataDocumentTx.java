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

import io.setl.bc.json.tx.external.DeleteDataDocument;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Valerio Trigari 13/02/2020.
 */
public class DeleteDataDocumentTx extends BaseTx {

  private SpaceId documentId;


  public DeleteDataDocumentTx(ObjectArrayReader reader) {
    super(reader);
    documentId = new SpaceId(reader.getReader());
  }


  public DeleteDataDocumentTx(DeleteDataDocument deleteDataDocument) {
    super(deleteDataDocument);
    documentId = deleteDataDocument.getDocumentId();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(documentId.encode());
    return accumulator;
  }


  public SpaceId getDocumentId() {
    return documentId;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_DELETE_DATA_DOCUMENT;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 2;
  }


  public void setDocumentId(SpaceId documentId) {
    this.documentId = documentId;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = documentId.encode();
    return p;
  }

}
