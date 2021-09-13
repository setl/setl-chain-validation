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

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.tx.external.CreateDataDocument;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 29/01/2020.
 */
public class CreateDataDocumentTx extends BaseTx {

  private DataDocument dataDocument;


  public CreateDataDocumentTx(ObjectArrayReader reader) {
    super(reader);
    dataDocument = new DataDocument(reader.getReader());
  }


  public CreateDataDocumentTx(CreateDataDocument createDataDocument) {
    super(createDataDocument);
    dataDocument = createDataDocument.getDataDocument();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(dataDocument.encode());
    return accumulator;
  }


  public DataDocument getDataDocument() {
    return dataDocument.copy();
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_CREATE_DATA_DOCUMENT;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public void setDataDocument(DataDocument dataDocument) {
    this.dataDocument = dataDocument;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = dataDocument.encode();
    return p;
  }

}
