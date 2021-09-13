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

import static io.setl.bc.pychain.state.entry.AddressEntry.addBalanceOnlyAddressEntry;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.StringUtils.cleanString;

import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.AssetIssueTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

@SuppressWarnings("squid:S1118") // Private Constructor.
public class AssetIssue {

  private static final Logger logger = LoggerFactory.getLogger(AssetIssue.class);


  /**
   * updatestate.
   * Issues new asset to the given address, debits issuance address accordingly.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(AssetIssueTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.ISSUE_ASSET;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          Number amount = thisTX.getAmount();

          if ((new Balance(amount)).lessThanZero()) {
            return new ReturnTuple(SuccessType.FAIL, "Invalid amount");
          }

          // Check From Address
          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          // Namespace ...
          String namespace = cleanString(thisTX.getNameSpace());
          String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + "|" + classid;

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace is locked : {0}", namespace)
            );
          }

          // Check POA Permission.
          PoaItem thisItem = null;

          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, fullAssetID, amount, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Get namespace details.
          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

          // Namespace Exist ?
          if (namespaceEntry == null) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` has not been registered.", namespace)
            );
          }

          // Namespace Owned ?
          if (!poaAddress.equals(namespaceEntry.getAddress())) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` is not controlled by the {1} Tx Address `{2}`", namespace, (thisTX.isPOA() ? "POA" : ""), poaAddress)
            );
          }

          // Asset locked ?
          if (stateSnapshot.isAssetLocked(fullAssetID)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Asset is locked : {0}", fullAssetID));
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Class Details.
          if (!namespaceEntry.containsAsset(classid)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Class `{0}` is not registered in namespace `{1}`.", classid, namespace));
          }
          // Other parameters

          // From - To Address, just says its OK and don't do anything.
          String toAddress = thisTX.getToAddress();

          if (toAddress.equals(poaAddress)) {
            return new ReturnTuple(SuccessType.PASS, "");
          }

          // OK Get account information :

          MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

          logger.info("Look:{} in addresses", fullAssetID);

          // Get From / To Address

          AddressEntry fromAddressEntry = assetBalanceTree.findAndMarkUpdated(poaAddress);
          if (fromAddressEntry == null) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("From Address `{0}` does not exist in state.", poaAddress));
          }

          AddressEntry toAddressEntry = assetBalanceTree.findAndMarkUpdated(toAddress);

          // Check if 'To' Address must already exist.

          if (toAddressEntry == null && stateSnapshot.getStateConfig().getMustRegister()) {
            return new ReturnTuple(
                SuccessType.FAIL,
                MessageFormat.format("Target Address `{0}` does not exist in state. MustRegister. Tx Hash {1}", toAddress, thisTX.getHash())
            );
          }

          // After this point, an exception could result in a corrupted state.
          couldCorrupt = true;

          if (toAddressEntry == null) {

            // addBalanceOnlyAddressEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
            // version 'VERSION_SET_FULL_ADDRESS'

            toAddressEntry = addBalanceOnlyAddressEntry(assetBalanceTree, toAddress, stateSnapshot.getVersion());
          }

          // Update Balances, adding 'To' address entry if required.
          Balance bFrom = fromAddressEntry.getAssetBalance(fullAssetID);
          Balance bTo = toAddressEntry.getAssetBalance(fullAssetID);

          // Calculate new balances
          Balance fromTotal = bFrom.subtract(amount);
          Balance toTotal = bTo.add(amount);

          toAddressEntry.setAssetBalance(fullAssetID, toTotal);
          fromAddressEntry.setAssetBalance(fullAssetID, fromTotal);

          if ((thisItem != null) && (thisTX.isPOA())) {

            thisItem.consume(amount);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

          logger.info("AssetIssueTx, Hash {} from:{} to:{}", thisTX.getHash(), bFrom, bTo);

        }

      }

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {

      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in AssetIssue.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in AssetIssue.updatestate.");
    }

  }

}
