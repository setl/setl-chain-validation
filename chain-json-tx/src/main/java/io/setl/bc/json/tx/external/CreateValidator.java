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

import io.setl.bc.json.data.DocumentValidator;
import io.setl.bc.json.tx.internal.CreateValidatorTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class CreateValidator extends JsonBaseTransaction {

  private DocumentValidator validator;


  public CreateValidator() {
    // do nothing
  }


  public CreateValidator(CreateValidatorTx tx) {
    super(tx);
    validator = tx.getValidator();
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public CreateValidator(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "validator", required = true) DocumentValidator validator
  ) {
    super(fromAddress);
    this.validator = validator;
  }


  @Override
  public CreateValidatorTx create() {
    return new CreateValidatorTx(this);
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_CREATE_VALIDATOR;
  }


  public DocumentValidator getValidator() {
    return validator;
  }


  @JsonProperty(value = "validator", required = true)
  @Schema(description = "The validator's specification", required = true)
  public void setValidator(DocumentValidator validator) {
    this.validator = validator;
  }

}
