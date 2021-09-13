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

import static com.google.common.base.Strings.isNullOrEmpty;

import static io.setl.common.StringUtils.notNull;

import java.math.BigInteger;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitment;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(
    description = "Commitment to an outward payment"
)
public class PayOut {

  @Schema(description = "The address signing the commitment. Only required when using a PoA commitment.")
  @JsonInclude(Include.NON_NULL)
  private String address;

  @Schema(description = "The amount of asset to pay. Only required when the contract data is not available to the server.", format = "int64")
  @JsonInclude(Include.NON_NULL)
  private BigInteger amount;

  @Schema(description = "The asset's ID. Only required when the contract data is not available to the server.")
  @JsonInclude(Include.NON_EMPTY)
  private String asset = "";

  @Schema(description = "The expression that provides the amount of asset to pay. Only required when the contract data is not available to the server.")
  @JsonInclude(Include.NON_NULL)
  private String expression;

  @Schema(description = "The index of this outward payment in the party's contract data list of outward payments. Required.")
  private int index = 0;

  @Schema(description = "The ID of the namespace that contains the asset. Only required when the contract data is not available to the server.")
  @JsonInclude(Include.NON_EMPTY)
  private String namespace = "";

  @Schema(description = "The public key that signed this outward payment")
  @JsonInclude(Include.NON_NULL)
  @JsonView({Output.class, Submission.class})
  private String publicKey;

  @Schema(description = "The signature for this outward payment")
  @JsonInclude(Include.NON_NULL)
  @JsonView({Output.class, Submission.class})
  private String signature;


  public PayOut() {
    // do nothing
  }


  public PayOut(DvpCommitment dvp) {
    address = dvp.getPayAddress();
    if (dvp.amount != null) {
      amount = dvp.amount.bigintValue();
      expression = null;
    } else {
      amount = null;
      expression = dvp.amountString;
    }
    asset = notNull(dvp.classID);
    namespace = notNull(dvp.namespace);
    publicKey = dvp.getPublicKey();
    signature = dvp.signature;
  }


  public DvpCommitment asInternal() {
    if (!(isNullOrEmpty(publicKey) && isNullOrEmpty(signature))) {
      // has public key and signature, so is internal representation
      return new DvpCommitment((long) index, publicKey, signature);
    }

    // NB: Does not have to specify amount nor expression as that is already in the contract data

    return new DvpCommitment(new Object[]{
        index,
        namespace,
        asset,
        amount != null ? amount : expression,
        isNullOrEmpty(address) ? publicKey : address,
        signature
    });
  }


  @Address
  public String getAddress() {
    return address;
  }


  public BigInteger getAmount() {
    return amount;
  }


  public String getAsset() {
    return asset;
  }


  public String getExpression() {
    return expression;
  }


  public int getIndex() {
    return index;
  }


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
  public boolean hasAddressOrPublicKey() {
    return publicKey != null || address != null;
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


  public void setExpression(String expression) {
    this.expression = expression;
  }


  public void setIndex(int index) {
    this.index = index;
  }


  public void setNamespace(String namespace) {
    this.namespace = notNull(namespace);
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}
