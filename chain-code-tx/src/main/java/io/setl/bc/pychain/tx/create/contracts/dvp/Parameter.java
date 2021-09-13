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
package io.setl.bc.pychain.tx.create.contracts.dvp;

import static io.setl.common.StringUtils.notNull;

import java.math.BigDecimal;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
public class Parameter {

  @Schema(
      description = "The address that owns this parameter"
  )
  @JsonInclude(Include.NON_NULL)
  private String address;

  @Schema(
      description = "The mathematical expression for this parameter's value. Either the expression or the value must be specified."
  )
  @JsonInclude(Include.NON_NULL)
  private String expression = "";

  @Schema(
      description = "Is this parameter an intermediate value to be evaluated in consensus and thus cannot be changed and does not require a signature?"
  )
  private boolean isCalculationOnly = false;

  @Schema(
      description = "Is this parameter used on multiple contracts, or specific to this one."
  )
  private boolean isContractSpecific = false;

  @Schema(
      description = "Evaluation order. Parameters with a lower order are evaluated first"
  )
  private int order = 0;

  @Schema(
      description = "The attorney's public key that signed this parameter"
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_NULL)
  private String poaPublicKey;

  @Schema(
      description = "The public key that signed this parameter"
  )
  @JsonInclude(Include.NON_NULL)
  private String publicKey;

  @Schema(
      description = "The signature approving this parameter"
  )
  @JsonInclude(Include.NON_NULL)
  private String signature;

  @Schema(
      description = "The value of this parameter. Either the value or the expression must be specified."
  )
  @JsonInclude(Include.NON_NULL)
  private BigDecimal value;


  public Parameter() {
    // do nothing
  }


  public Parameter(DvpParameter dvpParameter) {
    String authAddress = dvpParameter.getAddress();
    if (AddressUtil.verifyAddress(authAddress)) {
      address = authAddress;
      publicKey = null;
    } else {
      address = null;
      publicKey = authAddress;
    }
    poaPublicKey = dvpParameter.getPoaPublicKey();
    expression = dvpParameter.getValueString();
    Number n = dvpParameter.getValueNumber();
    if (n != null) {
      value = new BigDecimal(String.valueOf(n));
    }
    isContractSpecific = dvpParameter.contractSpecific != 0;
    order = dvpParameter.calculatedIndex;
    signature = dvpParameter.getSignature();
  }


  public DvpParameter asInternal() throws InvalidTransactionException {
    int contractSpecific = isContractSpecific ? 1 : 0;
    int calculationOnly = isCalculationOnly ? 1 : 0;

    // prefer a literal
    BigDecimal myValue = value;
    if (myValue == null && expression != null && expression.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")) {
      myValue = new BigDecimal(expression);
    }

    // prefer a public key
    String authority = publicKey != null ? publicKey : address;
    ContractData.require(authority, "Parameter must have an owning address or public key");

    DvpParameter parameter = (myValue != null)
        ? new DvpParameter(authority, myValue, order, contractSpecific, calculationOnly, signature)
        : new DvpParameter(address, expression, order, contractSpecific, calculationOnly, signature);

    // remember the PoA public key
    parameter.setPoaPublicKey(notNull(poaPublicKey));
    return parameter;
  }


  @Address
  public String getAddress() {
    return address;
  }


  public String getExpression() {
    return expression;
  }


  public int getOrder() {
    return order;
  }


  @PublicKey
  public String getPoaPublicKey() {
    return poaPublicKey;
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
  public boolean hasAddressOrPublicKey() {
    return publicKey != null || address != null;
  }


  @Hidden
  @JsonIgnore
  @AssertTrue
  public boolean hasValueOrExpression() {
    return value != null || expression != null;
  }


  public boolean isCalculationOnly() {
    return isCalculationOnly;
  }


  public boolean isContractSpecific() {
    return isContractSpecific;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setCalculationOnly(boolean calculationOnly) {
    isCalculationOnly = calculationOnly;
  }


  public void setContractSpecific(boolean contractSpecific) {
    isContractSpecific = contractSpecific;
  }


  public void setExpression(String expression) {
    this.expression = expression;
  }


  public void setOrder(int order) {
    this.order = order;
  }


  public void setPoaPublicKey(String poaPublicKey) {
    this.poaPublicKey = poaPublicKey;
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
