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
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
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
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.create.BaseEncumber.Participant;
import io.setl.bc.pychain.tx.create.contracts.ContractData;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
@Schema(
    description = "An encumbrance to created on successful contract completion"
)
public class AddEncumbrance {

  @Schema(
      description = "The address which will have some of its assets encumbered"
  )
  private String address;

  @Schema(
      description = "Administrators of the new encumbrance"
  )
  private List<Participant> administrators;

  @Schema(
      description = "The amount to be encumbered. Either the amount or an expression must be specified.",
      format = "int64"
  )
  @JsonInclude(Include.NON_NULL)
  private BigInteger amount;

  @Schema(
      description = "The asset to be encumbered"
  )
  private String asset;

  @Schema(
      description = "Beneficiaries of the new encumbrance"
  )
  private List<Participant> beneficiaries;

  @Schema(
      description = "An expression that can be evaluated to yield the amount to be encumbered. Either an expression or the amount must be specified."
  )
  @JsonInclude(Include.NON_NULL)
  private String expression;

  @Schema(
      description = "The namespace which contains the asset"
  )
  private String namespace;

  @Schema(
      description = "The attorney's public key that approved this encumbrance."
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_EMPTY)
  private String poaPublicKey = null;

  @Schema(
      description = "The public key associated with the address that will have some of its assets encumbered"
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_EMPTY)
  private String publicKey = null;

  @Schema(
      description = "The reference which will identify the new encumbrance"
  )
  private String reference;

  @Schema(
      description = "Signature confirming this encumbrance will be added"
  )
  @JsonView(Output.class)
  @JsonInclude(Include.NON_EMPTY)
  private String signature = "";


  public AddEncumbrance() {
    // do nothing
  }


  public AddEncumbrance(DvpAddEncumbrance addEncumbrance) {
    String fullAssetId = addEncumbrance.fullAssetID;
    int barPosition = fullAssetId.indexOf('|');
    asset = fullAssetId.substring(barPosition + 1);
    namespace = fullAssetId.substring(0, barPosition);

    String addressOrKey = addEncumbrance.getPublicKey();
    if (AddressUtil.verifyAddress(addressOrKey)) {
      address = addressOrKey;
      publicKey = null;
    } else if (addressOrKey != null) {
      address = null;
      publicKey = addressOrKey;
    }

    Balance b = addEncumbrance.amount;
    amount = (b != null) ? b.bigintValue() : null;
    expression = addEncumbrance.amountString;
    poaPublicKey = addEncumbrance.getPoaPublicKey();
    reference = addEncumbrance.reference;
    signature = addEncumbrance.getSignature();

    administrators = addEncumbrance.administrators.stream().map(Participant::new).collect(Collectors.toList());
    beneficiaries = addEncumbrance.beneficiaries.stream().map(Participant::new).collect(Collectors.toList());
  }


  public DvpAddEncumbrance asInternal() throws InvalidTransactionException {
    ContractData.require(administrators, "Encumbrance administrators must be specified for Add Encumbrance");
    ContractData.require(beneficiaries, "Encumbrance beneficiaries must be specified for Add Encumbrance");
    ContractData.require(reference, "Encumbrance reference must be specified for Add Encumbrance");
    ContractData.require(namespace, "Asset namespace must be specified for Add Encumbrance");
    ContractData.require(asset, "Asset ID must be specified for Add Encumbrance");
    if (amount == null && expression == null) {
      throw new InvalidTransactionException("Either amount or expression must be specified for Add Encumbrance");
    }

    DvpAddEncumbrance internal;
    BigInteger myAmount = amount;
    if (myAmount == null && expression.matches("-?[0-9]+")) {
      myAmount = new BigInteger(expression);
    }

    String id = publicKey != null ? publicKey : address;

    if (myAmount != null) {
      // amount takes precedence
      internal = new DvpAddEncumbrance(
          namespace + "|" + asset,
          reference,
          myAmount,
          id,
          notNull(poaPublicKey),
          signature
      );
    } else {
      internal = new DvpAddEncumbrance(
          namespace + "|" + asset,
          reference,
          expression,
          id,
          notNull(poaPublicKey),
          signature
      );
    }

    administrators.forEach(p -> internal.administrators.add(p.asDetail()));
    beneficiaries.forEach(p -> internal.beneficiaries.add(p.asDetail()));

    return internal;
  }


  @Address
  @NotNull
  public String getAddress() {
    return address;
  }


  @Valid
  @NotEmpty
  public List<@NotNull @Valid Participant> getAdministrators() {
    return administrators;
  }


  @NotNull
  public BigInteger getAmount() {
    return amount;
  }


  @NotNull
  public String getAsset() {
    return asset;
  }


  @Valid
  @NotEmpty
  public List<@NotNull @Valid Participant> getBeneficiaries() {
    return beneficiaries;
  }


  public String getExpression() {
    return expression;
  }


  @NotNull
  public String getNamespace() {
    return namespace;
  }


  @PublicKey
  public String getPoaPublicKey() {
    return poaPublicKey;
  }


  @PublicKey
  public String getPublicKey() {
    return publicKey;
  }


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
  public boolean hasAddressOrPublicKey() {
    return publicKey != null || address != null;
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


  public void setAdministrators(List<Participant> administrators) {
    this.administrators = administrators;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setAsset(String asset) {
    this.asset = cleanString(asset);
  }


  public void setBeneficiaries(List<Participant> beneficiaries) {
    this.beneficiaries = beneficiaries;
  }


  public void setExpression(String expression) {
    this.expression = expression;
  }


  public void setNamespace(String namespace) {
    this.namespace = cleanString(namespace);
  }


  public void setPoaPublicKey(String poaPublicKey) {
    this.poaPublicKey = poaPublicKey;
  }


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
