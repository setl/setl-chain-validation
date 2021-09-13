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

import static com.google.common.base.Strings.isNullOrEmpty;

import static io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned;
import static io.setl.bc.pychain.tx.create.AssetTransferXChain.assetTransferXChainUnsigned;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_COMMIT;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_COMMITS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.CommitContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData.AssetIn;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;

public class Nominate {

  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*
  Note Protocol & metadata are in the contract data as these fields do not exist on the Commit TX itself.

  'function' = nominate : // Java

   'address'        :
   'poa'            :
   'contractaddress':            #
   'contractdata'   :  {
     'contractaddress'    :      #
     'namespace',         :      #
     'class',             :      # or 'assetclass'

     'assetin'  : [[             # or 'assetsin'
                   Amount,       #
                   PublicKey,    # Assumed to be Authoring address if null or Empty.
                   Signature     # [contractaddress, Amount, FromAddress]. Signature only required if FromAddress != Tx Authoring address
                   ], ...
                  ]

     'protocol'           : "",  #
     'metadata'           : ""
     }
  */
  private static final Logger logger = LoggerFactory.getLogger(Nominate.class);


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

        if (priority == thisTX.getPriority()) {

          // TODO : Cope with ContractAddressArray - For Nominate
          String contractAddress = thisTX.getContractAddress().get(0);

          // Check Contract data. Must already exist.

          MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();
          ContractEntry thisContractEntry = contractsList.findAndMarkUpdated(contractAddress);
          if (thisContractEntry == null) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Contract Address must exist");
          }

          // Check From Address
          String authoringAddress = thisTX.getAuthoringAddress();
          if (!AddressUtil.verify(authoringAddress, thisTX.getAuthoringPublicKey(), AddressType.NORMAL)) {
            return new ReturnTuple(SuccessType.FAIL, "`AuthoringAddress` Address and Public key do not match.");
          }

          String effectiveAddress = authoringAddress;
          if (thisTX.isPOA()) {
            effectiveAddress = thisTX.getEffectiveAddress();
            if (!AddressUtil.verifyAddress(effectiveAddress)) {
              return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Invalid POA address {0}", effectiveAddress));
            }
          }

          // Address permissions.
          PoaItem thisItem = null;
          ReturnTuple aPerm;

          // Get TxContract Data
          NominateCommitData txData = (NominateCommitData) thisTX.getCommitmentData();

          if (txData == null) {
            return new ReturnTuple(SuccessType.FAIL, "Nominate : No Commitment Data");
          }

          // Get Contract Data
          TokensNominateContractData contractData = (TokensNominateContractData) thisContractEntry.getContractData();

          if (contractData == null) {
            logger.error("Nominate : ContractData missing for contract {}", contractAddress);
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Nominate : ContractData missing for " + contractAddress);
          }

          if (!CONTRACT_NAME_TOKENS_NOMINATE.equalsIgnoreCase(contractData.get__function())) {
            // Wrong Contract type.
            return new ReturnTuple(SuccessType.FAIL, "Contract type does not match this commit");
          }

          if (contractData.expiry < updateTime) {
            return new ReturnTuple(SuccessType.FAIL, "Nominate : Contract has expired");
          }

          // Check Assets in
          List<AssetIn> assetsIn = txData.getAssetsIn();

          if ((assetsIn == null) || (assetsIn.isEmpty())) {
            return new ReturnTuple(SuccessType.FAIL, "Nominate : Commitment Assets missing.");
          }

          // Validate POA

          if (thisTX.isPOA()) {

            if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
              // Address permissions.
              aPerm = checkPoaAddressPermissions(stateSnapshot, authoringAddress, effectiveAddress, thisTX.getTxType(), effectiveTxID,
                  poaAddressPermissions
              );
              if ((!checkOnly) && (aPerm.success != SuccessType.PASS)) {
                return aPerm;
              }
            }

            // Count assets in for POA check.
            Balance totalInput = BALANCE_ZERO;

            for (AssetIn thisAssetIn : assetsIn) {
              if (thisAssetIn != null) {
                totalInput = totalInput.add(thisAssetIn.amount.divideBy(contractData.blocksizein).multiplyBy(contractData.blocksizein));
              }
            }

            // Check POA Permission.
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), authoringAddress, effectiveAddress,
                effectiveTxID, contractAddress, totalInput, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }

            thisItem = (PoaItem) poaPerms.returnObject;
          } else {
            if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
              long keyPermission = stateSnapshot.getAddressPermissions(effectiveAddress);
              boolean hasPermission = stateSnapshot.canUseTx(effectiveAddress, thisTX.getTxType());

              if ((!hasPermission) && ((keyPermission & AP_COMMIT) == 0)) {
                return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
              }
            }
          }

          // Get namespace details.
          String namespace = contractData.getNamespace();
          String classid = contractData.getOutputtokenclass();
          boolean isIssuer = false;

          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

          // Namespace Exist ?
          // Note in some XChain scenarios, the Namespace definition just might not exist so this is not a show stopper.
          if (namespaceEntry != null) {

            // Namespace Owned ?
            if (namespaceEntry.getAddress().equals(contractData.issuingaddress)) {
              isIssuer = true;

              // Class Details. Check the class exists if issuer.
              if (!namespaceEntry.containsAsset(classid)) {
                return new ReturnTuple(
                    (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                    MessageFormat.format("Class `{0}` is not registered in namespace `{1}`.", classid, namespace)
                );
              }
            }
          }

          // Check Asset locking

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(contractData.getNamespace())) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace is locked : {0}", contractData.getNamespace())
            );
          }

          // In Asset locked ?
          if (stateSnapshot.isAssetLocked(contractData.getInputAssetID())) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Asset `{0}` is locked.", contractData.getInputAssetID())
            );
          }

          // Out Asset locked ?
          if (stateSnapshot.isAssetLocked(contractData.getOutputAssetID())) {
            return new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Asset `{0}` is locked.", contractData.getOutputAssetID())
            );
          }

          // Check Contract Address matches.
          if ((txData.contractAddress != null) && (!txData.contractAddress.isEmpty()) && (!txData.contractAddress.equals(contractData.get__address()))) {
            return new ReturnTuple(SuccessType.FAIL, "Nominate : Commitment and Contract addresses do not match.");
          }

          // Check Asset matches.
          if (!txData.getAssetID().equals(contractData.getInputAssetID())) {
            return new ReturnTuple(SuccessType.FAIL, "Nominate : Commitment and Contract Assets do not match.");
          }

          // Check Assets in

          MessageSignerVerifier verifier = MessageVerifierFactory.get();

          for (AssetIn thisAssetIn : assetsIn) {

            if (thisAssetIn == null) {
              continue;
            }

            if ((thisAssetIn.amount == null) || (thisAssetIn.amount.lessThanEqualZero())) {
              return new ReturnTuple(SuccessType.FAIL, "Nominate : Commitment amount invalid.");
            }

            // No Public Key specified or 'PublicKey' == TxAddress then no signature is required (unless given)

            if (isNullOrEmpty(thisAssetIn.signature)) {

              if (isNullOrEmpty(thisAssetIn.getPublicKey())
                  || (thisAssetIn.getPublicKey().equalsIgnoreCase(effectiveAddress))
                  || (thisAssetIn.getAddress().equalsIgnoreCase(effectiveAddress))) {

                // No Signature is OK if there is no public key (Address is Effective Address) or Effective address is given.
                continue;
              }

            }

            if (thisAssetIn.getPublicKey() == null) {
              return new ReturnTuple(SuccessType.FAIL, "Nominate : Commitment Public key missing.");
            }

            // OK, at this point thisAssetIn.publicKey has meaning, it is not blank and it is not relating to the effective address

            sigMessage = thisAssetIn.stringToHashToSign(contractAddress, authoringAddress, thisTX.getNonce());

            if (!verifier.verifySignature(sigMessage, thisAssetIn.getPublicKey(), thisAssetIn.signature)) {
              return new ReturnTuple(SuccessType.FAIL, "Nominate : Commitment signature invalid.");
            }

          } // for

          // OK , go ahead

          couldCorrupt = true;

          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

          AddressEntry hostAddressEntry;
          AddressEntry clientAddressEntry;
          hostAddressEntry = assetBalances.findAndMarkUpdated(contractData.issuingaddress);
          HashMap<String, Balance> cumulativeInput = new HashMap<>();
          HashMap<String, AddressEntry> clientBalances = new HashMap<>();
          String addressName;

          Balance inputBlockCount;
          Balance inputTokenAmount;
          Balance outputTokenAmount;
          Balance totalAddressInputTokenAmount;
          Balance totalContractOutputTokenAmount = BALANCE_ZERO;

          if (hostAddressEntry == null) {
            return new ReturnTuple(SuccessType.FAIL, "Nominate : Issuing Address is not in state.");
          }

          // Check Asset availability, tracking cumulative transfers.

          for (AssetIn thisAssetIn : assetsIn) {

            if (thisAssetIn == null) {
              continue;
            }

            // Resolve client address.

            if (isNullOrEmpty(thisAssetIn.getPublicKey()) || (thisAssetIn.getPublicKey().equalsIgnoreCase(effectiveAddress))) {
              addressName = effectiveAddress;
            } else {
              addressName = thisAssetIn.getAddress();
            }

            // Get new or cached Address Entry

            clientAddressEntry = clientBalances.get(addressName);

            if (clientAddressEntry == null) {
              clientAddressEntry = assetBalances.findAndMarkUpdated(addressName);

              // Check it.

              if (clientAddressEntry == null) {
                return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Nominate : Client Address is not in state.");
              }

              // Cache it.
              clientBalances.put(addressName, clientAddressEntry);
            }

            totalAddressInputTokenAmount = cumulativeInput.getOrDefault(addressName, BALANCE_ZERO);

            inputBlockCount = thisAssetIn.amount.divideBy(contractData.blocksizein); // intermediate //
            outputTokenAmount = inputBlockCount.multiplyBy(contractData.blocksizeout);
            inputTokenAmount = inputBlockCount.multiplyBy(contractData.blocksizein);

            totalAddressInputTokenAmount = totalAddressInputTokenAmount.add(inputTokenAmount);
            totalContractOutputTokenAmount = totalContractOutputTokenAmount.add(outputTokenAmount);

            cumulativeInput.put(addressName, totalAddressInputTokenAmount);

            if (clientAddressEntry.getAssetBalance(contractData.getInputAssetID()).compareTo(totalAddressInputTokenAmount) < 0) {
              // Insufficient Input Token
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Nominate : Client address has insufficient asset balance.");
            }

          }

          // Check Issuer balance.

          if ((!isIssuer)
              && (hostAddressEntry.getAssetBalance(contractData.getOutputAssetID()).lessThan(totalContractOutputTokenAmount))) {
            // Insufficient Output Token
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Nominate : Issuing address has insufficient asset balance.");
          }

          // OK.

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          couldCorrupt = true;

          // Set Client account(s).

          Balance totalInputTokenAmount = BALANCE_ZERO;
          Balance totalOutputTokenAmount = BALANCE_ZERO;
          String thisAddressName;
          AbstractTx effectiveTX;
          Balance thisAmount;

          for (Entry<String, Balance> cumulativeInputEntry : cumulativeInput.entrySet()) {

            thisAddressName = cumulativeInputEntry.getKey();
            inputTokenAmount = cumulativeInputEntry.getValue();

            clientAddressEntry = clientBalances.get(thisAddressName);

            outputTokenAmount = inputTokenAmount.divideBy(contractData.blocksizein).multiplyBy(contractData.blocksizeout);

            thisAmount = clientAddressEntry.getAssetBalance(contractData.getInputAssetID()).subtract(inputTokenAmount);
            clientAddressEntry.setAssetBalance(contractData.getInputAssetID(), thisAmount);
            clientAddressEntry.setAssetBalance(
                contractData.getOutputAssetID(),
                clientAddressEntry.getAssetBalance(contractData.getOutputAssetID()).add(outputTokenAmount)
            );

            totalInputTokenAmount = totalInputTokenAmount.add(inputTokenAmount);
            totalOutputTokenAmount = totalOutputTokenAmount.add(outputTokenAmount);

            // Effective TX
            // Client to Nominate

            effectiveTX = assetTransferXChainUnsigned(
                stateSnapshot.getChainId(),
                0,
                "",
                thisAddressName,
                contractData.getNamespace(),
                contractData.getInputtokenclass(),
                stateSnapshot.getChainId(),
                contractData.issuingaddress,
                inputTokenAmount,
                contractAddress,
                thisTX.getHash(),
                ""
            );

            stateSnapshot.addEffectiveTX(effectiveTX);

            // Effective TX
            // Nominate to Client

            if (isIssuer) {

              effectiveTX = assetIssueUnsigned(
                  stateSnapshot.getChainId(),
                  0,
                  "",
                  contractData.issuingaddress,
                  contractData.getNamespace(),
                  contractData.getOutputtokenclass(),
                  thisAddressName,
                  outputTokenAmount,
                  contractAddress,
                  thisTX.getHash(),
                  ""
              );

            } else {

              effectiveTX = assetTransferXChainUnsigned(
                  stateSnapshot.getChainId(),
                  0,
                  "",
                  contractData.issuingaddress,
                  contractData.getNamespace(),
                  contractData.getOutputtokenclass(),
                  stateSnapshot.getChainId(),
                  thisAddressName,
                  outputTokenAmount,
                  contractAddress,
                  thisTX.getHash(),
                  ""
              );

            }

            stateSnapshot.addEffectiveTX(effectiveTX);
          }

          // Set Host Account

          // For the input asset, if you are not the Issuer, then the balance is positive, if you are the issuer then
          // you do not want to 'tidy' zero balance entries anyway.

          thisAmount = hostAddressEntry.getAssetBalance(contractData.getInputAssetID()).add(totalInputTokenAmount);
          hostAddressEntry.setAssetBalance(contractData.getInputAssetID(), thisAmount);

          // For the output asset, 'tidy' zero balance entries, unless you are the issuer.

          thisAmount = hostAddressEntry.getAssetBalance(contractData.getOutputAssetID()).subtract(totalOutputTokenAmount);
          hostAddressEntry.setAssetBalance(contractData.getOutputAssetID(), thisAmount);

          // Update POA

          if (thisTX.isPOA() && (thisItem != null)) {
            thisItem.consume(totalInputTokenAmount);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), effectiveAddress);
            }
          }

          // housekeeping

          cumulativeInput.clear();
          clientBalances.clear();

          stateSnapshot.addLifeCycleEvent(ContractLifeCycle.COMMIT, contractAddress, txData.addresses());

        } // priority
      }

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in NewNominate.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in NewNominate.updatestate.");
    }

  }

}
