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
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.TransferToManyTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

public class TransferToMany {

  private static final Logger logger = LoggerFactory.getLogger(TransferToMany.class);


  /**
   * updatestate.
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
  public static ReturnTuple updatestate(TransferToManyTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    /*
    Note : Encumbrances are checked, but not consumed.
    Transfers from the Issuance address are allowed (if the authoring address is the Issuance address).
     */

    ReturnTuple rVal = null;
    StateSnapshot snapshotWrap = null;
    final TxType effectiveTxID = TxType.TRANSFER_ASSET_TO_MANY;
    boolean sameChain = true;

    try {

      // Namespace ...
      String namespace = cleanString(thisTX.getNameSpace());
      String classid = cleanString(thisTX.getClassId());
      final String fullAssetID = namespace + "|" + classid;

      // Quick test for 'Same-Chain' TX. Enforce Same-Chain for SYS|STAKE.
      sameChain = ((thisTX.getChainId() == thisTX.getToChainId()) || ("SYS|STAKE".equalsIgnoreCase(fullAssetID)));

      // Quick Amount check.
      if ((new Balance(thisTX.getAmount())).lessThanZero()) {
        rVal = new ReturnTuple(SuccessType.FAIL, "Tx Amount is negative.");
        return rVal;
      }

      boolean mustRegister = stateSnapshot.getStateConfig().getMustRegister();

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain ...

        // Verification : Apply only to the 'Native' chain...

        if (priority == thisTX.getPriority()) {

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA() && !AddressUtil.verifyAddress(poaAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
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
                effectiveTxID, fullAssetID, thisTX.getAmount(), checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          //# Do not enforce Namespace / Asset existence. For xChain assets, the Namespace or Class may not exist here (bad practice I think)
          //# For normal chain operation, if the asset balance exists, then the Namespace & Class must also !

          // Check namespace Ownership and note if we are the issuer.

          boolean isIssuer = false;

          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

          if ((namespaceEntry != null) && (poaAddress.equals(namespaceEntry.getAddress()))) {

            isIssuer = true;

          }

          if (!isIssuer) {

            // Namespace locked ?
            // IF the transfer is being made by the issuer, allow it anyway, otherwise, exit.

            if (stateSnapshot.isAssetLocked(namespace)) {
              rVal = new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
              return rVal;
            }

            // Asset locked ?
            if (stateSnapshot.isAssetLocked(fullAssetID)) {
              rVal = new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Asset is locked : {0}", fullAssetID));
              return rVal;
            }

          }

          //  Verify toChain exists.
          if ((!sameChain) && (stateSnapshot.getXChainSignNodesValue(thisTX.getToChainId()) == null)) {
            rVal = new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Tx destination chain, {0}, is not registered.", thisTX.getToChainId()));
            return rVal;
          }

          snapshotWrap = stateSnapshot.createSnapshot();

          // Get / Check From Address.
          MutableMerkle<AddressEntry> assetBalances = snapshotWrap.getAssetBalances();

          AddressEntry fromAddressEntry;
          if (checkOnly) {
            fromAddressEntry = assetBalances.find(poaAddress);
          } else {
            fromAddressEntry = assetBalances.findAndMarkUpdated(poaAddress);
          }

          if (fromAddressEntry == null) {
            rVal = new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("From Address `{0}` does not exist in state.", poaAddress)
            );
            return rVal;
          }

          Balance totalAmount = BALANCE_ZERO;
          Balance thisAmount;
          String toAddress;

          // Verify Amount vs Address Payments.

          for (Object thisPayment : thisTX.getToAddresses()) {
            toAddress = ((Object[]) thisPayment)[0].toString();  // Just to verify...
            thisAmount = new Balance(((Object[]) thisPayment)[1]);

            if (thisAmount.lessThanEqualZero()) {
              rVal = new ReturnTuple(
                  SuccessType.FAIL,
                  MessageFormat.format("Negative, or Zero, amount sent to address `{0}` : `{1}`", toAddress, thisAmount.toString())
              );
              return rVal;
            }

            totalAmount = totalAmount.add(thisAmount);
          }

          if (totalAmount.compareTo(thisTX.getAmount()) != 0) {
            rVal = new ReturnTuple(SuccessType.FAIL, "Address payments do not equal Tx Total amount.");
            return rVal;
          }

          // Check Asset present and un-encumbered.

          Balance fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);

          if (!isIssuer) {
            // Check existing balance and encumbrances (If not the issuer).
            Balance encumbrance = BALANCE_ZERO;

            AddressEncumbrances thisAddressEncumbrance = snapshotWrap.getEncumbrances().find(poaAddress);

            if (thisAddressEncumbrance != null) {
              encumbrance = thisAddressEncumbrance.getEncumbranceTotal(poaAddress, fullAssetID, updateTime);
            }

            // if ((fromAmount - encumbrance) < totalAmount) {
            if (fromAmount.lessThan(totalAmount.add(encumbrance))) {
              rVal = new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Insufficient un-encumbered asset available.");
              return rVal;
            }

          }

          // From == To Chain ? If so, check destination address.

          AddressEntry toAddressEntry;
          TreeMap<String, AddressEntry> toAddresses = new TreeMap<>();

          if (sameChain) {

            for (Object thisPayment : thisTX.getToAddresses()) {
              toAddress = (String) ((Object[]) thisPayment)[0];
              toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

              if (toAddressEntry == null) {
                if (mustRegister) {
                  rVal = new ReturnTuple(
                      (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                      MessageFormat.format("Target Address `{0}` does not exist in state. MustRegister. Tx Hash {1}",
                          toAddress, thisTX.getHash()
                      )
                  );
                  return rVal;
                }

                if (!checkOnly) {
                  toAddressEntry = addDefaultAddressEntry(assetBalances, toAddress, stateSnapshot.getVersion());
                }
              }

              toAddresses.put(toAddress, toAddressEntry);

            }
          }

          // Exit on Check Only.

          if (checkOnly) {
            rVal = new ReturnTuple(SuccessType.PASS, "Check Only.");
            return rVal;
          }

          // Transfer Assets :
          // Get `From` Balance Entry, checking that the Balance Map exists.

          fromAddressEntry.setAssetBalance(fullAssetID, fromAmount.subtract(totalAmount));

          if (sameChain) {

            Balance toAmount;

            for (Object thisPayment : thisTX.getToAddresses()) {
              toAddress = ((Object[]) thisPayment)[0].toString();
              toAddressEntry = toAddresses.get(toAddress);
              thisAmount = new Balance(((Object[]) thisPayment)[1]);

              toAmount = toAddressEntry.getAssetBalance(fullAssetID);
              toAddressEntry.setAssetBalance(fullAssetID, toAmount.add(thisAmount));
            } // for

          } else {
            // ! sameChain
            // Send Assets to 'Chain' Address.

            toAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getToChainId());
            toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

            if (toAddressEntry == null) {
              toAddressEntry = addDefaultAddressEntry(assetBalances, toAddress, stateSnapshot.getVersion());
            }

            Balance toAmount = toAddressEntry.getAssetBalance(fullAssetID);
            Balance newTo = (new Balance(toAmount)).add(thisTX.getAmount());
            toAddressEntry.setAssetBalance(fullAssetID, newTo);

          }

          // Update POA (After balance updates), on base state snapshot.

          if ((thisItem != null) && (thisTX.isPOA())) {

            thisItem.consume(totalAmount.getValue());

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

        } // priority

      } else if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {

        if ((!checkOnly) && ((new Balance(thisTX.getAmount())).greaterThanZero())) {

          snapshotWrap = stateSnapshot.createSnapshot();

          MutableMerkle<AddressEntry> assetBalances = snapshotWrap.getAssetBalances();
          String chainAddress = String.format(CHAIN_ADDRESS_FORMAT, thisTX.getChainId());

          // Get 'Chain' address entry.

          AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(chainAddress);

          if (fromAddressEntry == null) {
            fromAddressEntry = addDefaultAddressEntry(assetBalances, chainAddress, stateSnapshot.getVersion());
          }

          Balance fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);
          Balance newValue = fromAmount.subtract(thisTX.getAmount());
          fromAddressEntry.setAssetBalance(fullAssetID, newValue);

          // Crediting on xChain.
          // If a payment can not be made to the given address, then it will be returned to the issuing address on this chain.

          String toAddress;
          fromAddressEntry = null;
          AddressEntry toAddressEntry;
          Balance amount;
          Balance toAmount;

          // For each payment

          for (Object thisPayment : thisTX.getToAddresses()) {
            toAddress = (String) ((Object[]) thisPayment)[0];
            amount = new Balance(((Object[]) thisPayment)[1]);

            // Get payee address, create if you can or revert to the payer.

            toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);

            if (toAddressEntry == null) {
              if (mustRegister) {
                if (fromAddressEntry == null) {
                  fromAddressEntry = assetBalances.findAndMarkUpdated(thisTX.getPoaAddress());
                  if (fromAddressEntry == null) {
                    fromAddressEntry = addDefaultAddressEntry(assetBalances, thisTX.getPoaAddress(), stateSnapshot.getVersion());
                  }
                }

                toAddressEntry = fromAddressEntry;
              } else {
                toAddressEntry = addDefaultAddressEntry(assetBalances, toAddress, stateSnapshot.getVersion());
              }
            }

            // OK, resolved payment address.
            // Make payment.

            toAmount = toAddressEntry.getAssetBalance(fullAssetID);
            toAddressEntry.setAssetBalance(fullAssetID, toAmount.add(amount));
          }

        }

      }

      rVal = new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));
      return rVal;

    } catch (RuntimeException e) {
      if ((!sameChain) && (thisTX.getToChainId() == stateSnapshot.getChainId())) {
        logger.error(
            "Error in TransferToManyTx.updatestate() : XChain Transfer has failed. ASSETS LOST : Tx Hash {} from chain {}",
            thisTX.getHash(),
            thisTX.getChainId()
        );
      } else {
        logger.error("Error in TransferToManyTx.updatestate()", e);
      }

      rVal = new ReturnTuple(SuccessType.FAIL, "Error in TransferToManyTx.updatestate.");

    } finally {
      if ((!checkOnly) && (rVal != null) && (rVal.success == SuccessType.PASS) && (snapshotWrap != null)) {
        try {
          snapshotWrap.commit();
        } catch (StateSnapshotCorruptedException e) {
          logger.error("State snapshot was corrupted {}", e.getMessage());
          rVal = new ReturnTuple(SuccessType.FAIL, "State corruption in TransferToManyTx.updatestate.");
        }
      }
    }

    return rVal;

  }

}
