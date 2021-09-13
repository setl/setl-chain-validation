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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_XCHAINING;

import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.AddXChainTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.XChainParameters;

public class AddXChain {

  private static final Logger logger = LoggerFactory.getLogger(AddXChain.class);


  /**
   * updatestate.
   * <p>Adds a linked Cross-Chain definition to state.</p>
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(AddXChainTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      // On Chain ?
      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain
        // Verification : Apply only to the 'Native' chain...

        // Correct priority for update ?
        if (priority == thisTX.getPriority()) {

          // Check From Address
          String fromAddress = thisTX.getFromAddress();

          // Address permissions.
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
            boolean hasPermission = stateSnapshot.canUseTx(fromAddress, thisTX.getTxType());

            if ((!hasPermission) && (!checkOnly) && ((keyPermission & AP_XCHAINING) == 0)) {
              return new ReturnTuple(SuccessType.FAIL, "Inadequate Address permissioning");
            }
          }

          // Get Entry
          // [xchainid, existingheight, existingSignodes, chainparameters, existingStatus]

          couldCorrupt = true;

          XChainDetails existingItem = stateSnapshot.getXChainSignNodesValue(thisTX.getNewChainId());

          // New Values...

          final int newChainId = thisTX.getNewChainId();
          long newBlockHeight = thisTX.getNewBlockHeight();
          // Always accept Assets.
          final Long newParams = (thisTX.getNewChainParams() | XChainParameters.AcceptAssets);
          Long newStatus = 0L;
          SortedMap<String, Balance> newSignodes = null;

          Object[] tempSignodes = thisTX.getNewChainSignodesAsArray();
          if (tempSignodes.length > 0) {
            // If no signodes were provided with this TX, leave the existing ones in place, otherwise
            // replace the existing ones with the Signodes provided.

            newSignodes = new TreeMap<>();

            for (int index = 0, l = tempSignodes.length; index < l; index++) {
              String tempPub = (String) ((Object[]) tempSignodes[index])[0];
              Balance tempStake = new Balance(((Object[]) tempSignodes[index])[1]);

              newSignodes.put(tempPub, tempStake.add(newSignodes.getOrDefault(tempPub, Balance.BALANCE_ZERO)));
            }
          }

          // Selectively Use / Check vs Old values

          if (existingItem != null) {

            // Don't allow reduction in block height (Replay).

            newBlockHeight = Math.max(newBlockHeight, existingItem.getBlockHeight());

            // If the intent was only to update parameters, re-use the existing Signode data.

            if (newSignodes == null) {
              newSignodes = existingItem.getSignNodes();
            }

            // Keep Status Values.

            newStatus = existingItem.getStatus();
          } else {
            // No existing item.

          }

          // Dont allow chain links with no sig nodes.

          if ((newSignodes == null) || (newSignodes.isEmpty())) {
            return new ReturnTuple(SuccessType.FAIL, "No Signing addresses given for xChain");
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Update
          couldCorrupt = true;

          stateSnapshot.setXChainSignNodesValue(thisTX.getNewChainId(), new XChainDetails(newChainId, newBlockHeight, newSignodes, newParams, newStatus));
        }

      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in AddXChain.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in AddXChain.updatestate.");
    }

  }

}
