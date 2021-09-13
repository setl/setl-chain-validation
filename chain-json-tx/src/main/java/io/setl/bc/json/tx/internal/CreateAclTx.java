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

import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.tx.external.CreateAcl;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.state.tx.helper.AddressSet;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class CreateAclTx extends BaseTx {

  private Acl acl;


  public CreateAclTx(CreateAcl createAcl) {
    super(createAcl);
    acl = createAcl.getAcl();
  }


  public CreateAclTx(ObjectArrayReader encoded) {
    super(encoded);
    acl = new Acl(encoded.getReader());
  }


  @Override
  public Set<String> addresses() {
    HashSet<String> set = new HashSet<>();
    set.add(getFromAddress());
    set.addAll(acl.getAllAddresses());
    return AddressSet.of(set);
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    startHash(accumulator);
    accumulator.add(acl.encode());
    return accumulator;
  }


  public Acl getAcl() {
    return acl.copy();
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_CREATE_ACL;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public void setAcl(Acl acl) {
    this.acl = acl != null ? acl : Acl.EMPTY;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = acl.encode();
    return p;
  }
}
