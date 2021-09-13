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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.internal.DeleteAclRoleTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 27/02/2020.
 */
public class DeleteAclRole extends JsonBaseTransaction {

  /** The ACL Role to delete. */
  private SpaceId aclRoleId;

  /** Should the delete be forced?. */
  private boolean forceDelete;


  public DeleteAclRole() {
  }

  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DeleteAclRole(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "aclRoleId", required = true) SpaceId aclRoleId,
      @JsonProperty(value = "forceDelete", required = false) Boolean forceDelete
  ) {
    super(fromAddress);
    this.aclRoleId = aclRoleId;
    this.forceDelete = forceDelete != null && forceDelete.booleanValue();
  }


  /**
   * New instance from the internal representation.
   *
   * @param tx the internal representation
   */
  public DeleteAclRole(DeleteAclRoleTx tx) {
    super(tx);
    aclRoleId = tx.getAclRoleId();
    forceDelete = tx.isForceDelete();
  }


  @Override
  public DeleteAclRoleTx create() {
    return new DeleteAclRoleTx(this);
  }


  public SpaceId getAclRoleId() {
    return aclRoleId;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_DELETE_ACL_ROLE;
  }


  public boolean isForceDelete() {
    return forceDelete;
  }


  @JsonProperty(required = true)
  @Schema(required = true, description = "The ID of the ACL Role to be deleted.")
  public void setAclRoleId(SpaceId aclRoleId) {
    this.aclRoleId = SpaceId.notNull(aclRoleId);
  }


  @Schema(description = "Should the delete be forced?")
  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }

}
