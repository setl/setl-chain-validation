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

import io.setl.bc.json.tx.internal.UpdateAclTx;
import io.setl.common.CommonPy.TxType;

/**
 * Update an ACL.
 *
 * @author Valerio Trigari
 */
public class UpdateAcl extends JsonUpdateTransaction {

  private SpaceId aclId;


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public UpdateAcl(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "aclId", required = true) SpaceId aclId,
      @JsonProperty(value = "patch", required = true) JsonPatch patch
  ) {
    super(fromAddress, patch);
    this.aclId = aclId;
  }


  /**
   * New instance from the internal form of the transaction.
   *
   * @param tx the internal for
   */
  public UpdateAcl(UpdateAclTx tx) {
    super(tx);
    aclId = tx.getAclId();
  }


  public UpdateAcl() {
    // do nothing
  }


  @Override
  public UpdateAclTx create() {
    return new UpdateAclTx(this);
  }


  public SpaceId getAclId() {
    return aclId;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.JSON_UPDATE_ACL;
  }


  @JsonProperty(value = "aclId", required = true)
  public void setAclId(SpaceId aclId) {
    this.aclId = aclId;
  }

}
