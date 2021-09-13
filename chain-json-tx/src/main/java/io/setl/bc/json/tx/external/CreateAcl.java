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

import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.tx.internal.CreateAclTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class CreateAcl extends JsonBaseTransaction {

  private Acl acl = Acl.EMPTY;


  public CreateAcl() {
    // do nothing
  }


  public CreateAcl(CreateAclTx tx) {
    super(tx);
    acl = tx.getAcl();
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public CreateAcl(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "acl", required = true) Acl acl
  ) {
    super(fromAddress);
    setAcl(acl);
  }


  @Override
  public CreateAclTx create() {
    return new CreateAclTx(this);
  }


  public Acl getAcl() {
    return acl;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_CREATE_ACL;
  }


  @Schema(description = "The Access Control List", required = true)
  @JsonProperty(value = "acl", required = true)
  public final void setAcl(Acl acl) {
    this.acl = acl != null ? acl : Acl.EMPTY;
  }

}
