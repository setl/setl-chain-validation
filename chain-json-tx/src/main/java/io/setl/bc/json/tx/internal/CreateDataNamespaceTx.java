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

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.NamespacePrivileges;
import io.setl.bc.json.tx.external.CreateDataNamespace;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.state.tx.helper.AddressSet;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Simon Greatrix on 20/01/2020.
 */
public class CreateDataNamespaceTx extends BaseTx {

  private DataNamespace namespace;


  public CreateDataNamespaceTx(ObjectArrayReader reader) {
    super(reader);
    namespace = new DataNamespace(reader.getReader());
  }


  public CreateDataNamespaceTx(CreateDataNamespace createDataNamespace) {
    super(createDataNamespace);
    namespace = createDataNamespace.getNamespace();
  }


  @Override
  public Set<String> addresses() {
    HashSet<String> set = new HashSet<>();
    set.add(getFromAddress());
    NamespacePrivileges np = namespace.getPrivileges();
    set.addAll(np.getAllAddresses());
    return AddressSet.of(set);
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(namespace.encode());
    return accumulator;
  }


  public DataNamespace getNamespace() {
    return namespace.copy();
  }


  @Override
  public int getPriority() {
    return TxType.JSON_CREATE_DATA_NAMESPACE.getPriority();
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_CREATE_DATA_NAMESPACE;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public void setNamespace(DataNamespace namespace) {
    this.namespace = namespace;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = namespace.encode();
    return p;
  }
}
