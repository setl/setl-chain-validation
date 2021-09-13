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
package io.setl.bc.pychain.tx.updatestate.contracts;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.AddressUtil.verifyAddress;
import static io.setl.common.AddressUtil.verifyPublicKey;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CONTRACTS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.MAX_DVP_CONTRACTDURATION;
import static io.setl.common.CommonPy.ContractConstants.MAX_DVP_PAYMENT_METADATA_LENGTH;
import static io.setl.common.CommonPy.ContractConstants.MAX_DVP_STARTDELAY;
import static io.setl.common.CommonPy.ContractConstants.MAX_ENCUMBRANCE_NAME_LENGTH;
import static io.setl.common.CommonPy.EncumbranceConstants.HOLDER_LOCK;
import static io.setl.common.CommonPy.EncumbranceConstants.ISSUER_LOCK;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.NewContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.UpdateEvent;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.math.MathEval;

public class DVP {

  private static final Logger logger = LoggerFactory.getLogger(DVP.class);


  /**
   * Check all party identifiers are unique and assign an ID to any party with a null or empty ID.
   *
   * @param parties the parties
   *
   * @return true if a duplicate ID is found.
   */
  private static boolean checkPartyIdentifiers(List<DvpParty> parties) {
    HashSet<String> usedIds = new HashSet<>();
    for (DvpParty dvpParty : parties) {
      String id = dvpParty.partyIdentifier;
      if (!Strings.isNullOrEmpty(id) && !usedIds.add(id)) {
        // ID is reused
        return true;
      }
    }

    // assign any missing ID
    int idSrc = 1;
    for (DvpParty dvpParty : parties) {
      String id = dvpParty.partyIdentifier;
      if (Strings.isNullOrEmpty(id)) {
        do {
          id = Integer.toString(idSrc);
          idSrc++;
        }
        while (!usedIds.add(id));
        dvpParty.partyIdentifier = id;
      }
    }

    return false;
  }


  /**
   * updatestate.
   * <p>DvP Contract</p>
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   *
   * @return :
   */
  // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  // The cyclomatic complexity of methods should not exceed a defined threshold.
  // Control flow statements "if", "for", "while", "switch" and "try" should not be nested too deeply
  @SuppressWarnings({"squid:S2159", "squid:S134", "squid:MethodCyclomaticComplexity"})
  @SuppressFBWarnings("UC_USELESS_OBJECT") // assetBalances is not useless.
  public static ReturnTuple updatestate(NewContractInterface thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    boolean okToProcess = true;
    boolean encumbranceSpecified = false;
    final String thisFunctionName = CONTRACT_NAME_DVP_UK;
    final TxType effectiveTxID = TxType.NEW_CONTRACT;
    final TxType encumberTxID = TxType.ENCUMBER_ASSET;
    long addressPermissions = AP_CONTRACTS;
    String statusMessage = (checkOnly ? "Check Only." : "");
    DvpUkContractData contractData = null;
    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Verification : Apply only to the 'Native' chain...

        if ((thisTX.getTimestamp() > 0) && (Math.abs(thisTX.getTimestamp() - updateTime) > stateSnapshot.getStateConfig().getMaxTxAge())) {
          return new ReturnTuple(SuccessType.FAIL, "Tx Timestamp invalid.");
        }

        if (priority == thisTX.getPriority()) {

          String contractAddress = thisTX.getContractAddress();
          // String contractAddress = AddressUtil.publicKeyToAddress(thisTX.getEffectiveAddress(), AddressType.CONTRACT, thisTX.getNonce());

          // Check Contract data. Must not already exist.

          MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();
          if (contractsList.find(contractAddress) != null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Contract already exists..");
          }

          // Check From Address
          String attorneyAddress = thisTX.getAuthoringAddress();
          if (!AddressUtil.verify(attorneyAddress, thisTX.getAuthoringPublicKey(), AddressType.NORMAL)) {
            return new ReturnTuple(SuccessType.FAIL, "`From` Address and Public key do not match.");
          }

          String poaAddress = thisTX.getEffectiveAddress();

          PoaItem thisItem = null;
          ReturnTuple poaPerms;

          if (thisTX.isPOA()) {
            if (!verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }

            // Address permissions.

            if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
              // Address permissions.
              ReturnTuple aPerm = checkPoaAddressPermissions(stateSnapshot, attorneyAddress, poaAddress, thisTX.getTxType(), effectiveTxID,
                  addressPermissions
              );
              if ((!checkOnly) && (aPerm.success != SuccessType.PASS)) {
                return aPerm;
              }
            }

            // Check POA Permission.
            poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, "", 1L, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          } else {
            // NON POA Address Permissions
            if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
              long keyPermission = stateSnapshot.getAddressPermissions(attorneyAddress);
              boolean hasPermission = stateSnapshot.canUseTx(attorneyAddress, thisTX.getTxType());
              if ((!hasPermission) && ((keyPermission & addressPermissions) == 0)) {
                return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
              }

            }
          }

          // Get Contract Data
          contractData = new DvpUkContractData((DvpUkContractData) thisTX.getContractData());

          // issuingaddress, set it in the map explicitly.
          contractData.setIssuingaddress(poaAddress);

          // Validate Encumbrance name
          DvpEncumbrance thisEncumbrance = contractData.getEncumbrance();

          if (thisEncumbrance != null) {
            if (thisEncumbrance.encumbranceName.length() > MAX_ENCUMBRANCE_NAME_LENGTH) {
              return new ReturnTuple(SuccessType.FAIL, "Invalid DVP Encumbrance name.");
            }

            if (thisEncumbrance.useCreatorEncumbrance) {
              encumbranceSpecified = true;
            }
          }

          MessageSignerVerifier verifier = MessageVerifierFactory.get();

          // Check Parameters Data
          // ---------------------

          DvpParameter thisParameter;
          String tempString;
          String tempSig;
          String sigMessage;
          Set<String> tempParametersList = null;
          MathEval safeEvaluate = null;

          if (contractData.getParameters() != null) {
            tempParametersList = contractData.getParameters().keySet();

            for (Map.Entry<String, DvpParameter> entry : contractData.getParameters().entrySet()) {

              thisParameter = entry.getValue();

              if (thisParameter.calculationOnly == 0) {
                // not Calculation only...

                tempString = thisParameter.getAddress();
                if ((tempString == null) || (tempString.length() == 0)) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Bad Authorisation structure, public key / address, not a string, or zero length");
                }

                if ((!verifyPublicKey(tempString)) && (!verifyAddress(tempString))) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Authorisation, public key / Address not valid");
                }

                tempSig = thisParameter.getSignature();
                if ((tempSig != null) && (tempSig.length() > 0)) {

                  sigMessage = thisParameter.stringToHashToSign(entry.getKey(), contractAddress);

                  if (thisTX.isPOA() && poaAddress.equalsIgnoreCase(tempString)) {
                    // could be signed by the Authoring address on behalf of the POA Address.

                    if (!verifier.verifySignature(sigMessage, thisTX.getAuthoringPublicKey(), tempSig)) {
                      return new ReturnTuple(SuccessType.FAIL, "DVP : failed parameter signature");
                    }

                    // Save POA Public key.
                    thisParameter.setPoaPublicKey(thisTX.getAuthoringPublicKey());
                  } else {
                    if (!verifier.verifySignature(sigMessage, thisParameter.getAddress(), tempSig)) {
                      return new ReturnTuple(SuccessType.FAIL, "DVP : failed parameter sig : (sig,msg,pub) ");
                    }
                  }
                } else {
                  // Unsigned Parameters.
                  okToProcess = false;
                  statusMessage = "Unsigned Parameters";
                }
              } else {
                if ((thisParameter.getValue() == null)) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Calculation only fields must have some kind of value.");
                }
              }

            }
          }

          // Check AddEncumbrances Data
          // --------------------------

          if (contractData.getAddencumbrances() != null) {
            Set<String> addEncumbranceVariables;

            // If an encumbrance is being entered from a recipient (as a lock) and encumbrances were in use to start with, do not require
            // the encumbrance to be signed.

            HashSet<String> receipientSummary = null;

            if (encumbranceSpecified) {
              receipientSummary = new HashSet<>();

              List<DvpParty> parties = contractData.getParties();

              if ((parties != null) && (!parties.isEmpty())) {

                for (DvpParty thisParty : parties) {

                  if (thisParty.receiveList != null) {
                    for (DvpReceiveItem thisReceiveItem : thisParty.receiveList) {
                      if (thisReceiveItem != null) {
                        receipientSummary.add(thisReceiveItem.address + "|" + thisReceiveItem.getFullAssetID());
                      }
                    }
                  }
                }
              }
            }

            for (DvpAddEncumbrance thisAddEncumbrance : contractData.getAddencumbrances()) {

              // Verify Encumbrance name
              if (ISSUER_LOCK.equalsIgnoreCase(thisAddEncumbrance.reference) || HOLDER_LOCK.equalsIgnoreCase(thisAddEncumbrance.reference)) {
                return new ReturnTuple(
                    SuccessType.FAIL,
                    "DVP : Bad addEncumbrance reference. May not use reserved name `" + thisAddEncumbrance.reference + "`"
                );
              }

              // Verify Public Key

              tempString = thisAddEncumbrance.getPublicKey();
              if (Strings.isNullOrEmpty(tempString)) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure, public key / address, not a string, or zero length");
              }

              if ((!verifyPublicKey(tempString)) && (!verifyAddress(tempString))) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure, missing or bad public key / address");
              }

              // Verify Asset ID
              if (Strings.isNullOrEmpty(thisAddEncumbrance.fullAssetID)) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure, AssetID, not a string or zero length");
              }

              // Verify Reference
              if (Strings.isNullOrEmpty(thisAddEncumbrance.reference)) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure, Reference must not be null or zero length");
              }

              if (thisAddEncumbrance.amount != null) {
                if (thisAddEncumbrance.amount.lessThanEqualZero()) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure, Encumbrance Amount <= Zero. Reference "
                      + thisAddEncumbrance.reference);
                }
              } else if ((thisAddEncumbrance.amountString == null) || (thisAddEncumbrance.amountString.isEmpty())) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure, Encumbrance Amount is not specified. Reference "
                    + thisAddEncumbrance.reference);
              } else {
                // Check tokens..

                if (safeEvaluate == null) {
                  safeEvaluate = new MathEval();
                }

                if (tempParametersList == null) {
                  tempParametersList = new HashSet<>();
                }

                addEncumbranceVariables = safeEvaluate.getVariablesWithin(thisAddEncumbrance.amountString);
                if (!tempParametersList.containsAll(addEncumbranceVariables)) {
                  addEncumbranceVariables.removeAll(tempParametersList);

                  return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure. Reference "
                      + thisAddEncumbrance.reference
                      + " : Value string contains parameter values that do not exist : "
                      + addEncumbranceVariables);
                }
              }

              // beneficiaries and administrators : Validity of data types is enforced by the EncumbranceDetail class.

              tempSig = thisAddEncumbrance.getSignature();

              // AddEncumbrance : If there is a signature, then it is always checked.
              // If there is no signature, then it fails, UNLESS, it is deemed to be a lock transaction of the type
              // required in the Exchange / Registrar model. i.e. No beneficiary and relating to a DVP receipt.

              if (!Strings.isNullOrEmpty(tempSig)) {
                // Signature exists.

                sigMessage = computeHash(thisAddEncumbrance.objectToHashToSign(contractAddress));

                if (thisTX.isPOA() && poaAddress.equalsIgnoreCase(thisAddEncumbrance.getPublicKey())) {
                  // could be signed by the Authoring address on behalf of the POA Address.

                  if (!verifier.verifySignature(sigMessage, thisTX.getAuthoringPublicKey(), tempSig)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : failed addEncumbrance (poa) signature");
                  }

                  // Save POA Public key.
                  thisAddEncumbrance.setPoaPublicKey(thisTX.getAuthoringPublicKey());

                  // Check POA Permission.
                  ReturnTuple poaEncPerms =
                      checkPoaTransactionPermissions(
                          stateSnapshot,
                          updateTime,
                          thisTX.getPoaReference(),
                          attorneyAddress,
                          poaAddress,
                          encumberTxID,
                          thisAddEncumbrance.fullAssetID,
                          thisAddEncumbrance.amount,
                          checkOnly
                      );
                  if (poaEncPerms.success == SuccessType.FAIL) {
                    return poaEncPerms;
                  }

                } else {
                  // Not POA, check normally
                  if (!verifier.verifySignature(sigMessage, thisAddEncumbrance.getPublicKey(), tempSig)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : failed addEncumbrance signature");
                  }
                }
              } else {
                // No Signature, so...

                // receipientSummary is a list of Address|Assets that are receiving assets as part of this contract.
                // If the encumbrance being added relates to one of these recipients and is a 'lock' style Encumbrance, then it is OK.

                if ((receipientSummary != null) && (!receipientSummary.isEmpty())) {
                  // OK Encumbrances are in play (otherwise receipientSummary is null).

                  String assetID = thisAddEncumbrance.getPublicKey() + "|" + thisAddEncumbrance.fullAssetID;

                  if ((!receipientSummary.contains(assetID)) || ((thisAddEncumbrance.beneficiaries != null) && (!thisAddEncumbrance.beneficiaries.isEmpty()))) {
                    // No receipient for this Address/Asset or it is not a 'lock' style encumbrance.

                    okToProcess = false;
                    statusMessage = "Unsigned AddEncumbrances";
                  }
                } else {
                  // Unsigned AddEncumbrances.
                  okToProcess = false;
                  statusMessage = "Unsigned AddEncumbrances";
                }
              }

            } // For addEncumbrances.
          }

          // Check Authorisations Data
          // -------------------------

          if (contractData.getAuthorisations() != null) {

            String thisAuthorisationAddress;

            for (DvpAuthorisation thisAuthorisation : contractData.getAuthorisations()) {

              thisAuthorisationAddress = thisAuthorisation.getAddress();

              if (Strings.isNullOrEmpty(thisAuthorisationAddress)) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Bad Authorisation structure, public key / address, not a string, or zero length");
              }

              if ((!verifyPublicKey(thisAuthorisationAddress)) && (!verifyAddress(thisAuthorisationAddress))) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Authorisation, Bad public key / Address");
              }

              tempSig = thisAuthorisation.getSignature();

              if (!Strings.isNullOrEmpty(tempSig)) {
                sigMessage = thisAuthorisation.stringToHashToSign(contractAddress);

                if (thisTX.isPOA() && poaAddress.equalsIgnoreCase(thisAuthorisationAddress)) {
                  // could be signed by the Authoring address on behalf of the POA Address.

                  if (!verifier.verifySignature(sigMessage, thisTX.getAuthoringPublicKey(), tempSig)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : failed authorisation signature");
                  }

                  // Save POA Public key.
                  thisAuthorisation.setPoaPublicKey(thisTX.getAuthoringPublicKey());
                } else {
                  if (!verifier.verifySignature(sigMessage, thisAuthorisation.getAddress(), tempSig)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : failed authorisation sig : (contractAddress + authorisationID + `_` + Refused) ");
                  }
                }
              } else {
                // Unsigned Authorisation.
                okToProcess = false;
                statusMessage = "Unsigned Authorisation";
              }

            }
          }


          /*
           Check Party Data
           ----------------

           #Parties(See dvp_uk_create()also.)
           # [Count,
           #    [
           #PartyIdentifier,
           #SigAddr,
           #     [[PayAddress, Namespace, AssetID, Qty, Public Key, Signature], ...],
           #     [[ReceiveAddress, Namespace, AssetID, Qty], ...],
           #Public Key,
           #Signature,
           #MustSign
           #    ], ...
           # ]
          */

          List<DvpParty> parties = contractData.getParties();
          HashMap<String, Balance> assetBalances = new HashMap<>();
          String fullAssetID;

          if ((parties != null) && (!parties.isEmpty())) {
            // Each party must have a unique identifier
            if (checkPartyIdentifiers(parties)) {
              return new ReturnTuple(SuccessType.FAIL, "DVP : Every party must have a unique identifier");
            }
            //
            MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
            NamespaceEntry namespaceEntry;
            Set<String> paymentVariables;

            // Check each party entry.
            for (DvpParty thisParty : parties) {
              if (!Strings.isNullOrEmpty(thisParty.sigAddress)) {

                if (!verifyAddress(thisParty.sigAddress)) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : bad party address");
                }

                if ((!Strings.isNullOrEmpty(thisParty.publicKey)) && (!Strings.isNullOrEmpty(thisParty.signature))) {

                  if (!verifier.verifySignature(contractAddress, thisParty.publicKey, thisParty.signature)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : failed party sig : (sig,msg,pub)");
                  }

                  if (!AddressUtil.verify(thisParty.sigAddress, thisParty.publicKey, AddressType.NORMAL)) {
                    // Not signed by the Party Address, would be OK if the party is the poaAddress and the signature is for the attorney address.

                    if (!thisTX.isPOA()) {
                      return new ReturnTuple(SuccessType.FAIL, "DVP : Signature Public key does not match the party address.");
                    } else if ((!thisParty.sigAddress.equals(poaAddress)) || !AddressUtil.verify(attorneyAddress, thisParty.publicKey, AddressType.NORMAL)) {
                      return new ReturnTuple(SuccessType.FAIL, "DVP : PoA Invalid party credentials (Party Address or PoA Public Key).");
                    }
                  }

                } else if ((thisParty.mustSign) || ((!encumbranceSpecified) && (thisParty.payList.size() > 0))) {
                  // Party not signed, has payments but no encumbrance OR but MustSign.
                  okToProcess = false;
                  statusMessage = "Party not signed and no encumbrance or `mustsign`.";
                }
              }

              for (DvpPayItem thisPayItem : thisParty.payList) {

                if (thisPayItem.amountNumber != null) {

                  if (thisPayItem.amountNumber.lessThanZero()) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : Party payment, negative quantity");
                  }

                  fullAssetID = thisPayItem.namespace + "|" + thisPayItem.classID;
                  assetBalances.put(fullAssetID, assetBalances.getOrDefault(fullAssetID, BALANCE_ZERO).add(thisPayItem.amountNumber));
                } else {
                  // Validate String

                  if (safeEvaluate == null) {
                    safeEvaluate = new MathEval();
                  }

                  if (tempParametersList == null) {
                    tempParametersList = new HashSet<>();
                  }

                  paymentVariables = safeEvaluate.getVariablesWithin(thisPayItem.amountString);
                  if (!tempParametersList.containsAll(paymentVariables)) {
                    paymentVariables.removeAll(tempParametersList);

                    return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure. Party "
                        + thisParty.partyIdentifier
                        + " : Value string contains parameter values that do not exist : "
                        + paymentVariables);
                  }

                }

                if ((thisPayItem.metadata != null) && (thisPayItem.metadata.length() > MAX_DVP_PAYMENT_METADATA_LENGTH)) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Party metadata length > limit");
                }

                // If given a public key, but no Address, generate the address.
                if (((thisPayItem.address == null) || (thisPayItem.address.length() == 0))
                    && ((thisPayItem.publicKey != null) && (thisPayItem.publicKey.length() > 0))) {
                  thisPayItem.address = AddressUtil.publicKeyToAddress(thisPayItem.publicKey, AddressType.NORMAL);
                }

                if ((thisPayItem.address != null) && (thisPayItem.address.length() > 0)) {
                  // [[PayAddress, Namespace, AssetID, Qty, Public Key, Signature], ...],

                  // Valid address...

                  if (!verifyAddress(thisPayItem.address)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : Bad payment address");
                  }

                  // Check Address and public key match.

                  if (!Strings.isNullOrEmpty(thisPayItem.publicKey)) {

                    if (!AddressUtil.verify(thisPayItem.address, thisPayItem.publicKey, AddressType.NORMAL)) {
                      // Pay address does not match the signature address...

                      if (!thisTX.isPOA()) {
                        return new ReturnTuple(SuccessType.FAIL, "DVP : Pay Item,  Address and public key do not match.");
                      } else if ((thisPayItem.address.equals(poaAddress)) && AddressUtil.verify(attorneyAddress, thisPayItem.publicKey, AddressType.NORMAL)) {
                        // Pay Address is poaAddress and Signature Address is attorney address...

                        // Since the amount could easily be a string, do not check the pay amount in the POA, just the asset ID.
                        poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                            effectiveTxID, thisPayItem.getFullAssetID(), 0L, checkOnly
                        );

                        if (poaPerms.success == SuccessType.FAIL) {
                          return poaPerms;
                        }
                      } else {
                        return new ReturnTuple(SuccessType.FAIL, "DVP : Pay Item address does not match Attorney or POA Address.");
                      }
                    }
                  }

                  // Correct Issuance Address ?

                  if ((!checkOnly) && (thisPayItem.issuance)) {
                    namespaceEntry = namespaceTree.find(thisPayItem.namespace);

                    if ((namespaceEntry == null) || (!namespaceEntry.getAddress().equals(thisPayItem.address))
                        || (!namespaceEntry.containsAsset(thisPayItem.classID))) {
                      return new ReturnTuple(
                          (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                          "DVP : PayItem, Specified address does not control the Issuance Namespace."
                      );
                    }
                  } // issuance

                  // Check signature, if supplied.

                  if ((!Strings.isNullOrEmpty(thisPayItem.publicKey)) && (!Strings.isNullOrEmpty(thisPayItem.signature))) {

                    sigMessage = computeHash(thisPayItem.objectToHashToSign(contractAddress));

                    if (!verifier.verifySignature(sigMessage, thisPayItem.publicKey, thisPayItem.signature)) {
                      return new ReturnTuple(SuccessType.FAIL, "DVP : Party payment, invalid payment signature");
                    }

                  } else if (!encumbranceSpecified) {
                    if (okToProcess) {
                      okToProcess = false;
                      statusMessage = MessageFormat.format("Payment not signed and No Encumbrance : Party {0}, Asset {1}",
                          thisParty.partyIdentifier, thisPayItem.getFullAssetID()
                      );
                    }
                  }
                } else if (thisPayItem.issuance) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Can't use `Issuance` flag without specifying a payment address.");

                } else if ((!encumbranceSpecified) && (thisPayItem.amountNumber != null) && (thisPayItem.amountNumber.greaterThanZero())) {
                  // No Encumbrance, Not Signed, and Amount > 0
                  okToProcess = false;
                  statusMessage = "Payment : No Encumbrance, Not Signed, and Amount > 0";
                }

              }

              for (DvpReceiveItem thisReceiveItem : thisParty.receiveList) {

                // Check lockings, Only need to check one side, in this case, Receipts.

                // Namespace locked ?
                if (stateSnapshot.isAssetLocked(thisReceiveItem.namespace)) {
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                      MessageFormat.format("Namespace is locked : {0}", thisReceiveItem.namespace)
                  );
                }

                // Asset locked ?
                if (stateSnapshot.isAssetLocked(thisReceiveItem.getFullAssetID())) {
                  return new ReturnTuple(
                      (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                      MessageFormat.format("Asset `{0}` is locked.", thisReceiveItem.getFullAssetID())
                  );
                }

                // Amount...
                if (thisReceiveItem.amountNumber != null) {

                  if (thisReceiveItem.amountNumber.lessThanZero()) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : Party receipt negative quantity");
                  }

                  fullAssetID = thisReceiveItem.namespace + "|" + thisReceiveItem.classID;
                  assetBalances.put(fullAssetID, assetBalances.getOrDefault(fullAssetID, BALANCE_ZERO).subtract(thisReceiveItem.amountNumber));
                } else {
                  // Validate String

                  if (safeEvaluate == null) {
                    safeEvaluate = new MathEval();
                  }

                  if (tempParametersList == null) {
                    tempParametersList = new HashSet<>();
                  }

                  paymentVariables = safeEvaluate.getVariablesWithin(thisReceiveItem.amountString);

                  if (!tempParametersList.containsAll(paymentVariables)) {
                    paymentVariables.removeAll(tempParametersList);

                    return new ReturnTuple(SuccessType.FAIL, "DVP : Bad addEncumbrance structure. Party "
                        + thisParty.partyIdentifier
                        + " : Value string contains parameter values that do not exist : "
                        + paymentVariables);
                  }
                }

                if ((thisReceiveItem.address != null) && (thisReceiveItem.address.length() > 0)) {

                  if (!verifyAddress(thisReceiveItem.address)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : Bad receive address - " + thisReceiveItem.address);
                  }
                }
              }
            } // for Parties

            // All payments should net to Zero. (No miners fees here!)
            //Unless there are contract Parameters, in which case it is impossible to tell now.

            if ((contractData.getParameters() == null) || contractData.getParameters().isEmpty()) {
              final boolean[] allOK = {true};

              assetBalances.forEach((key, value) -> {
                if (!value.equalTo(0L)) {
                  allOK[0] = false;
                }
              });

              if (!allOK[0]) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Payments and Receipts do not balance");
              }
            }

          } else {
            return new ReturnTuple(SuccessType.FAIL, "DVP : No Parties");
          }

          // #Contract must have an expiry date.
          if ((contractData.getExpiry() == null) || (contractData.getExpiry() < updateTime)) {
            return new ReturnTuple(SuccessType.FAIL, "DVP : No or Bad Expiry Date");
          }

          // #Contract start date must be within (When ?).
          Long startDate = contractData.getStartdate();
          if ((startDate == null) || ((startDate - updateTime) > MAX_DVP_STARTDELAY)) {
            return new ReturnTuple(SuccessType.FAIL, "DVP : Start Date not soon enough.");
          }

          if (startDate > updateTime) {
            okToProcess = false;
          }

          if ((contractData.getExpiry() - Math.max(updateTime, contractData.getStartdate())) > MAX_DVP_CONTRACTDURATION) {
            return new ReturnTuple(SuccessType.FAIL, "DVP : Contract duration too long.");
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Finishing values.

          contractData.set__completed(false);
          contractData.set__address(contractAddress);
          contractData.set__function(thisFunctionName);
          contractData.setNextTimeEvent(updateTime, false);

          if (thisTX.isPOA()) {
            contractData.set__status(String.format("POA New Contract, on behalf of %s, entered by Attorney %s, TX %s.",
                poaAddress, attorneyAddress, thisTX.getHash()
            ));

            if (thisItem != null) {
              thisItem.consume(1L);

              if (thisItem.consumed()) {
                tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
              }
            }
          }

          stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());

          ContractEntry newContract = new ContractEntry(contractAddress, contractData.encode());
          contractsList.add(newContract);

          if (okToProcess) {
            stateSnapshot.addContractEvent(contractAddress, thisFunctionName, "commit", null);
          }
          stateSnapshot.addLifeCycleEvent(ContractLifeCycle.NEW, contractAddress, contractData.addresses());
        }

      }

      return new ReturnTuple(SuccessType.PASS, statusMessage);

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in NewContract(DVP).updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in NewContract(DVP).updatestate.");
    } finally {
      if (logger.isDebugEnabled() && contractData != null) {
        logger.debug("Contract data for Tx {} was:\n{}", thisTX.getHash(), UpdateEvent.prettyContractData(contractData));
      }
    }

  }


  private DVP() {
    // Sonarqube wants a private constructor.
  }

}
