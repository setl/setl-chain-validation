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
package io.setl.bc.pychain.state.tx;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxFromList {

  private static final CopyOnWriteArraySet<Decoder> ALL_DECODERS = new CopyOnWriteArraySet<>();

  private static final Logger logger = LoggerFactory.getLogger(TxFromList.class);



  public interface Decoder {

    /**
     * Decode a transaction.
     *
     * @param txType the TX type
     * @param txData the message packed representation
     *
     * @return the TX
     */
    AbstractTx decode(TxType txType, MPWrappedArray txData);

    /**
     * Get the set of TxTypes this decoder can decode.
     *
     * @return the set of TxTypes
     */
    Set<TxType> handles();
  }


  /**
   * Inject a dependency on another decoder.
   *
   * @param decoder the decoder
   */
  public static void registerDecoder(Decoder decoder) {
    ALL_DECODERS.add(decoder);
  }


  private static AbstractTx tryDecoders(TxType txt, MPWrappedArray txData) {
    for (Decoder decoder : ALL_DECODERS) {
      if (decoder.handles().contains(txt)) {
        return decoder.decode(txt, txData);
      }
    }
    logger.error("Unknown tx:{}", txt);
    throw new RuntimeException(String.format("Transaction %d not defined", txData.asInt(2)));
  }


  /**
   * Accept an MPWrappedArray object and return it as the appropriate sub-class object.
   *
   * @param txData : The input MPWrappedArray object
   *
   * @return : The returned object
   */
  public static AbstractTx txFromList(MPWrappedArray txData) {

    int txcode = txData.asInt(2);
    TxType txt = TxType.get(txcode);

    if (txt == null) {
      throw new RuntimeException(String.format("Transaction code %d not defined", txData.asInt(2)));
    }

    switch (txt) {
      case GRANT_POA:
        return PoaAddTx.decodeTX(txData);
      case REVOKE_POA:
        return PoaDeleteTx.decodeTX(txData);
      case POA_REGISTER_NAMESPACE:
        return PoaNamespaceRegisterTx.decodeTX(txData);
      case POA_TRANSFER_NAMESPACE:
        return PoaNamespaceTransferTx.decodeTX(txData);
      case POA_DELETE_NAMESPACE:
        return PoaNamespaceDeleteTx.decodeTX(txData);
      case POA_DELETE_ADDRESS:
        return PoaAddressDeleteTx.decodeTX(txData);
      case POA_REGISTER_ASSET_CLASS:
        return PoaAssetClassRegisterTx.decodeTX(txData);
      case POA_DELETE_ASSET_CLASS:
        return PoaAssetClassDeleteTx.decodeTX(txData);
      case POA_ISSUE_ASSET:
        return PoaAssetIssueTx.decodeTX(txData);
      case POA_ISSUE_AND_ENCUMBER_ASSET:
        return PoaAssetIssueEncumberTx.decodeTX(txData);
      case POA_TRANSFER_ASSET_X_CHAIN:
        return PoaAssetTransferXChainTx.decodeTX(txData);
      case POA_TRANSFER_ASSET_TO_MANY:
        return PoaTransferToManyTx.decodeTX(txData);
      case POA_TRANSFER_ASSET_FROM_MANY:
        return PoaTransferFromManyTx.decodeTX(txData);
      case POA_TRANSFER_ASSET_AS_ISSUER:
        return PoaIssuerTransferTx.decodeTX(txData);
      case POA_LOCK_ASSET:
        return PoaLockAssetTx.decodeTX(txData);
      case POA_LOCK_ASSET_HOLDING:
        return PoaLockHoldingTx.decodeTX(txData);
      case POA_UNLOCK_ASSET:
        return PoaUnLockAssetTx.decodeTX(txData);
      case POA_UNLOCK_ASSET_HOLDING:
        return PoaUnlockHoldingTx.decodeTX(txData);
      case POA_NEW_CONTRACT:
        return PoaNewContractTx.decodeTX(txData);
      case POA_COMMIT_TO_CONTRACT:
        return PoaCommitToContractTx.decodeTX(txData);
      case POA_ENCUMBER_ASSET:
        return PoaEncumberTx.decodeTX(txData);
      case POA_UNENCUMBER_ASSET:
        return PoaUnEncumberTx.decodeTX(txData);
      case POA_EXERCISE_ENCUMBRANCE:
        return PoaExerciseEncumbranceTx.decodeTX(txData);
      case ADD_X_CHAIN:
        return AddXChainTx.decodeTX(txData);
      case DELETE_ASSET_CLASS:
        return AssetClassDeleteTx.decodeTX(txData);
      case REGISTER_ASSET_CLASS:
        return AssetClassRegisterTx.decodeTX(txData);
      case UPDATE_ASSET_CLASS:
        return AssetClassUpdateTx.decodeTX(txData);
      case TRANSFER_ASSET:
        return AssetTransferTx.decodeTX(txData);
      case TRANSFER_ASSET_X_CHAIN:
        return AssetTransferXChainTx.decodeTX(txData);
      case ISSUE_ASSET:
        return AssetIssueTx.decodeTX(txData);
      case ISSUE_AND_ENCUMBER_ASSET:
        return AssetIssueEncumberTx.decodeTX(txData);
      case GRANT_VOTING_POWER:
        return BondTx.decodeTX(txData);
      case CANCEL_CONTRACT:
      case NOTIFY_CONTRACT:
      case DO_DIVIDEND:
        logger.error("Unknown tx:{}", txt);
        throw new RuntimeException(String.format("Transaction %d not defined", txData.asInt(2)));
      case COMMIT_TO_CONTRACT:
        return CommitToContractTx.decodeTX(txData);
      case ENCUMBER_ASSET:
        return EncumberTx.decodeTX(txData);
      case EXERCISE_ENCUMBRANCE:
        return ExerciseEncumbranceTx.decodeTX(txData);
      case TRANSFER_ASSET_AS_ISSUER:
        return IssuerTransferTx.decodeTX(txData);
      case LOCK_ASSET:
        return LockAssetTx.decodeTX(txData);
      case LOCK_ASSET_HOLDING:
        return LockHoldingTx.decodeTX(txData);
      case CREATE_MEMO:
        return MemoTx.decodeTX(txData);
      case DELETE_NAMESPACE:
        return NamespaceDeleteTx.decodeTX(txData);
      case REGISTER_NAMESPACE:
        return NamespaceRegisterTx.decodeTX(txData);
      case TRANSFER_NAMESPACE:
        return NamespaceTransferTx.decodeTX(txData);
      case NEW_CONTRACT:
        return NewContractTx.decodeTX(txData);
      case DO_NOTHING:
        return NullTx.decodeTX(txData);
      case DO_PRIVILEGED_OPERATION:
        return PrivilegedOperationTx.decode(txData);
      case REGISTER_ADDRESS:
        return RegisterAddressTx.decodeTX(txData);
      case DELETE_ADDRESS:
        return AddressDeleteTx.decodeTX(txData);
      case UPDATE_ADDRESS_PERMISSIONS:
        return AddressPermissionsTx.decodeTX(txData);
      case REMOVE_X_CHAIN:
        return DeleteXChainTx.decodeTX(txData);
      case SPLIT_STOCK:
        return StockSplitTx.decodeTX(txData);
      case TRANSFER_ASSET_TO_MANY:
        return TransferToManyTx.decodeTX(txData);
      case TRANSFER_ASSET_FROM_MANY:
        return TransferFromManyTx.decodeTX(txData);
      case REVOKE_VOTING_POWER:
        return UnBondTx.decodeTX(txData);
      case UNENCUMBER_ASSET:
        return UnEncumberTx.decodeTX(txData);
      case UNLOCK_ASSET:
        return UnLockAssetTx.decodeTX(txData);
      case UNLOCK_ASSET_HOLDING:
        return UnlockHoldingTx.decodeTX(txData);
      case X_CHAIN_TX_PACKAGE:
        return XChainTxPackageTx.decodeTX(txData);

      default:
        return tryDecoders(txt, txData);
    }
  }


  private TxFromList() {

  }
}

