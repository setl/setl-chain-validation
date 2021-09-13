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
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.XChainTypes.CHAIN_ADDRESS_FORMAT;
import static io.setl.common.StringUtils.cleanString;

import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.IssuerTransferTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

/**
 * PoaIssuerTransfer.
 */
public class IssuerTransfer {

  private static final Logger logger = LoggerFactory.getLogger(IssuerTransfer.class);


  /**
   * updatestate.
   * Issuer transfer ignores Encumbrances
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return true if this transaction has been or could be applied without error.
   */
  @SuppressWarnings("squid:S2159") // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  public static ReturnTuple updatestate(IssuerTransferTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.TRANSFER_ASSET_AS_ISSUER;
    boolean sameChain = true;

    try {

      final String namespace = cleanString(thisTX.getNameSpace());
      final String classid = cleanString(thisTX.getClassId());

      final String fullAssetID = namespace + "|" + classid;

      // Quick test for 'Same-Chain' TX. Enforce Same-Chain for SYS|STAKE.
      sameChain = ((thisTX.getChainId() == thisTX.getToChainId()) || ("SYS|STAKE".equalsIgnoreCase(fullAssetID)));

      // Quick Amount check.
      Balance txAmount = new Balance(thisTX.getAmount());
      if (txAmount.lessThanZero()) {
        return new ReturnTuple(SuccessType.FAIL, "Invalid amount");
      }

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          //  Verify toChain exists.
          if ((!sameChain) && (stateSnapshot.getXChainSignNodesValue(thisTX.getToChainId()) == null)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Tx destination chain, {0}, is not registered.", thisTX.getToChainId())
            );
          }

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          // Get Namespace record
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

          // Namespace Owned ?
          if (!poaAddress.equals(namespaceEntry.getAddress())) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` is not owned by `{1}`.", namespace, poaAddress)
            );
          }

          // Class exists.
          if (!namespaceEntry.containsAsset(classid)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Class `{0}` is not registered in namespace `{1}`.", classid, namespace)
            );
          }

          // Address permissions.

          /* TODO : No Address permissions for transfers, should have one for xChain ?
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            // Address permissions.
            ReturnTuple aPerm = checkPoaAddressPermissions(stateSnapshot, attorneyAddress, poaAddress, thisTX.getTxType().getId(), effectiveTxID,
                poaAddressPermissions);
            if ((!checkOnly) && (aPerm.success != SuccessType.PASS)) {
              return false;
            }
          }
          */

          // Check POA Permission.
          PoaItem thisItem = null;

          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, fullAssetID, txAmount, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          String sourceAddress = thisTX.getSourceAddress();
          if (!AddressUtil.verifyAddress(sourceAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid `Source` address {0}", sourceAddress));
          }

          String toAddress = thisTX.getToAddress();
          if (!AddressUtil.verifyAddress(toAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid `To` address {0}", toAddress));
          }

          // Check From != To Address
          if (sameChain && sourceAddress.equals(toAddress)) {
            // Could just return true here, but Prototype did not, so...
            return new ReturnTuple(SuccessType.FAIL, "`From` and `To` are the same address.");
          }

          // Get / Check From Address.
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          AddressEntry sourceAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getSourceAddress());

          if (sourceAddressEntry == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Source Address does not exist in state.");
          }

          // Check Asset present
          Balance fromAmount = sourceAddressEntry.getAssetBalance(fullAssetID);

          if (fromAmount.lessThan(txAmount.getValue())) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Insufficient asset available.");
          }

          // Get / Check To Address
          // Assets either go to 'To' address or the 'Chain address :

          AddressEntry toAddressEntry;

          if (sameChain) {

            // Get / Check To Address

            toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

            if (toAddressEntry == null) {
              // Check if 'To' Address must already exist.

              if (stateSnapshot.getStateConfig().getMustRegister()) {
                return new ReturnTuple(
                    (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                    MessageFormat.format("Target Address `{0}` does not exist in state. MustRegister. Tx Hash {1}", toAddress, thisTX.getHash())
                );
              }

            }

          } else {
            // Send Assets to 'Chain' Address.

            toAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getToChainId());
            toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

          }

          // Get `To` Balance Entry.
          Balance existingToAmount = BALANCE_ZERO;

          if (toAddressEntry != null) {
            existingToAmount = toAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance toTotal = existingToAmount.add(txAmount);

          // Exit on Check :

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Transfer Assets :

          couldCorrupt = true;

          // Add `To` Address / Balance

          if (toAddressEntry == null) {
            toAddressEntry = addDefaultAddressEntry(assetBalances, toAddress, stateSnapshot.getVersion());
          }

          // Update POA

          if ((thisItem != null) && (thisTX.isPOA())) {

            thisItem.consume(txAmount);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

          // OK, Set new balances.
          sourceAddressEntry.setAssetBalance(fullAssetID, fromAmount.subtract(txAmount));

          // 'To' could be local address ot destination 'Chain' address.
          toAddressEntry.setAssetBalance(fullAssetID, toTotal);

          logger.trace("PoaIssuerTransferTx from:{} to:{}", fromAmount, existingToAmount);
        }

      } else if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {

        // This TX is destined for this chain (and is not SYS|STAKE)....

        if ((!checkOnly) && (txAmount.greaterThanZero())) {

          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

          String chainAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getChainId());

          // Get 'Chain' address entry and debit...

          AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(chainAddress);

          // Check From and To balances for Overflow.

          Balance fromAmount = BALANCE_ZERO;

          if (fromAddressEntry != null) {
            fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance fromTotal = fromAmount.subtract(txAmount);

          Balance toAmount = BALANCE_ZERO;

          AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getToAddress());

          if ((toAddressEntry == null) && (stateSnapshot.getStateConfig().getMustRegister())) {
            // If addresses must be registered, return to the 'From' Address, even if it does not exist on this chain.

            toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getSourceAddress());
          }

          if (toAddressEntry != null) {
            toAmount = toAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance toTotal = toAmount.add(txAmount);

          // Create 'Chain' address entry if missing. (the `From` address for xChain Transfers).

          couldCorrupt = true;

          if (fromAddressEntry == null) {
            fromAddressEntry = addDefaultAddressEntry(assetBalances, chainAddress, stateSnapshot.getVersion());
          }

          // Create Target Address and Balance if missing.

          if (toAddressEntry == null) {

            if (stateSnapshot.getStateConfig().getMustRegister()) {

              // If addresses must be registered, return to the 'Source' Address, even if it does not exist on this chain.

              toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getSourceAddress());

              if (toAddressEntry == null) {
                toAddressEntry = addDefaultAddressEntry(assetBalances, thisTX.getSourceAddress(), stateSnapshot.getVersion());
              }

            } else {
              // OK to create 'To' Address :

              toAddressEntry = addDefaultAddressEntry(assetBalances, thisTX.getToAddress(), stateSnapshot.getVersion());
            }
          }

          // Update balances.
          fromAddressEntry.setAssetBalance(fullAssetID,fromTotal);

          // OK, credit to the 'To' Address.
          toAddressEntry.setAssetBalance(fullAssetID,toTotal);

        } // amount > 0

      }

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (RuntimeException e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {
        logger.error(
            "Error in PoaIssuerTransfer.updatestate() : XChain IssuerTransfer has failed. ASSETS LOST : Tx Hash {} from chain {}",
            thisTX.getHash(),
            thisTX.getChainId()
        );
      } else {
        logger.error("Error in PoaIssuerTransfer.updatestate()", e);
      }

      return new ReturnTuple(SuccessType.FAIL, "Error in PoaIssuerTransfer.updatestate.");
    }

  }

}
