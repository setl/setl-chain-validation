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

import static io.setl.common.StringUtils.cleanString;

import java.math.BigInteger;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(description = "Commit to an add encumbrance")
public class AddEncumbrance {

  /**
   * If an object is specified (non null), then to match it must equal to the other value.
   *
   * @param a value specified
   * @param b value to match against
   *
   * @return true if this combination will not match.
   */
  private static boolean match(Object a, Object b) {
    return a == null || a.equals(b);
  }


  @Schema(description = "Address which will have its assets encumbered")
  private String address;

  @Schema(description = "Amount to be encumbered. Must match the contract.", format = "int64")
  private BigInteger amount;

  @Schema(description = "ID of the asset to be encumbered. Must match the contract.")
  private String asset;

  @Schema(
      description = "The expression that can be evaluated to yield the amount to be encumbered. Must match the contract."
  )
  @JsonInclude(Include.NON_NULL)
  private String expression;

  /** Is the expression a literal number?. */
  private boolean expressionIsLiteral = false;

  @Schema(description = "ID of the namespace that contains the asset to be encumbered. Must match the contract.")
  private String namespace;

  @Schema(description = "The public key which signed this")
  @JsonView({Output.class, Submission.class})
  private String publicKey;

  @Schema(description = "Reference for the new encumbrance")
  private String reference;

  @Schema(description = "Signature to authorise adding this encumbrance")
  @JsonView({Output.class, Submission.class})
  private String signature;


  public AddEncumbrance() {
    // do nothing
  }


  public AddEncumbrance(DvpCommitEncumbrance encumbrance) {
    address = encumbrance.getAddress();
    if (encumbrance.amount != null) {
      amount = encumbrance.amount.bigintValue();
      expression = null;
      expressionIsLiteral = false;
    } else {
      amount = null;
      expression = encumbrance.amountString;
      expressionIsLiteral = expression.matches("-?[0-9]+");
    }
    asset = encumbrance.assetID;
    publicKey = encumbrance.getPublicKey();
    reference = encumbrance.reference;
    signature = encumbrance.signature;
  }


  public DvpCommitEncumbrance asInternal() throws InvalidTransactionException {
    String pk = publicKey != null ? publicKey : address;
    ContractData.require(pk, "Either public key or address must be specified");
    BigInteger myAmount = amount;
    if (expressionIsLiteral) {
      myAmount = new BigInteger(expression);
    }
    return myAmount != null
        ? new DvpCommitEncumbrance(pk, namespace + "|" + asset, reference, myAmount, signature)
        : new DvpCommitEncumbrance(pk, namespace + "|" + asset, reference, expression, signature);
  }


  public void copyFrom(DvpAddEncumbrance matched) {
    reference = matched.reference;
    expression = matched.amountString;
    if (matched.amount != null) {
      amount = matched.amount.bigintValue();
    }

    String fullAssetId = matched.fullAssetID;
    int p;
    if (fullAssetId != null && (p = fullAssetId.indexOf('|')) != -1) {
      namespace = fullAssetId.substring(0, p);
      asset = fullAssetId.substring(p + 1);
    }
    String pk = matched.getPublicKey();
    if (AddressUtil.verifyAddress(pk)) {
      address = pk;
    } else if (AddressUtil.verifyPublicKey(pk)) {
      publicKey = pk;
    }
  }


  @Address
  public String getAddress() {
    return address;
  }


  public BigInteger getAmount() {
    return amount;
  }


  @NotNull
  @NotEmpty
  public String getAsset() {
    return asset;
  }


  public String getExpression() {
    return expression;
  }


  @NotNull
  @NotEmpty
  public String getNamespace() {
    return namespace;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


  @NotNull
  @NotEmpty
  public String getReference() {
    return reference;
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


  @Hidden
  @JsonIgnore
  @AssertTrue
  public boolean hasPublicKeyOrAddress() {
    return publicKey != null || address != null;
  }


  /**
   * Match the namespace and asset against the full asset ID, allowing for either the asset or namespace to be omitted.
   * @param encumbrance the add-encumbrance to match against
   * @return true if a match
   */
  private boolean matchAsset(DvpAddEncumbrance encumbrance) {
    String fullAssetId = encumbrance.fullAssetID;
    int p = Math.max(0, fullAssetId.indexOf('|'));
    String n = fullAssetId.substring(0, p);
    String a = fullAssetId.substring(p + 1);
    return match(asset, a) && match(namespace, n);
  }


  /**
   * Match the "public key" against the address or public key being added.
   * @param encumbrance the add-encumbrance to match against
   * @return true if a match
   */
  private boolean matchPublicKey(DvpAddEncumbrance encumbrance) {
    String pk = encumbrance.getPublicKey();
    if (pk == null) {
      return true;
    }
    if (AddressUtil.verifyAddress(pk)) {
      return match(address, pk);
    }
    return match(publicKey, pk);
  }


  /**
   * Tests if this commit matches an add-encumbrance entry in the contract data. A match is recognised if every non-null field in this matches the corresponding
   * field in contract data.
   *
   * @param encumbrance the entry in the contract data
   *
   * @return true if this is a potential match
   */
  public boolean matches(DvpAddEncumbrance encumbrance) {
    return match(reference, encumbrance.reference)
        && (amount == null || (encumbrance.amount != null && amount.equals(encumbrance.amount.bigintValue())))
        && match(expression, encumbrance.amountString)
        && matchAsset(encumbrance)
        && matchPublicKey(encumbrance);
  }


  /**
   * Set the address which will have some of its asset encumbered.
   *
   * @param address the address
   */
  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(BigInteger amount) {
    if (amount == null) {
      this.amount = null;
      return;
    }

    this.amount = amount;
    this.expression = null;
    expressionIsLiteral = false;
  }


  public void setAsset(String asset) {
    this.asset = cleanString(asset);
  }


  /**
   * Set the expression.
   *
   * @param expression the expression
   */
  public void setExpression(String expression) {
    amount = null;
    this.expression = expression;
    expressionIsLiteral = expression != null && expression.matches("-?[0-9]+");
  }


  public void setNamespace(String namespace) {
    this.namespace = cleanString(namespace);
  }


  /**
   * Sets the public key which will sign this.
   *
   * @param publicKey the signing key
   */
  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setReference(String reference) {
    this.reference = reference;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }

}

