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

import io.setl.bc.json.data.acl.AclRole;
import io.setl.bc.json.tx.internal.CreateAclRoleTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class CreateAclRole extends JsonBaseTransaction {

  private AclRole aclRole = AclRole.EMPTY;


  public CreateAclRole() {
    // do nothing
  }


  public CreateAclRole(CreateAclRoleTx tx) {
    super(tx);
    aclRole = tx.getAclRole();
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public CreateAclRole(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "aclRole", required = true) AclRole aclRole
  ) {
    super(fromAddress);
    setAclRole(aclRole);
  }


  @Override
  public CreateAclRoleTx create() {
    return new CreateAclRoleTx(this);
  }


  public AclRole getAclRole() {
    return aclRole;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_CREATE_ACL_ROLE;
  }


  @Schema(description = "The Access Control List Role definition", required = true)
  @JsonProperty(value = "aclRole", required = true)
  public final void setAclRole(AclRole aclRole) {
    this.aclRole = aclRole != null ? aclRole : AclRole.EMPTY;
  }


}
