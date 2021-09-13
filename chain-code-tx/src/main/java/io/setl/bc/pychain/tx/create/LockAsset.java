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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.LockAssetTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = TxExternalNames.LOCK_ASSET, allOf = BaseTransaction.class)
@JsonDeserialize
public class LockAsset extends BaseLockAsset {

  /**
   * lockAssetUnsigned().
   * <p>Create an unsigned LockAssetTx</p>
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param nameSpace   :
   * @param classId     :
   * @param lockDetails :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static LockAssetTx lockAssetUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String lockDetails,
      String metadata,
      String poa
  ) {

    LockAssetTx rVal = new LockAssetTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        cleanString(classId),
        lockDetails,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  public LockAsset() {
    // do nothing
  }


  public LockAsset(LockAssetTx tx) {
    super(tx);
  }


  @Override
  public LockAssetTx create() {
    LockAssetTx rVal = new LockAssetTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getNameSpace(),
        getClassId(),
        getLockDetails(),
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


  @Override
  public TxType getTxType() {
    return TxType.LOCK_ASSET;
  }

}
