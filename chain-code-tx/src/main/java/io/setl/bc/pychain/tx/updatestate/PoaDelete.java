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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POAS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.PoaDeleteTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;

public class PoaDelete {

  private static final Logger logger = LoggerFactory.getLogger(PoaDelete.class);


  /**
   * updatestate.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(PoaDeleteTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          // Check From Address
          String fromAddress = thisTX.getFromAddress();

          // Address permissions.
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
            boolean hasPermission = stateSnapshot.canUseTx(fromAddress, thisTX.getTxType());

            if ((!hasPermission) && ((keyPermission & AP_POAS) == 0)) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
            }
          }

          //
          String issuingAddress = thisTX.getIssuingAddress();
          if ((issuingAddress == null) || (issuingAddress.length() == 0)) {
            issuingAddress = fromAddress;
          }

          MutableMerkle<PoaEntry> poaList = stateSnapshot.getPowerOfAttorneys();
          PoaEntry addressPOA;
          if (checkOnly) {
            addressPOA = poaList.find(issuingAddress);
          } else {
            couldCorrupt = true;
            addressPOA = poaList.findAndMarkUpdated(issuingAddress);
          }

          if (addressPOA == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Address has no POAs");
          }

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          String reference = thisTX.getReference();
          String fullReference = addressPOA.getFullReference(reference);

          PoaHeader thisHeader = addressPOA.getReference(reference);

          if (thisHeader != null) {
            // If this Delete instruction is from the granting address allow deletion.
            // If this Reference has expired allow anyone to delete it. (allows for automated tidy daemons).
            if ((issuingAddress.equals(fromAddress)) || (thisHeader.expiryDate < updateTime)) {
              poaList.delete(fullReference);

              addressPOA.removeReference(reference);

              if (addressPOA.getReferenceCount() == 0) {
                poaList.delete(issuingAddress);
              }
            }
          } else {
            // Header does not exist, so Detail should not also.
            // Un-necessary ?
            poaList.delete(fullReference);
          }

        }

      } else {
        // Other XChain ...
        // Not supported.
      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in PoaDelete.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in PoaDelete.updatestate.");
    }

  }

}
