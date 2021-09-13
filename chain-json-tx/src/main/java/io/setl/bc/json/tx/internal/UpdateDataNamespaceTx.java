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

import io.setl.bc.json.tx.external.UpdateDataNamespace;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Valerio Trigari.
 */
public class UpdateDataNamespaceTx extends BaseUpdateTx {

  private String namespaceId;


  public UpdateDataNamespaceTx(ObjectArrayReader reader) {
    super(reader);
    namespaceId = reader.getString();
  }


  public UpdateDataNamespaceTx(UpdateDataNamespace updateDataNamespace) {
    super(updateDataNamespace);
    namespaceId = updateDataNamespace.getNamespaceId();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hacc) {
    super.startHash(hacc);
    hacc.add(namespaceId);
    return hacc;
  }


  public String getNamespaceId() {
    return namespaceId;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_DATA_NAMESPACE;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public void setNamespaceId(String namespaceId) {
    this.namespaceId = namespaceId;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = namespaceId;
    return p;
  }

}
