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
package io.setl.bc.pychain.tx.updatestate;

import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.common.CommonPy.XChainParameters;
import io.setl.common.CommonPy.XChainTypes;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XChainTxPackage {

  private static final Logger logger = LoggerFactory.getLogger(XChainTxPackage.class);


  /**
   * updatestate.
   * Makes no change to state, it's only useful function is to advance the nonce, check consensus or appear fast.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(XChainTxPackageTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      // Check that this Tx is on the correct chain.

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain

        // Verification : Apply only to the 'Native' chain...

        if ((thisTX.getTimestamp() > 0) && (Math.abs(thisTX.getTimestamp() - updateTime) > stateSnapshot.getStateConfig().getMaxTxAge())) {
          return new ReturnTuple(SuccessType.FAIL, "Tx Timestamp invalid.");
        }

        if (priority == thisTX.getPriority()) {

          // xchainsignodes - xChainID : [xchainid, existingheight, exitsingSignodes, chainparameters, existingStatus]

          XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(thisTX.getFromChainID());

          if (xChainDetails == null) {
            return new ReturnTuple(SuccessType.FAIL, "xChain details do not exist.");
          }

          long lastXcHeight = xChainDetails.getBlockHeight();
          long xcParameters = xChainDetails.getParameters();

          if (thisTX.getBlockHeight() != (lastXcHeight + 1)) {
            return new ReturnTuple(SuccessType.FAIL, "Bad BlockHeight.");
          }

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Parameters

          boolean doNamespace = ((xcParameters & XChainParameters.AcceptNamespaces) != 0L);
          boolean doClass = ((xcParameters & XChainParameters.AcceptClasses) != 0L);
          boolean doAssets = ((xcParameters & XChainParameters.AcceptAssets) != 0L);
          boolean doAddresses = ((xcParameters & XChainParameters.AcceptAddress) != 0L);

          Set<TxType> namespaceTypes = XChainTypes.XChainTxNamespaceTypes;
          Set<TxType> classTypes = XChainTypes.XChainTxClassTypes;
          Set<TxType> assetTypes = XChainTypes.XChainTxAssetTypes;
          Set<TxType> bondTypes = XChainTypes.XChainTxBondTypes;
          Set<TxType> addressTypes = XChainTypes.XChainTxAddressTypes;

          // TODO Use hashlist
          // Pair<String, Integer>[] hashlist = thisTX.getTxHashList();
          Long blocktime = thisTX.getBlockTimestamp();

          Object[] thisTxList = thisTX.getTxList();
          AbstractTx listTx;
          boolean doIt = false;
          couldCorrupt = true;

          // Any Transactions in this package...
          if ((thisTxList != null) && (thisTxList.length > 0)) {

            // OK, loop...
            for (int index = 0, l = thisTxList.length; index < l; index++) {

              listTx = TxFromList.txFromList(new MPWrappedArrayImpl(thisTxList[index]));
              doIt = false;

              // Don't process 'Bad' Transactions.

              if (!listTx.isGood()) {
                continue;
              }

              /*
                # Skip cross chain Transactions that originated on this chain
                # (they should not be in a xChain Tx package delivered here and should already have been processed
                # if by some chance they do end up in a package.)
               */

              if (listTx.getChainId() == stateSnapshot.getChainId()) {
                continue;
              }

              // Should this TX be applied, according to the XCParameters ?

              if (doAddresses && (addressTypes.contains(listTx.getTxType()))) {
                doIt = true;
              } else if (doNamespace && (namespaceTypes.contains(listTx.getTxType()))) {
                doIt = true;
              } else if (doClass && (classTypes.contains(listTx.getTxType()))) {
                doIt = true;
              } else if (doAssets && (assetTypes.contains(listTx.getTxType()))) {
                doIt = true;
              } else if (bondTypes.contains(listTx.getTxType())) {
                doIt = true;
              }

              // OK, do it.
              // Note Tx is processed in the context of the Block Time from which it came.

              if (doIt) {
                UpdateState.update(listTx, stateSnapshot, blocktime, listTx.getPriority(), checkOnly);
              }

            }
          }

          // Update xChain Block Height. Details may have been changed by the transaction we just processed.
          int chainId = thisTX.getFromChainID();
          xChainDetails = stateSnapshot.getXChainSignNodesValue(chainId);
          stateSnapshot.setXChainSignNodesValue(chainId, xChainDetails.setBlockHeight(thisTX.getBlockHeight()));

        }

      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in XChainTxPackageTx.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in XChainTxPackageTx.updatestate.");
    }

  }

}
