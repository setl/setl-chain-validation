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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.AssetIssueTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@JsonClassDescription("Issue an asset to an address.")
@Schema(name = TxExternalNames.ISSUE_ASSET, description = "Issue an asset to an address.", allOf = BaseTransaction.class)
@JsonDeserialize
public class AssetIssue extends BaseAssetIssue {

  /**
   * assetIssueUnsigned().
   * <p>Create unsigned AssetIssueTx</p>
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param nameSpace   :
   * @param classId     :
   * @param toAddress   :
   * @param amount      :
   * @param protocol    :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static AssetIssueTx assetIssueUnsigned(
      int chainID, long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa

  ) {

    AssetIssueTx rVal = new AssetIssueTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        cleanString(classId),
        toAddress,
        amount,
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


  public AssetIssue() {
    super(BigInteger.ZERO, UNSET_VALUE, UNSET_VALUE, UNSET_VALUE, UNSET_VALUE);
  }


  /**
   * New instance.
   *
   * @param amount      the amount
   * @param classId     the class Id
   * @param fromAddress the from address
   * @param nameSpace   the name space
   * @param toAddress   the to address
   */
  @JsonCreator
  public AssetIssue(
      @JsonProperty("amount") @NotNull BigInteger amount,
      @JsonProperty("classId") @NotNull String classId,
      @JsonProperty("fromAddress") @NotNull String fromAddress,
      @JsonProperty("namespace") @NotNull String nameSpace,
      @JsonProperty("toAddress") @NotNull String toAddress
  ) {
    super(amount, classId, fromAddress, nameSpace, toAddress);
  }


  public AssetIssue(AssetIssueTx tx) {
    super(tx);
  }


  @Override
  public AssetIssueTx create() {
    AssetIssueTx rVal = new AssetIssueTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        cleanString(getNameSpace()),
        cleanString(getClassId()),
        getToAddress(),
        getAmount(),
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


  @Override
  public TxType getTxType() {
    return TxType.ISSUE_ASSET;
  }
}
