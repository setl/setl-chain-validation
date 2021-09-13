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

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.BondTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;

public abstract class Bond {

  private static final Logger logger = LoggerFactory.getLogger(Bond.class);


  /**
   * addDefaultSignodeEntry.
   * <p>Utility method to add a default Signode record.
   * This function may be called in multiple places and exists to simplify and standardise this process.</p>
   *
   * @param signNodes        :
   * @param signodePublicKey :
   *
   * @return :
   */
  public static SignNodeEntry addDefaultSignodeEntry(MutableMerkle<SignNodeEntry> signNodes, String signodePublicKey) {

    SignNodeEntry signodeEntry = new SignNodeEntry(signodePublicKey, "", 0, 0, 0, 0);

    signNodes.add(signodeEntry);

    return signodeEntry;
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
  public static ReturnTuple updatestate(BondTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    /*
    Note : Encumbrances are checked, but not consumed.
     */
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
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "SYS namespace is locked");
          }

          // Asset locked ?
          if (stateSnapshot.isAssetLocked(fullAssetID)) {
            return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), "STAKE asset is locked");
          }

          Balance amount = new Balance(thisTX.getAmount());
          if (amount.lessThanZero()) {
            return new ReturnTuple(SuccessType.FAIL, "Invalid Tx Amount");
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            return new ReturnTuple(SuccessType.PASS, "Check Only.");
          }

          // Get / Check From Address.
          MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

          logger.trace("Look:{} in addresses", fullAssetID);

          AddressEntry fromAddressEntry;

          fromAddressEntry = assetBalances.findAndMarkUpdated(fromAddress);

          if (fromAddressEntry == null) {
            return new ReturnTuple(
                (SuccessType.FAIL),
                MessageFormat.format("From Address `{0}` does not exist in state.", fromAddress)
            );
          }

          // Check Asset present
          Balance encumbrance = BALANCE_ZERO;

          AddressEncumbrances thisAddressEncumbrance = stateSnapshot.getEncumbrances().find(fromAddress);
          if (thisAddressEncumbrance != null) {
            encumbrance = thisAddressEncumbrance.getEncumbranceTotal(fromAddress, fullAssetID, updateTime);
          }

          Balance fromAmount = fromAddressEntry.getAssetBalance(fullAssetID);

          if (fromAmount.lessThan(encumbrance.add(amount))) {
            return new ReturnTuple(SuccessType.FAIL, "Insufficient un-encumbered asset available.");
          }

          // Get / Check 'To' Key
          MutableMerkle<SignNodeEntry> signNodes = stateSnapshot.getSignNodes();

          SignNodeEntry toSignodeEntry = signNodes.findAndMarkUpdated(thisTX.getToSignodePubKey());
          if (toSignodeEntry == null) {

            couldCorrupt = true;
            toSignodeEntry = addDefaultSignodeEntry(signNodes, thisTX.getToSignodePubKey());

          }

          // Transfer Assets :
          couldCorrupt = true;

          // OK, Set new balances.
          Balance newAmount = fromAmount.subtract(amount);
          fromAddressEntry.setAssetBalance(fullAssetID, newAmount);

          toSignodeEntry.incrementBalance(amount);
          toSignodeEntry.setReturnAddress(thisTX.getReturnAddress());

          logger.trace("BondTx from:{}", fromAmount.getValue());

        }

      } else {

        XChainDetails xChainDetails = stateSnapshot.getXChainSignNodesValue(thisTX.getChainId());

        if (xChainDetails != null) {

          Map<String, Balance> newSignodes = new TreeMap<>(xChainDetails.getSignNodes());

          String thisPubKey = thisTX.getToSignodePubKey();

          if (thisPubKey != null) {

            Balance thisBondAmount = new Balance(thisTX.getAmount());

            Balance existingBalance = newSignodes.get(thisPubKey);
            if (existingBalance != null) {
              newSignodes.put(thisPubKey, existingBalance.add(thisBondAmount));
            } else {
              newSignodes.put(thisPubKey, thisBondAmount);
            }

            couldCorrupt = true;

            stateSnapshot.setXChainSignNodesValue(thisTX.getChainId(), xChainDetails.setSignNodes(newSignodes));
          }
        }

      }

      return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

    } catch (Exception e) {
      if (couldCorrupt) {
        stateSnapshot.setCorrupted(true, thisTX.getHash());
      }

      logger.error("Error in Bond.updatestate()", e);

      return new ReturnTuple(SuccessType.FAIL, "Error in Bond.Updatestate");
    }

  }


}
