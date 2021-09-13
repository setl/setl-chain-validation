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

import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaAddressPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.checkPoaTransactionPermissions;
import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CONTRACTS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.tx.NewContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

public class TokensNominate {

  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*
  'function' = tokens_nominate :

       {
       'namespace'        :
       'inputtokenclass'  : String,         #
       'outputtokenclass' : String,         #
       'blocksizein'      : Int,            # Integer, > 0
       'blocksizeout'     :
       'events'        : [                  # List of events that can occur.
                            'expiry'
                         ],
       'expiry'        : nnn,               # long(UTC Unix Epoch time, seconds)
       'protocol'      : "",                # User Data
       'metadata'      : ""

       }
  */
  private static final Logger logger = LoggerFactory.getLogger(TokensNominate.class);


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
  public static ReturnTuple updatestate(NewContractInterface thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;
    final String thisFunctionName = CONTRACT_NAME_TOKENS_NOMINATE;
    final TxType effectiveTxID = TxType.NEW_CONTRACT;
    long poaAddressPermissions = AP_CONTRACTS;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Verification : Apply only to the 'Native' chain...

        if ((thisTX.getTimestamp() > 0) && (Math.abs(thisTX.getTimestamp() - updateTime) > stateSnapshot.getStateConfig().getMaxTxAge())) {
          return new ReturnTuple(SuccessType.FAIL, "Tx Timestamp invalid.");
        }

        if (priority == thisTX.getPriority()) {

          String contractAddress = thisTX.getContractAddress();

          // Check Contract data. Must not already exist.

          MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();
          if (contractsList.find(contractAddress) != null) {
            return new ReturnTuple(SuccessType.FAIL, "Contract already exists..");
          }

          // Check Attorney Address
          String attorneyAddress = thisTX.getAuthoringAddress();
          if (!AddressUtil.verify(attorneyAddress, thisTX.getAuthoringPublicKey(), AddressType.NORMAL)) {
            return new ReturnTuple(SuccessType.FAIL, "`From` Address and Public key do not match.");
          }

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
                return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
              }
            }
          }
          // Get Contract Data
          TokensNominateContractData contractData = new TokensNominateContractData((TokensNominateContractData) thisTX.getContractData());

          // issuingaddress, set it in the map explicitly.
          contractData.issuingaddress = poaAddress;

          // Validate contractData
          if ((contractData.getNamespace() == null) || (contractData.getInputtokenclass() == null) || (contractData.getOutputtokenclass() == null)) {
            return new ReturnTuple(SuccessType.FAIL, "Tokens : Contract Assets are null");
          }

          if ((contractData.getNamespace().length() == 0)
              || (contractData.getInputtokenclass().length() == 0)
              || (contractData.getOutputtokenclass().length() == 0)) {
            return new ReturnTuple(SuccessType.FAIL, "Tokens : Contract Assets are not given");
          }

          if ((contractData.blocksizein.lessThan(1L)) || (contractData.blocksizeout.lessThan(1L))) {
            return new ReturnTuple(SuccessType.FAIL, "Tokens : Invalid Block Size");
          }

          // Contract must have an expiry date.
          if (contractData.expiry < updateTime) {
            return new ReturnTuple(SuccessType.FAIL, "Tokens : Contract is past expiry date");
          }

          contractData.set__address(contractAddress);
          contractData.set__function(thisFunctionName);
          contractData.setNextTimeEvent(updateTime, false);

          // Check POA Permission.
          PoaItem thisItem = null;
          if (thisTX.isPOA()) {
            ReturnTuple poaPerms = checkPoaTransactionPermissions(stateSnapshot, updateTime, thisTX.getPoaReference(), attorneyAddress, poaAddress,
                effectiveTxID, new String[]{contractData.getInputAssetID(), contractData.getOutputAssetID()}, 1L, checkOnly
            );
            if (poaPerms.success == SuccessType.FAIL) {
              return poaPerms;
            }
            thisItem = (PoaItem) poaPerms.returnObject;
          }

          // Exit on Check-Only.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          couldCorrupt = true;

          if ((thisTX.isPOA()) && (thisItem != null)) {
            thisItem.consume(1L);

            if (thisItem.consumed()) {
              tidyPoaReference(stateSnapshot, updateTime, thisTX.getPoaReference(), poaAddress);

            }
          }

          stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());
          stateSnapshot.addLifeCycleEvent(ContractLifeCycle.NEW, contractAddress, contractData.addresses());

          contractsList.add(new ContractEntry(contractAddress, contractData.encode()));
        }
      }

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in TokensNominate.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in TokensNominate.updatestate.");
    }

  }

}
