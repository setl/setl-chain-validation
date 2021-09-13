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

import static io.setl.bc.pychain.tx.create.Encumber.DICT_PROPERTIES;
import static io.setl.common.StringUtils.cleanString;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAssetIssueEncumberTx;
import io.setl.bc.pychain.tx.create.BaseEncumber.Participant;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

@JsonClassDescription("(POA) Issue an asset to an address and add an Encumbrance simultaneously.")
@Schema(name = TxExternalNames.POA_ISSUE_AND_ENCUMBER_ASSET,
    description = "(POA) Issue an asset to an address and add an Encumbrance simultaneously.",
    allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaAssetIssueAndEncumber extends BaseAssetIssueEncumber {

  /**
   * PoaAssetIssueAndEncumberUnsigned().
   * <p>Create unsigned AssetIssueEncumberTx</p>
   *
   * @param chainID      :
   * @param nonce        :
   * @param fromPubKey   :
   * @param fromAddress  :
   * @param poaAddress   :
   * @param poaReference :
   * @param nameSpace    :
   * @param classId      :
   * @param toAddress    :
   * @param amount       :
   * @param txDictData   :
   * @param protocol     :
   * @param metadata     :
   * @param poa          :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaAssetIssueEncumberTx poaAssetIssueAndEncumberUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String toAddress,
      Number amount,
      Map<String, Object> txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    TreeMap<String, Object> dict = new TreeMap<>();
    for (Entry<String, Object> e : txDictData.entrySet()) {
      String k = e.getKey();
      if (DICT_PROPERTIES.contains(k)) {
        dict.putIfAbsent(k, e.getValue());
      }
    }

    Encumber.cleanMap(dict);

    PoaAssetIssueEncumberTx rVal = new PoaAssetIssueEncumberTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        cleanString(nameSpace),
        cleanString(classId),
        toAddress,
        amount,
        new MPWrappedMap<>(dict),
        "",
        -1,
        poa,
        Instant.now().getEpochSecond(),
        protocol,
        metadata
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonProperty("poaAddress")
  @Schema(description = "Attorney's address")
  @NotNull
  @Address
  private String poaAddress;

  @JsonProperty("poaReference")
  @Schema(description = "Attorney's reference for this transaction.")
  @NotNull
  private String poaReference;

  /**
   * New instance.
   *
   * @param amount       the amount
   * @param classId      the class Id
   * @param fromAddress  the from address
   * @param poaAddress   the PoA address
   * @param poaReference the PoA referece
   * @param nameSpace    the name space
   * @param toAddress    the to address
   */
  @JsonCreator
  public PoaAssetIssueAndEncumber(
      @JsonProperty("amount") @NotNull BigInteger amount,
      @JsonProperty("classId") @NotNull String classId,
      @JsonProperty("fromAddress") @NotNull String fromAddress,
      @JsonProperty("namespace") @NotNull String nameSpace,
      @JsonProperty("poaAddress") @NotNull String poaAddress,
      @JsonProperty("poaReference") @NotNull String poaReference,
      @JsonProperty("toAddress") @NotNull String toAddress
  ) {
    super(amount, classId, fromAddress, nameSpace, toAddress);
    this.poaAddress = poaAddress;
    this.poaReference = poaReference;
  }


  /**
   * New instance.
   */
  public PoaAssetIssueAndEncumber() {
    super(BigInteger.ZERO, UNSET_VALUE, UNSET_VALUE, UNSET_VALUE, UNSET_VALUE);
    poaAddress = UNSET_VALUE;
    poaReference = UNSET_VALUE;
  }


  /**
   * Create a new instance from the TXI representation.
   *
   * @param tx the TXI representation
   */
  public PoaAssetIssueAndEncumber(PoaAssetIssueEncumberTx tx) {
    super(tx);
    poaAddress = tx.getPoaAddress();
    poaReference = tx.getPoaReference();
  }


  @Override
  public PoaAssetIssueEncumberTx create() {
    PoaAssetIssueEncumberTx rVal = new PoaAssetIssueEncumberTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getPoaAddress(),
        getPoaReference(),
        cleanString(getNameSpace()),
        cleanString(getClassId()),
        getToAddress(),
        getAmount(),
        getReference(),
        getBeneficiaries().stream().map(Participant::encode).toArray(),
        getAdministrators().stream().map(Participant::encode).toArray(),
        getSignature(),
        getHeight(),
        getPoa(),
        getTimestamp(),
        getProtocol(),
        getMetadata()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public String getPoaAddress() {
    return poaAddress;
  }


  public String getPoaReference() {
    return poaReference;
  }


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.POA_ISSUE_AND_ENCUMBER_ASSET;
  }


  public void setPoaAddress(@NotNull String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(@NotNull String poaReference) {
    this.poaReference = poaReference;
  }

}



