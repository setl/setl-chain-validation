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
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static io.setl.common.StringUtils.cleanString;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.AssetClassRegisterTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.common.CommonPy.XChainParameters;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetClassRegister {

  private static final Logger logger = LoggerFactory.getLogger(AssetClassRegister.class);


  /**
   * updatestate.
   * Adds an Asset Class definition to state.
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
  public static ReturnTuple updatestate(AssetClassRegisterTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.REGISTER_ASSET_CLASS;
    long poaAddressPermissions = AP_CLASSES;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          final String namespace = cleanString(thisTX.getNameSpace());
          final String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + "|" + classid;

          if (namespace.isEmpty()) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Tx Namespace is empty.");
          }

          if (classid.isEmpty()) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Tx Class is empty.");
          }

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
          }

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          // Address permissions.

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

          if (!poaAddress.equals(namespaceEntry.getAddress())) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` is not controlled by the {1} Tx Address `{2}`",
                namespace, (thisTX.isPOA() ? "POA" : ""),  poaAddress));
          }

          //
          if (namespaceEntry.containsAsset(classid)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Class `{0}` already exists.", classid));
          }

          //
          if (!checkOnly) {
            couldCorrupt = true;

            if ((thisItem != null) && (thisTX.isPOA())) {

              thisItem.consume(1L);

              if (thisItem.consumed()) {
                tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
              }
            }

            namespaceEntry.setAsset(new Asset(classid, thisTX.getMetadata()));

            // Check for and delete if necessary any Asset lock.

            if (stateSnapshot.isAssetLocked(fullAssetID)) {
              stateSnapshot.removeAssetLockValue(fullAssetID);
            }

          }


        }

      } else {
        // Other Chain ...

        XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(thisTX.getChainId());

        if (xChainDetails != null) {
          long xcParameters = xChainDetails.getParameters();

          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(thisTX.getNameSpace());

          if (namespaceEntry == null) {
            couldCorrupt = true;
            namespaceTree.add(new NamespaceEntry(thisTX.getNameSpace(), thisTX.getPoaAddress(), ""));
            namespaceEntry = namespaceTree.findAndMarkUpdated(thisTX.getNameSpace());
          }

          if (namespaceEntry.containsAsset(thisTX.getClassId())) {
            if ((xcParameters & XChainParameters.ExternalClassPriority) != 0L) {
              couldCorrupt = true;
              namespaceEntry.setAsset(new Asset(thisTX.getClassId(), thisTX.getMetadata()));
            }
          } else {
            couldCorrupt = true;
            namespaceEntry.setAsset(new Asset(thisTX.getClassId(), thisTX.getMetadata()));

          }

        } // xChain Details.
      } // Other Chain

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in AssetClassRegister.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in AssetClassRegister.updatestate.");
    }

  }


  private AssetClassRegister() {

  }
}
