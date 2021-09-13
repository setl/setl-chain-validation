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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static io.setl.common.StringUtils.cleanString;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.AssetClassUpdateTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.XChainParameters;

/**
 * AssetClassUpdate.
 */
public class AssetClassUpdate {

  private static final Logger logger = LoggerFactory.getLogger(AssetClassUpdate.class);


  /**
   * updatestate.
   * Updates an existing Asset Class definition in state.
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
  public static ReturnTuple updatestate(AssetClassUpdateTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          if (thisTX.getNameSpace() == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Tx Namespace is Null.");
          }

          if (thisTX.getClassId() == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Tx Class is Null.");
          }

          String namespace = cleanString(thisTX.getNameSpace());
          String classid = cleanString(thisTX.getClassId());

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
          }

          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();

          NamespaceEntry namespaceEntry;
          if (checkOnly) {
            namespaceEntry = namespaceTree.find(namespace);
          } else {
            namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
          }

          if (namespaceEntry == null) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` has not been registered.", namespace)
            );
          }

          String fromAddress = thisTX.getFromAddress();

          // Address permissions.
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
            boolean hasPermission = stateSnapshot.canUseTx(fromAddress, thisTX.getTxType());
            if ((!hasPermission) && ((keyPermission & AP_CLASSES) == 0)) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
            }
          }

          if (fromAddress.equals(namespaceEntry.getAddress())) {

            if (!checkOnly) {
              if (namespaceEntry.containsAsset(classid)) {
                couldCorrupt = true;
                namespaceEntry.setAsset(new Asset(classid, thisTX.getMetadata()));
              } else {
                return new ReturnTuple(SuccessType.FAIL, classid + " class does not already exists");
              }
            }
          } else {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), namespace + " is not controlled by the Tx `From` Address");
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
            namespaceTree.add(new NamespaceEntry(thisTX.getNameSpace(), thisTX.getFromAddress(), ""));
            namespaceEntry = namespaceTree.findAndMarkUpdated(thisTX.getNameSpace());
          }

          if (namespaceEntry.containsAsset(thisTX.getClassId())) {
            if ((xcParameters & XChainParameters.ExternalClassPriority) != 0L) {
              namespaceEntry.setAsset(new Asset(thisTX.getClassId(), thisTX.getMetadata()));
            }
          } else {
            namespaceEntry.setAsset(new Asset(thisTX.getClassId(), thisTX.getMetadata()));

          }

        } // xChain Details.

      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in AssetClassUpdate.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in AssetClassUpdate.updatestate.");
    }

  }


  private AssetClassUpdate() {
  }

}


