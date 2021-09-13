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

import io.setl.bc.json.tx.external.DeleteDataNamespace;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * Transaction to delete a JSON namespace.
 *
 * @author Valerio Trigari
 */
public class DeleteDataNamespaceTx extends BaseTx {

  private boolean forceDelete;

  private String namespaceId;


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public DeleteDataNamespaceTx(ObjectArrayReader reader) {
    super(reader);
    namespaceId = reader.getString();
    forceDelete = reader.getBoolean();
  }


  /**
   * New instance from the external representation.
   *
   * @param deleteDataDocument the representation
   */
  public DeleteDataNamespaceTx(DeleteDataNamespace deleteDataDocument) {
    super(deleteDataDocument);
    namespaceId = deleteDataDocument.getNamespaceId();
    forceDelete = deleteDataDocument.isForceDelete();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(namespaceId);
    accumulator.add(forceDelete);
    return accumulator;
  }


  public String getNamespaceId() {
    return namespaceId;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_DELETE_DATA_NAMESPACE;
  }


  public boolean isForceDelete() {
    return forceDelete;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 2;
  }


  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }


  public void setNamespaceId(String namespaceId) {
    this.namespaceId = namespaceId;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = namespaceId;
    encoded[p++] = forceDelete;
    return p;
  }

}
