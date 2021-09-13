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

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.external.UpdateAcl;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Valerio Trigari, 17/02/2020.
 */
public class UpdateAclTx extends BaseUpdateTx {

  private SpaceId aclId;


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public UpdateAclTx(ObjectArrayReader reader) {
    super(reader);
    aclId = new SpaceId(reader.getReader());
  }


  /**
   * New instance from the external representation of the transaction.
   *
   * @param updateAcl the external representation
   */
  public UpdateAclTx(UpdateAcl updateAcl) {
    super(updateAcl);
    aclId = updateAcl.getAclId();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(aclId.encode());
    return accumulator;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = aclId.encode();
    return p;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_ACL;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public SpaceId getAclId() {
    return aclId;
  }


  public void setAclId(SpaceId aclId) {
    this.aclId = aclId;
  }

}
