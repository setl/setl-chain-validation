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
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.EncumbranceConstants.HOLDER_LOCK;
import static io.setl.common.CommonPy.EncumbranceConstants.ISSUER_LOCK;
import static io.setl.common.StringUtils.cleanString;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;


public class Encumber {

  private static final Logger logger = LoggerFactory.getLogger(Encumber.class);


  /**
   * Check if all the details are valid. At least one detail is required.
   *
   * @param detailArray the details
   *
   * @return true if all are valid
   */
  public static ReturnTuple checkEncumbranceDetailIsValid(EncumbranceDetail[] detailArray) {

    if ((detailArray == null) || (detailArray.length == 0)) {
      return new ReturnTuple(SuccessType.FAIL, "None specified.");
    }

    for (EncumbranceDetail thisDetail : detailArray) {
      if (thisDetail == null) {
        return new ReturnTuple(SuccessType.FAIL, "Null entry in list.");
      }

      if (!AddressUtil.verifyAddress(thisDetail.address)) {
        return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid address : `{0}`", thisDetail.address));
      }

      if (thisDetail.startTime < 0) {
        return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid start time : `{0}`", thisDetail.startTime));
      }

      if (thisDetail.endTime < 0) {
        return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid end time : `{0}`", thisDetail.endTime));
      }
    }

    return new ReturnTuple(SuccessType.PASS, "");
  }


  /**
   * updatestate.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(EncumberTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    /*
    Encumbrances are stored in a separate Merkle tree in state, indexed by the Address that granted the encumbrance.
    The merkle data item is an AddressEncumbrances object, the principal property of which is the encumbranceList, a map of
    type <String, AssetEncumbrances> that stores Encumbrance items against each asset. The key <String> specifies an asset
    identifier in the usual Namespace|Class format, value is an AssetEncumbrances object described below.

    The AssetEncumbrances object holds a list of EncumbranceEntry Objects each of which will have a reference
    name, an encumbrance amount and a list of beneficiaries and administrators to this encumbrance entry.
    The beneficiaries and administrators are each a list of EncumbranceDetail Objects. Each detail object
    specifies an address and a start and end time (UTC Unix epoch seconds). The time range defines the period
    during which the beneficiary may benefit from the encumbrance or the administrator may exercise or delete.
    A beneficiary may not exercise an encumbrance unless they are an administrator also.

    There exists the concept of an Encumbrance priority. Priority is important when Encumbrances exist for more asset
    than is owned, there needs to be a way to determine who gets the asset and who does not. In practice it is important the all
    encumbrances do not rank equally otherwise an asset owner could just encumber to another Address and move all their asset making encumbrances meaningless.

    At present, Encumbrances are prioritised in the order in which they are
    added, there are internal fields that would enable specific priorities to be specified, but this functionality is neither
    developed or exposed. At present the priority is re-calculated each time an EncumbranceEntry is added or deleted with
    EncumbranceEntries being held in the AssetEncumbrances List in priority order for simplicity.

    The only exception is, optionally, when an Encumbrance is created as a result of the execution of a contract, if an
    asset is sent to an address with an accompanying encumbrance it was considered that that encumbrance should take priority,
    thus, this encumbrance may be specified as high priority and will be appended to the front of the AssetEncumbrances List.

    Encumbrance Entries must be maintained using the methods on the AssetEncumbrances class :
        addEncumbrance      : Add an additional Encumbrance with the option to replace or extend an existing reference.
        consumeEncumbrance  : Reduce an encumbrance balance consuming from the front.
                              That is, if only part of an encumbrance covered by an asset balance then the part that is covered will be reduced.
                              The idea is if an encumbrance of 100 is only covered by 40 of asset (leaving 60 uncovered) and 20 is 'exercised' (consumed) the
                              resulting situation is 20 covered and 60 uncovered.
        reduceEncumbrance   : Reduce an encumbrance balance consuming from the back.
                              That is, if only part of an encumbrance covered by an asset balance then the part that is not covered will be reduced.
                              The idea is if an encumbrance of 100 is only covered by 40 of asset (leaving 60 uncovered) and 20 is un-Encumbered (reduced) the
                              resulting situation is 40 covered and 40 uncovered.
        For an encumbrance fully covered by an asset holding, there is no functional difference between `consume` and `reduce`.

    The AssetEncumbrances class also has methods for checking encumbrance validity and coverage (given an asset balance).
    An expired Encumbrance may be deleted by anyone.
     */

    boolean couldCorrupt = false;

    // effectiveTxID is used exclusively for checking POA permissions
    final TxType effectiveTxID = (thisTX.isHoldingLockTx() ? TxType.LOCK_ASSET_HOLDING : TxType.ENCUMBER_ASSET);

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          String attorneyAddress = thisTX.getFromAddress();

          String poaAddress = thisTX.getPoaAddress();
          if (thisTX.isPOA() && !AddressUtil.verifyAddress(poaAddress)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
          }

          Number amount = thisTX.getAmount();

          if ((new Balance(amount)).lessThanZero()) {
            return new ReturnTuple(SuccessType.FAIL, "Negative amount");
          }

          // Namespace ...
          String namespace = cleanString(thisTX.getNameSpace());

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace is locked : {0}", namespace)
            );
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

          // Is PoaAddress the namespace owner ?
          boolean isAssetIssuer = poaAddress.equals(namespaceEntry.getAddress());

          String subjectAddress = poaAddress; // Future feature set will be to allow the use of the subjectAddress field.

          if (isAssetIssuer) {
            subjectAddress = thisTX.getSubjectaddress();

            if ((subjectAddress == null) || (subjectAddress.isEmpty())) {
              subjectAddress = poaAddress;
            } else {
              if (!AddressUtil.verifyAddress(subjectAddress)) {
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid SubjectAddress address {0}", subjectAddress));
              }
            }

            if (!poaAddress.equals(subjectAddress) && !AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid Subject address {0}", subjectAddress));
            }
          }

          String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + "|" + classid;

          if (classid.length() == 0) {
            return new ReturnTuple(SuccessType.FAIL, "Class name not given");
          }

          // Asset locked ?
          if ((!isAssetIssuer) && (stateSnapshot.isAssetLocked(fullAssetID))) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Asset `{0}` is locked.", fullAssetID));
          }

          // Class Details, Exist ?
          if (!namespaceEntry.containsAsset(classid)) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Class `{0}` is not registered in namespace `{1}`.", classid, namespace)
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

          // Other parameters
          Map<String, Object> txDict = thisTX.getEncumbrancesAsMap();
          Object tempObject;

          tempObject = txDict.get("administrators");
          if (tempObject == null) {
            tempObject = new EncumbranceDetail[0];
          }
          EncumbranceDetail[] administrators = (EncumbranceDetail[]) tempObject;

          tempObject = txDict.get("beneficiaries");
          if (tempObject == null) {
            tempObject = new EncumbranceDetail[0];
          }
          EncumbranceDetail[] beneficiaries = (EncumbranceDetail[]) tempObject;

          // Updated to allow empty beneficiaries for Encumbrance entries (Effectively a flexible holding lock).

          if (thisTX.isHoldingLockTx()) {
            if (beneficiaries.length > 0) {
              return new ReturnTuple(SuccessType.FAIL, "Beneficiaries may not be specified for a Holding Lock Tx.");
            }

            // Administrators is not optional for Holding lock

            administrators = new EncumbranceDetail[1];
            administrators[0] = new EncumbranceDetail(poaAddress, 0L, Long.MAX_VALUE);
          } else {
            ReturnTuple isOK = checkEncumbranceDetailIsValid(administrators);

            if (isOK.success != SuccessType.PASS) {
              return new ReturnTuple(SuccessType.FAIL, "Administrators : " + isOK.status);
            }

            // If beneficiaries exist, use 'checkEncumbranceDetailIsValid' to check that the addresses are valid.

            if (beneficiaries.length > 0) {
              isOK = checkEncumbranceDetailIsValid(beneficiaries);

              if (isOK.success != SuccessType.PASS) {
                return new ReturnTuple(SuccessType.FAIL, "Beneficiaries : " + isOK.status);
              }
            }
          }

          String reference;

          // by default, we do not over-write existing references, so the default is to be cumulative.
          boolean isCumulative = (Boolean) txDict.getOrDefault("iscumulative", true);
          boolean highPriority = false;

          if (thisTX.isHoldingLockTx()) {

            if (isAssetIssuer) {
              reference = ISSUER_LOCK;
            } else {
              return new ReturnTuple(
                  (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  "Encumber : Holding lock may only be entered by the Asset Issuer `" + namespaceEntry.getAddress() + "`"
              );
            }

            // Locks are always cumulative
            isCumulative = true;

            highPriority = true;

          } else {

            reference = (String) txDict.getOrDefault("reference", null);
            if ((reference == null) || (reference.length() == 0)) {
              reference = thisTX.getHash();
            }

            // Verify Encumbrance name is not reserved.

            if (ISSUER_LOCK.equalsIgnoreCase(reference) || HOLDER_LOCK.equalsIgnoreCase(reference)) {
              return new ReturnTuple(SuccessType.FAIL, "Encumber : Bad Encumbrance reference. May not use reserved name `" + reference + "`");
            }

          }

          // Verify new entry is acceptable

          EncumbranceEntry newEntry = new EncumbranceEntry(reference, amount, Arrays.asList(beneficiaries), Arrays.asList(administrators));

          AddressEncumbrances thisAddressEncumbrance = stateSnapshot.getEncumbrances().find(subjectAddress);

          if (thisAddressEncumbrance != null) {
            EncumbranceEntry oldEntry = thisAddressEncumbrance.getAnyEncumbranceByReference(fullAssetID, reference);
            if (oldEntry != null) {
              if (isCumulative) {
                // Administrators and beneficiaries must match.
                if (!oldEntry.canAccumulate(newEntry)) {
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                      "Encumber : Administrators and beneficiaries for a cumulative encumbrance must match existing administrators and beneficiaries."
                  );
                }
              } else {
                // Reference must not already exist.
                return new ReturnTuple(
                    (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                    "Encumber : Reference `" + reference + "` for non-cumulative encumbrance is already in use."
                );
              }
            }
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          couldCorrupt = true;

          thisAddressEncumbrance = stateSnapshot.getEncumbrances().findAndMarkUpdated(subjectAddress);

          if (thisAddressEncumbrance == null) {
            thisAddressEncumbrance = new AddressEncumbrances(subjectAddress);
            stateSnapshot.getEncumbrances().add(thisAddressEncumbrance);
          }

          if ((thisItem != null) && (thisTX.isPOA())) {

            thisItem.consume(amount);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
            }
          }

          // Get Existing Balance. This is so that Encumbrance priorities may be updated appropriately.

          Balance existingBalance = BALANCE_ZERO;
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          AddressEntry thisAddressEntry = assetBalances.findAndMarkUpdated(subjectAddress);

          if (thisAddressEntry != null) {
            existingBalance = thisAddressEntry.getAssetBalance(fullAssetID);
          }

          // Add new Encumbrance entry

          if (!thisAddressEncumbrance.setEncumbranceEntry(fullAssetID, existingBalance, updateTime, newEntry, isCumulative, highPriority)) {
            if (thisTX.isHoldingLockTx()) {
              return new ReturnTuple(SuccessType.FAIL, "Failed to set Holding Lock Encumbrance Entry");
            } else {
              return new ReturnTuple(SuccessType.FAIL, "Failed to set Encumbrance Entry");
            }
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

      logger.error("Error in Encumber.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in Encumber.updatestate.");
    }

  }

}
