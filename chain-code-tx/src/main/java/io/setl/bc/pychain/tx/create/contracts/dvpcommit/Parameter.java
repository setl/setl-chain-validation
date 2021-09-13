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
package io.setl.bc.pychain.tx.create.contracts.dvpcommit;

import java.math.BigDecimal;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParameter;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(
    description = "A commitment to a parameter in the contract"
)
public class Parameter {

  @Schema(
      description = "The address that is committing to this parameter"
  )
  private String address;

  @Schema(
      description = "The expression that provides the parameter. Either the value or the expression must be specified."
  )
  private String expression;

  @Schema(
      description = "Is this parameter specific to this contract, or applicable to many contracts?"
  )
  private boolean isContractSpecific = false;

  @Schema(
      description = "The public key that signed this parameter"
  )
  @JsonView({Output.class, Submission.class})
  private String publicKey;

  @Schema(
      description = "The signature for this parameter"
  )
  @JsonView({Output.class, Submission.class})
  private String signature;

  @Schema(
      description = "The value of the parameter. Either the value or the expression must be specified."
  )
  private BigDecimal value;


  public Parameter() {
    // do nothing
  }


  public Parameter(DvpCommitParameter parameter) {
    String addressOrKey = parameter.getPublicKey();
    Number n = parameter.getValueNumber();
    if (n != null) {
      if (n instanceof BigDecimal) {
        value = (BigDecimal) n;
      } else {
        value = new BigDecimal(n.toString());
      }
      expression = null;
    } else {
      value = null;
      expression = parameter.getValueString();
    }
    isContractSpecific = parameter.contractSpecific != null && parameter.contractSpecific != 0;
    signature = parameter.signature;

    if (AddressUtil.verifyAddress(addressOrKey)) {
      address = addressOrKey;
      publicKey = null;
    } else {
      address = null;
      publicKey = addressOrKey;
    }
  }


  public DvpCommitParameter asInternal(String name) throws InvalidTransactionException {
    Object v = value;
    if (v == null && expression != null) {
      if (expression.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")) {
        v = new BigDecimal(expression);
      } else {
        v = expression;
      }
    }
    ContractData.require(v, "Either value or expression must be specified for parameter");
    return new DvpCommitParameter(name, v, isContractSpecific ? 1 : 0, publicKey, signature);
  }


  @Address
  public String getAddress() {
    return address;
  }


  public String getExpression() {
    return expression;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  public String getSignature() {
    return signature;
  }


  public BigDecimal getValue() {
    return value;
  }


  @Hidden
  @JsonIgnore
  @AssertTrue
  public boolean hasValueOrExpression() {
    return value != null || expression != null;
  }


  public boolean isContractSpecific() {
    return isContractSpecific;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setContractSpecific(boolean contractSpecific) {
    isContractSpecific = contractSpecific;
  }


  public void setExpression(String expression) {
    this.expression = expression;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }


  public void setValue(BigDecimal value) {
    this.value = value;
  }

}
