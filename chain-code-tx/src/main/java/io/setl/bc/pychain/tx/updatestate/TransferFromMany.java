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
import static io.setl.common.StringUtils.cleanString;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.TransferFromManyTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.util.Constants;

@SuppressWarnings("squid:S1118") // Suppress private constructor warning.
public class TransferFromMany {

  private static final Logger logger = LoggerFactory.getLogger(TransferFromMany.class);


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
  public static ReturnTuple updatestate(TransferFromManyTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    /*
    Note : Encumbrances are checked, but not consumed.
    Transfers from the Issuance address are allowed (if the authoring address is the Issuance address).
     */

    ReturnTuple rVal = null;
    StateSnapshot snapshotWrap = null;
    TxType effectiveTxID = TxType.TRANSFER_ASSET_FROM_MANY;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Verification.

        if (priority == thisTX.getPriority()) {

          // Namespace ...
          String namespace = cleanString(thisTX.getNameSpace());
          String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + "|" + classid;

          // Quick Amount check.
          if ((new Balance(thisTX.getAmount())).lessThanZero()) {
            rVal = new ReturnTuple(SuccessType.FAIL, "Tx Amount is negative.");
            return rVal;
          }

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if ((poaAddress == null) || (poaAddress.isEmpty())) {
              poaAddress = attorneyAddress;
            } else {
              if (!AddressUtil.verifyAddress(poaAddress)) {
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
              }
            }
          }

          // Address permissions.

          /* TODO : Check. No Address permissions for transfers.
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

          // Check namespace Ownership and that we are the issuer.

          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

          if (namespaceEntry == null) {
            rVal = new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` is not registered.", namespace)
            );
            return rVal;
          }

          if (!poaAddress.equals(namespaceEntry.getAddress())) {
            rVal = new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` is not owned by `{1}`.", namespace, poaAddress)
            );
            return rVal;
          }

          snapshotWrap = stateSnapshot.createSnapshot();

          // Get / Check From Address.
          MutableMerkle<AddressEntry> assetBalances = snapshotWrap.getAssetBalances();

          AddressEntry issuerAddressEntry;
          if (checkOnly) {
            issuerAddressEntry = assetBalances.find(poaAddress);
          } else {
            issuerAddressEntry = assetBalances.findAndMarkUpdated(poaAddress);
          }

          if (issuerAddressEntry == null) {
            // Damned odd that the issuer address does not exist.
            // xChain would make this possible.

            if (!checkOnly) {
              issuerAddressEntry = addDefaultAddressEntry(assetBalances, poaAddress, stateSnapshot.getVersion());
            } else {
              // CheckOnly, dummy entry.
              issuerAddressEntry = new AddressEntry(poaAddress, 0L, 0L, 0L);
            }
          }

          Balance amount = BALANCE_ZERO;
          Balance thisAmount;
          String thisAddress;
          int tempIndex = 0;

          // Verify Amount vs Address Payments.

          for (Object thisPayment : thisTX.getSubjectAddresses()) {
            thisAddress = ((Object[]) thisPayment)[0].toString();  // Just to verify...
            thisAmount = new Balance(((Object[]) thisPayment)[1]);

            if ((thisAddress == null) || (thisAddress.length() == 0)) {
              rVal = new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid payment address, index {0}.", tempIndex));
              return rVal;
            }

            if (thisAmount.lessThanZero()) {
              rVal = new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Amount transferred from `{0}` is negative : {1}", thisAddress, thisAmount));
              return rVal;
            }

            amount = amount.add(thisAmount);
            tempIndex++;
          }

          if ((amount.lessThanZero()) || (!amount.equalTo(thisTX.getAmount()))) {
            rVal = new ReturnTuple(SuccessType.FAIL, "Address receipts do not equal Tx Total amount, or negative.");
            return rVal;
          }

          if (amount.equalTo(Constants.L_ZERO)) {
            rVal = new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));
            return rVal;
          }

          // Check Asset present and transfer.

          AddressEntry subjectAddressEntry;
          Balance subjectAmount;

          for (Object thisPayment : thisTX.getSubjectAddresses()) {
            thisAddress = (String) ((Object[]) thisPayment)[0];  // Just to verify...
            thisAmount = new Balance(((Object[]) thisPayment)[1]);

            if (thisAmount.greaterThanZero()) {
              if (checkOnly) {
                subjectAddressEntry = assetBalances.find(thisAddress);
              } else {
                subjectAddressEntry = assetBalances.findAndMarkUpdated(thisAddress);
              }

              if (subjectAddressEntry == null) {
                rVal = new ReturnTuple(
                    (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                    MessageFormat.format("Address `{0}` does not exists in state.", thisAddress)
                );
                return rVal;
              }

              subjectAmount = subjectAddressEntry.getAssetBalance(fullAssetID);

              if (subjectAmount.lessThan(thisAmount)) {
                rVal = new ReturnTuple(
                    (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                    MessageFormat.format("Address `{0}` has insufficient balance.", thisAddress)
                );
                return rVal;
              }

              if (!checkOnly) {
                Balance newTotal = subjectAmount.subtract(thisAmount);
                subjectAddressEntry.setAssetBalance(fullAssetID, newTotal);
              }
            }

          }

          if (!checkOnly) {
            issuerAddressEntry.setAssetBalance(fullAssetID, issuerAddressEntry.getAssetBalance(fullAssetID).add(thisTX.getAmount()));

            // Update POA

            if ((thisItem != null) && (thisTX.isPOA())) {

              thisItem.consume(thisTX.getAmount());

              if (thisItem.consumed()) {
                tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
              }
            }

          }

        } // priority

      }

      rVal = new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));
      return rVal;

    } catch (Exception e) {

      logger.error("Error in TransferFromManyTx.updatestate()", e);
      rVal = new ReturnTuple(SuccessType.FAIL, "Error in TransferFromManyTx.updatestate.");
    } finally {

      if ((!checkOnly) && (rVal != null) && (rVal.success == SuccessType.PASS) && (snapshotWrap != null)) {
        try {
          snapshotWrap.commit();
        } catch (StateSnapshotCorruptedException e) {
          logger.error("State snapshot was corrupted {}", e.getMessage());
          rVal = new ReturnTuple(SuccessType.FAIL, "State corruption in TransferFromManyTx.updatestate.");
        }
      }
    }

    return rVal;

  }

}
