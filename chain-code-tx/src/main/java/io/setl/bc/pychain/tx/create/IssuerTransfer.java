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
import io.setl.bc.pychain.state.tx.IssuerTransferTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = TxExternalNames.TRANSFER_ASSET_AS_ISSUER, allOf = BaseTransaction.class)
@JsonDeserialize
public class IssuerTransfer extends BaseIssuerTransfer {

  /**
   * Return a new, unsigned IssuerTransferTx.
   *
   * @param chainID       : Chain to operate on.
   * @param nonce         : Tx Nonce.
   * @param fromPubKey    : Authoring (Issuer) Public key.
   * @param fromAddress   : Authoring (Issuer) Address.
   * @param nameSpace     : Asset Namespace. (Must be controlled by 'fromPubKey'.
   * @param classId       : Asset Class.
   * @param sourceAddress : Address from which to move assets.
   * @param toChainId     : Destination Chain.
   * @param toAddress     : Destination Address.
   * @param amount        : Amount to move.
   * @param protocol      : Protocol, info only.
   * @param metadata      : metadata, info only.
   * @param poa           : POA.
   *
   * @return : new IssuerTransferTx.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static IssuerTransferTx issuerTransferUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String sourceAddress,
      int toChainId,
      String toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa) {

    IssuerTransferTx rVal = new IssuerTransferTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        cleanString(classId),
        sourceAddress,
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


  public IssuerTransfer() {
    // do nothing
  }


  public IssuerTransfer(IssuerTransferTx tx) {
    super(tx);
  }


  @Override
  public IssuerTransferTx create() {
    IssuerTransferTx rVal = new IssuerTransferTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getNameSpace(),
        getClassId(),
        getSourceAddress(),
        getToChainId(),
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


  @Override
  public TxType getTxType() {
    return TxType.TRANSFER_ASSET_AS_ISSUER;
  }

}
