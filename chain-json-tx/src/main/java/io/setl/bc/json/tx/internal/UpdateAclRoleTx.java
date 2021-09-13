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
import io.setl.bc.json.tx.external.UpdateAclRole;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Valerio Trigari, 17/02/2020.
 */
public class UpdateAclRoleTx extends BaseUpdateTx {

  private SpaceId roleId;


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public UpdateAclRoleTx(ObjectArrayReader reader) {
    super(reader);
    roleId = new SpaceId(reader.getReader());
  }


  /**
   * New instance from the external representation of the transaction.
   *
   * @param updateAclRole the external representation
   */
  public UpdateAclRoleTx(UpdateAclRole updateAclRole) {
    super(updateAclRole);
    roleId = updateAclRole.getRoleId();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.addAll(roleId.encode());
    return accumulator;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = roleId.encode();
    return p;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_ACL_ROLE;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public SpaceId getRoleId() {
    return roleId;
  }


  public void setRoleId(SpaceId roleId) {
    this.roleId = roleId;
  }

}
