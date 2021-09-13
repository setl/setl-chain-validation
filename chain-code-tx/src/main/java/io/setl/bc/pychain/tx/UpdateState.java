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
package io.setl.bc.pychain.tx;

import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.AddXChainTx;
import io.setl.bc.pychain.state.tx.AddressDeleteTx;
import io.setl.bc.pychain.state.tx.AddressPermissionsTx;
import io.setl.bc.pychain.state.tx.AssetClassDeleteTx;
import io.setl.bc.pychain.state.tx.AssetClassRegisterTx;
import io.setl.bc.pychain.state.tx.AssetClassUpdateTx;
import io.setl.bc.pychain.state.tx.AssetIssueEncumberTx;
import io.setl.bc.pychain.state.tx.AssetIssueTx;
import io.setl.bc.pychain.state.tx.AssetTransferTx;
import io.setl.bc.pychain.state.tx.AssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.BondTx;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.DeleteXChainTx;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.state.tx.ExerciseEncumbranceTx;
import io.setl.bc.pychain.state.tx.IssuerTransferTx;
import io.setl.bc.pychain.state.tx.LockAssetTx;
import io.setl.bc.pychain.state.tx.MemoTx;
import io.setl.bc.pychain.state.tx.NamespaceDeleteTx;
import io.setl.bc.pychain.state.tx.NamespaceRegisterTx;
import io.setl.bc.pychain.state.tx.NamespaceTransferTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.PoaAddressDeleteTx;
import io.setl.bc.pychain.state.tx.PoaAssetClassDeleteTx;
import io.setl.bc.pychain.state.tx.PoaAssetClassRegisterTx;
import io.setl.bc.pychain.state.tx.PoaAssetIssueEncumberTx;
import io.setl.bc.pychain.state.tx.PoaAssetIssueTx;
import io.setl.bc.pychain.state.tx.PoaAssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.PoaDeleteTx;
import io.setl.bc.pychain.state.tx.PoaEncumberTx;
import io.setl.bc.pychain.state.tx.PoaExerciseEncumbranceTx;
import io.setl.bc.pychain.state.tx.PoaIssuerTransferTx;
import io.setl.bc.pychain.state.tx.PoaLockAssetTx;
import io.setl.bc.pychain.state.tx.PoaNamespaceDeleteTx;
import io.setl.bc.pychain.state.tx.PoaNamespaceRegisterTx;
import io.setl.bc.pychain.state.tx.PoaNamespaceTransferTx;
import io.setl.bc.pychain.state.tx.PoaNewContractTx;
import io.setl.bc.pychain.state.tx.PoaTransferFromManyTx;
import io.setl.bc.pychain.state.tx.PoaTransferToManyTx;
import io.setl.bc.pychain.state.tx.PoaUnEncumberTx;
import io.setl.bc.pychain.state.tx.PoaUnLockAssetTx;
import io.setl.bc.pychain.state.tx.RegisterAddressTx;
import io.setl.bc.pychain.state.tx.StockSplitTx;
import io.setl.bc.pychain.state.tx.TransferFromManyTx;
import io.setl.bc.pychain.state.tx.TransferToManyTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.UnBondTx;
import io.setl.bc.pychain.state.tx.UnEncumberTx;
import io.setl.bc.pychain.state.tx.UnLockAssetTx;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.bc.pychain.tx.updatestate.AddXChain;
import io.setl.bc.pychain.tx.updatestate.AddressDelete;
import io.setl.bc.pychain.tx.updatestate.AddressPermissions;
import io.setl.bc.pychain.tx.updatestate.AssetClassDelete;
import io.setl.bc.pychain.tx.updatestate.AssetClassRegister;
import io.setl.bc.pychain.tx.updatestate.AssetClassUpdate;
import io.setl.bc.pychain.tx.updatestate.AssetIssue;
import io.setl.bc.pychain.tx.updatestate.AssetIssueAndEncumber;
import io.setl.bc.pychain.tx.updatestate.AssetTransfer;
import io.setl.bc.pychain.tx.updatestate.AssetTransferXChain;
import io.setl.bc.pychain.tx.updatestate.Bond;
import io.setl.bc.pychain.tx.updatestate.CommitToContract;
import io.setl.bc.pychain.tx.updatestate.DeleteXChain;
import io.setl.bc.pychain.tx.updatestate.Encumber;
import io.setl.bc.pychain.tx.updatestate.ExerciseEncumbrance;
import io.setl.bc.pychain.tx.updatestate.IssuerTransfer;
import io.setl.bc.pychain.tx.updatestate.LockAsset;
import io.setl.bc.pychain.tx.updatestate.Memo;
import io.setl.bc.pychain.tx.updatestate.NamespaceDelete;
import io.setl.bc.pychain.tx.updatestate.NamespaceRegister;
import io.setl.bc.pychain.tx.updatestate.NamespaceTransfer;
import io.setl.bc.pychain.tx.updatestate.NewContract;
import io.setl.bc.pychain.tx.updatestate.Null;
import io.setl.bc.pychain.tx.updatestate.PoaAdd;
import io.setl.bc.pychain.tx.updatestate.PoaCommitToContract;
import io.setl.bc.pychain.tx.updatestate.PoaDelete;
import io.setl.bc.pychain.tx.updatestate.PoaNewContract;
import io.setl.bc.pychain.tx.updatestate.PrivilegedOperation;
import io.setl.bc.pychain.tx.updatestate.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.StockSplit;
import io.setl.bc.pychain.tx.updatestate.TransferFromMany;
import io.setl.bc.pychain.tx.updatestate.TransferToMany;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UnBond;
import io.setl.bc.pychain.tx.updatestate.UnEncumber;
import io.setl.bc.pychain.tx.updatestate.UnLockAsset;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.XChainTxPackage;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

/**
 * UpdateState.
 */
public class UpdateState {

  private static final CopyOnWriteArraySet<StateUpdater> ALL_UPDATERS = new CopyOnWriteArraySet<>();

  private static final Logger logger = LoggerFactory.getLogger(UpdateState.class);



  public interface StateUpdater {

    /**
     * Get the set of TxTypes this decoder can decode.
     *
     * @return the set of TxTypes
     */
    Set<TxType> handles();

    /**
     * Update state with a transaction.
     *
     * @param tx            the transaction
     * @param stateSnapshot the snapshot of state to update
     * @param updateTime    the logical time of the update
     * @param priority      the current transaction processing priority
     * @param checkOnly     if true, only check that the transaction can probably be applied
     *
     * @return the result of the update attempt
     */
    ReturnTuple update(Txi tx, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly);

  }


  @Nonnull
  public static ReturnTuple checkUpdate(Txi thisTX, StateSnapshot stateSnapshot, long updateTime, int priority) {
    return doUpdate(thisTX, stateSnapshot, updateTime, priority, true);
  }


  private static void doStandardTests(Txi thisTX, StateSnapshot stateSnapshot, long updateTime, int priority) throws TxFailedException {
    // only check if appropriate priority and local chain.
    if (priority != thisTX.getPriority() || ((AbstractTx) thisTX).getChainId() != stateSnapshot.getChainId()) {
      return;
    }

    // validate the timestamp
    if ((thisTX.getTimestamp() > 0) && (Math.abs(thisTX.getTimestamp() - updateTime) > stateSnapshot.getStateConfig().getMaxTxAge())) {
      throw TxProcessor.fail("Tx Timestamp invalid.");
    }

    // Cross chain packages do not come from a specific address, so no further standard tests apply
    if (thisTX.getTxType() == TxType.X_CHAIN_TX_PACKAGE) {
      return;
    }

    // validate the "from" address.
    if (!AddressUtil.verify(thisTX.getFromAddress(), thisTX.getFromPublicKey(), AddressType.NORMAL)) {
      throw TxProcessor.fail("`From` Address and Public key do not match.");
    }

    // The from address is required, or must be created, except for the Register Address TX
    if (thisTX.getTxType() != TxType.REGISTER_ADDRESS) {
      AddressRules.checkAndCreateAddresses(stateSnapshot, Set.of(thisTX.getFromAddress()));
    }

    // POA transactions must have an existing POA address and the POA address must be valid
    if (thisTX.isPOA()) {
      String poaAddress = ((AbstractTx) thisTX).getPoaAddress();
      AddressRules.checkAddress(poaAddress);
      MerkleRules.checkExists("POA Address", stateSnapshot.getMerkle(AddressEntry.class), ((AbstractTx) thisTX).getPoaAddress());
    }
  }


  /**
   * Update the state with a transaction.
   *
   * @param thisTX        the transaction to apply
   * @param stateSnapshot the state snapshot
   * @param updateTime    the time of this update
   * @param priority      the transaction priority
   * @param checkOnly     if true, only check TX is valid
   *
   * @return the result of the update
   */
  // "switch" statements should not have too many "case" clauses
  // Methods should not be too complex
  @SuppressWarnings({"squid:S1479", "squid:MethodCyclomaticComplexity"})
  @Nonnull
  public static ReturnTuple doUpdate(Txi thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    ReturnTuple rVal = null;

    try {
      doStandardTests(thisTX, stateSnapshot, updateTime, priority);

      switch (thisTX.getTxType()) {
        case DO_PRIVILEGED_OPERATION:
          return new PrivilegedOperation().update(thisTX, stateSnapshot, updateTime, priority, checkOnly);
        case GRANT_VOTING_POWER:
          rVal = Bond.updatestate((BondTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case REVOKE_VOTING_POWER:
          rVal = UnBond.updatestate((UnBondTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case DO_NOTHING:
          rVal = Null.updatestate((NullTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case REGISTER_NAMESPACE:
          rVal = NamespaceRegister.updatestate((NamespaceRegisterTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case REGISTER_ASSET_CLASS:
          rVal = AssetClassRegister.updatestate((AssetClassRegisterTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case UPDATE_ASSET_CLASS:
          rVal = AssetClassUpdate.updatestate((AssetClassUpdateTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case DELETE_ASSET_CLASS:
          rVal = AssetClassDelete.updatestate((AssetClassDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case ISSUE_ASSET:
          rVal = AssetIssue.updatestate((AssetIssueTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case ISSUE_AND_ENCUMBER_ASSET:
          rVal = AssetIssueAndEncumber.updatestate((AssetIssueEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case TRANSFER_ASSET:
          rVal = AssetTransfer.updatestate((AssetTransferTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case DELETE_ADDRESS:
          rVal = AddressDelete.updatestate((AddressDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case REGISTER_ADDRESS:
          rVal = RegisterAddress.updatestate((RegisterAddressTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case UPDATE_ADDRESS_PERMISSIONS:
          rVal = AddressPermissions.updatestate((AddressPermissionsTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case TRANSFER_NAMESPACE:
          rVal = NamespaceTransfer.updatestate((NamespaceTransferTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case DELETE_NAMESPACE:
          rVal = NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case CREATE_MEMO:
          rVal = Memo.updatestate((MemoTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case TRANSFER_ASSET_X_CHAIN:
          rVal = AssetTransferXChain.updatestate((AssetTransferXChainTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case TRANSFER_ASSET_AS_ISSUER:
          rVal = IssuerTransfer.updatestate((IssuerTransferTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case X_CHAIN_TX_PACKAGE:
          rVal = XChainTxPackage.updatestate((XChainTxPackageTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case ADD_X_CHAIN:
          rVal = AddXChain.updatestate((AddXChainTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case REMOVE_X_CHAIN:
          rVal = DeleteXChain.updatestate((DeleteXChainTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case SPLIT_STOCK:
          rVal = StockSplit.updatestate((StockSplitTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case DO_DIVIDEND:
        case CANCEL_CONTRACT:
        case NOTIFY_CONTRACT:
          // Currently unimplemented
          break;

        case ENCUMBER_ASSET:
          rVal = Encumber.updatestate((EncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case UNENCUMBER_ASSET:
          rVal = UnEncumber.updatestate((UnEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case EXERCISE_ENCUMBRANCE:
          rVal = ExerciseEncumbrance.updatestate((ExerciseEncumbranceTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case LOCK_ASSET:
          rVal = LockAsset.updatestate((LockAssetTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case LOCK_ASSET_HOLDING:
          rVal = Encumber.updatestate((EncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case UNLOCK_ASSET:
          rVal = UnLockAsset.updatestate((UnLockAssetTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case UNLOCK_ASSET_HOLDING:
          rVal = UnEncumber.updatestate((UnEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case GRANT_POA:
          rVal = PoaAdd.updatestate((PoaAddTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case REVOKE_POA:
          rVal = PoaDelete.updatestate((PoaDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_REGISTER_NAMESPACE:
          rVal = NamespaceRegister.updatestate((PoaNamespaceRegisterTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_TRANSFER_NAMESPACE:
          rVal = NamespaceTransfer.updatestate((PoaNamespaceTransferTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_DELETE_NAMESPACE:
          rVal = NamespaceDelete.updatestate((PoaNamespaceDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_DELETE_ADDRESS:
          rVal = AddressDelete.updatestate((PoaAddressDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_REGISTER_ASSET_CLASS:
          rVal = AssetClassRegister.updatestate((PoaAssetClassRegisterTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_DELETE_ASSET_CLASS:
          rVal = AssetClassDelete.updatestate((PoaAssetClassDeleteTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_ISSUE_ASSET:
          rVal = AssetIssue.updatestate((PoaAssetIssueTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_ISSUE_AND_ENCUMBER_ASSET:
          rVal = AssetIssueAndEncumber.updatestate((PoaAssetIssueEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_TRANSFER_ASSET_X_CHAIN:
          rVal = AssetTransferXChain.updatestate((PoaAssetTransferXChainTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_TRANSFER_ASSET_TO_MANY:
          rVal = TransferToMany.updatestate((PoaTransferToManyTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_TRANSFER_ASSET_FROM_MANY:
          rVal = TransferFromMany.updatestate((PoaTransferFromManyTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_TRANSFER_ASSET_AS_ISSUER:
          rVal = IssuerTransfer.updatestate((PoaIssuerTransferTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_LOCK_ASSET:
          rVal = LockAsset.updatestate((PoaLockAssetTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_LOCK_ASSET_HOLDING:
          rVal = Encumber.updatestate((EncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_UNLOCK_ASSET:
          rVal = UnLockAsset.updatestate((PoaUnLockAssetTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_UNLOCK_ASSET_HOLDING:
          rVal = UnEncumber.updatestate((UnEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_NEW_CONTRACT:
          rVal = PoaNewContract.updatestate((PoaNewContractTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_COMMIT_TO_CONTRACT:
          rVal = PoaCommitToContract.updatestate((CommitToContractTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_ENCUMBER_ASSET:
          rVal = Encumber.updatestate((PoaEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_UNENCUMBER_ASSET:
          rVal = UnEncumber.updatestate((PoaUnEncumberTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case POA_EXERCISE_ENCUMBRANCE:
          rVal = ExerciseEncumbrance.updatestate((PoaExerciseEncumbranceTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case TRANSFER_ASSET_TO_MANY:
          rVal = TransferToMany.updatestate((TransferToManyTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case TRANSFER_ASSET_FROM_MANY:
          rVal = TransferFromMany.updatestate((TransferFromManyTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case NEW_CONTRACT:
          rVal = NewContract.updatestate((NewContractTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        case COMMIT_TO_CONTRACT:
          rVal = CommitToContract.updatestate((CommitToContractTx) thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;

        default:
          rVal = tryUpdaters(thisTX, stateSnapshot, updateTime, priority, checkOnly);
          return rVal;
      }
    } catch (TxFailedException txFailedException) {
      rVal = txFailedException.getFailure(checkOnly);
      return rVal;
    } finally {
      if (rVal != null) {
        if ((!checkOnly) && (rVal.success != SuccessType.PASS)) {
          if (logger.isWarnEnabled()) {
            logger.warn(MessageFormat.format("Failed Tx {0}:{1} returned status `{2}`", thisTX.getHash(), thisTX.getNonce(), rVal.status));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format(
                "Tx {0}:{1} Type `{2}`, Status `{3}`",
                thisTX.getHash(),
                thisTX.getNonce(),
                thisTX.getTxType().getLongName(),
                rVal.status
            ));
          }
        }
      }
    }

    throw new NotImplementedException("TX Name :" + thisTX.getTxType().getLongName());

  }


  /**
   * Inject a dependency on another state updater.
   *
   * @param updater the state updater
   */
  public static void registerStateUpdater(StateUpdater updater) {
    ALL_UPDATERS.add(updater);
  }


  private static ReturnTuple tryUpdaters(Txi txi, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    for (StateUpdater updater : ALL_UPDATERS) {
      if (updater.handles().contains(txi.getTxType())) {
        return updater.update(txi, stateSnapshot, updateTime, priority, checkOnly);
      }
    }
    throw new NotImplementedException("TX Name :" + txi.getTxType().name());
  }


  /**
   * update(). Generic Update function for 'Txi' interface objects. Calls appropriate underlying method.
   *
   * @param thisTX        : TX Object
   * @param stateSnapshot : State handle
   * @param updateTime    : Time of Update (Block time)
   * @param priority      : Priority of this Update (Either TX Priority or 0).
   * @param checkOnly     : If true, Check transaction for ability to be applied - DO NOT APPLY CHANGES.
   *
   * @return :
   */
  public static boolean update(Txi thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    return doUpdate(thisTX, stateSnapshot, updateTime, priority, checkOnly).success == SuccessType.PASS;
  }


  private UpdateState() {
  }

}
