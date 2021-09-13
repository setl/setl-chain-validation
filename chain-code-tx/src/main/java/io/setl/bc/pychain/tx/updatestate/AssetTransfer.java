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
import static io.setl.common.StringUtils.cleanString;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AssetTransferTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.common.Balance;

public class AssetTransfer extends TxProcessor<AssetTransferTx> {

  private static final Logger logger = LoggerFactory.getLogger(AssetTransfer.class);


  /**
   * updatestate. <p>Apply an AssetTransfer TX to State.</p>
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(AssetTransferTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    return new AssetTransfer().update(thisTX, stateSnapshot, updateTime, priority, checkOnly);
  }


  @Override
  protected Class<AssetTransferTx> fulfills() {
    return AssetTransferTx.class;
  }


  @Override
  protected ReturnTuple tryApply(AssetTransferTx txi, StateSnapshot snapshot, long updateTime) throws TxFailedException {
    // Create the default addresses if necessary
    String toAddress = txi.getToAddress();
    String fromAddress = txi.getFromAddress();
    AddressRules.checkAndCreateAddresses(snapshot, Set.of(toAddress, fromAddress));

    final String fullAssetID = cleanString(txi.getNameSpace()) + "|" + cleanString(txi.getClassId());
    Number amount = txi.getAmount();

    // Check from address has sufficient assets to make the transfer
    MutableMerkle<AddressEntry> assetBalances = snapshot.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(fromAddress);

    // Check Asset present
    Balance fromTotal = fromAddressEntry.getAssetBalance(fullAssetID);
    Balance encumbered = BALANCE_ZERO;
    AddressEncumbrances thisAddressEncumbrance = snapshot.getEncumbrances().find(fromAddress);
    if (thisAddressEncumbrance != null) {
      encumbered = thisAddressEncumbrance.getEncumbranceTotal(fromAddress, fullAssetID, updateTime);
    }

    if (fromTotal.lessThan(encumbered.add(amount))) {
      throw fail("Insufficient un-encumbered asset available.");
    }

    // Asset can be transferred
    fromAddressEntry.setAssetBalance(fullAssetID, fromTotal.subtract(amount));
    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(toAddress);
    Balance toTotal = toAddressEntry.getAssetBalance(fullAssetID);
    toAddressEntry.setAssetBalance(fullAssetID, toTotal.add(amount));

    logger.trace("AssetTransferTx from:{} to:{}", fromTotal, toTotal);

    // all done
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(AssetTransferTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Check the addresses
    String toAddress = AddressRules.checkAddress(snapshot, txi.getToAddress());
    String fromAddress = AddressRules.checkAddress(snapshot, txi.getFromAddress());
    if (fromAddress.equals(toAddress)) {
      throw fail("`From` and `To` Addresses are the same.");
    }

    // Check the asset's namespace is not locked
    String namespace = cleanString(txi.getNameSpace());
    if (snapshot.isAssetLocked(namespace)) {
      throw fail("Namespace is locked : {0}", namespace);
    }

    // Check the asset itself is not locked
    String classid = cleanString(txi.getClassId());
    final String fullAssetID = namespace + "|" + classid;
    if (snapshot.isAssetLocked(fullAssetID)) {
      throw fail("Asset `{0}` is locked.", fullAssetID);
    }

    Number amount = txi.getAmount();
    if ((new Balance(amount)).lessThanZero()) {
      throw fail("Invalid amount (<= 0)");
    }

    // Looks good
    return null;
  }

}
