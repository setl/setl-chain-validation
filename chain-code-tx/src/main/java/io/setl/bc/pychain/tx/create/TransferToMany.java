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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.TransferToManyTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = TxExternalNames.TRANSFER_ASSET_TO_MANY, allOf = BaseTransaction.class)
@JsonDeserialize
public class TransferToMany extends BaseTransferToMany {

  /**
   * transferToManyUnsigned().
   * <p>Return new, unsigned TransferToManyTx.</p>
   *
   * @param chainID     : int,    Blockchain ID
   * @param nonce       : int,    TX Nonce (Unique, sequential)
   * @param fromPubKey  : String, 'From' Address Public Key, Relates to From Address and TX Signature.
   * @param fromAddress : String, Address from which Asset will be taken
   * @param nameSpace   : String, Asset namespace
   * @param classId     : String, Asset Class
   * @param toChainId   : int,    Destination Chain ID.
   * @param toAddress   : Obj[],  [[Address, Amount], ...]  Specification of payment addresses.
   * @param amount      : long,   CheckSum. Must equal Sum of amounts specified in toAddress
   * @param protocol    : String, Info Only.
   * @param metadata    : String, Info Only.
   * @param poa         :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static TransferToManyTx transferToManyUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      int toChainId,
      Object[] toAddress,
      Number amount,
      String protocol,
      String metadata,
      String poa
  ) {

    TransferToManyTx rVal = new TransferToManyTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
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


  public TransferToMany() {
    // do nothing
  }


  public TransferToMany(TransferToManyTx tx) {
    super(tx);
  }


  @Override
  public TransferToManyTx create() {
    fixAmount();

    // If to-chain-id is unset, assume same chain
    if (getToChainId() == -1) {
      setToChainId(getChainId());
    }

    TransferToManyTx rVal = new TransferToManyTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getNameSpace(),
        getClassId(),
        getToChainId(),
        getTransfers().stream().map(t -> t.encode()).toArray(),
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
    return TxType.TRANSFER_ASSET_TO_MANY;
  }
}
