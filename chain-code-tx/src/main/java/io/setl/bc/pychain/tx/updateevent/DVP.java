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
package io.setl.bc.pychain.tx.updateevent;

import static io.setl.bc.pychain.state.entry.AddressEntry.addBalanceOnlyAddressEntry;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.DVP_RETRY_ON_ASSETS_UNAVAILABLE;
import static io.setl.common.CommonPy.VersionConstants.DVP_SET_STATUS;
import static io.setl.common.StringUtils.cleanString;

import java.math.MathContext;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAssetIssueTx;
import io.setl.bc.pychain.state.tx.PoaAssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.math.MathEval;

public class DVP {

  private static final MathContext MC_HALFUP = new MathContext(0, RoundingMode.HALF_UP);

  private static final Logger logger = LoggerFactory.getLogger(DVP.class);


  /**
   * dvp_uk_delete().
   *
   * @param stateSnapshot   :
   * @param contractAddress :
   */
  private static void dvpUkDelete(StateSnapshot stateSnapshot, String contractAddress, Set<String> userAddresses) {

    stateSnapshot.getContracts().delete(contractAddress);
    stateSnapshot.addLifeCycleEvent(ContractLifeCycle.DELETE, contractAddress, userAddresses);

  }


  /**
   * innerUpdateEvent.
   * <p>DVP Update Event.</p>
   *
   * @param stateSnapshot   :
   * @param contractEntry   :
   * @param updateTime      :
   * @param contractAddress :
   * @param eventData       :
   *
   * @return :
   */
  // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  // Methods should not be too complex
  // Control flow statements "if", "for", "while", "switch" and "try" should not be nested too deeply
  // String literals should not be duplicated
  // "switch case" clauses should not have too many lines of code
  // Exception handlers should preserve the original exceptions : we want to handle safeEvaluate errors
  @SuppressWarnings({"squid:S2159", "squid:S134", "squid:S1151", "squid:S1192", "squid:S1166", "squid:MethodCyclomaticComplexity"})
  private static ReturnTuple innerUpdateEvent(
      StateSnapshot stateSnapshot,
      ContractEntry contractEntry,
      long updateTime,
      String contractAddress,
      EventData eventData,
      boolean checkOnly
  ) {

    String contractEncumbranceName = null;
    boolean contractEncumbrance = false;

    // No Contract entry ? :

    if (contractEntry == null) {

      if ("time".equalsIgnoreCase(eventData.getEventName())) {
        // Just a 'Ghost' time update, ignore.
        return new ReturnTuple(SuccessType.PASS, "Ghost time update.");
      }

      logger.error("DVP updateEvent() contractEntry == null, eventType =  {}. Address : {}", eventData.getEventName(), contractAddress);
      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.PASS), MessageFormat
          .format("DVP updateEvent() contractEntry == null, eventType = {0}. Address : {1}", eventData.getEventName(), contractAddress));
    }

    // Get Contract data.

    DvpUkContractData contractData = (DvpUkContractData) contractEntry.getContractData();

    // Validate data

    if (contractData == null) {
      logger.error("DVP Event, ContractData missing for contract {}", contractAddress);
      return new ReturnTuple(
          (checkOnly ? SuccessType.WARNING : SuccessType.PASS),
          MessageFormat.format("ContractData missing for contract {0}", contractAddress)
      );
    }

    if (!CONTRACT_NAME_DVP_UK.equalsIgnoreCase(contractData.get__function())) {
      logger.error("DVP updateEvent() fed contract of type : {}. Address : {}", contractEntry.getContractData().getContractType(), contractAddress);
      return new ReturnTuple(SuccessType.FAIL, MessageFormat
          .format("DVP updateEvent() fed contract of type : {0}. Address : {1}", contractEntry.getContractData().getContractType(), contractAddress));
    }

    if (!contractAddress.equalsIgnoreCase(contractData.get__address())) {
      logger.error("Contract type mismatch in contract {}", contractAddress);
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Contract address mismatch in contract {0}.", contractAddress));
    }

    // Get Contract Encumbrance details, if any.

    if (contractData.getEncumbrance() != null) {
      contractEncumbranceName = contractData.getEncumbrance().encumbranceName;
      contractEncumbrance = contractData.getEncumbrance().useCreatorEncumbrance;
    }

    if ((contractEncumbranceName == null) || (contractEncumbranceName.isEmpty())) {
      contractEncumbranceName = contractAddress;
    }

    // Now, What type of event is this...

    switch (eventData.getEventName().toLowerCase()) {

      case "time":
        // Time event : Either for contract expiry, contract tidy (post expiry) or Contract execution (if previously insufficient assets).

        long referenceTime = eventData.getLongValue();

        if (contractData.get__completed() != 0) {

          // Contract is completed, just delete it ?

          if (!checkOnly) {

            if (updateTime > contractData.get__canceltime()) {
              logger.debug("Deleting completed contract {}", contractAddress);
              dvpUkDelete(stateSnapshot, contractAddress, contractData.addresses());
            } else {
              stateSnapshot.addContractEventTime(contractAddress, (contractData.get__canceltime() + 5L));
            }

          }

          return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only. Contract already completed." : "Contract already completed."));
        }

        if (referenceTime >= contractData.get__timeevent()) {
          // OK, past trigger time, what to do ?

          if (referenceTime >= contractData.getExpiry()) {
            // Expiry
            // In production, this should trigger some external notification.

            logger.info("Expiry of contract : {}", contractAddress);

            // Simple Delete

            if (!checkOnly) {
              dvpUkDelete(stateSnapshot, contractAddress, contractData.addresses());
              stateSnapshot.addLifeCycleEvent(ContractLifeCycle.EXPIRE, contractAddress, contractData.addresses());

              // Check for Contract-Specific Encumbrances against payments
              // Delete them if they exist.

              // Time event : Check for Contract-Specific Encumbrances against payments
              // Delete them if they exist.

              if (contractEncumbrance && (contractEncumbranceName.equalsIgnoreCase(contractAddress))) {
                TreeMap<String, AddressEncumbrances> addressEncumbrances = new TreeMap<>();
                AddressEncumbrances thisAddressEncumbrance;
                AssetEncumbrances assetEncumbrance;

                // Iterate Parties...
                List<DvpParty> partiesData = contractData.getParties();

                if (partiesData != null) {
                  for (DvpParty thisContractParty : partiesData) {

                    // Iterate payments
                    // Cache Encumbrance data for performance reasons.

                    for (DvpPayItem thisPayment : thisContractParty.payList) {

                      if (addressEncumbrances.containsKey(thisPayment.address)) {
                        thisAddressEncumbrance = addressEncumbrances.getOrDefault(thisPayment.address, null);
                      } else {
                        if (checkOnly) {
                          thisAddressEncumbrance = stateSnapshot.getEncumbrances().find(thisPayment.address);
                        } else {
                          thisAddressEncumbrance = stateSnapshot.getEncumbrances().findAndMarkUpdated(thisPayment.address);
                        }
                        addressEncumbrances.put(thisPayment.address, thisAddressEncumbrance);
                      }

                      if (thisAddressEncumbrance == null) {
                        continue;
                      }

                      // Get AssetEncumbrance relating to this Payment Asset.
                      assetEncumbrance = thisAddressEncumbrance.getAssetEncumbrance(thisPayment.namespace, thisPayment.classID);

                      if (assetEncumbrance == null) {
                        continue;
                      }

                      // Remove encumbrance relating to this contractName, removing the entire AssetEncumbrance entry if it is empty.
                      if (!assetEncumbrance.removeEncumbrance(contractEncumbranceName).isEmpty()) {

                        // Remove if empty,  Asset encumbrance.
                        if (assetEncumbrance.encumbrancesIsEmpty()) {
                          thisAddressEncumbrance.removeAssetEncumbrance(thisPayment.namespace, thisPayment.classID);
                        }

                        // Trim Address Encumbrance if empty
                        if (thisAddressEncumbrance.getEncumbranceList().isEmpty()) {
                          stateSnapshot.getEncumbrances().delete(thisPayment.address);
                        }
                      }

                    } // For Payment

                  } // For Party
                }

              } // if contractEncumbrance

            } // !checkOnly.

            return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only. Expired." : "Expired."));

          } else {
            // Not Expiry, Evaluate Contract
            // Use 'commit' event to perform evaluation, should perhaps be done in a separate function.

            contractData.setNextTimeEvent(referenceTime, false);

            // Set next time event
            if (!checkOnly) {
              stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());
            }

            // trigger evaluation
            return updateEvent(stateSnapshot, referenceTime, contractAddress, new EventData(contractAddress, contractData.get__function(), "commit", ""),
                checkOnly
            );

          } // else

        }

        return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only. Time event processed." : "Time event processed."));

      case "commit":

        // OK, Commit has occurred, Evaluate Contract
        // Check Contract Terms, action if fulfilled.
        // We are not checking signatures here, the assumption is that if they are there then they are good.
        //
        // Signature validation occurs in the 'updateState' function.
        // Thereafter, if a signature is present it is assumed to be good.
        // SIGNATURES ARE NOT RE-CHECKED. Can I be any clearer ? MAKE SURE THAT THEY ARE CHECKED IN UPDATESTATE !

        if (contractData.get__completed() != 0) {
          // Contract is completed, can't enter further commitments.
          return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only. Completed." : "Completed."));
        }

        Long contractStart = contractData.getStartdate();

        if (updateTime >= contractStart) {

          // *****************************
          // Check required Authorisations
          // *****************************

          List<DvpAuthorisation> authorisations = contractData.getAuthorisations();

          if (authorisations != null) {
            for (DvpAuthorisation thisAuthorisation : authorisations) {

              if (thisAuthorisation.getSignature() == null || thisAuthorisation.getSignature().isEmpty()) {
                return new ReturnTuple(
                    (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                    MessageFormat.format("Unsigned Authorisation : {0}", thisAuthorisation.authorisationID)
                );
              }

              if (thisAuthorisation.getRefused()) {
                return new ReturnTuple(
                    (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                    MessageFormat.format("Authorisation refused : {0}", thisAuthorisation.authorisationID)
                );
              }

            }
          }

          // *******************************
          // Check AddEncumbrance signatures
          // Only checks Encumbrances that are not 'lock' encumbrances.
          // Will be checked again later, but for performance, do this now.
          // *******************************

          List<DvpAddEncumbrance> addencumbrancesData = contractData.getAddencumbrances();

          if (addencumbrancesData != null) {
            // There is an 'addEncumbrance' condition :

            for (DvpAddEncumbrance thisAddEncumbrance : addencumbrancesData) {
              // For each Encumbrance to be added :

              if ((thisAddEncumbrance.beneficiaries != null)
                  && (!thisAddEncumbrance.beneficiaries.isEmpty())
                  && (thisAddEncumbrance.getSignature() == null || thisAddEncumbrance.getSignature().isEmpty())) {
                // There are beneficiaries and no Signature : Fail.

                return new ReturnTuple(
                    (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                    MessageFormat.format("Unsigned AddEncumbrance : {0}", thisAddEncumbrance.reference)
                );
              }
            }
          }

          // ***********************************
          // Check required parameter signatures
          // ***********************************

          boolean hasParameters = false;
          MathEval safeEvaluate = null;

          Map<String, DvpParameter> parameterData = contractData.getParameters();

          if (parameterData != null) {
            for (Entry<String, DvpParameter> thisEntry : parameterData.entrySet()) {
              hasParameters = true;

              if ((thisEntry.getValue().calculationOnly == 0) && (thisEntry.getValue().getSignature() == null || thisEntry.getValue()
                  .getSignature()
                  .isEmpty())) {
                return new ReturnTuple((checkOnly ? SuccessType.FAIL : SuccessType.PASS), MessageFormat.format("Unsigned Parameter : {0}", thisEntry.getKey()));
              }
            }

            // Evaluate Parameters
            safeEvaluate = new MathEval();

            if (hasParameters) {
              List<Entry<String, DvpParameter>> sortedList = new ArrayList<>(parameterData.entrySet());

              sortedList.sort((p1, p2) -> {
                if (p1.getValue().calculatedIndex != p2.getValue().calculatedIndex) {
                  return Integer.compare(p1.getValue().calculatedIndex, p2.getValue().calculatedIndex);
                }

                return p1.getKey().compareTo(p2.getKey());
              });

              for (Entry<String, DvpParameter> thisParameter : sortedList) {

                try {
                  safeEvaluate.setConstant(
                      thisParameter.getKey().toLowerCase(),
                      ((thisParameter.getValue().getValueNumber() == null)
                          ? safeEvaluate.evaluate(thisParameter.getValue().getValueString())
                          : thisParameter.getValue().getValue())
                  );
                } catch (Exception e) {
                  // Exception (Unset Constant presumably)
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                      MessageFormat.format(
                          "Contract {0}, Failed to evaluate parameter {1} : {2}",
                          contractAddress,
                          thisParameter.getKey(),
                          thisParameter.getValue().getValue()
                      )
                  );
                }
              }
            }
          }

          // ****************************************************
          // 1) Check Payments are signed, Check Funds Available.
          // ****************************************************

          // paymentsSummaryList will contain a list of payments and receipts. The List items will be copies of Contract data and thus disposable.
          ArrayList<DvpPayItem> paymentsSummaryList = new ArrayList<>();

          boolean fundsAvailable = true;
          String missingFundsDetail = "";

          HashMap<String, Balance> tempAddressHoldings = new HashMap<>();
          HashMap<String, Balance> tempUnencumberedHoldings = new HashMap<>();
          HashMap<String, Balance> tempReceipts = new HashMap<>();
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          AddressEntry thisAddressEntry;

          //
          HashMap<String, AddressEntry> addressEntryCache = new HashMap<>();

          // Simple performance cache for address AddressEncumbrances
          HashMap<String, AddressEncumbrances> addressEncumbrances = new HashMap<>();

          // Cache of aggregated Encumbrance Entries (copied). Used for performance and so that cumulative consumptions are counted correctly.
          HashMap<String, EncumbranceEntry> tempEncumbrances = new HashMap<>();

          AddressEncumbrances thisAddressEncumbrance = null;
          EncumbranceEntry thisEncumbrance = null;
          String itemKey;
          String thisEncumbranceName = null;

          List<DvpParty> partiesData = contractData.getParties();

          if (partiesData != null) {

            // This try loop exists in order to catch the `ArithmeticException` errors that may be generated by the Math.xxxExact method.
            // These Math methods must be called inside an exception handler in this code.

            for (DvpParty thisContractParty : partiesData) {
              // Party details     : [PartyID, SigAddress, PayList, ReceiveList, PublicKey, Signature]
              // Paylist details   : [Address1, NameSpace1, AssetID1, Qty1, Public Key, Signature]

              // Payment Signatures and Balances

              // Party Signature missing ?
              // Check Party signature if there is no Encumbrance in use or the Party 'MustSign'.

              if ((thisContractParty.sigAddress.isEmpty())
                  || (thisContractParty.publicKey.isEmpty())
                  || (thisContractParty.signature.isEmpty())) {
                /*
                  # OK, this party has not signed. This is only OK if :
                  #     Not 'MustSign' and either :
                  #        a) There are no payments and receipts all have addresses
                  #        b) contractEncumbrance is True and all movements have addresses

                  # Party not signed, but allow this if there are (no payments or contractEncumbrance) and all movements have an address
                  # Unless MustSign [6] is True
                 */

                if (thisContractParty.mustSign) {
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                      MessageFormat.format("Party not signed (must sign) : {0}", thisContractParty.partyIdentifier)
                  );
                }

                if ((!contractEncumbrance) && (thisContractParty.payList != null) && (!thisContractParty.payList.isEmpty())) {
                  //Has Payments and no Encumbrance specified.
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                      MessageFormat.format("Party not signed (has payments) : {0}", thisContractParty.partyIdentifier)
                  );
                }

                // At this point, either 'contractEncumbrance' is true or there are no `pay` items...

              } // if contractParty not signed.

              // OK this party is OK.
              // Check each payment
              Balance paymentQuantity;
              Balance receiptQuantity;
              String assetKey;
              Balance addressHolding;
              Balance unencumberedHolding;
              DvpPayItem summaryPayment;

              // *************************
              // Process Receipts first. This allows receipts (to tempReceipts) to be used in the payments section.
              // *************************

              if (thisContractParty.receiveList != null) {
                for (DvpReceiveItem thisReceipt : thisContractParty.receiveList) {

                  // Check lockings, Only need to check one side, in this case, Receipts.

                  // Namespace locked ?
                  if (stateSnapshot.isAssetLocked(thisReceipt.namespace)) {
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format("Namespace is locked : {0}", thisReceipt.namespace)
                    );
                  }

                  // Asset locked ?
                  if (stateSnapshot.isAssetLocked(thisReceipt.getFullAssetID())) {
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format("Asset `{0}` is locked.", thisReceipt.getFullAssetID())
                    );
                  }

                  // Validate receipt addresses ? Should have already been done if it is present at all

                  if (thisReceipt.getAmount() instanceof Number) {
                    receiptQuantity = Balance.abs(new Balance(thisReceipt.getAmount()));
                  } else {
                    try {
                      receiptQuantity = new Balance(safeEvaluate.evaluate(thisReceipt.getAmount().toString()).round(MC_HALFUP).toBigInteger().abs());
                    } catch (Exception e) {
                      return new ReturnTuple(
                          (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                          MessageFormat.format(
                              "Contract {0}, Party {1}, Asset {2}, Failed to evaluate receipt : {3}",
                              contractAddress,
                              thisContractParty.partyIdentifier,
                              thisReceipt.getFullAssetID(),
                              thisReceipt.getAmount()
                          )
                      );
                    }
                  }

                  //

                  if (receiptQuantity.lessThanZero()) {
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format(
                            "Contract {0}, Party {1}, Asset {2},  Negative receipt : {3}",
                            contractAddress,
                            thisContractParty.partyIdentifier,
                            thisReceipt.getFullAssetID(),
                            thisReceipt.getAmount()
                        )
                    );
                  }

                  if (receiptQuantity.greaterThanZero()) {

                    if (thisReceipt.address.length() == 0) {
                      return new ReturnTuple(
                          (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                          MessageFormat.format("Receipt has no Address : Party {0}, Asset {1}",
                              thisContractParty.partyIdentifier, thisReceipt.getFullAssetID()
                          )
                      );
                    }

                    assetKey = thisReceipt.address + "|" + thisReceipt.getFullAssetID();
                    tempReceipts.put(assetKey, (tempReceipts.getOrDefault(assetKey, BALANCE_ZERO).add(receiptQuantity)));

                    // paymentsSummaryList.append([receiptAddress, receiptNamespace, receiptClass, abs(receiptQuantity), True, False, ''])

                    summaryPayment = new DvpPayItem(
                        thisReceipt.address,
                        thisReceipt.namespace,
                        thisReceipt.classID,
                        receiptQuantity,
                        "null", "null", true, "", ""
                    );
                    paymentsSummaryList.add(summaryPayment);

                  }
                }
              }

              // *************************
              // Now check party payments.
              // *************************

              boolean paymentIsSigned;

              if ((fundsAvailable) && (thisContractParty.payList != null)) {

                for (DvpPayItem thisPayment : thisContractParty.payList) {

                  //

                  if (contractEncumbrance) {
                    thisEncumbranceName =
                        ((thisPayment.encumbrance != null) && (thisPayment.encumbrance.length() > 0)) ? thisPayment.encumbrance : contractEncumbranceName;
                  }

                  // Payment Address OK ?

                  if ((thisPayment.address == null) || (thisPayment.address.isEmpty())) {
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat
                            .format("Payment has no Address : Party {0}, Asset {1}", thisContractParty.partyIdentifier, thisPayment.getFullAssetID())
                    );
                  }

                  // Check for locking

                  if (stateSnapshot.isAssetLocked(thisPayment.namespace) || stateSnapshot.isAssetLocked(thisPayment.getFullAssetID())) {
                    logger.info("dvp_uk_updateevent() failed to commit. Locked asset. {} from {}", thisPayment.getFullAssetID(), thisPayment.address);
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format("Payment Asset locked : Party {0}, Asset {1}", thisContractParty.partyIdentifier, thisPayment.getFullAssetID())
                    );
                  }

                  // Resolve quantity

                  if (thisPayment.getAmount() instanceof Number) {
                    paymentQuantity = Balance.abs(new Balance(thisPayment.getAmount()));
                  } else {
                    try {
                      paymentQuantity = new Balance(safeEvaluate.evaluate(thisPayment.getAmount().toString()).round(MC_HALFUP).toBigInteger().abs());

                    } catch (Exception e) {
                      return new ReturnTuple(
                          (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                          MessageFormat.format(
                              "Contract {0}, Party {1}, Asset {2}, Failed to evaluate payment : {3}",
                              contractAddress,
                              thisContractParty.partyIdentifier,
                              thisPayment.getFullAssetID(),
                              thisPayment.getAmount()
                          )
                      );
                    }
                  }

                  if (paymentQuantity.lessThanZero()) {
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format(
                            "Contract {0}, Party {1}, Asset {2}, Negative payment : {3}",
                            contractAddress,
                            thisContractParty.partyIdentifier,
                            thisPayment.getFullAssetID(),
                            thisPayment.getAmount()
                        )
                    );
                  }

                  if (paymentQuantity.equalTo(0L)) {
                    continue;
                  }

                  // Get Address Encumbrance Entry, needed to get UnEncumbered balance and for Encumbrance checking further on.

                  if (contractEncumbrance) {

                    if ((thisEncumbranceName != null) && (!thisEncumbranceName.isEmpty())) {
                      thisAddressEncumbrance = addressEncumbrances.get(thisPayment.address);

                      if (thisAddressEncumbrance == null) {

                        if (!checkOnly) {
                          thisAddressEncumbrance = stateSnapshot.getEncumbrances().findAndMarkUpdated(thisPayment.address); // Mark Updated always ?
                        } else {
                          thisAddressEncumbrance = stateSnapshot.getEncumbrances().find(thisPayment.address);
                        }

                        addressEncumbrances.put(thisPayment.address, thisAddressEncumbrance);
                      }
                    } else {
                      thisAddressEncumbrance = null;
                    }
                  }

                  // Get holding : Full and UnEncumbered.

                  assetKey = thisPayment.address + "|" + thisPayment.getFullAssetID();

                  addressHolding = tempAddressHoldings.get(assetKey);

                  if (addressHolding == null) {
                    // Get Address holdings

                    if (checkOnly) {
                      thisAddressEntry = assetBalances.find(thisPayment.address);
                    } else {
                      thisAddressEntry = assetBalances.findAndMarkUpdated(thisPayment.address);
                      addressEntryCache.put(thisPayment.address, thisAddressEntry);
                    }

                    if (thisAddressEntry != null) {
                      addressHolding = tempReceipts.getOrDefault(assetKey, BALANCE_ZERO).add(thisAddressEntry.getAssetBalance(thisPayment.getFullAssetID()));
                    } else {
                      addressHolding = tempReceipts.getOrDefault(assetKey, BALANCE_ZERO);
                    }

                    tempAddressHoldings.put(assetKey, addressHolding);

                    tempUnencumberedHoldings.put(assetKey, addressHolding.subtract(
                        (thisAddressEncumbrance == null ? 0L
                            : thisAddressEncumbrance.getEncumbranceTotal(thisPayment.address, thisPayment.getFullAssetID(), updateTime))));
                  }

                  // paymentsSummaryList holds a set of actual balance movements, one for each payment or receipt.
                  // For convenience, these payments are represented as DvpPayItem objects which has a number of non-persisted
                  // values to facilitate this process.

                  summaryPayment = new DvpPayItem(thisPayment);
                  summaryPayment.amountNumber = paymentQuantity.multiplyBy(-1L);
                  paymentsSummaryList.add(summaryPayment);
                  summaryPayment.effectiveEncumbrance = null;

                  // summaryPayment and 'thisPayment' are the same, but, ALL CHANGES MUST BE MADE TO summaryPayment if they are to be used later on !

                  // Check sig details present
                  // Signature should have been previously checked

                  paymentIsSigned = thisPayment.isSigned();

                  if ((!contractEncumbrance) && (!paymentIsSigned)) {
                    // Not Signed and Encumbrances not enabled.
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format("Payment not signed : Party {0}, Asset {1}", thisContractParty.partyIdentifier, thisPayment.getFullAssetID())
                    );
                  }

                  // Get Encumbrance Entry for this Payment...
                  // -----------------------------------------

                  // Resolve Encumbrance Name, get from cache or add to temp cache as needed.

                  if (contractEncumbrance) {

                    itemKey = thisPayment.address + "|" + thisPayment.getFullAssetID() + "|" + thisEncumbranceName;

                    thisEncumbrance = tempEncumbrances.get(itemKey);

                    if ((thisEncumbrance == null) && (thisAddressEncumbrance != null)) {
                      // Try to get aggregated encumbrance entry (Exclude if Expired or not Beneficiary)

                      if (thisPayment.issuance) {
                        // Does not need to be 'Available' for issuance.
                        // isBeneficiaryValid checked below
                        thisEncumbrance = thisAddressEncumbrance.getAggregateByReference(thisPayment.getFullAssetID(), thisEncumbranceName);
                      } else {
                        // isBeneficiaryValid checked below
                        // Only get the amount available to this encumbrance
                        thisEncumbrance = thisAddressEncumbrance.getAggregateAvailableByReference(
                            thisPayment.getFullAssetID(),
                            thisEncumbranceName,
                            addressHolding.getValue()
                        );
                      }

                      if (thisEncumbrance != null) {

                        // 'Ignore' if expired.
                        if (thisEncumbrance.hasExpired(updateTime)) {
                          thisEncumbrance = null;
                        }
                      }

                      if (thisEncumbrance != null) {

                        // 'Ignore' if not beneficiary.
                        // TODO We ought to check that the issuing address is both a beneficiary AND an administrator. As matters stand a non-administrator
                        // beneficiary could use a contract to exercise an encumbrance when the exercise encumbrance transaction would not allow them to do
                        // so.
                        if (!thisEncumbrance.isBeneficiaryValid(contractData.getIssuingaddress(), updateTime)) {
                          thisEncumbrance = null;
                        }

                      }

                      // Save a copy to tempEncumbrances : we are going to reduce it...
                      // getAggregateByReference() returns a copy, so no need to do it again.
                      tempEncumbrances.put(itemKey, thisEncumbrance);

                    } // End of Get Encumbrance

                  } else {
                    thisEncumbrance = null;
                  } // If contractEncumbrance.

                  // If the payment is not signed and is not covered by an encumbrance, then it REALLY is not valid now...

                  if ((thisEncumbrance == null) && (!paymentIsSigned)) {

                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format("Payment is not signed and no Encumbrance (or expired or not beneficiary) : Address {0}, Asset {1}",
                            thisPayment.address, thisPayment.getFullAssetID()
                        )
                    );

                  }

                  // OK, At this point the payment is either signed, has an Encumbrance or both...

                  if (thisEncumbrance == null) {

                    summaryPayment.effectiveEncumbrance = null;

                    // OK, must be signed but no Encumbrance, check vs the available balance unless it is an issuance.

                    if (!thisPayment.issuance) {
                      // Not an issuance, check available balance
                      unencumberedHolding = tempUnencumberedHoldings.getOrDefault(assetKey, BALANCE_ZERO);

                      if (unencumberedHolding.compareTo(paymentQuantity) < 0) {
                        fundsAvailable = false;
                        missingFundsDetail = MessageFormat.format("Asset not available : Address {0}, Asset {1}",
                            thisPayment.address, thisPayment.getFullAssetID()
                        );
                      } else {
                        tempUnencumberedHoldings.put(assetKey, unencumberedHolding.subtract(paymentQuantity));
                      }
                    }

                  } else {
                    // Use Encumbrance

                    if (thisEncumbrance.amount.greaterThanZero()) {
                      summaryPayment.effectiveEncumbrance = thisEncumbranceName;
                    } else {
                      summaryPayment.effectiveEncumbrance = null;
                    }

                    // Reduce Encumbrance on cached copy.
                    thisEncumbrance.amount = thisEncumbrance.amount.subtract(paymentQuantity);
                    summaryPayment.encumbranceAmount = paymentQuantity;

                    // None left ?

                    if (thisEncumbrance.amount.lessThanZero()) {
                      // AnyFreeBalance ?

                      unencumberedHolding = tempUnencumberedHoldings.getOrDefault(assetKey, BALANCE_ZERO);

                      if ((paymentIsSigned) && (unencumberedHolding.add(thisEncumbrance.amount).compareTo(0L) >= 0L)) {
                        // For a signed payment, use available unencumbered holding if Encumbrance is exhausted.

                        unencumberedHolding = unencumberedHolding.add(thisEncumbrance.amount); // thisEncumbrance.amount is negative at this point.
                        tempUnencumberedHoldings.put(assetKey, unencumberedHolding);

                        // Should be the (-ve) amount actually taken from the Encumbrance.
                        summaryPayment.encumbranceAmount = summaryPayment.encumbranceAmount.subtract(thisEncumbrance.amount);

                        thisEncumbrance.amount = BALANCE_ZERO;

                      } else {
                        // Not Signed or no unencumbered holding.

                        if (paymentIsSigned) {
                          // The problem is not Signing (at this point) so do not fail conclusively, fail so as to trigger a timed re-evaluation.
                          // Ignore the 'isIssuance' flag as this was an issuance against an existing encumbrance.
                          // Am assuming that the encumbrance was put in place to limit the available issuance.

                          if (thisPayment.issuance) {
                            // Exit, as the issuance Encumbrance is assumed to be static and will not change (certainly do not want to poll)
                            return new ReturnTuple(
                                (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                                MessageFormat.format("Payment is a signed issuance, but Encumbrance is exhausted : Address {0}, Asset {1}, Encumbrance {2}",
                                    thisPayment.address, thisPayment.getFullAssetID(), thisEncumbranceName
                                )
                            );
                          } else {
                            missingFundsDetail = MessageFormat.format("Payment is signed but Encumbrance exhausted : Address {0}, Asset {1}, Encumbrance {2}",
                                thisPayment.address, thisPayment.getFullAssetID(), thisEncumbranceName
                            );
                            fundsAvailable = false;
                          }

                        } else {
                          return new ReturnTuple(
                              (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                              MessageFormat.format("Payment not signed and Encumbrance exhausted : Address {0}, Asset {1}, Encumbrance {2}",
                                  thisPayment.address, thisPayment.getFullAssetID(), thisEncumbranceName
                              )
                          );
                        }
                      }

                    } // Encumbrance exhausted.

                  } // Use Encumbrance

                  if (!fundsAvailable) {
                    break;
                  }

                } // For thisPayment

              } // paylist != null

              if (!fundsAvailable) {
                break;
              }

            } // For thisContractParty


            /*
            // Obsolete code. NPP Apr 2019

            // Check receipts overflow :

            String keyName;
            String[] keyParts;

            for (Entry<String, Balance> thisEntry : tempReceipts.entrySet()) {
              keyName = thisEntry.getKey();

              if ((keyName != null) && (!keyName.isEmpty())) {
                keyParts = keyName.split("\\|", 2);

                if (keyParts.length == 2) {
                  thisAddressEntry = addressEntryCache.get(keyParts[0]);

                  if (thisAddressEntry == null) {
                    thisAddressEntry = assetBalances.findAndMarkUpdated(keyParts[0]);
                  }

                }
              }
            }
            */

          } // partiesData != null

          // *******************************
          // Check AddEncumbrance signatures
          // Only checks Encumbrances that are not 'lock' encumbrances.
          // Will be checked again later, but for performance, do this now.
          //
          // Check Encumbrance Amounts, where Amount is expressed as a string.
          //
          // NOTE : tempReceipts is run down from this point.
          // *******************************

          if (addencumbrancesData != null) {
            String assetKey;
            Balance tempHolding;

            for (DvpAddEncumbrance thisAddEncumbrance : addencumbrancesData) {

              // Check / Calculate Amount,

              if ((thisAddEncumbrance.amount == null) && (thisAddEncumbrance.amountString != null) && (!thisAddEncumbrance.amountString.isEmpty())) {
                try {
                  thisAddEncumbrance.calculatedAmount = new Balance(safeEvaluate.evaluate(thisAddEncumbrance.amountString).round(MC_HALFUP).toBigInteger());

                } catch (Exception e) {
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                      MessageFormat.format("Bad AddEncumbrance amount string : {0}", thisAddEncumbrance.amountString)
                  );
                }
              }

              // Signatures / Valid Lock amount.

              if (thisAddEncumbrance.getSignature() == null || thisAddEncumbrance.getSignature().isEmpty()) {

                if ((thisAddEncumbrance.beneficiaries == null) || (thisAddEncumbrance.beneficiaries.isEmpty())) {
                  // Is the encumbered amount within a contract receipt ?

                  assetKey = thisAddEncumbrance.getPublicKey() + "|" + thisAddEncumbrance.fullAssetID;
                  tempHolding = tempReceipts.getOrDefault(assetKey, BALANCE_ZERO);

                  if (tempHolding.greaterThanEqualTo(thisAddEncumbrance.encumbranceAmount())) {
                    tempReceipts.put(assetKey, tempHolding.subtract(thisAddEncumbrance.encumbranceAmount()));
                  } else {
                    return new ReturnTuple(
                        (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                        MessageFormat.format("Unsigned AddEncumbrance (lock) : {0}", thisAddEncumbrance.reference)
                    );
                  }

                } else {
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                      MessageFormat.format("Unsigned AddEncumbrance : {0}", thisAddEncumbrance.reference)
                  );
                }
              }

            }
          }


          /* *******************************************
          // OK, is it all signed, are the funds available ?
          // Any other conditions precedent ?
          *********************************************/

          if (fundsAvailable) {

            // *******************************************
            // Final check of paymentsSummaryList
            // Check payments balance.
            // *******************************************

            HashMap<String, Balance> paymentCheck = new HashMap<>();

            paymentsSummaryList.forEach(thisPayment ->
                paymentCheck.put(
                    thisPayment.getFullAssetID(),
                    paymentCheck.getOrDefault(thisPayment.getFullAssetID(), BALANCE_ZERO).add(thisPayment.amountNumber)
                )
            );

            for (Entry<String, Balance> thisEntry : paymentCheck.entrySet()) {
              if (!thisEntry.getValue().equalTo(0L)) {
                return new ReturnTuple(
                    (checkOnly ? SuccessType.FAIL : SuccessType.PASS),
                    MessageFormat.format("Unbalanced Asset, payments not equal to receipts : {0}", thisEntry.getKey())
                );
              }
            }

            /* *******************************************
             # Signed and available
             # OK, move the assets
             ********************************************/

            if (checkOnly) {
              return new ReturnTuple(SuccessType.PASS, "Check Only.");
            }

            Balance newQuantity;
            AssetEncumbrances thisAssetEncumbrance;
            TreeMap<String, List<DvpPayItem>> paymentsByAsset = new TreeMap<>();
            TreeMap<String, List<DvpPayItem>> receiptsByAsset = new TreeMap<>();

            Collections.sort(paymentsSummaryList);

            for (DvpPayItem thisPayment : paymentsSummaryList) {

              // Save Payments and Receipts by Asset ID to enable Effective Transaction generation later.

              if (!paymentsByAsset.containsKey(thisPayment.getFullAssetID())) {
                paymentsByAsset.put(thisPayment.getFullAssetID(), new ArrayList<>());
                receiptsByAsset.put(thisPayment.getFullAssetID(), new ArrayList<>());
              }
              if (thisPayment.amountNumber.lessThanEqualZero()) {
                paymentsByAsset.get(thisPayment.getFullAssetID()).add(thisPayment);
              } else {
                receiptsByAsset.get(thisPayment.getFullAssetID()).add(thisPayment);
              }

              // Get Address entry.

              if (addressEntryCache.containsKey(thisPayment.address)) {
                thisAddressEntry = addressEntryCache.get(thisPayment.address);
              } else {
                thisAddressEntry = assetBalances.findAndMarkUpdated(thisPayment.address);
                addressEntryCache.put(thisPayment.address, thisAddressEntry);
              }

              if (thisAddressEntry == null) {
                // addBalanceOnlyAddressEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
                // version 'VERSION_SET_FULL_ADDRESS'

                thisAddressEntry = addBalanceOnlyAddressEntry(assetBalances, thisPayment.address, stateSnapshot.getVersion());
                addressEntryCache.put(thisPayment.address, thisAddressEntry);
              }

              // Make Payment / Receipt.
              newQuantity = thisAddressEntry.getAssetBalance(thisPayment.getFullAssetID()).add(thisPayment.amountNumber);
              thisAddressEntry.setAssetBalance(thisPayment.getFullAssetID(), newQuantity);

              /*
               # Exercise encumbrance
               # Encumbrances are always exercised if specified. (NPP 2017 06 14 Orange)
               # If an encumbrance exists
               */

              thisEncumbranceName = thisPayment.effectiveEncumbrance;

              if ((contractEncumbrance) && (thisEncumbranceName != null) && (thisPayment.amountNumber.lessThanZero()) && (thisEncumbranceName.length() > 0)) {

                // Get Encumbrances at this payment Address. Cached in addressEncumbrances or from State.

                thisAddressEncumbrance = addressEncumbrances.get(thisPayment.address);

                if (thisAddressEncumbrance != null) {

                  // Get AssetEncumbrance, consume
                  thisAssetEncumbrance = thisAddressEncumbrance.getAssetEncumbrance(thisPayment.getFullAssetID());

                  if (thisAssetEncumbrance != null) {
                    thisAssetEncumbrance.consumeEncumbrance(thisEncumbranceName, Balance.abs(thisPayment.encumbranceAmount));

                    if (thisAssetEncumbrance.encumbrancesIsEmpty()) {
                      thisAddressEncumbrance.removeAssetEncumbrance(thisPayment.getFullAssetID());
                    }

                    if (thisAddressEncumbrance.getEncumbranceList().isEmpty()) {
                      stateSnapshot.getEncumbrances().delete(thisPayment.address);
                    }
                  }

                } // thisAddressEncumbrance != null
              } // contractEncumbrance and Payment

            } // for thisPayment

            /*
              # After for.
              # If necessary, we want to delete any Contract specific encumbrances as they should not be used for anything else...
              # So, for each payment related encumbrance, if there is an encumbranceName == ContractAddress, delete it.
             */

            if ((contractEncumbrance) && (contractEncumbranceName.equalsIgnoreCase(contractAddress))) {

              for (DvpPayItem thisPayment : paymentsSummaryList) {

                if (thisPayment.amountNumber.greaterThanZero()) {
                  continue;
                }

                thisAddressEncumbrance = addressEncumbrances.get(thisPayment.address);

                if ((thisAddressEncumbrance != null) && (!thisAddressEncumbrance.getEncumbranceList().isEmpty())) {

                  // Get AssetEncumbrance relating to this Payment Asset.
                  thisAssetEncumbrance = thisAddressEncumbrance.getAssetEncumbrance(thisPayment.getFullAssetID());

                  if ((thisAssetEncumbrance != null) && (!thisAssetEncumbrance.encumbrancesIsEmpty())) {

                    if (!thisAssetEncumbrance.removeEncumbrance(contractEncumbranceName).isEmpty()) {

                      // Remove if empty,  Asset encumbrance.
                      if (thisAssetEncumbrance.encumbrancesIsEmpty()) {
                        thisAddressEncumbrance.removeAssetEncumbrance(thisPayment.getFullAssetID());
                      }

                      if (thisAddressEncumbrance.getEncumbranceList().isEmpty()) {
                        stateSnapshot.getEncumbrances().delete(thisPayment.address);
                      }
                    }
                  }
                }
              }
            }

            // Add Contract encumbrances
            Balance existingBalance;

            if ((addencumbrancesData != null) && (!addencumbrancesData.isEmpty())) {
              String thisAddress;

              // Aggregate Address receipts in order to determine high priority Encumbrances
              // Assets received as part of this DVP will be encumbered preferentially by any encumbrance put in place by this contract.

              HashMap<String, Balance> receipts = new HashMap<>();
              String assetKey;

              for (DvpPayItem thisPayment : paymentsSummaryList) {

                if (thisPayment.amountNumber.lessThanEqualZero()) {
                  continue;
                }

                assetKey = thisPayment.address + "|" + thisPayment.getFullAssetID();

                receipts.put(assetKey, receipts.getOrDefault(assetKey, BALANCE_ZERO).add(thisPayment.amountNumber));
              }

              // OK, iterate Encumbrances to be added...

              Balance dvpReceipt;
              Balance thisEncumbranceAmount;

              for (DvpAddEncumbrance thisAddEncumbrance : addencumbrancesData) {

                if (AddressUtil.verifyAddress(thisAddEncumbrance.getPublicKey())) {
                  thisAddress = thisAddEncumbrance.getPublicKey();
                } else {
                  // If this is an EdDSA key, we have no way of knowing if the corresponding address should be Base-58 or Base-64. All we can do is assume
                  // the default settings will produce the correct address.
                  thisAddress = AddressUtil.publicKeyToAddress(thisAddEncumbrance.getPublicKey(), AddressType.NORMAL);
                }

                thisAddressEncumbrance = stateSnapshot.getEncumbrances().findAndMarkUpdated(thisAddress);

                if (thisAddressEncumbrance == null) {
                  thisAddressEncumbrance = new AddressEncumbrances(thisAddress);
                  stateSnapshot.getEncumbrances().add(thisAddressEncumbrance);
                }

                existingBalance = BALANCE_ZERO;

                if (addressEntryCache.containsKey(thisAddress)) {
                  thisAddressEntry = addressEntryCache.get(thisAddress);
                } else {
                  thisAddressEntry = assetBalances.findAndMarkUpdated(thisAddress);
                }

                if (thisAddressEntry != null) {
                  existingBalance = thisAddressEntry.getAssetBalance(thisAddEncumbrance.fullAssetID);
                }

                assetKey = thisAddress + "|" + thisAddEncumbrance.fullAssetID;
                dvpReceipt = receipts.getOrDefault(assetKey, BALANCE_ZERO);

                // Get addEncumbrance amount :
                thisEncumbranceAmount = thisAddEncumbrance.encumbranceAmount();

                if (dvpReceipt.lessThanEqualZero()) {
                  // Normal priority
                  if (thisEncumbranceAmount.greaterThanZero()
                      &&
                      !thisAddressEncumbrance.setEncumbranceEntry(
                          thisAddEncumbrance.fullAssetID,
                          existingBalance,
                          updateTime,
                          new EncumbranceEntry(
                              thisAddEncumbrance.reference,
                              thisEncumbranceAmount,
                              thisAddEncumbrance.beneficiaries,
                              thisAddEncumbrance.administrators
                          ),
                          true,   // CUMULATIVE
                          false
                      )  // HIGH PRIORITY
                  ) {
                    String m = String.format("Unable to accumulate encumbrance \"%s\".", thisAddEncumbrance.reference);
                    stateSnapshot.setCorrupted(true, m);
                    return new ReturnTuple((checkOnly ? SuccessType.FAIL : SuccessType.PASS), m);
                  }
                } else {
                  // Allow that there might not be enough DVP asset to cover the whole encumbrance, in which case there
                  // should be a High priority part and a standard priority remainder.

                  // High Priority
                  Balance hpEncumbranceAmount = Balance.min(dvpReceipt, thisEncumbranceAmount);
                  if (hpEncumbranceAmount.greaterThanZero()
                      &&
                      !thisAddressEncumbrance.setEncumbranceEntry(
                          thisAddEncumbrance.fullAssetID,
                          existingBalance,
                          updateTime,
                          new EncumbranceEntry(
                              thisAddEncumbrance.reference,
                              hpEncumbranceAmount,
                              thisAddEncumbrance.beneficiaries,
                              thisAddEncumbrance.administrators
                          ),
                          true,   // CUMULATIVE
                          true
                      )  // HIGH PRIORITY
                  ) {
                    String m = String.format("Unable to accumulate encumbrance \"%s\".", thisAddEncumbrance.reference);
                    stateSnapshot.setCorrupted(true, m);
                    return new ReturnTuple((checkOnly ? SuccessType.FAIL : SuccessType.PASS), m);
                  }

                  dvpReceipt = dvpReceipt.subtract(thisEncumbranceAmount);

                  if (dvpReceipt.lessThanZero()) {
                    // Normal priority
                    Balance npEncumbranceAmount = dvpReceipt.multiplyBy(-1L);
                    if (npEncumbranceAmount.greaterThanZero()
                        &&
                        !thisAddressEncumbrance.setEncumbranceEntry(
                            thisAddEncumbrance.fullAssetID,
                            existingBalance,
                            updateTime,
                            new EncumbranceEntry(
                                thisAddEncumbrance.reference,
                                npEncumbranceAmount,
                                thisAddEncumbrance.beneficiaries,
                                thisAddEncumbrance.administrators
                            ),
                            true,   // CUMULATIVE
                            false
                        )  // HIGH PRIORITY
                    ) {
                      String m = String.format("Unable to accumulate encumbrance \"%s\".", thisAddEncumbrance.reference);
                      stateSnapshot.setCorrupted(true, m);
                      return new ReturnTuple((checkOnly ? SuccessType.FAIL : SuccessType.PASS), m);
                    }
                  }

                  receipts.put(assetKey, dvpReceipt);
                }
              }
            }

            //
            contractData.set__completed(true);

            // Set or Clear Time Events.
            stateSnapshot.removeContractEventTime(contractAddress, contractData.get__timeevent());

            contractData.setNextTimeEvent(updateTime, false);

            stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());

            /*
            # Calculate Effective Transactions
            # NOTE : MAY ASSUME PAYMENT SUMMARY LIST IS TRASHABLE!
             */

            if (!paymentsByAsset.isEmpty()) {
              int nonce = 0;
              DvpPayItem thisPayment = null;
              DvpPayItem thisReceipt = null;
              AbstractTx thisTX;
              List<DvpPayItem> thisPayments;
              List<DvpPayItem> thisReceipts;

              for (Entry<String, List<DvpPayItem>> entry : paymentsByAsset.entrySet()) {
                String fullAssetID = entry.getKey();
                thisPayments = entry.getValue();
                thisReceipts = receiptsByAsset.get(fullAssetID);

                // Check for Easy

                if ((thisPayments.size() == 1) && (thisReceipts.size() == 1)) {
                  thisPayment = thisPayments.get(0);
                  thisReceipt = thisReceipts.get(0);

                  if (thisPayment.issuance) {
                    thisTX = new PoaAssetIssueTx(
                        stateSnapshot.getChainId(),
                        0,
                        "",
                        nonce++,
                        true,
                        "",
                        contractAddress,
                        thisPayment.address,
                        "",
                        cleanString(thisPayment.namespace),
                        cleanString(thisPayment.classID),
                        thisReceipt.address,
                        Balance.abs(thisPayment.amountNumber),
                        "",
                        stateSnapshot.getHeight(),
                        "",
                        updateTime,
                        contractAddress,
                        thisPayment.metadata
                    );
                  } else {
                    thisTX = new PoaAssetTransferXChainTx(
                        stateSnapshot.getChainId(),
                        0,
                        "",
                        nonce++,
                        true,
                        "",
                        contractAddress,
                        thisPayment.address,
                        "",
                        cleanString(thisPayment.namespace),
                        cleanString(thisPayment.classID),
                        stateSnapshot.getChainId(),
                        thisReceipt.address,
                        Balance.abs(thisPayment.amountNumber),
                        "",
                        contractAddress,
                        thisPayment.metadata,
                        stateSnapshot.getHeight(),
                        "",
                        updateTime
                    );
                  }

                  thisTX.setHash(Hash.computeHash(thisTX));
                  stateSnapshot.addEffectiveTX(thisTX);

                } else {

                  int payIndex = 0;
                  Balance payCumul = BALANCE_ZERO;
                  int recIndex = 0;
                  Balance recCumul = BALANCE_ZERO;
                  Balance thisQty;
                  boolean loop = true;

                  while (loop) {
                    if (payCumul.lessThanEqualZero()) {
                      if (payIndex < thisPayments.size()) {
                        thisPayment = thisPayments.get(payIndex);
                        payCumul = Balance.abs(thisPayment.amountNumber);
                      } else {
                        loop = false;
                      }
                      payIndex += 1;
                    }

                    if (recCumul.lessThanEqualZero()) {
                      if (recIndex < thisReceipts.size()) {
                        thisReceipt = thisReceipts.get(recIndex);
                        recCumul = thisReceipt.amountNumber;
                      } else {
                        loop = false;
                      }

                      recIndex += 1;
                    }

                    if ((payCumul.greaterThanZero()) && (recCumul.greaterThanZero())) {
                      thisQty = Balance.min(payCumul, recCumul);

                      // Check if the payment is issuance

                      if (thisPayment.issuance) {
                        thisTX = new PoaAssetIssueTx(
                            stateSnapshot.getChainId(),
                            0,
                            "",
                            nonce++,
                            true,
                            "",
                            contractAddress,
                            thisPayment.address,
                            "",
                            cleanString(thisPayment.namespace),
                            cleanString(thisPayment.classID),
                            thisReceipt.address,
                            thisQty,
                            "",
                            stateSnapshot.getHeight(),
                            "",
                            updateTime,
                            contractAddress,
                            thisPayment.metadata
                        );

                      } else {
                        thisTX = new PoaAssetTransferXChainTx(
                            stateSnapshot.getChainId(),
                            0,
                            "",
                            nonce++,
                            true,
                            "",
                            contractAddress,
                            thisPayment.address,
                            "",
                            cleanString(thisPayment.namespace),
                            cleanString(thisPayment.classID),
                            stateSnapshot.getChainId(),
                            thisReceipt.address,
                            thisQty,
                            "",
                            contractAddress,
                            thisPayment.metadata,
                            stateSnapshot.getHeight(),
                            "",
                            updateTime
                        );
                      }

                      thisTX.setHash(Hash.computeHash(thisTX));
                      stateSnapshot.addEffectiveTX(thisTX);

                      payCumul = payCumul.subtract(thisQty);
                      recCumul = recCumul.subtract(thisQty);
                    }
                  } // While loop

                } // else easy

              } // for fullAssetID

            } // if paymentsByAsset

            stateSnapshot.addLifeCycleEvent(ContractLifeCycle.COMPLETE, contractAddress, contractData.addresses());
            return new ReturnTuple(SuccessType.PASS, "Contract completed");

          } else {

            // Signed but not fundsAvailable. Re-Evaluate later...
            // Set or Clear Time Events.

            stateSnapshot.removeContractEventTime(contractAddress, contractData.get__timeevent());

            contractData.setNextTimeEvent(updateTime + DVP_RETRY_ON_ASSETS_UNAVAILABLE, true);

            stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());

            return new ReturnTuple((checkOnly ? SuccessType.FAIL : SuccessType.PASS), MessageFormat.format("Insufficient Asset : {0}", missingFundsDetail));
          }

        } else {
          // Before contract start : Do nothing.
          return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only. Contract not yet started." : "Contract not yet started."));
        }

      default:

        break;

    }

    return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

  }


  /**
   * updateEvent.
   * <p>DVP Update Event.</p>
   *
   * @param stateSnapshot   :
   * @param updateTime      :
   * @param contractAddress :
   * @param eventData       :
   *
   * @return :
   */
  // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  // Methods should not be too complex
  // Control flow statements "if", "for", "while", "switch" and "try" should not be nested too deeply
  // String literals should not be duplicated
  // "switch case" clauses should not have too many lines of code
  // Exception handlers should preserve the original exceptions : we want to handle safeEvaluate errors
  @SuppressWarnings({"squid:S2159", "squid:S134", "squid:S1151", "squid:S1192", "squid:S1166", "squid:MethodCyclomaticComplexity"})
  public static ReturnTuple updateEvent(StateSnapshot stateSnapshot, long updateTime, String contractAddress, EventData eventData, boolean checkOnly) {

    /*
     Wrapper for 'innerUpdateEvent()', the purposed of which is to trap the Status returned from the Event function and to save it to the Contract Data.
     */

    ContractEntry contractEntry = stateSnapshot.getContracts().findAndMarkUpdated(contractAddress);

    ReturnTuple rVal = innerUpdateEvent(stateSnapshot, contractEntry, updateTime, contractAddress, eventData, checkOnly);

    // No Contract entry ? :

    if (contractEntry == null) {
      return rVal;
    }

    if ((!checkOnly) && (rVal != null) && (rVal.status != null)
        && stateSnapshot.getVersion() >= DVP_SET_STATUS) {
      DvpUkContractData contractData = (DvpUkContractData) contractEntry.getContractData();
      contractData.set__status(rVal.status);
    }

    return rVal;
  }


  private DVP() {
    // Sonarqube wants a private constructor.
  }

}
