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
package io.setl.bc.json.tx.external;

import javax.annotation.Nonnull;
import javax.json.JsonPatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.setl.bc.json.tx.internal.UpdateAclRoleTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Valerio Trigari, 17/02/2020.
 * @author Simon Greatrix.
 */
public class UpdateAclRole extends JsonUpdateTransaction {

  private SpaceId roleId;


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public UpdateAclRole(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "roleId", required = true) SpaceId roleId,
      @JsonProperty(value = "patch", required = true) JsonPatch patch
  ) {
    super(fromAddress, patch);
    this.roleId = roleId;
  }


  /**
   * New instance from the internal form of the transaction.
   *
   * @param tx the internal for
   */
  public UpdateAclRole(UpdateAclRoleTx tx) {
    super(tx);
    roleId = tx.getRoleId();
  }


  public UpdateAclRole() {
    // do nothing
  }


  @Override
  public UpdateAclRoleTx create() {
    return new UpdateAclRoleTx(this);
  }


  public SpaceId getRoleId() {
    return roleId;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_ACL_ROLE;
  }


  @JsonProperty(value = "roleId", required = true)
  public void setRoleId(SpaceId roleId) {
    this.roleId = roleId;
  }

}
