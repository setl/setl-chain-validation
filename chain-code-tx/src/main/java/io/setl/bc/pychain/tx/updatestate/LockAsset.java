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

import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_LOCKING;
import static io.setl.common.StringUtils.cleanString;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.LockAssetTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PoaLockAsset.
 */
public class LockAsset {

  private static final Logger logger = LoggerFactory.getLogger(LockAsset.class);


  /**
   * updatestate.
   * Adds an Asset Class lock to state.
   * Will overwrite an existing entry.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return true if this transaction has been or could be applied without error.
   */
  @SuppressWarnings("squid:S3776") // Cognitive Complexity
  public static ReturnTuple updatestate(LockAssetTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.LOCK_ASSET;
    long poaAddressPermissions = AP_LOCKING;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          final String namespace = cleanString(thisTX.getNameSpace());
          final String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + (classid.length() > 0 ? "|" + classid : "");

          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            ReturnTuple aPerm;

            if (thisTX.isPOA()) {
              aPerm = checkPoaAddressPermissions(stateSnapshot, attorneyAddress, poaAddress, thisTX.getTxType(), effectiveTxID,
                  poaAddressPermissions);
            } else {
              aPerm = checkAddressPermissions(stateSnapshot, attorneyAddress, thisTX.getTxType(), poaAddressPermissions);
            }

            if ((!checkOnly) && (aPerm.success != SuccessType.PASS)) {
              return aPerm;
            }
          }

          // Check POA Permission.
          PoaItem thisItem = null;

          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, fullAssetID, 1L, checkOnly);
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Get Namespace details.
          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();

          NamespaceEntry namespaceEntry;
          if (checkOnly) {
            namespaceEntry = namespaceTree.find(namespace);
          } else {
            namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
          }

          if (namespaceEntry == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` has not been registered.", namespace));
          }

          // Namespace Owned ?
          if (!poaAddress.equals(namespaceEntry.getAddress())) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` is not controlled by the Tx Address {1}.", namespace, poaAddress));
          }

          // Class exists ?
          // No point in locking a class that does not exist as the lock will be removed when it is created (Also possible attack vector).
          if (classid.length() > 0) {
            if (!namespaceEntry.containsAsset(classid)) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  MessageFormat.format("Asset `{0}` has not been registered.", fullAssetID));
            }
          }

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          couldCorrupt = true;

          if ((thisItem != null) && (thisTX.isPOA())) {

            thisItem.consume(1L);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

          stateSnapshot.setAssetLockValue(fullAssetID, LockedAsset.Type.FULL);

        }

      }

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in PoaLockAsset.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in PoaLockAsset.updatestate.");
    }

  }


  private LockAsset() {

  }
}
