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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

public class AssetTransferXChain {

  private static final Logger logger = LoggerFactory.getLogger(AssetTransferXChain.class);


  /**
   * updatestate().
   * <p>Update the state (snapshot) consistent with the given PoaAssetTransferXChainTx.</p>
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   *
   * @return :
   */
  @SuppressWarnings("squid:S2159") // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  public static ReturnTuple updatestate(AssetTransferXChainTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    /*
    Note : Encumbrances are checked, but not consumed.
     */

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.TRANSFER_ASSET_X_CHAIN;
    boolean sameChain = true;

    try {

      // Namespace ...
      String namespace = cleanString(thisTX.getNameSpace());
      String classid = cleanString(thisTX.getClassId());
      final String fullAssetID = namespace + "|" + classid;

      // Quick test for 'Same-Chain' TX. Enforce Same-Chain for SYS|STAKE.
      sameChain = ((thisTX.getChainId() == thisTX.getToChainId()) || ("SYS|STAKE".equalsIgnoreCase(fullAssetID)));

      // Quick Amount check.
      Balance amount = new Balance(thisTX.getAmount());

      if (amount.lessThanZero()) {
        return new ReturnTuple(SuccessType.FAIL, "Invalid amount");
      }

      // Is the 'From' Chain, this chain ?

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain ...

        // Verification : Apply only to the 'Native' chain...

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

          String toAddress = thisTX.getToAddress();
          if (!AddressUtil.verifyAddress(toAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid `To` address {0}", toAddress));
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
                effectiveTxID, fullAssetID, amount, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          //# Do not enforce Namespace / Asset existence. For xChain assets, the Namespace or Class may not exist here (bad practice I think)
          //# For normal chain operation, if the asset balance exists, then the Namespace & Class must also !

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace is locked : {0}", namespace)
            );
          }

          // Asset locked ?
          if (stateSnapshot.isAssetLocked(fullAssetID)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Asset is locked : {0}", fullAssetID)
            );
          }

          // Check From != To Address
          if (sameChain && poaAddress.equals(toAddress)) {
            // Could just return true here, but Python did not, so...
            return new ReturnTuple(SuccessType.FAIL, "`From` and `To` are the same address.");
          }

          // Get / Check From Address.
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

          AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(poaAddress);
          if (fromAddressEntry == null) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("From Address `{0}` does not exist in state.", poaAddress)
            );
          }

          // Check Asset present and un-encumbered.

          Balance encumbrance = BALANCE_ZERO;

          AddressEncumbrances thisAddressEncumbrance = stateSnapshot.getEncumbrances().find(poaAddress);

          if (thisAddressEncumbrance != null) {
            encumbrance = thisAddressEncumbrance.getEncumbranceTotal(poaAddress, fullAssetID, updateTime);
          }

          Balance fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);

          if (fromAmount.lessThan(encumbrance.add(amount))) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                "Insufficient un-encumbered asset available."
            );
          }

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // From == To Chain ? If so, check destination address.

          AddressEntry toAddressEntry;

          if (sameChain) {

            // Get / Check To Address

            toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

            if (toAddressEntry == null) {
              // Check if 'To' Address must already exist.

              if (stateSnapshot.getStateConfig().getMustRegister()) {
                return new ReturnTuple(
                    SuccessType.FAIL,
                    MessageFormat.format("Target Address `{0}` does not exist in state. MustRegister.", toAddress)
                );
              }

            }
          } else {
            // Send Assets to 'Chain' Address.

            toAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getToChainId());
            toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

          }

          // Check 'From' total balance.
          Balance fromTotal = fromAmount.subtract(amount);

          // OK Start snapshot update :
          couldCorrupt = true;

          // Create 'To' Address if necessary.
          if (toAddressEntry == null) {
            toAddressEntry = addDefaultAddressEntry(assetBalances, toAddress, stateSnapshot.getVersion());
          }
          // Check 'To' total balance.
          Balance toTotal = toAddressEntry.getAssetBalance(fullAssetID).add(amount);

          // Transfer Assets :
          fromAddressEntry.setAssetBalance(fullAssetID, fromTotal);
          toAddressEntry.setAssetBalance(fullAssetID, toTotal);

          // Update POA
          if ((thisItem != null) && (thisTX.isPOA())) {
            thisItem.consume(amount);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

          logger.trace("AssetTransferXChain from:{} to:{}", fromAmount, toTotal);

          // Phew, DONE.

        } // priority

      } else if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {

        // This TX is destined for this chain (and is not SYS|STAKE)....

        if ((!checkOnly) && (amount.greaterThanZero())) {

          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          String chainAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getChainId());

          AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(chainAddress);

          // Check From and To balances for Overflow.

          Balance fromAmount = BALANCE_ZERO;

          if (fromAddressEntry != null) {
            fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance fromTotal = fromAmount.subtract(amount);

          Balance toAmount = BALANCE_ZERO;

          AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getToAddress());

          if ((toAddressEntry == null) && (stateSnapshot.getStateConfig().getMustRegister())) {
            // If addresses must be registered, return to the 'From' Address, even if it does not exist on this chain.

            toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getPoaAddress());
          }

          if (toAddressEntry != null) {
            toAmount = toAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance toTotal = toAmount.add(amount);

          // Create 'Chain' address entry if missing. (the `From` address for xChain Transfers).

          couldCorrupt = true;

          if (fromAddressEntry == null) {
            fromAddressEntry = addDefaultAddressEntry(assetBalances, chainAddress, stateSnapshot.getVersion());
          }

          // Create Target Address and Balance if missing.

          if (toAddressEntry == null) {

            if (stateSnapshot.getStateConfig().getMustRegister()) {

              // If addresses must be registered, return to the 'From' Address, even if it does not exist on this chain.

              toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getPoaAddress());

              if (toAddressEntry == null) {
                toAddressEntry = addDefaultAddressEntry(assetBalances, thisTX.getPoaAddress(), stateSnapshot.getVersion());
              }

            } else {
              // OK to create 'To' Address :

              toAddressEntry = addDefaultAddressEntry(assetBalances, thisTX.getToAddress(), stateSnapshot.getVersion());
            }
          }

          // Update balances.
          toAddressEntry.setAssetBalance(fullAssetID, toTotal);
          fromAddressEntry.setAssetBalance(fullAssetID, fromTotal);

        } // amount > 0

      } // else if

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (RuntimeException e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {
        logger.error(
            "Error in AssetTransferXChain.updatestate() : XChain Transfer has failed. ASSETS LOST : Tx Hash {} from chain {}",
            thisTX.getHash(),
            thisTX.getChainId()
        );
      } else {
        logger.error("Error in AssetTransferXChain.updatestate()", e);
      }

      return new ReturnTuple(SuccessType.FAIL, "Error in AssetTransferXChain.updatestate.");
    }

  }


}
