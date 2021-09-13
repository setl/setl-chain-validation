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
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CONTRACTS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.tx.NewContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;

public class Exchange {

  private static final Logger logger = LoggerFactory.getLogger(Exchange.class);


  /**
   * Exchange Contract, updatestate().
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   */
  public static ReturnTuple updatestate(NewContractInterface thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final String thisFunctionName = CONTRACT_NAME_EXCHANGE;
    final TxType effectiveTxID = TxType.NEW_CONTRACT;
    long poaAddressPermissions = AP_CONTRACTS;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Verification : Apply only to the 'Native' chain...

        if (priority == thisTX.getPriority()) {

          String contractAddress = thisTX.getContractAddress();

          // Check Contract data. Must not already exist.

          MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();
          if (contractsList.find(contractAddress) != null) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Contract already exists : Contract {0}, Tx Hash {1}", contractAddress, thisTX.getHash())
            );
          }

          // Check Attorney Address
          String attorneyAddress = thisTX.getAuthoringAddress();
          if (!AddressUtil.verify(attorneyAddress, thisTX.getAuthoringPublicKey(), AddressType.NORMAL)) {
            return new ReturnTuple(SuccessType.FAIL, "`From` Address and Public key do not match.");
          }

          String poaAddress = thisTX.getEffectiveAddress();

          // Check Address permissions (and PoA Address)

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

          // Get Contract Data
          ExchangeContractData contractData = new ExchangeContractData((ExchangeContractData) thisTX.getContractData());

          // issuingaddress, set it in the map explicitly.
          contractData.setIssuingaddress(poaAddress);

          // Validate contractData
          // Validate Namespace, classIDs and amounts are OK.
          // Validate signatures as necessary

          if (contractData.getInputs().isEmpty()) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : No Input Assets specified : Tx Hash {0}", thisTX.getHash()));
          }

          if (contractData.getOutputs().isEmpty()) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : No Output Assets specified : Tx Hash {0}", thisTX.getHash()));
          }

          // Validate BlockSizes and Max / Min Blocks.

          if (contractData.getMaxblocks().greaterThanZero() && contractData.getMaxblocks().lessThan(contractData.getMinblocks())) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Max Blocks < Min blocks : Tx Hash {0}", thisTX.getHash()));
          }

          if (contractData.getMaxblocks().lessThanZero()) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Max Blocks < Zero : Tx Hash {0}", thisTX.getHash()));
          }

          // Validate Start Date and Expiry Date
          if (contractData.getStartdate() > contractData.getExpiry()) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Start Date is after Expiry : Tx Hash {0}", thisTX.getHash()));
          }

          if (contractData.getExpiry() < updateTime) {
            return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Expiry is before creation time : Tx Hash {0}", thisTX.getHash()));
          }

          // Check Inputs

          HashMap<String, HashMap<String, HashSet<String>>> poasToCheck = null; // {Address : {PoaRef : [Assets]}}
          if (thisTX.isPOA()) {
            poasToCheck = new HashMap<>(contractData.getOutputs().size());
          }

          ArrayList<PoaItem> poaItems = null;
          HashSet<String> checkedAssets = new HashSet<>();
          String poaReference;

          for (NominateAsset thisAsset : contractData.getInputs()) {

            if (Strings.isNullOrEmpty(thisAsset.namespace)
                || Strings.isNullOrEmpty(thisAsset.classid)
                || (thisAsset.blocksize.lessThanEqualZero())) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Invalid Input Asset : Asset {0}, Blocksize {1}, Tx Hash {2}",
                  thisAsset.getFullAssetID(), thisAsset.blocksize, thisTX.getHash()
              ));
            }

            if (checkedAssets.contains(thisAsset.getFullAssetID())) {
              // Duplicate Input Asset

              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Duplicate Input Asset : Asset {0}, Tx Hash {1}",
                  thisAsset.getFullAssetID(), thisTX.getHash()
              ));
            }

            if (!Strings.isNullOrEmpty(thisAsset.getAddress())) {
              // check thisAsset.address is a valid address

              if (!AddressUtil.verifyAddress(thisAsset.getAddress())) {
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : invalid Input Address : Address {0}, Tx Hash {1}",
                    thisAsset.getAddress(), thisTX.getHash()
                ));
              }
            }

            checkedAssets.add(thisAsset.getFullAssetID());

            // Collect PoAs to check

            if (poasToCheck != null) {

              // Poa Reference : Use Asset one or fall back to TxOne if no Asset reference.

              poaReference = (Strings.isNullOrEmpty(thisAsset.reference) ? thisTX.getPoaReference() : thisAsset.reference);

              // Address Entry
              HashMap<String, HashSet<String>> referenceMap = poasToCheck.get(contractData.getIssuingaddress());

              if (referenceMap == null) {
                referenceMap = new HashMap<>();
                poasToCheck.put(contractData.getIssuingaddress(), referenceMap);
              }

              // Reference Entry
              if (!referenceMap.containsKey(poaReference)) {
                referenceMap.put(poaReference, new HashSet<>());
              }

              referenceMap.get(poaReference).add(thisAsset.getFullAssetID());
            }

          } // Inputs

          // Check Outputs

          checkedAssets.clear();
          String combinedAssetID;
          String outputAddress;
          String pubKeyAddress;
          String sigMessage;
          MessageSignerVerifier verifier = null;

          for (NominateAsset thisAsset : contractData.getOutputs()) {

            // Basic details OK ?

            if (Strings.isNullOrEmpty(thisAsset.namespace)
                || Strings.isNullOrEmpty(thisAsset.classid)
                || (thisAsset.blocksize.lessThanEqualZero())) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Invalid Output Asset : Asset {0}, Blocksize {1}, Tx Hash {2}",
                  thisAsset.getFullAssetID(), thisAsset.blocksize, thisTX.getHash()
              ));
            }

            // Check for Duplicates

            outputAddress = (Strings.isNullOrEmpty(thisAsset.getAddress()) ? contractData.getIssuingaddress() : thisAsset.getAddress());

            combinedAssetID = (outputAddress + "|" + thisAsset.getFullAssetID()).toLowerCase();

            if (checkedAssets.contains(combinedAssetID)) {
              // Duplicate Asset

              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Duplicate Output Asset : Asset {0}, Tx Hash {1}",
                  combinedAssetID, thisTX.getHash()
              ));
            }

            checkedAssets.add(combinedAssetID);

            // Collect PoAs to check

            if (poasToCheck != null) {

              // Poa Reference : Use Asset one or fall back to TxOne if no Asset reference.

              poaReference = (Strings.isNullOrEmpty(thisAsset.reference) ? thisTX.getPoaReference() : thisAsset.reference);

              // Address Entry
              HashMap<String, HashSet<String>> referenceMap = poasToCheck.get(outputAddress);

              if (referenceMap == null) {
                referenceMap = new HashMap<>();
                poasToCheck.put(outputAddress, referenceMap);
              }

              // Reference Entry
              if (!referenceMap.containsKey(poaReference)) {
                referenceMap.put(poaReference, new HashSet<>());
              }

              referenceMap.get(poaReference).add(thisAsset.getFullAssetID());
            }

            // If Payment address is missing or is the Contract Issuing Address, then don't require or check signatures.

            if (Strings.isNullOrEmpty(thisAsset.getAddress()) || thisAsset.getAddress().equalsIgnoreCase(contractData.getIssuingaddress())) {
              thisAsset.publickey = null;
              thisAsset.signature = null;
              continue;
            }

            // check thisAsset.address is a valid address

            if (!AddressUtil.verifyAddress(thisAsset.getAddress())) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : invalid Output Address : Address {0}, Tx Hash {1}",
                  thisAsset.getAddress(), thisTX.getHash()
              ));
            }

            // Signature present ?

            if (Strings.isNullOrEmpty(thisAsset.publickey) || Strings.isNullOrEmpty(thisAsset.signature)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Exchange : Unsigned Output for address {0} : Tx Hash {1}", thisAsset.getAddress(),
                  thisTX.getHash()
              ));
            }

            // Check signature.
            if (!AddressUtil.verify(thisAsset.getAddress(), thisAsset.publickey, AddressType.NORMAL)) {
              if ((!thisTX.isPOA()) || !AddressUtil.verify(attorneyAddress, thisAsset.publickey, AddressType.NORMAL)) {
                // Unacceptable Public key
                return new ReturnTuple(SuccessType.FAIL, MessageFormat.format(
                    "Exchange : Unacceptable PublicKey for Output : Key {0}, Tx Hash {1}",
                    thisAsset.publickey,
                    thisTX.getHash()
                ));
              }
            }

            sigMessage = computeHash(thisAsset.objectToHashToSign(contractAddress));

            if (verifier == null) {
              verifier = MessageVerifierFactory.get();
            }

            if (!verifier.verifySignature(sigMessage, thisAsset.publickey, thisAsset.signature)) {
              return new ReturnTuple(SuccessType.FAIL, "Exchange : Invalid output signature");
            }

          } // Outputs

          // Check PoAs.

          if (thisTX.isPOA() && (poasToCheck != null)) {

            poaItems = new ArrayList<>();

            // For each Address...
            for (Entry<String, HashMap<String, HashSet<String>>> addressEntry : poasToCheck.entrySet()) {

              // And for each PoA reference used from that address...
              for (Entry<String, HashSet<String>> referenceEntry : addressEntry.getValue().entrySet()) {

                // Check PoA...
                ReturnTuple poaPerms = checkPoaTransactionPermissions(
                    stateSnapshot,
                    updateTime,
                    referenceEntry.getKey(),
                    attorneyAddress,
                    addressEntry.getKey(),
                    effectiveTxID,
                    referenceEntry.getValue().toArray(new String[0]),
                    1L,
                    checkOnly
                );

                if (poaPerms.success == SuccessType.FAIL) {
                  return poaPerms;
                }

                poaItems.add((PoaItem) poaPerms.returnObject);
              }
            }
          } // Is PoA

          // Set key values.

          contractData.set__address(contractAddress);
          contractData.set__function(thisFunctionName);
          contractData.setNextTimeEvent(updateTime, false);

          // Exit on Check-Only.

          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          couldCorrupt = true;

          // Consume PoAs

          if ((thisTX.isPOA()) && (poaItems != null)) {

            for (PoaItem thisItem : poaItems) {
              thisItem.consume(1L);

              if (thisItem.consumed()) {
                tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);

              }
            }
          }

          // Add Contract to State

          stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());
          stateSnapshot.addLifeCycleEvent(ContractLifeCycle.NEW, contractAddress, contractData.addresses());

          contractsList.add(new ContractEntry(contractAddress, contractData.encode()));

        } // Priority

      } // Chain

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in Exchange.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in Exchange.updatestate.");
    }

  }

}
