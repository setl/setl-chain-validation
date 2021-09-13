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
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.ExerciseEncumbranceTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;


public class ExerciseEncumbrance {

  private static final Logger logger = LoggerFactory.getLogger(ExerciseEncumbrance.class);


  /**
   * updatestate.
   * Exercise the requested encumbrance : Consume Encumbrance / Transfer Asset.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  @SuppressWarnings("squid:S2159") // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  public static ReturnTuple updatestate(ExerciseEncumbranceTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final TxType effectiveTxID = TxType.EXERCISE_ENCUMBRANCE;
    boolean sameChain = true;

    try {

      // Namespace ...
      final String namespace = cleanString(thisTX.getNameSpace());
      final String classid = cleanString(thisTX.getClassId());
      final String fullAssetID = namespace + "|" + classid;

      // Quick test for 'Same-Chain' TX. Enforce Same-Chain for SYS|STAKE.
      sameChain = ((thisTX.getChainId() == thisTX.getToChainId()) || ("SYS|STAKE".equalsIgnoreCase(fullAssetID)));

      // Quick Amount check.
      Number exerciseAmount = thisTX.getAmount();

      if ((new Balance(exerciseAmount)).lessThanZero()) {
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

          // POA address is valid ?
          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          // Recipient address is valid ?
          String toAddress = thisTX.getToAddress();
          if (!AddressUtil.verifyAddress(toAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid `To` address {0}", toAddress));
          }

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
          }

          // Asset locked ?
          if (stateSnapshot.isAssetLocked(fullAssetID)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Asset `{0}` is locked.", fullAssetID));
          }

          // Check POA Permission.
          PoaItem thisItem = null;

          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, fullAssetID, exerciseAmount, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Get / Check Subject Address.

          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          String subjectAddress = thisTX.getSubjectAddress();

          AddressEntry subjectAddressEntry = assetBalances.findAndMarkUpdated(subjectAddress);
          if (subjectAddressEntry == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "`Subject` Address does not exist.");
          }

          Balance subjectAmount = subjectAddressEntry.getAssetBalance(fullAssetID);

          // From == To Chain ? If so, check destination address.

          AddressEntry toAddressEntry;
          Balance toAmount = BALANCE_ZERO;
          Balance toTotal = new Balance(exerciseAmount);

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

          // Create 'To' Address if necessary.

          if (toAddressEntry == null) {

            couldCorrupt = true;

            toAddressEntry = addDefaultAddressEntry(assetBalances, toAddress, stateSnapshot.getVersion());

          } else {
            toAmount = toAddressEntry.getAssetBalance(fullAssetID);
            toTotal = toAmount.add(exerciseAmount);
          }

          // Check Asset present and un-encumbered.

          AddressEncumbrances thisAddressEncumbrance;
          if (checkOnly) {
            thisAddressEncumbrance = stateSnapshot.getEncumbrances().find(subjectAddress);
          } else {
            thisAddressEncumbrance = stateSnapshot.getEncumbrances().findAndMarkUpdated(subjectAddress);
          }

          if (thisAddressEncumbrance == null) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Address `{0}` has no Encumbrances.", subjectAddress)
            );
          }

          AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

          if (thisAssetEncumbrances == null) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Address `{0}` has no Encumbrances for `{1}`.", subjectAddress, fullAssetID)
            );
          }

          String reference = thisTX.getReference();

          // Admins & Beneficiary is tested below.
          EncumbranceEntry thisEncumbrance = thisAssetEncumbrances.getAggregateByReference(reference);

          if (thisEncumbrance == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Encumbrance `{0}` does not exist.", reference));
          }

          if (thisEncumbrance.amount.lessThan(exerciseAmount)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Insufficient Encumbrance at subject address.");
          }

          if (thisAssetEncumbrances.availableToEncumbrance(subjectAmount, reference, updateTime).lessThan(exerciseAmount)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Insufficient Asset available at subject address.");
          }

          boolean adminFromValid = thisEncumbrance.isAdministratorValid(poaAddress, updateTime);
          boolean beneficiaryFromValid = thisEncumbrance.isBeneficiaryValid(poaAddress, updateTime);
          boolean beneiciaryToValid = thisEncumbrance.isBeneficiaryValid(thisTX.getToAddress(), updateTime);

          // Need to be an administrator to exercise, if beneficiary, then may send anywhere, otherwise can only send to beneficiary.
          // if (ImTheAdministrator and toIsBeneficiary) or (ImAdministrator and ImBeneficiary)
          if (adminFromValid && (beneficiaryFromValid || beneiciaryToValid)) {

            if (checkOnly) {
              return new ReturnTuple(SuccessType.PASS, "Check Only.");
            }

            couldCorrupt = true;

            // Transfer Asset
            subjectAddressEntry.setAssetBalance(fullAssetID, subjectAmount.subtract(exerciseAmount));

            // 'To' could be local address ot destination 'Chain' address.
            toAddressEntry.setAssetBalance(fullAssetID, toTotal);

            logger.trace("AssetTransferTx from:{} to:{}", subjectAmount, toAmount);

            // consume POA

            if ((thisItem != null) && (thisTX.isPOA())) {

              thisItem.consume(exerciseAmount);

              if (thisItem.consumed()) {
                tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
              }
            }

            // consume Encumbrance

            if (thisEncumbrance.amount.lessThanEqualTo(exerciseAmount)) {
              // remove
              thisAssetEncumbrances.removeEncumbrance(reference);

              if (thisAssetEncumbrances.encumbrancesIsEmpty()) {
                thisAddressEncumbrance.removeAssetEncumbrance(fullAssetID);

                if (thisAddressEncumbrance.getEncumbranceList().isEmpty()) {
                  stateSnapshot.getEncumbrances().delete(subjectAddress);
                }
              }
            } else {
              // reduce
              thisAssetEncumbrances.consumeEncumbrance(reference, exerciseAmount);
            }

          } else {
            // No permissions.
            return new ReturnTuple(SuccessType.FAIL, "No permission for this Encumbrance.");
          }

        }

      } else if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {

        // This TX is destined for this chain (and is not SYS|STAKE)....

        if ((!checkOnly) && ((new Balance(exerciseAmount)).greaterThanZero())) {

          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          String chainAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getChainId());

          AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(chainAddress);

          // Check From and To balances for Overflow.

          Balance fromAmount = BALANCE_ZERO;

          if (fromAddressEntry != null) {
            fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance fromTotal = fromAmount.subtract(exerciseAmount);

          Balance toAmount = BALANCE_ZERO;

          AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getToAddress());
          Map<String, Balance> toBalances = null;

          if ((toAddressEntry == null) && (stateSnapshot.getStateConfig().getMustRegister())) {
            // If addresses must be registered, return to the 'From' Address, even if it does not exist on this chain.

            toAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getPoaAddress());
          }

          if (toAddressEntry != null) {
            toAmount = toAddressEntry.getAssetBalance(fullAssetID);
          }

          Balance toTotal = toAmount.add(exerciseAmount);

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

      return new ReturnTuple(SuccessType.FAIL, "Error in ExerciseEncumbrance.updatestate.");

    }

  }

}
