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

import java.time.Instant;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.AssetClassUpdateTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.UPDATE_ASSET_CLASS, description = "Update the definition of an asset class.", allOf = BaseTransaction.class)
@JsonDeserialize
public class AssetClassUpdate extends BaseTransaction {

  /**
   * assetClassRegisterUnsigned().
   * <p>Create an unsigned AssetClassUpdateTx</p>
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param nameSpace   :
   * @param classId     :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static AssetClassUpdateTx assetClassUpdateUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String metadata,
      String poa
  ) {

    AssetClassUpdateTx rVal = new AssetClassUpdateTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        cleanString(classId),
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonAlias("fromaddress")
  @JsonProperty("address")
  @Schema(description = "The issuing address. Must be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  private String address;

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

  @JsonAlias("frompubkey")
  @JsonProperty("publicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  private String publicKey;

  public AssetClassUpdate() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public AssetClassUpdate(AssetClassUpdateTx tx) {
    super(tx);
    setClassId(tx.getClassId());
    setAddress(tx.getFromAddress());
    setPublicKey(tx.getFromPublicKey());
    setMetadata(tx.getMetadata());
    setNameSpace(tx.getNameSpace());
  }


  @Override
  public AssetClassUpdateTx create() {
    AssetClassUpdateTx rVal = new AssetClassUpdateTx(
        getChainId(),
        4,
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getNameSpace(),
        getClassId(),
        getMetadata(),
        getSignature(),
        getHeight(),
        getPoa(),
        getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public String getAddress() {
    return address;
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


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getPublicKey();
  }


  public String getPublicKey() {
    return publicKey;
  }


  @Override
  public TxType getTxType() {
    return TxType.UPDATE_ASSET_CLASS;
  }


  public void setAddress(String address) {
    this.address = address;
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


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

}

