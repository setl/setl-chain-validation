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

import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_BONDING;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.UnBondTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;

public class UnBond {

  private static final Logger logger = LoggerFactory.getLogger(UnBond.class);


  /**
   * decrementSignode.
   * <p>Utility function to reduce Signode balance and increment the Signode Address nonce.</p>
   *
   * @param signodeEntry   :
   * @param amount         : Stake amount.
   * @param incrementNonce : Boolean.
   *
   * @return : boolean, success.
   */
  public static boolean decrementSignode(SignNodeEntry signodeEntry, Number amount, boolean incrementNonce) {

    if (signodeEntry == null) {
      return false;
    }

    if (signodeEntry.getBalance().lessThan(amount)) {
      return false;
    }

    signodeEntry.decrementBalance(amount);

    if (incrementNonce) {
      // Note, at present, Nonces for Unbonding appear to be treated as normal nonces, a 'normal' address for the
      // Sig Key is created and updated. This is not strictly as originally intended, but is consistent with Python.
      // Thus, this Nonce Update is only for appearances.
      signodeEntry.incrementNonce();
    }

    return true;
  }


  /**
   * updatestate.
   * <p>Apply an AssetTransfer TX to State.</p>
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
  public static ReturnTuple updatestate(UnBondTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    boolean couldCorrupt = false;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Native chain

        // Verification : Apply only to the 'Native' chain...

        if (priority == thisTX.getPriority()) {

          String fromAddress = thisTX.getFromAddress();

          // Address permissions.
          if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
            long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
            boolean hasPermission = stateSnapshot.canUseTx(fromAddress, thisTX.getTxType());
            if ((!hasPermission) && ((keyPermission & AP_BONDING) == 0)) {
              return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "Inadequate Address permissioning");
            }
          }

          //# Do not enforce Namespace / Asset existence. For xChain assets, the Namespace or Class may not exist here (bad practice I think)
          //# For normal chain operation, if the asset balance exists, then the Namespace & Class must also !

          // Namespace ...
          String namespace = "SYS";
          String classid = "STAKE";

          final String fullAssetID = namespace + "|" + classid;

          // Namespace locked ?
          if (stateSnapshot.isAssetLocked(namespace)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Namespace is locked : {0}", namespace));
          }

          // Asset locked ?
          if (stateSnapshot.isAssetLocked(fullAssetID)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("Asset `{0}` is locked.", fullAssetID));
          }

          Balance amount = new Balance(thisTX.getAmount());
          if (amount.lessThanZero()) {
            return new ReturnTuple(SuccessType.FAIL, "Invalid Tx Amount");
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // As of this point, Returntype is FAIL not WARNING as checkOnly is always false and the conditional test annoys sonarqube.

          MutableMerkle<SignNodeEntry> signNodes = stateSnapshot.getSignNodes();
          SignNodeEntry fromSignodeEntry = signNodes.findAndMarkUpdated(thisTX.getFromPublicKey());

          if (fromSignodeEntry == null) {
            return new ReturnTuple(SuccessType.FAIL, "'From' Signing Key does not exist in state.");
          }

          String returnAddress = thisTX.getToAddress();
          if ((returnAddress == null) || (returnAddress.isEmpty())) {
            returnAddress = fromSignodeEntry.getReturnAddress();
          }

          MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();
          AddressEntry toAddressEntry = assetBalanceTree.findAndMarkUpdated(returnAddress);

          // Check that the recipient Address is OK. For Stake, must always exist !
          if (toAddressEntry == null) {
            return new ReturnTuple(
                SuccessType.FAIL,
                MessageFormat.format("Target Address `{0}` does not exist in state. MustRegister. Tx Hash {1}", returnAddress, thisTX.getHash())
            );
          }

          // Check not the last stake...

          final Balance[] power = {new Balance(0L)};

          stateSnapshot.getSignNodes().forEach(n -> power[0] = power[0].add(n.getBalance()));

          if (power[0].lessThanEqualTo(amount.getValue())) {
            // Available Stake is <= removed stake : Fail - There can not be no stake left !
            return new ReturnTuple(
                SuccessType.FAIL,
                MessageFormat.format("Can not UNBOND, There would be no STAKE remaining. TX {0}, PubKey {1}, Amount {2}", thisTX.getHash(),
                    thisTX.getFromPublicKey(), thisTX.getAmount().toString()
                )
            );
          }

          //

          couldCorrupt = true;

          if (decrementSignode(fromSignodeEntry, amount.getValue(), true)) {
            // If Decrement Signode OK, Update the recipient Address.

            if (!amount.equalTo(0L)) {

              // Set new balance.
              Balance bTo = toAddressEntry.getAssetBalance(fullAssetID);
              toAddressEntry.setAssetBalance(fullAssetID, bTo.add(amount));
            }

            //
            if (fromSignodeEntry.getBalance().equalTo(BALANCE_ZERO)) {
              signNodes.delete(thisTX.getFromPublicKey());
            }

          } else {
            return new ReturnTuple(SuccessType.FAIL, "Failed to update Signing Key entry.");
          }

        } // Priority

      } else {

        XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(thisTX.getChainId());

        // Can only unbond if the chain is known, and the public key is recognized
        String publicKey = thisTX.getFromPublicKey();
        if (xChainDetails != null && publicKey != null) {
          Balance unbondedAmount = new Balance(thisTX.getAmount());
          Map<String, Balance> existingSignNodes = xChainDetails.getSignNodes();

          Balance currentBalance = existingSignNodes.get(publicKey);
          if (currentBalance != null) {
            // There is something to unbond
            Map<String, Balance> newSignNodes = new TreeMap<>(existingSignNodes);

            if (currentBalance.greaterThan(unbondedAmount)) {
              // Holding is reduced
              Balance newHolding = currentBalance.subtract(unbondedAmount);
              newSignNodes.put(publicKey, newHolding);
            } else {
              // Holding is removed
              newSignNodes.remove(publicKey);
            }

            stateSnapshot.setXChainSignNodesValue(thisTX.getChainId(), xChainDetails.setSignNodes(newSignNodes));
          }
        } // if xChainDetails
      } // Else ChainID

      return new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in UnBond.updatestate()", e);

      return new ReturnTuple(SuccessType.FAIL, "Error in UnBond.updatestate.");

    }
  }

}
