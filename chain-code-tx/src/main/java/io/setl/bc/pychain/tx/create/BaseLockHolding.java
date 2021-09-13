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
package io.setl.bc.pychain.tx.create;

import static io.setl.common.StringUtils.cleanString;

import java.math.BigInteger;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.LockHoldingTx;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
public abstract class BaseLockHolding extends BaseTransaction {

  @JsonProperty("amount")
  @Schema(description = "Amount of holding to be locked.", format = "int64")
  private BigInteger amount = BigInteger.ZERO;

  @JsonProperty("classId")
  @JsonAlias("instrument")
  @Schema(description = "The identifier of the asset's class.")
  @NotNull
  private String classId;

  @JsonAlias("fromaddress")
  @JsonProperty("address")
  @Schema(description = "The address enacting the lock. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String fromAddress;

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the locking address. Normally derived from the wallet.")
  @PublicKey
  private String fromPublicKey;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("namespace")
  @Schema(description = "The name of the namespace which contains the asset class.")
  @NotNull
  private String nameSpace;

  @JsonProperty("protocol")
  @Schema(description = "Protocol associated with the lock.")
  private String protocol;

  @JsonProperty("subjectAddress")
  @Schema(description = "The subject's address")
  @Address
  private String subjectAddress;


  public BaseLockHolding() {
    // do nothing
  }


  /**
   * Convert TX to a representation.
   *
   * @param tx the TX
   */
  public BaseLockHolding(LockHoldingTx tx) {
    super(tx);
    setAmount(toBigInteger(tx.getAmount()));
    setClassId(tx.getClassId());
    setFromAddress(tx.getFromAddress());
    setFromPublicKey(tx.getFromPublicKey());
    setMetadata(tx.getMetadata());
    setNameSpace(tx.getNameSpace());
    setProtocol(tx.getProtocol());
    setSubjectAddress(tx.getSubjectaddress());
  }


  public abstract LockHoldingTx create();


  public BigInteger getAmount() {
    return amount;
  }


  public String getClassId() {
    return classId;
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getFromPublicKey() {
    return fromPublicKey;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNameSpace() {
    return nameSpace;
  }


  @Nonnull
  @JsonIgnore
  @Hidden
  @Override
  public String getNonceAddress() {
    return fromAddress;
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getFromPublicKey();
  }


  public String getProtocol() {
    return protocol;
  }


  public String getSubjectAddress() {
    return subjectAddress;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setClassId(String classId) {
    this.classId = cleanString(classId);
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }


  public void setFromPublicKey(String fromPublicKey) {
    this.fromPublicKey = fromPublicKey;
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setNameSpace(String nameSpace) {
    this.nameSpace = cleanString(nameSpace);
  }


  @Override
  public void setNoncePublicKey(String key) {
    setFromPublicKey(key);
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setSubjectAddress(String subjectAddress) {
    this.subjectAddress = subjectAddress;
  }

}
