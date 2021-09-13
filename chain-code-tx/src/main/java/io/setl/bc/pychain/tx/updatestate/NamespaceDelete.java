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
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_NAMESPACES;
import static io.setl.common.StringUtils.cleanString;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.NamespaceDeleteTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.common.CommonPy.XChainParameters;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PoaNamespaceDelete.
 */
public class NamespaceDelete {

  private static final Logger logger = LoggerFactory.getLogger(NamespaceDelete.class);


  /**
   * updatestate.
   * Adds a Namespace definition to state.
   * Will update metadata on an existing entry.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(NamespaceDeleteTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.DELETE_NAMESPACE;
    long poaAddressPermissions = AP_NAMESPACES;

    try {

      // On Chain ?
      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // Correct priority for update ?
        if (priority == thisTX.getPriority()) {

          // Check From Address
          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA() && !AddressUtil.verifyAddress(poaAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
          }

          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            ReturnTuple aPerm;

            if (thisTX.isPOA()) {
              aPerm = checkPoaAddressPermissions(stateSnapshot, attorneyAddress, poaAddress, thisTX.getTxType(), effectiveTxID, poaAddressPermissions);
            } else {
              aPerm = checkAddressPermissions(stateSnapshot, attorneyAddress, thisTX.getTxType(), poaAddressPermissions);
            }

            if ((!checkOnly) && (aPerm.success != SuccessType.PASS)) {
              return aPerm;
            }
          }

          // Check Namespace Locking.
          String namespace = cleanString(thisTX.getNameSpace());

          if (namespace.length() == 0) {
            return new ReturnTuple(SuccessType.FAIL, "Namespace is not given.");
          }

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
          }

          // Check POA Permission.
          PoaItem thisItem = null;

          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, thisTX.getNameSpace(), 1L, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Crikey, appears OK ...

          // Get namespace entry.
          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();

          NamespaceEntry namespaceEntry = namespaceTree.find(namespace);
          if (namespaceEntry == null ) {
            // Success by default
            return new ReturnTuple(SuccessType.PASS, "");
          }

          // Namespace Owned ?
          if (!poaAddress.equals(namespaceEntry.getAddress())) {
            return new ReturnTuple(
                checkOnly ? SuccessType.WARNING : SuccessType.FAIL,
                MessageFormat.format("Can not delete namespace `{0}`, not owned by {1}", namespace, poaAddress)
            );
          }

          if (!namespaceEntry.isEmpty()) {
            return new ReturnTuple(
                checkOnly ? SuccessType.WARNING : SuccessType.FAIL,
                MessageFormat.format("Can not delete namespace `{0}`, asset classes exist.", namespace)
            );
          }

          // If only checking, not updating, this is the time to exit.
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

          namespaceTree.delete(namespace);

        }

      } else {

        XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(thisTX.getChainId());

        if (xChainDetails != null) {
          long xcParameters = xChainDetails.getParameters();
          boolean acceptNamespace = ((xcParameters & XChainParameters.AcceptNamespaces) != 0L);

          if (acceptNamespace) {
            MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
            NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(thisTX.getNameSpace());

            // Delete if present.
            if (namespaceEntry != null) {
              namespaceTree.delete(thisTX.getNameSpace());
            }
          }

        } // xChain Details.

      } // Other Chain

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in NamespaceDelete.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in NamespaceDelete.");
    }

  }

}
