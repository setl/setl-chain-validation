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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_ADMIN;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASS_ADMIN;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_MANAGER;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_MANAGER_CONTROL;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_NAMESPACE;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_NAMESPACE_ADMIN;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_ADMIN;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AddressPermissionsTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;

public class AddressPermissions {

  private static final Logger logger = LoggerFactory.getLogger(AddressPermissions.class);


  /**
   * updatestate.
   * <p>Update state for Address Permissions Tx.</p>
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(AddressPermissionsTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId() && priority == thisTX.getPriority()) {

        String fromAddress = thisTX.getFromAddress();

        if (!(fromAddress.equals(thisTX.getToAddress()) || AddressUtil.verifyAddress(thisTX.getToAddress()))) {
          return new ReturnTuple(SuccessType.FAIL, "`To` Address is not valid.");
        }

        // If only checking, not updating, this is the time to exit.
        if (checkOnly) {
          return new ReturnTuple(SuccessType.PASS, "Check Only.");
        }

        // Address permissions.
        long newPermission = 0L;
        boolean hasPermission = false;
        boolean setTxIDs = false;
        String newAddress = thisTX.getToAddress();

        if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
          long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
          long existingPermission = stateSnapshot.getAddressPermissions(newAddress);

          if (keyPermission >= 0) {
            if ((keyPermission & AP_ADMIN) != 0) {
              newPermission = thisTX.getAddressPermissions();
              hasPermission = true;
              setTxIDs = true;
            } else if ((keyPermission & AP_MANAGER) != 0) {
              existingPermission = existingPermission & (~AP_MANAGER_CONTROL);
              newPermission = existingPermission | thisTX.getAddressPermissions() | AP_MANAGER_CONTROL;
              hasPermission = true;

              if ((AP_MANAGER_CONTROL & AP_TX_ADMIN) != 0) {
                setTxIDs = true;
              }
            } else {
              if ((keyPermission & AP_NAMESPACE_ADMIN) != 0) {
                existingPermission = existingPermission & (~AP_NAMESPACE);
                newPermission = existingPermission | newPermission | (thisTX.getAddressPermissions() & AP_NAMESPACE);
                existingPermission = newPermission;
                hasPermission = true;
              }

              if ((keyPermission & AP_CLASS_ADMIN) != 0) {
                existingPermission = existingPermission & (~AP_CLASS);
                newPermission = existingPermission | newPermission | (thisTX.getAddressPermissions() & AP_CLASS);
                existingPermission = newPermission;
                hasPermission = true;
              }

              if ((keyPermission & AP_TX_ADMIN) != 0) {
                existingPermission = existingPermission & (~AP_TX_LIST);
                newPermission = existingPermission | newPermission | (thisTX.getAddressPermissions() & AP_TX_LIST);
                hasPermission = true;
                setTxIDs = true;
              }
            }
          }
        }

        if (!hasPermission) {
          return new ReturnTuple(SuccessType.FAIL, "Inadequate Address permissioning");
        }

        MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

        AddressEntry addressEntry = assetBalances.findAndMarkUpdated(newAddress);

        if (addressEntry == null) {

          couldCorrupt = true;

          addDefaultAddressEntry(assetBalances, newAddress, stateSnapshot.getVersion());

          addressEntry = assetBalances.findAndMarkUpdated(newAddress);
        }

        addressEntry.setAddressPermissions(newPermission);

        if (setTxIDs) {
          addressEntry.setAuthorisedTx(thisTX.getAddressTransactions());
        }

      }

      // Do not propagate Address permissions xChain.

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {

      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in AddressPermissions.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in AddressPermissions.updatestate.");

    }

  }

}
