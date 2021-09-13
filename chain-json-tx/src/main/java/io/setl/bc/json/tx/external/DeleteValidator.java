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

import io.setl.bc.json.tx.internal.DeleteValidatorTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 27/02/2020.
 */
public class DeleteValidator extends JsonBaseTransaction {

  /** Should the delete be forced?. */
  private boolean forceDelete;

  /** The validator to delete. */
  private SpaceId validatorId;


  public DeleteValidator() {
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DeleteValidator(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "validatorId", required = true) SpaceId validatorId,
      @JsonProperty(value = "forceDelete", required = false) Boolean forceDelete
  ) {
    super(fromAddress);
    this.validatorId = validatorId;
    this.forceDelete = forceDelete != null && forceDelete.booleanValue();
  }


  /**
   * New instance from the internal representation.
   *
   * @param tx the internal representation
   */
  public DeleteValidator(DeleteValidatorTx tx) {
    super(tx);
    validatorId = tx.getValidatorId();
    forceDelete = tx.isForceDelete();
  }


  @Override
  public DeleteValidatorTx create() {
    return new DeleteValidatorTx(this);
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_DELETE_VALIDATOR;
  }


  public SpaceId getValidatorId() {
    return validatorId;
  }


  public boolean isForceDelete() {
    return forceDelete;
  }


  @Schema(description = "Should the delete be forced?")
  public void setForceDelete(boolean forceDelete) {
    this.forceDelete = forceDelete;
  }


  @JsonProperty(required = true)
  @Schema(required = true, description = "The ID of the validator to be deleted.")
  public void setValidatorId(SpaceId validatorId) {
    this.validatorId = SpaceId.notNull(validatorId);
  }

}
