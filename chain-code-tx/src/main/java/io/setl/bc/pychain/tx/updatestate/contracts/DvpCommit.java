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
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_COMMITS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.tx.CommitContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitAuthorise;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitCancel;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitReceive;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitment;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.UpdateEvent;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;

public class DvpCommit {

  private static final Logger logger = LoggerFactory.getLogger(DvpCommit.class);


  /**
   * Find the party that is making a commitment, if any. All parties have unique identifiers, but multiple parties can have the same signing address. If they
   * do share a signing address, a single commitment can sign all of them, so it is possible to commit more than one party at a time.
   *
   * @param allParties  all the parties in the contract data
   * @param commitParty the party being committed
   *
   * @return the matched parties (possibly empty)
   */
  private static List<DvpParty> matchParties(List<DvpParty> allParties, DvpCommitParty commitParty, boolean isPOA, String poaAddress) {
    if (commitParty == null) {
      // Not a party commitment at all
      return Collections.emptyList();
    }

    List<DvpParty> matches = new ArrayList<>();
    for (DvpParty thisParty : allParties) {
      boolean partyIDsMatch = thisParty.partyIdentifier.equals(commitParty.partyIdentifier);
      boolean sigAddressMatch = AddressUtil.verify(thisParty.sigAddress, commitParty.publicKey, AddressType.NORMAL);

      if ((!sigAddressMatch) && isPOA) {
        // IF POA, allow to match on POA Address, provided the signature is valid (as it must be at this point).
        sigAddressMatch = thisParty.sigAddress.equals(poaAddress);
      }

      boolean matchOnPartyID = (partyIDsMatch && (sigAddressMatch || (thisParty.sigAddress.length() == 0)));

      boolean matchOnSigAddress = (sigAddressMatch
          && (partyIDsMatch || (thisParty.partyIdentifier.length() == 0) || (commitParty.partyIdentifier.length() == 0)));

      if (matchOnPartyID || matchOnSigAddress) {
        matches.add(thisParty);
      }
    }

    return matches;
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
  // The cyclomatic complexity of methods should not exceed a defined threshold.
  // Loops should not contain more than a single "break" or "continue" statement
  // Control flow statements "if", "for", "while", "switch" and "try" should not be nested too deeply
  @SuppressWarnings({"squid:S134", "squid:S135", "squid:MethodCyclomaticComplexity"})
  public static ReturnTuple updatestate(CommitContractInterface thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    boolean contractHasChanged = false;
    String sigMessage;
    final String thisFunctionName = CONTRACT_NAME_DVP_UK_COMMIT;
    final TxType effectiveTxID = TxType.COMMIT_TO_CONTRACT;
    final long addressPermissions = AP_COMMITS;
    DvpUkContractData contractData = null;
    String contractAddress;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Verification : Apply only to the 'Native' chain...

        if (priority == thisTX.getPriority()) {

          // Check From Address
          String attorneyAddress = thisTX.getAuthoringAddress();
          if (!AddressUtil.verify(attorneyAddress, thisTX.getAuthoringPublicKey(), AddressType.NORMAL)) {
            return new ReturnTuple(SuccessType.FAIL, "`From` Address and Public key do not match.");
          }

          String poaAddress = thisTX.getEffectiveAddress();

          // One-off POA and Address permission Checks...

          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
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

          //

          List<String> contractAddresses = thisTX.getContractAddress();
          boolean isSingleContractAddress = (contractAddresses.size() == 1);
          boolean firstPass = true;

          HashSet<String> processedContracts = new HashSet<>(contractAddresses.size());

          //

          for (String thisContractAddress : contractAddresses) {

            contractAddress = thisContractAddress;

            // Check for duplicated contractAddresses.

            if ((!firstPass) && (processedContracts.contains(contractAddress))) {
              continue;
            } else if (!isSingleContractAddress) {
              processedContracts.add(contractAddress);

              // Code is configured this way so that 'continue's do not affect 'firstPass'.
              if (firstPass && (processedContracts.size() > 1)) {
                firstPass = false;
              }
            }

            // Check Contract data. Must already exist.
            contractData = null;

            MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();
            ContractEntry thisContractEntry = contractsList.findAndMarkUpdated(contractAddress);
            if (thisContractEntry == null) {
              if (isSingleContractAddress) {
                return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Contract Address must exist");
              } else {
                logger.debug("Commit : Contract data for contract {} does not exist.", contractAddress);
                continue;
              }
            }

            PoaItem thisItem = null;
            ReturnTuple poaPerms;

            if (thisTX.isPOA()) {

              // Check POA Permission.
              poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                  effectiveTxID, contractAddress, 1L, checkOnly
              );
              if (poaPerms.success == SuccessType.FAIL) {
                return poaPerms;
              }

              thisItem = (PoaItem) poaPerms.returnObject;

            }

            // Get Contract Data
            contractData = (DvpUkContractData) thisContractEntry.getContractData();

            // Basic validation.

            if (contractData == null) {
              logger.error("ContractData missing for contract {}", contractAddress);
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "DVP : ContractData missing for " + contractAddress);
            }

            if (!CONTRACT_NAME_DVP_UK.equalsIgnoreCase(contractData.get__function())) {
              // Wrong Contract type.
              return new ReturnTuple(SuccessType.FAIL, "Contract type does not match this commit");
            }

            if (logger.isDebugEnabled()) {
              logger.debug("Contract data for contract {} pre-commit is\n{}", contractAddress, UpdateEvent.prettyContractData(contractData));
            }

            if (contractData.get__completed() != 0) {
              if (isSingleContractAddress) {
                return new ReturnTuple(SuccessType.PASS, "DVP : Contract is completed");
              } else {
                logger.debug("Commit : Contract {} is completed.", contractAddress);
                continue;
              }
            }

            // Get TxContract Data
            DvpUKCommitData txData = (DvpUKCommitData) thisTX.getCommitmentData();

            MessageSignerVerifier verifier = MessageVerifierFactory.get();

            // Cancel Data
            // -----------

            if (isSingleContractAddress && (txData.getCancel() != null)) {
              DvpCommitCancel cancel = txData.getCancel();

              if ((cancel.signature != null) && (cancel.signature.length() > 0) && (cancel.publicKey != null) && (cancel.publicKey.length() > 0)) {

                // Cancel comes from Authoring address  OR from the POA Address.

                if (contractData.getIssuingaddress().equals(poaAddress)
                    || AddressUtil.verify(contractData.getIssuingaddress(), cancel.publicKey, AddressType.NORMAL)) {
                  // Signature Verifies ? :
                  sigMessage = cancel.stringToHashToSign(contractAddress);

                  if (!verifier.verifySignature(sigMessage, cancel.publicKey, cancel.signature)) {
                    return new ReturnTuple(SuccessType.FAIL, "DVP : Cancel Signature not valid ");
                  }

                  // If only checking, not updating, this is the time to exit.
                  if (checkOnly) {
                    return new ReturnTuple(SuccessType.PASS, "Check Only.");
                  }

                  // OK, Delete.
                  contractData.set__completed(true);
                  contractData.set__status("Cancelled by TX " + thisTX.getHash());
                  contractData.set__canceltime(updateTime);

                  // Set or Clear Time Events.
                  stateSnapshot.removeContractEventTime(contractAddress, contractData.get__timeevent());

                  contractData.setNextTimeEvent(updateTime, false);

                  stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());
                  stateSnapshot.addLifeCycleEvent(ContractLifeCycle.CANCEL, contractAddress, contractData.addresses());

                  return new ReturnTuple(SuccessType.PASS, "Contract Cancelled");
                } else {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Cancel instruction is not from the contract author.");
                }

              }
            } // cancel

            // Check Signatures

            /*
            Re Party commitments :
            If the Commitment contains party details, check party signature and resolve 'matchedParty'. This allows the 'Party' sigs
            to be updated later on.
            Next, check any Payment or Receipt commitments, if given.
            Payment and Receipt commitments will only be checked if the Party signatures are provided.
            */

            DvpCommitParty commitSigData = txData.getParty();
            List<DvpParty> matchedParties = matchParties(contractData.getParties(), commitSigData, thisTX.isPOA(), poaAddress);
            String commitSigAddress = "";

            // Parameter signtures.
            // --------------------

            if (txData.getParameters() != null) {
              // Just checks the signature at this point

              for (DvpCommitParameter thisParameter : txData.getParameters()) {

                sigMessage = thisParameter.stringToHashToSign(contractAddress);

                if (!verifier.verifySignature(sigMessage, thisParameter.getPublicKey(), thisParameter.signature)) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Invalid Parameter signature");
                }
              }
            } else {
              // Parameters is null, should only have one contract address...

              if (!isSingleContractAddress) {
                return new ReturnTuple(
                    SuccessType.FAIL,
                    "DVP Commit : A commit to multiple addresses must be for setting parameters. Bad TX " + thisTX.getHash()
                );
              }

            } // getParameters

            // Payment Commitments
            // -------------------

            if (isSingleContractAddress && (commitSigData != null)) {

              // Is Signature valid ?
              if (!verifier.verifySignature(contractAddress, commitSigData.publicKey, commitSigData.signature)) {
                return new ReturnTuple(SuccessType.FAIL, "DVP : Commit Signature not valid ");
              }

              commitSigAddress = AddressUtil.publicKeyToAddress(commitSigData.publicKey, AddressType.NORMAL);

              // CommitmentTx has Pay or Receive commitments...

              if (((txData.getCommitment() != null) && (!txData.getCommitment().isEmpty()))
                  || ((txData.getReceive() != null) && (!txData.getReceive().isEmpty()))) {

                for (DvpParty thisParty : matchedParties) {
                  // Check Commitments, if given.

                  if ((txData.getCommitment() != null) && (!thisParty.payList.isEmpty())) {

                    for (DvpCommitment thisPaymentCommitment : txData.getCommitment()) {

                      if ((thisPaymentCommitment.index >= 0) && (thisPaymentCommitment.index < thisParty.payList.size())) {

                        String payAddress = thisPaymentCommitment.getPayAddress();

                        DvpPayItem thisPayment = thisParty.payList.get(thisPaymentCommitment.index.intValue());

                        // Payment Address given or not ...
                        if (Strings.isNullOrEmpty(thisPayment.address)) {
                          if (thisTX.isPOA()) {
                            // Can't have empty Payment address for a POA (the author ends up paying!).
                            return new ReturnTuple(SuccessType.FAIL, "DVP : Can not POA Commit to a payment with no payment address.");
                          }
                        } else {

                          // If the Commitment address does not match the Payment address...
                          if (!thisPayment.address.equals(payAddress)) {

                            if (thisTX.isPOA()) {
                              // If the Authoring Address did not sign or the Payment Address is not the POA Address, then exit.
                              if ((!attorneyAddress.equals(payAddress)) || !AddressUtil.verify(poaAddress, thisPayment.publicKey, AddressType.NORMAL)) {
                                return new ReturnTuple(SuccessType.FAIL, "DVP : Payment commitment address does not match payment value. ");
                              }
                            } else {
                              // Not POA, No excuses...
                              return new ReturnTuple(SuccessType.FAIL, "DVP : Payment commitment address does not match payment value. ");
                            }
                          }
                        }

                        sigMessage = computeHash(thisPayment.objectToHashToSign(contractAddress));

                        if (!verifier.verifySignature(sigMessage, thisPaymentCommitment.getPublicKey(), thisPaymentCommitment.signature)) {
                          return new ReturnTuple(SuccessType.FAIL, "DVP : Payment Signature not valid");
                        }
                      } else {
                        return new ReturnTuple(SuccessType.FAIL, "DVP : No matching payment.");
                      }
                    } // for getCommitment
                  }

                  if ((txData.getReceive() != null) && (!thisParty.receiveList.isEmpty())) {

                    List<DvpReceiveItem> partyReceiveList = thisParty.receiveList;

                    for (DvpCommitReceive thisReceiveDetail : txData.getReceive()) {

                      if ((thisReceiveDetail.index >= 0) && (thisReceiveDetail.index < partyReceiveList.size())) {

                        if (!Strings.isNullOrEmpty(thisReceiveDetail.address)) {

                          // ! Valid address.
                          if (!AddressUtil.verifyAddress(thisReceiveDetail.address)) {
                            return new ReturnTuple(SuccessType.FAIL, "DVP : Invalid receipt address.");
                          }

                          if (thisTX.isPOA() && (!thisReceiveDetail.address.equalsIgnoreCase(poaAddress))) {
                            return new ReturnTuple(SuccessType.FAIL, "DVP : POA can not provide payment address except the poa Address.");
                          }
                        }
                      } else {
                        return new ReturnTuple(
                            SuccessType.FAIL,
                            String.format("DVP : No matching receipt for commit item # %s", thisReceiveDetail.index.toString())
                        );
                      }
                    } // for thisReceiveDetail
                  }
                }
              }
            } // commitSigData

            // Authorise, check signature : May be multi-Committed to :
            // --------------------------------------------------------

            if (txData.getAuthorise() != null) {
              // Just checks the signature at this point

              for (DvpCommitAuthorise thisAuthorise : txData.getAuthorise()) {

                // Verify Signature

                sigMessage = thisAuthorise.stringToHashToSign(contractAddress);

                if (!verifier.verifySignature(sigMessage, thisAuthorise.getPublicKey(), thisAuthorise.signature)) {
                  return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("DVP : Invalid signature on authorisation : {0}", thisAuthorise.authId));
                }
              }
            } // getAuthorise

            if (isSingleContractAddress && (txData.getEncumbrances() != null)) {
              // Just checks the signature at this point

              for (DvpCommitEncumbrance thisEncumbrance : txData.getEncumbrances()) {

                sigMessage = computeHash(thisEncumbrance.objectToHashToSign(contractAddress));

                if (!verifier.verifySignature(sigMessage, thisEncumbrance.getPublicKey(), thisEncumbrance.signature)) {
                  return new ReturnTuple(SuccessType.FAIL, "DVP : Invalid Encumbrance signature");
                }
              }
            } // getEncumbrances

            // *********************************************
            // OK, Verifications are done, do updates...
            // *********************************************

            couldCorrupt = true;

            // If only checking, not updating, this is the time to exit.
            if (checkOnly) {
              return new ReturnTuple(SuccessType.PASS, "Check Only.");
            }

            if (txData.getAuthorise() != null) {
              // Apply Authorisation signatures
              // Check Address matches, previous checks were only for signatures.
              List<DvpAuthorisation> underlyingAuthorisations = contractData.getAuthorisations();
              String[] undAddresses;

              // For each Commit Authorisation...
              for (DvpCommitAuthorise thisCommitAuthorise : txData.getAuthorise()) {

                String thisCommitAuthoriseAddress = thisCommitAuthorise.getAddress();

                // Loop through required authorisations...
                for (DvpAuthorisation thisContractAuthorisation : underlyingAuthorisations) {

                  // If AuthIDs match
                  if (thisCommitAuthorise.authId.equalsIgnoreCase(thisContractAuthorisation.authorisationID)) {

                    // Check Address (or Addresses) match...

                    if (thisContractAuthorisation.getAddress() != null) {
                      undAddresses = new String[]{thisContractAuthorisation.getAddress()};
                    } else {
                      undAddresses = thisContractAuthorisation.getAddresses();
                    }

                    if (undAddresses != null) {
                      for (String thisUnderlyingAddress : undAddresses) {

                        if (
                            ((!thisTX.isPOA())
                                && ((thisUnderlyingAddress.equalsIgnoreCase(thisCommitAuthorise.getPublicKey()))
                                || (thisUnderlyingAddress.equalsIgnoreCase(thisCommitAuthoriseAddress))))

                                || (thisTX.isPOA()
                                && (attorneyAddress.equalsIgnoreCase(thisCommitAuthoriseAddress)) && thisUnderlyingAddress.equalsIgnoreCase(poaAddress))) {

                          // Check Authorisation Types are compatible ?

                          if (thisContractAuthorisation.isContractSpecific() && (!thisCommitAuthorise.isContractSpecific())) {
                            // Can not commit to a specific authorisation from a non-specific commitment
                            logger.info("Commit : Authorisation is not contract-specific to contract {}, authId {}, Address {}", contractAddress,
                                thisContractAuthorisation.authorisationID, thisCommitAuthoriseAddress
                            );

                            continue;
                          }

                          contractHasChanged = true;

                          if (thisTX.isPOA()) {
                            thisContractAuthorisation.setPoaPublicKey(thisCommitAuthorise.getPublicKey());
                            thisContractAuthorisation.setAddress(poaAddress);
                          } else {
                            thisContractAuthorisation.setAddress(thisCommitAuthorise.getPublicKey());
                          }

                          thisContractAuthorisation.setSignature(thisCommitAuthorise.signature);
                          thisContractAuthorisation.setRefused(thisCommitAuthorise.refused);

                          if (!Strings.isNullOrEmpty(thisCommitAuthorise.metadata)) {
                            thisContractAuthorisation.setMetadata(thisCommitAuthorise.metadata);
                          }

                          break;
                        }
                      }
                    }
                  }
                }
              }
            } // if txData.getAuthorise

            if (txData.getParameters() != null) {

              Map<String, DvpParameter> underlyingParameters = contractData.getParameters();
              List<DvpCommitParameter> parametersData = txData.getParameters();

              if (!underlyingParameters.isEmpty()) {
                for (DvpCommitParameter thisCommitParameter : parametersData) {

                  DvpParameter thisUnderlying = underlyingParameters.get(thisCommitParameter.parameterName);

                  if ((thisUnderlying == null) || (thisUnderlying.calculationOnly != 0) || (thisUnderlying.getAddress() == null) || (
                      thisUnderlying.getAddress().length() == 0)) {
                    // Calculation only
                    continue;
                  }

                  if (
                      ((!thisTX.isPOA())
                          && (thisUnderlying.getAddress().equalsIgnoreCase(thisCommitParameter.getPublicKey()))
                          || (thisUnderlying.getAddress().equalsIgnoreCase(thisCommitParameter.getAddress())))
                          || (thisTX.isPOA()
                          && attorneyAddress.equalsIgnoreCase(thisCommitParameter.getAddress())
                          && thisUnderlying.getAddress().equalsIgnoreCase(poaAddress)
                      )
                  ) {

                    contractHasChanged = true;

                    if (thisTX.isPOA()) {
                      thisUnderlying.setPoaPublicKey(thisCommitParameter.getPublicKey());
                    } else {
                      thisUnderlying.setAddress(thisCommitParameter.getPublicKey());
                    }

                    if (thisCommitParameter.getValueNumber() != null) {
                      thisUnderlying.setValue(thisCommitParameter.getValueNumber());
                    } else if (thisCommitParameter.getValueString() != null) {
                      thisUnderlying.setValue(thisCommitParameter.getValueString());
                    }
                    thisUnderlying.setSignature(thisCommitParameter.signature);
                  }
                }
              }
            } // txData.getParameters

            if (isSingleContractAddress && (txData.getEncumbrances() != null)) {

              List<DvpAddEncumbrance> underlyingAddEncumbrance = contractData.getAddencumbrances();
              List<DvpCommitEncumbrance> encumbranceData = txData.getEncumbrances();

              if (!underlyingAddEncumbrance.isEmpty()) {

                for (DvpCommitEncumbrance thisEncumbrance : encumbranceData) {

                  String thisAddress = thisEncumbrance.getAddress();

                  for (DvpAddEncumbrance thisUnderlyingEnc : underlyingAddEncumbrance) {

                    // Encumbrance is OK if :
                    // AssetID, Reference and Amount match and ...
                    if ((thisUnderlyingEnc.fullAssetID.equalsIgnoreCase(thisEncumbrance.assetID))
                        && (thisUnderlyingEnc.reference.equalsIgnoreCase(thisEncumbrance.reference))
                        && (
                        (thisUnderlyingEnc.amount != null && thisUnderlyingEnc.amount.equalTo(thisEncumbrance.amount))
                            || (thisUnderlyingEnc.amountString != null && thisUnderlyingEnc.amountString.equals(thisEncumbrance.amountString))
                    )
                        &&
                        // This Commitment matches the underlying PublicKey or Address OR
                        // In the case of a POA, the Commitment if from the Authoring Address and the Underlying Encumbrance matches the POA Address.
                        (
                            ((!thisTX.isPOA())
                                && (thisUnderlyingEnc.getPublicKey().equalsIgnoreCase(thisAddress)
                                || thisUnderlyingEnc.getPublicKey().equalsIgnoreCase(thisEncumbrance.getPublicKey())))
                                || (thisTX.isPOA()
                                && attorneyAddress.equalsIgnoreCase(thisAddress)
                                && thisUnderlyingEnc.getPublicKey().equalsIgnoreCase(poaAddress)
                            )
                        )) {

                      contractHasChanged = true;

                      if (thisTX.isPOA()) {
                        thisUnderlyingEnc.setPoaPublicKey(thisEncumbrance.getPublicKey());
                      } else {
                        thisUnderlyingEnc.setPublicKey(thisEncumbrance.getPublicKey());
                      }

                      thisUnderlyingEnc.setSignature(thisEncumbrance.signature);

                    }
                  }
                }
              }
            } // txData.getEncumbrances

            if (isSingleContractAddress && (commitSigData != null)) {
              for (DvpParty matchedParty : matchedParties) {
                // Sign for party

                if ((matchedParty.partyIdentifier == null) || (matchedParty.partyIdentifier.length() == 0)) {
                  contractHasChanged = true;
                  matchedParty.partyIdentifier = commitSigData.partyIdentifier;
                }

                if (((matchedParty.sigAddress == null) || (matchedParty.sigAddress.length() == 0))
                    || ((matchedParty.publicKey == null) || (matchedParty.publicKey.length() == 0))
                    || ((matchedParty.signature == null) || (matchedParty.signature.length() == 0))) {

                  contractHasChanged = true;
                  matchedParty.sigAddress = commitSigAddress;
                  matchedParty.publicKey = commitSigData.publicKey;
                  matchedParty.signature = commitSigData.signature;
                }

            /*
             # OK, now check for payment commitments
             # Paylist details   :  [Address1, NameSpace1, AssetID1, Qty1, Public Key, Signature]
             # PayCommit Details :  [Index, PublicKey, Signature]
             # Signatures and Addresses Must already have been checked.
             */

                if (txData.getCommitment() != null) {
                  for (DvpCommitment thisPaymentCommitment : txData.getCommitment()) {

                    String payAddress = thisPaymentCommitment.getPayAddress();

                    if ((matchedParty.payList != null) && (thisPaymentCommitment.index >= 0L) && (thisPaymentCommitment.index < matchedParty.payList.size())) {
                      DvpPayItem thisPayment = matchedParty.payList.get(thisPaymentCommitment.index.intValue());

                      // Payment Address specified, but not match the commitment...
                      // Same checks as earlier, but can not duck out now as it could corrupt state, so just ignore (continue).

                      if (Strings.isNullOrEmpty(thisPayment.address) && (thisTX.isPOA())) {
                        continue;
                      } else {
                        if (!thisPayment.address.equalsIgnoreCase(payAddress)) {
                          if (thisTX.isPOA()) {
                            // If the Authoring Address did not sign or the Payment Address is not the POA Address, then exit.
                            if ((!attorneyAddress.equalsIgnoreCase(payAddress)) || (!thisPayment.address.equalsIgnoreCase(poaAddress))) {
                              continue;
                            }
                          } else {
                            // Payment Address not match
                            continue;
                          }
                        }
                      }

                      // If commit Address, Public Key and Signature are present, then set them.
                      contractHasChanged = true;

                      if (!thisTX.isPOA()) {
                        thisPayment.address = thisPaymentCommitment.getPayAddress();
                      }

                      thisPayment.publicKey = thisPaymentCommitment.getPublicKey();
                      thisPayment.signature = thisPaymentCommitment.signature;
                    }
                  }
                }

                //  Update and Receiving Addresses
                // Can not set Receiving addresses from POA.

                if (txData.getReceive() != null) {
                  for (DvpCommitReceive thisReceiveDetail : txData.getReceive()) {
                    contractHasChanged = true;
                    matchedParty.receiveList.get(thisReceiveDetail.index.intValue()).address = thisReceiveDetail.address;
                  }
                }


              }
            } // commitSigData

            if (thisTX.isPOA()) {
              contractData.set__status(String.format("POA Commit to Contract, on behalf of %s, entered by Attorney %s, TX %s.",
                  poaAddress, attorneyAddress, thisTX.getHash()
              ));

              if (thisItem != null) {
                thisItem.consume(1L);

                if (thisItem.consumed()) {
                  tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);
                }
              }
            }

            // Register contract event, if something has changed.
            if (contractHasChanged) {
              stateSnapshot.addContractEvent(contractAddress, thisFunctionName, "commit", null);
            }

            // Even if nothing has changed, we record that someone committed.
            stateSnapshot.addLifeCycleEvent(ContractLifeCycle.COMMIT, contractAddress, contractData.addresses());

            if (logger.isDebugEnabled()) {
              logger.debug("Contract data for contract {} pre-commit is\n{}", contractAddress, UpdateEvent.prettyContractData(contractData));
            }

          } // for : contractAddress

        } // priority
      } // Chain

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in NewDvPCommit.updatestate()", e);

      if (logger.isDebugEnabled() && contractData != null) {
        logger.debug("Contract data for contract {} pre-commit is\n{}", thisTX.getContractAddress(), UpdateEvent.prettyContractData(contractData));
      }

      return new ReturnTuple(SuccessType.FAIL, "Error in NewDvPCommit.updatestate.");
    }

  }


  private DvpCommit() {
    // Sonarqube wants a private constructor.
  }

}
