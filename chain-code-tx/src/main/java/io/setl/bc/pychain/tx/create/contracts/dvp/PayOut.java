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
import static io.setl.common.StringUtils.notNull;

import java.math.BigInteger;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 28/08/2020.
 */
@Schema(
    description = "A payment out from an address's balance. Outward payments must be signed, unless covered by the contract encumbrance."
)
public class PayOut {

  @Schema(
      description = "The address that is sending the payments. Defaults to the party's signing address."
  )
  private String address;

  @Schema(
      description = "The amount of asset transferred. Either the amount or the expression must be specified.", format = "int64"
  )
  @JsonInclude(Include.NON_NULL)
  private BigInteger amount;

  @Schema(
      description = "The asset's ID"
  )
  private String asset = "";

  @Schema(
      description = "A reference to an encumbrance that can be used to fulfill this payment. Overrides the contract encumbrance."
  )
  @JsonInclude(Include.NON_NULL)
  private String encumbrance;

  @Schema(
      description = "An expression that specified the amount of asset transferred. Either the amount or the expression must be specified."
  )
  @JsonInclude(Include.NON_EMPTY)
  private String expression = "";

  @Schema(
      description = "If the party has signed, and is the issuer of the asset, issue new assets to fulfill the contract"
  )
  private boolean isIssuance;

  @Schema(
      description = "Additional data associated with this payment"
  )
  @JsonInclude(Include.NON_EMPTY)
  private String metadata = "";

  @Schema(
      description = "The ID of the namespace that contains the asset"
  )
  private String namespace = "";

  @Schema(
      description = "The public key which signed this payment"
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_NULL)
  private String publicKey;

  @Schema(
      description = "The address's signature on this payment"
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_NULL)
  private String signature;


  public PayOut() {
    // do nothing
  }


  public PayOut(DvpPayItem out) {
    address = out.address;
    if (out.amountNumber != null) {
      amount = out.amountNumber.bigintValue();
      expression = null;
    } else {
      amount = null;
      expression = out.amountString;
    }
    asset = out.classID;
    encumbrance = out.encumbrance;
    isIssuance = out.issuance;
    metadata = out.metadata;
    namespace = out.namespace;
    publicKey = out.publicKey;
    signature = out.signature;
  }


  public DvpPayItem asInternal() {
    BigInteger myAmount = amount;
    if (myAmount == null && expression != null && expression.matches("-?[0-9]+")) {
      myAmount = new BigInteger(expression);
    }
    if (myAmount != null) {
      return new DvpPayItem(address, namespace, asset, myAmount, publicKey, signature, isIssuance, metadata, encumbrance);
    }
    return new DvpPayItem(address, namespace, asset, expression, publicKey, signature, isIssuance, metadata, encumbrance);
  }


  @Address
  public String getAddress() {
    return address;
  }


  public BigInteger getAmount() {
    return amount;
  }


  @NotNull
  public String getAsset() {
    return asset;
  }


  public String getEncumbrance() {
    return encumbrance;
  }


  public String getExpression() {
    return expression;
  }


  public String getMetadata() {
    return metadata;
  }


  @NotNull
  public String getNamespace() {
    return namespace;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  public String getSignature() {
    return signature;
  }


  @Hidden
  @JsonIgnore
  @AssertTrue
  public boolean hasAmountOrExpression() {
    return amount != null || expression != null;
  }


  public boolean isIssuance() {
    return isIssuance;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setAsset(String asset) {
    this.asset = notNull(asset);
  }


  public void setEncumbrance(String encumbrance) {
    this.encumbrance = encumbrance;
  }


  public void setExpression(String expression) {
    this.expression = expression;
  }


  public void setIssuance(boolean issuance) {
    isIssuance = issuance;
  }


  public void setMetadata(String metadata) {
    this.metadata = notNull(metadata);
  }


  public void setNamespace(String namespace) {
    this.namespace = cleanString(namespace);
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
