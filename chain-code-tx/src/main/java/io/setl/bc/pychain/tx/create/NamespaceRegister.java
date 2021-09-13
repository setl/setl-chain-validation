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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.NamespaceRegisterTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;

@Schema(name = TxExternalNames.REGISTER_NAMESPACE, allOf = BaseTransaction.class)
@JsonDeserialize
public class NamespaceRegister extends BaseNamespaceRegister {


  /**
   * namespaceRegisterUnsigned().
   * <p>Create Unsigned NamespaceRegisterTx</p>
   *
   * @param chainID     :
   * @param nonce       :
   * @param fromPubKey  :
   * @param fromAddress :
   * @param nameSpace   :
   * @param metadata    :
   * @param poa         :
   *
   * @return :
   */
  public static NamespaceRegisterTx namespaceRegisterUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String metadata,
      String poa
  ) {

    NamespaceRegisterTx rVal = new NamespaceRegisterTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  public NamespaceRegister() {
    // do nothing
  }


  public NamespaceRegister(NamespaceRegisterTx tx) {
    super(tx);
  }


  @Override
  public NamespaceRegisterTx create() {
    NamespaceRegisterTx rVal = new NamespaceRegisterTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getNameSpace(),
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
    return TxType.REGISTER_NAMESPACE;
  }


}
