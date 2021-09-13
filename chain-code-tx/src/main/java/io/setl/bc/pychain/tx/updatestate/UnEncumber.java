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

import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.CommonPy.EncumbranceConstants.ISSUER_LOCK;
import static io.setl.common.StringUtils.cleanString;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.UnEncumberTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S1118"}) // Private Constructor.
public class UnEncumber {

  private static final Logger logger = LoggerFactory.getLogger(UnEncumber.class);


  /**
   * updatestate. Un-Encumber asset.
   *
   * @param thisTX : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime : Block time.
   * @param priority : Update priority, passed from multi-pass block update process.
   * @param checkOnly :
   * @return :
   */
  @SuppressWarnings({"squid:S3776"}) // Cognitive Complexity
  public static ReturnTuple updatestate(UnEncumberTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    // effectiveTxID is used exclusively for checking POA permissions
    final TxType effectiveTxID = (thisTX.isHoldingLockTx() ? TxType.UNLOCK_ASSET_HOLDING : TxType.UNENCUMBER_ASSET);

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }
          }

          Number unEncumberAmount = thisTX.getAmount();
          if ((new Balance(unEncumberAmount)).lessThanZero()) {
            return new ReturnTuple(SuccessType.FAIL, "Invalid amount");
          }

          // Namespace ...
          final String namespace = cleanString(thisTX.getNameSpace());
          final String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + "|" + classid;
          boolean isAssetIssuer = false;

          // Check POA Permission.
          PoaItem thisItem = null;

          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, fullAssetID, unEncumberAmount, checkOnly);
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Other parameters

          String subjectAddress = (thisTX.getSubjectaddress() == null || thisTX.getSubjectaddress().isEmpty()) ? poaAddress
              : thisTX.getSubjectaddress(); // Use subjectAddress field if not empty.

          AddressEncumbrances thisAddressEncumbrance = stateSnapshot.getEncumbrances().findAndMarkUpdated(subjectAddress);

          if (thisAddressEncumbrance == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Address `{0}` has no Encumbrances.", subjectAddress));
          }

          AssetEncumbrances thisAssetEncumbrance = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
          if (thisAssetEncumbrance == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Address `{0}` has no Encumbrances for `{1}`.", subjectAddress, fullAssetID));
          }

          String reference = thisTX.getReference();

          if (thisTX.isHoldingLockTx()) {
            // Check isIssuer

            MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
            NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

            // Namespace Exist ?
            if (namespaceEntry == null) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  MessageFormat.format("Namespace `{0}` has not been registered.", namespace));
            }

            // Is PoaAddress the namespace owner ?
            isAssetIssuer = poaAddress.equals(namespaceEntry.getAddress());

            if (!isAssetIssuer) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  MessageFormat.format("UnlockHolding. Not the Asset issuer address : {0} must be {1}",
                  poaAddress,
                  namespaceEntry.getAddress()));
            }

            // Fixed reference for Issuer Lockholding.
            reference = ISSUER_LOCK;
          }

          // Admin / Beneficiary / Expired is tested below.
          EncumbranceEntry thisEncumbrance = thisAssetEncumbrance.getAggregateByReference(reference);
          if (thisEncumbrance == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Encumbrance `{0}` does not exist.", reference));
          }

          // Permission to delete / reduce ?
          // Encumbrance may be cancelled by a beneficiary, by an administrator or by anyone if there are no remaining beneficiaries

          if (isAssetIssuer // If this is true, then 'isHoldingLockTx' and 'isAssetIssuer', so should be allowed regardless of 'administrator'
              || thisEncumbrance.isAdministratorValid(poaAddress, updateTime) // Admin Rights
              || thisEncumbrance.isBeneficiaryValid(poaAddress, updateTime)   // Beneficiary (Should this be allowed?)
              || thisEncumbrance.hasExpired(updateTime)                       // Expired
              ) {

            if (checkOnly) {
              return new ReturnTuple(SuccessType.PASS, "Check Only.");
            }

            couldCorrupt = true;

            if ((thisItem != null) && (thisTX.isPOA())) {

              thisItem.consume(unEncumberAmount);

              if (thisItem.consumed()) {
                tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
              }
            }

            if (thisEncumbrance.amount.lessThanEqualTo(unEncumberAmount)) {
              // remove
              thisAssetEncumbrance.removeEncumbrance(reference);

              if (thisAssetEncumbrance.encumbrancesIsEmpty()) {
                thisAddressEncumbrance.removeAssetEncumbrance(fullAssetID);

                if (thisAddressEncumbrance.getEncumbranceList().isEmpty()) {
                  stateSnapshot.getEncumbrances().delete(subjectAddress);
                }
              }

            } else {
              // reduce
              thisAssetEncumbrance.reduceEncumbrance(reference, unEncumberAmount);
            }

          } else {
            // No permissions.
            return new ReturnTuple(SuccessType.FAIL, "No permission for this Encumbrance.");
          }

        }

      } else {
        // Other Chain ...
        // Not supported.
      }

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in UnEncumber.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in UnEncumber.updatestate.");
    }

  }

}
