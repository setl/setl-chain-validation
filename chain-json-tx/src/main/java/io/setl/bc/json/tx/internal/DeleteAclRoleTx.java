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

import io.setl.bc.json.tx.external.DeleteAclRole;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 27/02/2020.
 */
public class DeleteAclRoleTx extends BaseTx {

  /** The ACL Role to be deleted. */
  private SpaceId aclRoleId;

  /** Should the delete be forced?. */
  private boolean forceDelete;


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public DeleteAclRoleTx(ObjectArrayReader reader) {
    super(reader);
    aclRoleId = new SpaceId(reader.getReader());
    forceDelete = reader.getBoolean();
  }

  /**
   * New instance from the external representation.
   *
   * @param deleteAclRole the external representation
   */
  public DeleteAclRoleTx(DeleteAclRole deleteAclRole) {
    super(deleteAclRole);
    aclRoleId = deleteAclRole.getAclRoleId();
    forceDelete = deleteAclRole.isForceDelete();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(aclRoleId.encode());
    accumulator.add(forceDelete);
    return accumulator;
  }


  public SpaceId getAclRoleId() {
    return aclRoleId;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_DELETE_ACL_ROLE;
  }


  public boolean isForceDelete() {
    return forceDelete;
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 2;
  }


  public void setAclRoleId(SpaceId aclRoleId) {
    this.aclRoleId = aclRoleId;
  }


  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = aclRoleId.encode();
    encoded[p++] = forceDelete;
    return p;
  }

}
