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

import static io.setl.common.StringUtils.cleanString;

import java.math.BigInteger;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.validation.annotations.Address;

/**
 * @author Simon Greatrix on 28/08/2020.
 */
@Schema(
    description = "A payment into an address's balance. Inward payments do not need to be signed."
)
public class PayIn {

  @Schema(
      description = "The address which will receive the payment. Defaults to the party's signing address."
  )
  private String address;

  @JsonProperty("amount")
  @Min(0)
  @Schema(description = "The amount of asset transferred. Either the amount or the expression must be specified.", format = "int64")
  @JsonInclude(Include.NON_NULL)
  private BigInteger amount;

  @Schema(
      description = "The asset's ID"
  )
  private String asset = "";

  @Schema(
      description = "An expression that specified the amount of asset transferred. Either the amount or the expression must be specified."
  )
  @JsonInclude(Include.NON_NULL)
  private String expression = "";

  @Schema(
      description = "The ID of the namespace that contains the asset"
  )
  private String namespace = "";


  public PayIn() {
    // do nothing
  }


  public PayIn(DvpReceiveItem in) {
    address = in.address;
    if (in.amountNumber != null) {
      amount = in.amountNumber.bigintValue();
      expression = null;
    } else {
      amount = null;
      expression = in.amountString;
    }
    asset = in.classID;
    namespace = in.namespace;
  }


  public DvpReceiveItem asInternal() throws InvalidTransactionException {
    BigInteger myAmount = amount;
    if (myAmount == null && expression != null && expression.matches("-?[0-9]+")) {
      myAmount = new BigInteger(expression);
    }
    if (myAmount != null) {
      return new DvpReceiveItem(address, namespace, asset, amount);
    }
    if (expression == null) {
      throw new InvalidTransactionException("Either amount or expression must be specified for pay-in");
    }
    return new DvpReceiveItem(address, namespace, asset, expression);
  }


  @Address
  public String getAddress() {
    return address;
  }


  @NotNull
  public String getAsset() {
    return asset;
  }


  public String getExpression() {
    return expression;
  }


  @NotNull
  public String getNamespace() {
    return namespace;
  }


  @Hidden
  @JsonIgnore
  @AssertTrue
  public boolean hasAmountOrExpression() {
    return amount != null || expression != null;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setAsset(String asset) {
    this.asset = cleanString(asset);
  }


  public void setExpression(String expression) {
    this.expression = expression;
  }


  public void setNamespace(String namespace) {
    this.namespace = cleanString(namespace);
  }

}
