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

import io.setl.bc.json.tx.internal.DeleteAclTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 27/02/2020.
 */
public class DeleteAcl extends JsonBaseTransaction {

  /** The ACL to delete. */
  private SpaceId aclId;

  /** Should the delete be forced?. */
  private boolean forceDelete;


  public DeleteAcl() {
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DeleteAcl(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "aclId", required = true) SpaceId aclId,
      @JsonProperty(value = "forceDelete", required = false) Boolean forceDelete
  ) {
    super(fromAddress);
    this.aclId = aclId;
    this.forceDelete = forceDelete != null && forceDelete.booleanValue();
  }


  /**
   * New instance from the internal representation.
   *
   * @param tx the internal representation
   */
  public DeleteAcl(DeleteAclTx tx) {
    super(tx);
    aclId = tx.getAclId();
    forceDelete = tx.isForceDelete();
  }


  @Override
  public DeleteAclTx create() {
    return new DeleteAclTx(this);
  }


  public SpaceId getAclId() {
    return aclId;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_DELETE_ACL;
  }


  public boolean isForceDelete() {
    return forceDelete;
  }


  @JsonProperty(required = true)
  @Schema(required = true, description = "The ID of the ACL to be deleted.")
  public void setAclId(SpaceId aclId) {
    this.aclId = SpaceId.notNull(aclId);
  }


  @Schema(description = "Should the delete be forced?")
  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }

}
