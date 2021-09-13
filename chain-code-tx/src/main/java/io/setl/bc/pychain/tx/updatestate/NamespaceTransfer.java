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
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_NAMESPACES;
import static io.setl.common.StringUtils.cleanString;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.NamespaceTransferTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.common.CommonPy.XChainParameters;

/**
 * NamespaceTransfer.
 */
public class NamespaceTransfer {

  private static final Logger logger = LoggerFactory.getLogger(NamespaceTransfer.class);


  private static void moveNegativeHoldings(
      NamespaceTransferTx thisTX,
      MutableMerkle<AddressEntry> assetBalances,
      NamespaceEntry namespaceEntry,
      int version
  ) {

    // Move negative holdings across with ownership of the namespace.

    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getPoaAddress());
    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getToAddress());

    String fullAssetID;
    Balance fromAmount;
    Balance toAmount;

    if (fromAddressEntry != null) {

      Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();

      if (fromBalances != null) {

        // Check 'To' Address exists.
        if (toAddressEntry == null) {
          toAddressEntry = addDefaultAddressEntry(assetBalances, thisTX.getToAddress(), version);
        }

        Map<String, Balance> toBalances = toAddressEntry.getClassBalance();
        if (toBalances == null) {
          // Dead code in state version VERSION_SET_FULL_ADDRESS or above.
          toBalances = toAddressEntry.makeClassBalance();
        }

        // Get a copy of the Classes associated with this Namespace. We need a copy as we will be changing the class balance map.
        Set<String> classNames = namespaceEntry.getAllAssetNames();

        // For each class, move any negative balances.
        for (String className : classNames) {

          // Asset ID :
          fullAssetID = thisTX.getNameSpace() + "|" + className;

          // Amounts
          fromAmount = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
          toAmount = toBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

          // Move...
          if (fromAmount.lessThanZero()) {
            Balance newAmount = toAmount.add(fromAmount);
            toAddressEntry.setAssetBalance(fullAssetID, newAmount);
            fromAddressEntry.setAssetBalance(fullAssetID, BALANCE_ZERO);

            logger.trace("NamespaceTransferTx Asset {} moved {}", fullAssetID, fromAmount);
          }
        }
      }
    }
  }


  /**
   * updatestate.
   * Adds a Namespace definition to state.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(NamespaceTransferTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.TRANSFER_NAMESPACE;
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
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          // Check To Address
          String toAddress = thisTX.getToAddress();

          if (!AddressUtil.verifyAddress(toAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid to address {0}", toAddress));
          }

          // Get Entry
          String namespace = cleanString(thisTX.getNameSpace());

          if (namespace.length() == 0) {
            return new ReturnTuple(SuccessType.FAIL, "Namespace is not given.");
          }

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
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
                effectiveTxID, thisTX.getNameSpace(), 1L, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Get namespace entry.
          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);

          if (namespaceEntry == null) {
            return new ReturnTuple(
                checkOnly ? SuccessType.WARNING : SuccessType.FAIL,
                MessageFormat.format("Namespace `{0}` has not been registered.", namespace)
            );
          } else {
            // Namespace Owned ?
            if (!poaAddress.equals(namespaceEntry.getAddress())) {
              return new ReturnTuple(
                  checkOnly ? SuccessType.WARNING : SuccessType.FAIL,
                  MessageFormat.format("Can not update namespace `{0}`, not owned by {1}", namespace, poaAddress)
              );
            }
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Add or Update as necessary.
          if ((thisItem != null) && (thisTX.isPOA())) {

            thisItem.consume(1L);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

          //
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

          // Get / Check To Address
          AddressEntry toAddressEntry = assetBalances.find(thisTX.getToAddress());
          if (toAddressEntry == null) {
            // Check if 'To' Address must already exist.

            if (stateSnapshot.getStateConfig().getMustRegister()) {
              return new ReturnTuple(
                  SuccessType.FAIL,
                  MessageFormat.format("Target Address `{0}` does not exist in state. MustRegister. Tx Hash {1}", thisTX.getToAddress(), thisTX.getHash())
              );
            }

          }

          couldCorrupt = true;

          namespaceEntry.setMetadata(thisTX.getMetadata());
          namespaceEntry.setAddress(thisTX.getToAddress());

          // Move negative holdings across with ownership of the namespace.
          // Move Issuance holdings

          moveNegativeHoldings(thisTX, assetBalances, namespaceEntry, stateSnapshot.getVersion());

        }

      } else {

        XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(thisTX.getChainId());

        if (xChainDetails != null) {
          long xcParameters = xChainDetails.getParameters();
          boolean externalNamespace = ((xcParameters & XChainParameters.ExternalNamespacePriority) != 0L);

          if (externalNamespace) {

            MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
            NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(thisTX.getNameSpace());
            MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

            // Add or Update as necessary.
            if (namespaceEntry == null) {
              namespaceTree.add(new NamespaceEntry(thisTX.getNameSpace(), thisTX.getPoaAddress(), thisTX.getMetadata()));
            } else {
              // Overwrite (keeping class details).
              namespaceEntry.setAddress(thisTX.getToAddress());

              // Move Issuance holdings
              moveNegativeHoldings(thisTX, assetBalances, namespaceEntry, stateSnapshot.getVersion());

            }

          } // Take External Namespace updates.
        } // xChin Details.

      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in NamespaceTransfer.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in NamespaceTransfer.updatestate.");
    }

  }

}
