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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.bc.pychain.state.tx.Hash.computeHash;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaDetail;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.getPoaDetailEntry;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_COMMITS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.CommitContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;

public class ExchangeCommit {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeCommit.class);


  private static ReturnTuple checkNamespace(StateSnapshot stateSnapshot, NominateAsset thisInput, String issuingaddress, String hash, boolean checkOnly) {

    // Namespace locked ?
    if (stateSnapshot.isAssetLocked(thisInput.namespace)) {
      return new ReturnTuple(
          (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
          MessageFormat.format("Exchange Commit : Namespace is locked : {0}. Tx Hash {1}", thisInput.namespace, hash)
      );
    }

    // Asset locked ?
    if (stateSnapshot.isAssetLocked(thisInput.getFullAssetID())) {
      return new ReturnTuple(
          (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
          MessageFormat.format("Exchange Commit : Asset `{0}` is locked. Tx Hash {1}", thisInput.getFullAssetID(), hash)
      );
    }

    MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();

    NamespaceEntry namespaceEntry = namespaceTree.find(thisInput.namespace);

    // Namespace Exist ?
    // Note in some XChain scenarios, the Namespace definition just might not exist so this is not a show stopper.
    if (namespaceEntry != null) {

      // Namespace Owned ?
      if (namespaceEntry.getAddress().equals(issuingaddress)) {
        thisInput.isIssuer = true;

        // Class Details. Check the class exists if issuer.
        if (!namespaceEntry.containsAsset(thisInput.classid)) {
          return new ReturnTuple(
              (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
              MessageFormat.format(
                  "Exchange Commit : Class `{0}` is not registered in Namespace `{1}`. Tx Hash {2}",
                  thisInput.classid, thisInput.namespace, hash
              )
          );
        }
      } else {
        thisInput.isIssuer = false;
      }
    }

    return new ReturnTuple(SuccessType.PASS, "OK");
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
  @SuppressWarnings("squid:S2159") // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  public static ReturnTuple updatestate(CommitContractInterface thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    String sigMessage;
    long poaAddressPermissions = AP_COMMITS;
    final TxType effectiveTxID = TxType.COMMIT_TO_CONTRACT;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Verification : Apply only to the 'Native' chain...

        if ((thisTX.getTimestamp() > 0) && (Math.abs(thisTX.getTimestamp() - updateTime) > stateSnapshot.getStateConfig().getMaxTxAge())) {
          return new ReturnTuple(SuccessType.FAIL, "Tx Timestamp invalid.");
        }

        if (priority == thisTX.getPriority()) {

          // Cope with ContractAddressArray? : Non sensical to commit to multiple Exchanges simultaneously.
          String contractAddress = thisTX.getContractAddress().get(0);

          // Check Contract data. Must already exist.

          MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();
          ContractEntry thisContractEntry = contractsList.findAndMarkUpdated(contractAddress);
          if (thisContractEntry == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Contract Address must exist");
          }

          // Check Attorney (Authoring) Address
          String attorneyAddress = thisTX.getAuthoringAddress();
          if (!AddressUtil.verify(attorneyAddress, thisTX.getAuthoringPublicKey(), AddressType.NORMAL)) {
            return new ReturnTuple(SuccessType.FAIL, "`From` Address and Public key do not match.");
          }

          // Address permissions.
          // Check Address permissions (and PoA Address)
          String poaAddress = thisTX.getEffectiveAddress();

          if (thisTX.isPOA()) {
            if (!AddressUtil.verifyAddress(poaAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", poaAddress));
            }

            // Address permissions
            if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
              ReturnTuple aPerm = checkPoaAddressPermissions(stateSnapshot, attorneyAddress, poaAddress, thisTX.getTxType(), effectiveTxID,
                  poaAddressPermissions
              );
              if ((!checkOnly) && (aPerm.success != SuccessType.PASS)) {
                return aPerm;
              }
            }
          } else {
            if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
              long keyPermission = stateSnapshot.getAddressPermissions(attorneyAddress);
              boolean hasPermission = stateSnapshot.canUseTx(attorneyAddress, thisTX.getTxType());
              if ((!hasPermission) && ((keyPermission & poaAddressPermissions) == 0)) {
                return new ReturnTuple(
                    (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                    MessageFormat.format("Inadequate Address permissioning : Tx Hash {0}", thisTX.getHash())
                );
              }
            }
          }

          // Get TxContract Data
          ExchangeCommitData txCommitData = (ExchangeCommitData) thisTX.getCommitmentData();

          if (txCommitData == null) {
            return new ReturnTuple(SuccessType.FAIL, "Exchange Commit : No Commitment Data");
          }

          // Get Contract Data
          ExchangeContractData contractData = (ExchangeContractData) thisContractEntry.getContractData();

          if (contractData == null) {
            logger.error("Exchange Commit : ContractData missing for contract {}", contractAddress);
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Exchange Commit : ContractData missing for " + contractAddress);
          }

          // Check Contract Address matches.
          if (!Strings.isNullOrEmpty(txCommitData.contractAddress) && (!txCommitData.contractAddress.equals(contractData.get__address()))) {
            return new ReturnTuple(SuccessType.FAIL, "Exchange Commit : Commitment and Contract addresses do not match.");
          }

          if (!CONTRACT_NAME_EXCHANGE.equalsIgnoreCase(contractData.get__function())) {
            // Wrong Contract type.
            return new ReturnTuple(
                SuccessType.FAIL,
                MessageFormat.format("Exchange Commit : Type mismatch, Contract `{0}` type expected is {1}, Commit is {2} TxHash {3}",
                    contractAddress, CONTRACT_NAME_EXCHANGE, contractData.get__function(), thisTX.getAuthoringAddress()
                )
            );
          }

          if (contractData.getStartdate() > updateTime) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Exchange Commit : Contract `{0}` has not yet started. Start {1}, Update {2}, TxHash {3}",
                    contractAddress, contractData.getStartdate(), updateTime, thisTX.getAuthoringAddress()
                )
            );
          }

          if (contractData.getExpiry() < updateTime) {
            return new ReturnTuple(
                SuccessType.FAIL,
                MessageFormat.format("Exchange Commit : Contract `{0}` has expired. Expiry {1}, Update {2}, TxHash {3}",
                    contractAddress, contractData.getExpiry(), updateTime, thisTX.getAuthoringAddress()
                )
            );
          }

          if (contractData.getInputs().isEmpty()) {
            // Should not happen, but it would be a quick exit...
            return new ReturnTuple(
                SuccessType.FAIL,
                MessageFormat.format("Exchange Commit : Contract `{0}` has no inputs. TxHash {1}", contractAddress, thisTX.getAuthoringAddress())
            );
          }

          //
          String destinationAddress = (Strings.isNullOrEmpty(txCommitData.toaddress) ? poaAddress : txCommitData.toaddress);
          HashSet<String> requiredAddresses = new HashSet<>();

          if (!destinationAddress.equals(poaAddress)) {
            // Verify destination address
            if (!AddressUtil.verifyAddress(destinationAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid Destination address {0}", destinationAddress));
            }

            requiredAddresses.add(destinationAddress);

          }

          // Check Assets in
          // Collate committed assets, check for no duplicates, check PoAs
          // 'poasToCheck' will also be the PoAs to consume...

          HashMap<String, NominateAsset> committedAssets = new HashMap<>();
          HashMap<String, HashMap<String, HashMap<String, Number>>> poasToCheck = null; // Format is {Address : {PoaRef : [Assets]}}
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();
          HashMap<String, AddressEntry> addressesMap = new HashMap<>(requiredAddresses.size());

          String pubKeyAddress;
          String inputAddress;
          String poaReference;
          MessageSignerVerifier verifier = null;

          for (NominateAsset thisAssetIn : txCommitData.getAssetsIn()) {

            // Duplicate ?
            if (committedAssets.containsKey(thisAssetIn.getFullAssetID())) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange Commit : Duplicate Input Asset : Asset {0}, Tx Hash {1}",
                  thisAssetIn.getFullAssetID(), thisTX.getHash()
              ));
            }

            // +ve Amount
            if (thisAssetIn.getAmount().lessThanEqualZero()) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange Commit : Input Asset amount <= 0 : Asset {0}, Amount {1}, Tx Hash {2}",
                  thisAssetIn.getFullAssetID(), thisAssetIn.getAmount(), thisTX.getHash()
              ));
            }

            committedAssets.put(thisAssetIn.getFullAssetID(), thisAssetIn);

            // Check Signature, If necessary, if present.

            if (Strings.isNullOrEmpty(thisAssetIn.getAddress()) || thisAssetIn.getAddress().equals(poaAddress)) {
              // No signature necessary (PoAs maybe).

              inputAddress = poaAddress;
            } else {
              inputAddress = thisAssetIn.getAddress();

              // Signature present ?

              if (Strings.isNullOrEmpty(thisAssetIn.publickey) || Strings.isNullOrEmpty(thisAssetIn.signature)) {
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                    "Exchange Commit : Unsigned Input for address {0}, Asset {1} : Tx Hash {2}",
                    thisAssetIn.getAddress(),
                    thisAssetIn.getFullAssetID(),
                    thisTX.getHash()
                ));
              }

              // Check signature.
              if (!AddressUtil.verify(thisAssetIn.getAddress(), thisAssetIn.publickey, AddressType.NORMAL)) {
                if ((!thisTX.isPOA()) || !AddressUtil.verify(attorneyAddress, thisAssetIn.publickey, AddressType.NORMAL)) {
                  // Unacceptable Public key
                  return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                      "Exchange Commit : Unacceptable PublicKey for Input : Key {0}, Tx Hash {1}",
                      thisAssetIn.publickey,
                      thisTX.getHash()
                  ));
                }
              }

              sigMessage = computeHash(thisAssetIn.objectToHashToSign(contractAddress));

              if (verifier == null) {
                verifier = MessageVerifierFactory.get();
              }

              if (!verifier.verifySignature(sigMessage, thisAssetIn.publickey, thisAssetIn.signature)) {
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange Commit : Invalid input signature : Asset {0}, Tx Hash {1}",
                    thisAssetIn.getFullAssetID(), thisTX.getHash()
                ));
              }

            } // Check Signatures

            // Save active addresses

            requiredAddresses.add(inputAddress);

            // Check available balance

            if (!addressesMap.containsKey(inputAddress)) {
              addressesMap.put(inputAddress, assetBalances.findAndMarkUpdated(inputAddress));
            }

            AddressEntry thisAddressEntry = addressesMap.get(inputAddress);

            if (thisAddressEntry == null) {
              // Commit Input Address does not exist
              return new ReturnTuple(
                  (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  MessageFormat.format("Exchange Commit : Input address does not exist : Address {0}, Tx Hash {1}",
                      inputAddress, thisTX.getHash()
                  )
              );
            }

            if (thisAddressEntry.getAssetBalance(thisAssetIn.getFullAssetID()).lessThan(thisAssetIn.getAmount())) {
              // Insufficient Commit balance
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format(
                  "Exchange Commit : Insufficient Commitment balance : Address {0}, Asset {1}, Amount {2}, Required {3}, Tx Hash {4}",
                  inputAddress,
                  thisAssetIn.getFullAssetID(),
                  thisAddressEntry.getAssetBalance(thisAssetIn.getFullAssetID()),
                  thisAssetIn.getAmount(),
                  thisTX.getHash()
              ));
            }

            // Collect PoAs to check

            if (thisTX.isPOA()) {

              // Poa Reference : Use Asset one or fall back to TxOne if no Asset reference.

              poaReference = (Strings.isNullOrEmpty(thisAssetIn.reference) ? thisTX.getPoaReference() : thisAssetIn.reference);

              if (poasToCheck == null) {
                poasToCheck = new HashMap<>(txCommitData.getAssetsIn().size());
              }

              // Address Entry
              HashMap<String, HashMap<String, Number>> referenceMap = poasToCheck.get(inputAddress);

              if (referenceMap == null) {
                referenceMap = new HashMap<>();
                poasToCheck.put(inputAddress, referenceMap);
              }

              HashMap<String, Number> thisItem = referenceMap.get(poaReference);
              if (thisItem == null) {
                thisItem = new HashMap<>();
                thisItem.put(thisAssetIn.getFullAssetID(), thisAssetIn.getAmount());
                referenceMap.put(poaReference, thisItem);
              } else {
                thisItem.put(thisAssetIn.getFullAssetID(), thisAssetIn.getAmount().add(thisItem.getOrDefault(thisAssetIn.getFullAssetID(), 0)));
              }

            }

          } // for input assets

          // Check collected PoAs.

          String[] tempStringArray = new String[1];
          StateSnapshot snapshotWrap = null;
          PoaDetail thisDetail;

          if (thisTX.isPOA() && (poasToCheck != null)) {

            snapshotWrap = stateSnapshot.createSnapshot();

            // For each Address...
            for (Entry<String, HashMap<String, HashMap<String, Number>>> addressEntry : poasToCheck.entrySet()) {

              // And for each PoA reference used from that address...
              for (Entry<String, HashMap<String, Number>> referenceEntry : addressEntry.getValue().entrySet()) {

                // Get ReferenceEntry

                ReturnTuple rVal = getPoaDetailEntry(snapshotWrap, updateTime, poaAddress, referenceEntry.getKey(), false);

                if (rVal.success == SuccessType.PASS) {

                  thisDetail = (PoaDetail) rVal.returnObject;

                  for (Entry<String, Number> assetRequired : referenceEntry.getValue().entrySet()) {

                    tempStringArray[0] = assetRequired.getKey();

                    // We have a PoA Entry to check, it is either OK or Not. 'checkOnly' should not have an effect at this point.
                    ReturnTuple poaPerms = checkPoaDetail(
                        referenceEntry.getKey(),
                        thisDetail,
                        attorneyAddress,
                        poaAddress,
                        effectiveTxID,
                        tempStringArray,
                        assetRequired.getValue()
                    );

                    if (poaPerms.success != SuccessType.PASS) {
                      return poaPerms;
                    }

                    PoaItem thisItem = (PoaItem) poaPerms.returnObject;
                    thisItem.consume(assetRequired.getValue());

                    // Can consume here because we are using a SnapshotWrap which will be discarded, not the 'real' snapshot.
                    if (thisItem.consumed()) {
                      tidyPoaReference(snapshotWrap, updateTime, referenceEntry.getKey(), poaAddress);
                    }
                  }

                } else {
                  // Status could be WARNING, if checkOnly.
                  return rVal;
                }

              }
            }
          } // Is PoA, Check.

          // Check Namespaces
          // Asset Locking ? Check the contract Inputs and Outputs
          // Check Assets match
          // Check Block counts

          ReturnTuple cns;
          Number blockMultiple = null;
          Number thisMultiple = null;
          NominateAsset commitmentInput = null;

          for (NominateAsset thisContractInput : contractData.getInputs()) {

            // Check for NS / Asset locked and looks for NS existence.
            cns = checkNamespace(stateSnapshot, thisContractInput, contractData.getIssuingaddress(), thisTX.getHash(), checkOnly);

            if (cns.success != SuccessType.PASS) {
              return cns;
            }

            // Check Input asset is in the commit

            commitmentInput = committedAssets.get(thisContractInput.getFullAssetID());

            if (commitmentInput == null) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange Commit : Required Input Asset not given : Asset {0}, Tx Hash {1}",
                  thisContractInput.getFullAssetID(), thisTX.getHash()
              ));
            }

            // Input address limitation ?
            if (!Strings.isNullOrEmpty(thisContractInput.getAddress())) {
              if (!thisContractInput.getAddress().equalsIgnoreCase(
                  Strings.isNullOrEmpty(commitmentInput.getAddress()) ? poaAddress : commitmentInput.getAddress())) {
                return new ReturnTuple(
                    SuccessType.FAIL,
                    MessageFormat.format("Exchange Commit : Input Asset not from required address : Asset {0}, Tx Hash {1}",
                        thisContractInput.getFullAssetID(), thisTX.getHash()
                    )
                );
              }
            }

            // Check / Establish block multiple
            if (!commitmentInput.getAmount().modulus(thisContractInput.blocksize).equalTo(0L)) {
              return new ReturnTuple(
                  SuccessType.FAIL,
                  MessageFormat.format(
                      "Exchange Commit : Input Asset amount is not a multiple of the Contract block size : Asset {0}, Amount {1}, BlockSize {2}, Tx Hash {3}",
                      thisContractInput.getFullAssetID(),
                      commitmentInput.getAmount(),
                      thisContractInput.blocksize,
                      thisTX.getHash()
                  )
              );
            }

            thisMultiple = commitmentInput.getAmount().divideBy(thisContractInput.blocksize);

            if (blockMultiple == null) {
              blockMultiple = thisMultiple;
            } else if (!((Balance) blockMultiple).equalTo(thisMultiple)) {

              return new ReturnTuple(
                  SuccessType.FAIL,
                  MessageFormat.format(
                      "Exchange Commit : Input Asset does not have the same block size : Asset {0}, Amount {1}, BlockCount {2}, Expected {3}, Tx Hash {4}",
                      thisContractInput.getFullAssetID(),
                      commitmentInput.getAmount(),
                      thisMultiple,
                      blockMultiple,
                      thisTX.getHash()
                  )
              );
            }

          }

          // Validate Block Multiple

          if (contractData.getMinblocks().greaterThan(blockMultiple)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                "Exchange Commit : Commitment Input Block count of {0} is less than Contract minimum of {1} : Tx Hash {2}",
                blockMultiple,
                contractData.getMinblocks(),
                thisTX.getHash()
            ));
          }

          if (contractData.getMaxblocks().greaterThanZero() && contractData.getMaxblocks().lessThan(blockMultiple)) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                "Exchange Commit : Commitment Input Block count of {0} exceeds Contract limit of {1} : Tx Hash {2}",
                blockMultiple,
                contractData.getMaxblocks(),
                thisTX.getHash()
            ));
          }

          //

          String outputAddress;

          for (NominateAsset thisOutputAsset : contractData.getOutputs()) {

            // 'checkNamespace()' also sets isIssuer.
            cns = checkNamespace(stateSnapshot, thisOutputAsset, contractData.getIssuingaddress(), thisTX.getHash(), checkOnly);

            if (cns.success != SuccessType.PASS) {
              // Return here could be 'Warning'.
              return cns;
            }

            // Save active addresses

            if (Strings.isNullOrEmpty(thisOutputAsset.getAddress())) {
              outputAddress = contractData.getIssuingaddress();
            } else {
              outputAddress = thisOutputAsset.getAddress();
            }

            requiredAddresses.add(outputAddress);

            // Check output balances

            if (!addressesMap.containsKey(outputAddress)) {
              addressesMap.put(outputAddress, assetBalances.findAndMarkUpdated(outputAddress));
            }

            AddressEntry thisAddressEntry = addressesMap.get(outputAddress);

            if (thisAddressEntry == null) {
              // Commit Output Address does not exist
              return new ReturnTuple(
                  (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  MessageFormat.format("Exchange Commit : Output address does not exist or has no balances : Address {0}, Tx Hash {1}",
                      outputAddress, thisTX.getHash()
                  )
              );
            }

            if (!thisOutputAsset.isIssuer) {
              if (thisAddressEntry.getAssetBalance(thisOutputAsset.getFullAssetID()).lessThan(thisOutputAsset.blocksize.multiplyBy(blockMultiple))) {
                // Insufficient Contract balance
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                    "Exchange Commit : Insufficient Contract balance : Address {0}, Asset {1}, Amount {2}, Required {3}, Tx Hash {4}",
                    outputAddress,
                    thisOutputAsset.getFullAssetID(),
                    thisAddressEntry.getAssetBalance(thisOutputAsset.getFullAssetID()),
                    thisOutputAsset.blocksize.multiplyBy(blockMultiple),
                    thisTX.getHash()
                ));
              }
            }

          }

          // Collect Active Addresses, if not already collected

          for (String thisAddress : requiredAddresses) {
            if (!addressesMap.containsKey(thisAddress)) {
              addressesMap.put(thisAddress, assetBalances.findAndMarkUpdated(thisAddress));
            }
          }

          // Check Destination address exists

          AddressEntry toAddressEntry = addressesMap.get(destinationAddress);

          if (toAddressEntry == null) {
            // Check if 'To' Address must already exist.

            if (stateSnapshot.getStateConfig().getMustRegister()) {
              return new ReturnTuple(
                  (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                  MessageFormat.format(
                      "Exchange Commit : Target Address `{0}` does not exist in state. MustRegister. Tx Hash {1}",
                      destinationAddress,
                      thisTX.getHash()
                  )
              );
            }

            if (checkOnly) {
              return new ReturnTuple(SuccessType.PASS, "Check Only.");
            }

            couldCorrupt = true;

            // addBalanceOnlyAddressEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
            // version 'VERSION_SET_FULL_ADDRESS'

            addDefaultAddressEntry(assetBalances, destinationAddress, stateSnapshot.getVersion());

          }

          // Recap :
          // We have : comittedAssets.put(thisCommitAsset.getFullAssetID(), thisCommitAsset);
          // addressesMap has all active addresses
          // blockMultiple has been set and validated.
          // I need : Inputs and outputs neatly.

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          couldCorrupt = true;

          // OK Move Commit Assets to to the contract address, then move output assets to Commit Address

          AddressEntry contractInAddress = addressesMap.get(contractData.getIssuingaddress());
          AddressEntry contractIssuanceAddress = contractInAddress;
          AddressEntry contractOutAddress;
          AddressEntry commitInAddress;
          AddressEntry commitOutAddress;
          NominateAsset commitedAsset;
          Balance inAmount;
          Balance outAmount;
          Balance existingBalance;
          Balance newBalance;

          if (contractInAddress == null) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                "Exchange Commit : Failed to get Address entry for Contract input assets {0}, Tx Hash {1}",
                contractData.getIssuingaddress(), thisTX.getHash()
            ));
          }

          // For each input....
          for (NominateAsset thisContractInput : contractData.getInputs()) {

            // Get the Committed Asset
            commitedAsset = committedAssets.get(thisContractInput.getFullAssetID());

            // Get commitment address
            commitInAddress = addressesMap.get(Strings.isNullOrEmpty(commitedAsset.getAddress()) ? poaAddress : commitedAsset.getAddress());

            if (commitInAddress == null) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                  "Exchange Commit : Failed to get Address entry for Commitment input assets {0}, Tx Hash {1}",
                  (Strings.isNullOrEmpty(commitedAsset.getAddress()) ? poaAddress : commitedAsset.getAddress()),
                  thisTX.getHash()
              ));
            }

            // Move Input asset from 'commitInAddress' to 'contractInAddress'

            // Take from Commit ...
            inAmount = commitedAsset.getAmount();
            existingBalance = commitInAddress.getAssetBalance(commitedAsset.getFullAssetID());

            if (inAmount.greaterThan(existingBalance)) {
              // Should have already been checked !

              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                  "Exchange Commit : Insufficient Commit balance : Address {0}, Asset {1}, Amount {2}, Required {3}, Tx Hash {4}",
                  Strings.isNullOrEmpty(commitedAsset.getAddress()) ? poaAddress : commitedAsset.getAddress(),
                  commitedAsset.getFullAssetID(),
                  existingBalance,
                  inAmount,
                  thisTX.getHash()
              ));
            }

            newBalance = existingBalance.subtract(inAmount);
            commitInAddress.setAssetBalance(commitedAsset.getFullAssetID(), newBalance);

            // Give to Contract ...
            existingBalance = contractInAddress.getAssetBalance(commitedAsset.getFullAssetID());
            contractInAddress.setAssetBalance(commitedAsset.getFullAssetID(), existingBalance.add(inAmount));

            // Consume PoA
            if ((thisTX.isPOA()) && (snapshotWrap != null)) {

              // Commit 'temporary' PoA updates to the snapshot.
              snapshotWrap.commit();

            }
          }

          // For each output....
          for (NominateAsset thisOutputAsset : contractData.getOutputs()) {

            // Output Asset from specified address or contractIssuance Address
            contractOutAddress = (Strings.isNullOrEmpty(
                thisOutputAsset.getAddress()) ? contractIssuanceAddress : addressesMap.get(thisOutputAsset.getAddress()));

            outAmount = thisOutputAsset.blocksize.multiplyBy(blockMultiple);

            // Take from Contract ...
            existingBalance = contractOutAddress.getAssetBalance(thisOutputAsset.getFullAssetID());

            if (!thisOutputAsset.isIssuer) {
              if (existingBalance.lessThan(outAmount)) {
                // Insufficient Output asset. // Should have already been checked !
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                    "Exchange Commit : Insufficient Contract balance : Address {0}, Asset {1}, Amount {2}, Required {3}, Tx Hash {4}",
                    Strings.isNullOrEmpty(thisOutputAsset.getAddress()) ? contractIssuanceAddress : addressesMap.get(thisOutputAsset.getAddress()),
                    thisOutputAsset.getFullAssetID(),
                    existingBalance,
                    outAmount,
                    thisTX.getHash()
                ));
              }
            }

            newBalance = existingBalance.subtract(outAmount);
            contractOutAddress.setAssetBalance(thisOutputAsset.getFullAssetID(), newBalance);

            // Give to Commit ...

            commitOutAddress = addressesMap.get(destinationAddress);
            existingBalance = commitOutAddress.getAssetBalance(thisOutputAsset.getFullAssetID());
            commitOutAddress.setAssetBalance(thisOutputAsset.getFullAssetID(), existingBalance.add(outAmount));
          }

        } // Priority

      } // Chain

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in ExchangeCommit.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in ExchangeCommit.updatestate.");
    }

  }

}
