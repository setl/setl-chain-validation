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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultNonceEntry;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.StringUtils.cleanString;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.StockSplitTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;


public class StockSplit {

  private static final Logger logger = LoggerFactory.getLogger(StockSplit.class);


  /**
   * updatestate.
   * Process Stoc Split / Consolidation : Demo use only !
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   *
   * @return :
   */
  @SuppressWarnings("squid:S2159") // Suppress '.equals on different types' warning. The Balance class overrides and allows.
  public static ReturnTuple updatestate(StockSplitTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {

    StateSnapshot snapshotWrap = null;
    ReturnTuple rVal = null;

    try {

      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        // Native chain

        // Verification : Apply only to the 'Native' chain...

        // OK, Update checks and update ...

        if (priority == thisTX.getPriority()) {

          String fromAddress = thisTX.getFromAddress();

          // Namespace ...
          String namespace = cleanString(thisTX.getNameSpace());

          // Get namespace details.
          MutableMerkle<NamespaceEntry> namespaceTree = stateSnapshot.getNamespaces();
          NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

          // Namespace Exist ?
          if (namespaceEntry == null) {
            rVal = new ReturnTuple(
                (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
                MessageFormat.format("Namespace `{0}` has not been registered.", namespace)
            );
            return rVal;
          }

          // Namespace Owned ?
          if (!fromAddress.equals(namespaceEntry.getAddress())) {
            rVal = new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), namespace + " is not controlled by the Tx `From` Address");
            return rVal;
          }

          String classid = cleanString(thisTX.getClassId());
          final String fullAssetID = namespace + "|" + classid;

          // Class Details.
          if (!namespaceEntry.containsAsset(classid)) {
            rVal = new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), classid + " asset is not defined");
            return rVal;
          }
          // Other parameters

          double ratio = thisTX.getRatio();

          // Amount negative ?
          if (ratio < 0D) {
            rVal = new ReturnTuple(SuccessType.FAIL, " Zero Stock-Split ratio.");
            return rVal;
          }

          // If only checking, not updating, this is the time to exit.
          if (checkOnly) {
            rVal = new ReturnTuple(SuccessType.PASS, "Check Only.");
            return rVal;
          }

          // OK Get account information :

          snapshotWrap = stateSnapshot.createSnapshot();

          MutableMerkle<AddressEntry> assetBalanceTree = snapshotWrap.getAssetBalances();

          logger.info("Look:{} in addresses", fullAssetID);

          // Get From / To Address

          AddressEntry fromAddressEntry = assetBalanceTree.findAndMarkUpdated(fromAddress);
          if (fromAddressEntry == null) {
            fromAddressEntry = addDefaultNonceEntry(assetBalanceTree, fromAddress, stateSnapshot.getVersion());
          }

          Balance cumulativeCreation = BALANCE_ZERO;
          Balance existingBalance;
          BigDecimal newDoubleBalance;
          Balance newLongBalance;

          for (AddressEntry toAddressEntry : assetBalanceTree) {
            if (!toAddressEntry.getAddress().equalsIgnoreCase(fromAddress)) {
              if (!toAddressEntry.getAssetBalance(fullAssetID).equalZero()) {
                toAddressEntry = assetBalanceTree.findAndMarkUpdated(toAddressEntry.getAddress());
                existingBalance = toAddressEntry.getAssetBalance(fullAssetID);

                newDoubleBalance = (new BigDecimal(existingBalance.bigintValue()).multiply(BigDecimal.valueOf(ratio)))
                    .round(new MathContext(0, RoundingMode.HALF_UP));

                // if (newDoubleBalance > Long.MAX_VALUE) {
                //   rVal = new ReturnTuple(false, "Stock split would exceed Maximum allowed balance value.");
                //   return rVal;
                //}

                newLongBalance = new Balance(newDoubleBalance.toBigInteger());
                cumulativeCreation = cumulativeCreation.add(newLongBalance.subtract(existingBalance));
                toAddressEntry.setAssetBalance(fullAssetID, newLongBalance);
              }
            }
          }

          if (!cumulativeCreation.equalTo(0L)) {
            Balance toTotal = fromAddressEntry.getAssetBalance(fullAssetID).subtract(cumulativeCreation);
            fromAddressEntry.setAssetBalance(fullAssetID, toTotal);
          }

          logger.info("StockSplitTx, Hash {}", thisTX.getHash());
        }

      }

      rVal = new ReturnTuple(SuccessType.PASS, "");

    } catch (Exception e) {
      logger.error("Error in StockSplit.updatestate()", e);
      rVal = new ReturnTuple(SuccessType.FAIL, "Error in StockSplit.updatestate.");

    } finally {

      if ((!checkOnly) && (rVal != null) && (rVal.success == SuccessType.PASS) && (snapshotWrap != null)) {
        try {
          snapshotWrap.commit();
        } catch (StateSnapshotCorruptedException e) {
          logger.error("State snapshot was corrupted {}", e.getMessage());
          rVal = new ReturnTuple(SuccessType.FAIL, "State corruption in StockSplit.updatestate.");
        }
      }

    }

    return rVal;

  }

}
