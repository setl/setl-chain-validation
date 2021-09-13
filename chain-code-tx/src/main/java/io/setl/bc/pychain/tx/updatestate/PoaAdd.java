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
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;

public class PoaAdd {

  private static final Logger logger = LoggerFactory.getLogger(PoaAdd.class);


  /**
   * addDefaultAddressEntry().
   * <p>Helper function to add a default AddressEntry.
   * This helps to ensure consistency when adding this data from multiple places in code.</p>
   *
   * @param poaList : Asset Balances List.
   * @param address :
   *
   * @return :
   */
  public static PoaEntry addDefaultPoaEntry(MutableMerkle<PoaEntry> poaList, String address) {
    PoaEntry entry = new PoaEntry(0, address);
    poaList.add(entry);
    return entry;
  }


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
  public static ReturnTuple updatestate(PoaAddTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          // Check From Address
          String fromAddress = thisTX.getFromAddress();

          // Check Attorney Address
          if (!AddressUtil.verifyAddress(thisTX.getAttorneyAddress())) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid Attorney address {0}", thisTX.getAttorneyAddress()));
          }

          // Already expired.
          if (thisTX.getEndDate() < updateTime) {
            return new ReturnTuple(SuccessType.FAIL, "POA Expiry date is in the past.");
          }

          // Address permissions.
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
            boolean hasPermission = stateSnapshot.canUseTx(fromAddress, thisTX.getTxType());
            if ((!hasPermission) && ((keyPermission & AP_POAS) == 0)) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
            }
          }

          // Update...

          MutableMerkle<PoaEntry> poaList = stateSnapshot.getPowerOfAttorneys();
          PoaEntry addressPOA;
          if (checkOnly) {
            addressPOA = poaList.find(fromAddress);
            if (addressPOA == null) {
              return new ReturnTuple(SuccessType.PASS, "Check Only.");
            }
          } else {
            couldCorrupt = true;
            addressPOA = poaList.findAndMarkUpdated(fromAddress);
            if (addressPOA == null) {
              addressPOA = addDefaultPoaEntry(poaList, fromAddress);
            }
          }

          if (addressPOA.hasReference(thisTX.getReference())) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("POA Reference already exists : `{0}`", thisTX.getReference()));
          }

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          if (addressPOA.setReference(thisTX.getReference(), thisTX.getStartDate(), thisTX.getEndDate()) == null) {
            // Urgh, failed to set reference.
            logger.error("Error in PoaAdd.updatestate(), failed to set Reference, Tx Hash {}", thisTX.getHash());
            return new ReturnTuple(SuccessType.FAIL, "POA : Failed to set POA.");
          }

          PoaEntry poaEntry = addDefaultPoaEntry(poaList, addressPOA.getFullReference(thisTX.getReference()));

          poaEntry.setDetail(thisTX.getReference(), fromAddress, thisTX.getAttorneyAddress(), thisTX.getStartDate(), thisTX.getEndDate(), thisTX.getPoaItems());
        }


      } else {
        // Other Chain ...
        // Not supported.
      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in PoaAdd.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in PoaAdd.updatestate.");
    }

  }

}
