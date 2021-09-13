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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.AssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonClassDescription("Transfer an asset to another block chain.")
@Schema(name = TxExternalNames.TRANSFER_ASSET_X_CHAIN, description = "Transfer an asset to another block chain.", allOf = BaseTransaction.class)
@JsonDeserialize
public class AssetTransferXChain extends BaseAssetTransfer {

  /**
   * assetTransferXChainUnsigned().
   * <p>Create an unsigned XChain Asset Transfer transaction</p>
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param nameSpace   :
   * @param classId     :
   * @param toChainId   :
   * @param toAddress   :
   * @param amount      :
   * @param protocol    :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static AssetTransferXChainTx assetTransferXChainUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      int toChainId,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa
  ) {

    AssetTransferXChainTx rVal = new AssetTransferXChainTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        cleanString(classId),
        toChainId,
        toAddress,
        amount,
        "",
        protocol,
        metadata,
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;

  }


  @JsonAlias("tochainid")
  @JsonProperty("toChainId")
  @Schema(description = "ID of the destination chain.")
  private int toChainId;


  public AssetTransferXChain() {
    // do nothing
  }


  public AssetTransferXChain(AssetTransferXChainTx tx) {
    super(tx);
    toChainId = tx.getToChainId();
  }


  @Override
  public AssetTransferXChainTx create() {
    AssetTransferXChainTx rVal = new AssetTransferXChainTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getNameSpace(),
        getClassId(),
        toChainId,
        getToAddress(),
        getAmount(),
        getSignature(),
        getProtocol(),
        getMetadata(),
        getHeight(),
        getPoa(),
        getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public int getToChainId() {
    return toChainId;
  }


  @Override
  public TxType getTxType() {
    return TxType.TRANSFER_ASSET_X_CHAIN;
  }


  public void setToChainId(int toChainId) {
    this.toChainId = toChainId;
  }

}
