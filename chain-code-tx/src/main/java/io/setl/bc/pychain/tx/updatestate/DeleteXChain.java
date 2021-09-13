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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.DeleteXChainTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;

public class DeleteXChain {

  private static final Logger logger = LoggerFactory.getLogger(DeleteXChain.class);


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
  public static ReturnTuple updatestate(DeleteXChainTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      // On Chain ?
      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // Correct priority for update ?
        if (priority == thisTX.getPriority()) {

          String fromAddress = thisTX.getFromAddress();

          // Address permissions.
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
            boolean hasPermission = stateSnapshot.canUseTx(fromAddress, thisTX.getTxType());

            if ((!hasPermission) && ((keyPermission & AP_XCHAINING) == 0)) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
            }
          }

          // Get Entry
          // [xchainid, existingheight, existingSignodes, chainparameters, existingStatus]

          XChainDetails existingItem = stateSnapshot.getXChainSignNodesValue(thisTX.getXChainId());

          if (existingItem == null) {
            return new ReturnTuple(SuccessType.FAIL, "No existing chain data");
          }

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // New Values...
          // We keep an entry to preserve the block height in case the connection is re-established.
          int newChainId = thisTX.getXChainId();
          couldCorrupt = true;
          stateSnapshot.setXChainSignNodesValue(newChainId, new XChainDetails(newChainId, existingItem.getBlockHeight(), Collections.emptySortedMap(), 0, -1));
        }

      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in DeleteXChain.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in DeleteXChain.updatestate.");
    }

  }

}
