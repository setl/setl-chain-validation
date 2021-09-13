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
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.UnEncumberTx;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
public abstract class BaseUnEncumber extends BaseTransaction {

  /**
   * UnEncumberTx.
   * <p>Return unsigned UnEncumberTx transaction.</p>
   *
   * @param chainId        : Chain to apply Tx to.
   * @param nonce          : Tx nonce to use.
   * @param fromPubKey     : Authoring Public Key.
   * @param fromAddress    : Authoring Address.
   * @param nameSpace      : Namespace of asset to unencumber.
   * @param classId        : Class of asset to unencumber.
   * @param subjectAddress : Addreess against which encumbrance is held.
   * @param reference      : Encumbrance reference.
   * @param amount         : Amount to Un-Encumber.
   * @param protocol       : Info only.
   * @param metadata       : Info only.
   * @param poa            : POA data.
   *
   * @return : UnEncumberTx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static UnEncumberTx unencumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String subjectAddress,
      String reference,
      long amount,
      String protocol,
      String metadata,
      String poa
  ) {

    UnEncumberTx tx = new UnEncumberTx(
        chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        subjectAddress,
        reference,
        nameSpace,
        classId,
        amount,
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );
    tx.setHash(Hash.computeHash(tx));
    return tx;
  }

  @JsonProperty("address")
  @JsonAlias("fromaddress")
  @Schema(description = "The address which owns the asset. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("amount")
  @Schema(description = "The amount to un-encumber.", format = "int64")
  private BigInteger amount;

  @JsonProperty("classId")
  @JsonAlias("instrument")
  @Schema(description = "The identifier of the asset's class.")
  @NotNull
  private String classId;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("namespace")
  @Schema(description = "The name of the namespace which contains the asset class.")
  @NotNull
  private String nameSpace;

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to implement the asset issue.")
  private String protocol = "";

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("reference")
  @Schema(description = "The reference for this encumbrance.")
  private String reference;

  @Schema(description = "The subject's address (reserved for future use).")
  @JsonProperty("subjectAddress")
  @Address
  private String subjectAddress;

  public BaseUnEncumber() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public BaseUnEncumber(UnEncumberTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setAmount(toBigInteger(tx.getAmount()));
    setClassId(tx.getClassId());
    setMetadata(tx.getMetadata());
    setNameSpace(tx.getNameSpace());
    setProtocol(tx.getProtocol());
    setPublicKey(tx.getFromPublicKey());
    setReference(tx.getReference());
    setSubjectAddress(tx.getSubjectaddress());
  }


  public String getAddress() {
    return address;
  }


  public BigInteger getAmount() {
    return amount;
  }


  public String getClassId() {
    return classId;
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
    return address;
  }


  @Override
  @JsonIgnore
  @Hidden
  public String getNoncePublicKey() {
    return getPublicKey();
  }


  public String getProtocol() {
    return protocol;
  }


  public String getPublicKey() {
    return publicKey;
  }


  public String getReference() {
    return reference;
  }


  public String getSubjectAddress() {
    return subjectAddress;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setClassId(String classId) {
    this.classId = cleanString(classId);
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setNameSpace(String nameSpace) {
    this.nameSpace = cleanString(nameSpace);
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setReference(String reference) {
    this.reference = reference;
  }


  public void setSubjectAddress(String subjectAddress) {
    this.subjectAddress = subjectAddress;
  }

}
