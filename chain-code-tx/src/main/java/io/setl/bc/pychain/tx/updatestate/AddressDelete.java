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

import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_ADDRESSES;
import static io.setl.common.StringUtils.logSafe;

import java.util.Map;
import java.util.Optional;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.AddressDeleteTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.PoaRules;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;

public class AddressDelete extends TxProcessor<AddressDeleteTx> {

  /**
   * updatestate.
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   *
   * @return :
   */
  public static ReturnTuple updatestate(AddressDeleteTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    return new AddressDelete().update(thisTX, stateSnapshot, updateTime, priority, checkOnly);
  }


  private PoaItem poaItem = null;


  private void checkAddressCanBeDeleted(StateSnapshot snapshot, long updateTime, String poaAddress) throws TxFailedException {
    // Check address time out
    MutableMerkle<AddressEntry> assetBalances = snapshot.getMerkle(AddressEntry.class);
    AddressEntry addressEntry = assetBalances.find(poaAddress);
    Long addressTime = addressEntry.getUpdateTime();
    if (addressTime == null) {
      addressTime = 0L;
    }

    long addressAge = updateTime - addressTime;
    long minimumAge = snapshot.getStateConfig().getMinAddressAgeToDelete();
    if (addressAge < minimumAge) {
      throw fail("Address has not been unused for long enough to delete : {0}, {1} seconds of {2}", poaAddress, addressAge, minimumAge);
    }

    // Check for No / Only zero balances.
    Map<String, Balance> classBalance = addressEntry.getClassBalance();
    if (classBalance != null) {
      Optional<Balance> nonZeroBalance = classBalance.values().stream().filter(balance -> !(balance == null || balance.equalZero())).findAny();
      if (nonZeroBalance.isPresent()) {
        throw fail("Address has remaining balances.");
      }
    }

    // Check if owner of a namespace. This requires a scan of state, unfortunately.
    MutableMerkle<NamespaceEntry> namespaces = snapshot.getMerkle(NamespaceEntry.class);
    for (NamespaceEntry entry : namespaces) {
      if (entry != null && poaAddress.equals(entry.getAddress())) {
        throw fail("Address {0} cannot be deleted because it owns namespace {1}", poaAddress, logSafe(entry.getKey()));
      }
    }
  }


  @Override
  protected Class<AddressDeleteTx> fulfills() {
    return AddressDeleteTx.class;
  }


  @Override
  protected ReturnTuple tryApply(AddressDeleteTx txi, StateSnapshot snapshot, long updateTime) throws TxFailedException {
    String poaAddress = txi.getPoaAddress();

    // Delete all encumbrances associated with the address
    snapshot.getMerkle(AddressEncumbrances.class).delete(poaAddress);

    // Delete all PoAs
    MutableMerkle<PoaEntry> poaEntries = snapshot.getMerkle(PoaEntry.class);
    PoaEntry addressPOA = poaEntries.findAndMarkUpdated(poaAddress);
    if (addressPOA != null) {
      for (PoaHeader header : addressPOA.getHeaders()) {
        String fullReference = addressPOA.getFullReference(header.reference);
        poaEntries.delete(fullReference);
      }
      poaEntries.delete(addressPOA.getKey());
    }

    // Delete the address
    snapshot.getMerkle(AddressEntry.class).delete(poaAddress);
    return null;
  }


  @Override
  protected ReturnTuple tryApplyPoa(AddressDeleteTx txi, StateSnapshot snapshot, long updateTime) throws TxFailedException {
    tryApply(txi, snapshot, updateTime);

    poaItem.consume(1L);
    if (poaItem.consumed()) {
      tidyPoaReference(snapshot, updateTime, txi.getPoaReference(), txi.getPoaAddress());
    }

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(AddressDeleteTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    String attorneyAddress = txi.getFromAddress();
    String poaAddress = txi.getPoaAddress();

    AddressRules.checkAddressPermissions(snapshot, attorneyAddress, TxType.DELETE_ADDRESS, AP_ADDRESSES);

    checkAddressCanBeDeleted(snapshot, updateTime, poaAddress);
    return null;
  }


  @Override
  protected ReturnTuple tryChecksPoa(AddressDeleteTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    String attorneyAddress = txi.getFromAddress();
    String poaAddress = txi.getPoaAddress();
    String poaReference = txi.getPoaReference();

    PoaRules.checkPoaAddressPermissions(snapshot, attorneyAddress, poaAddress, txi.getTxType(), TxType.DELETE_ADDRESS, AP_ADDRESSES);
    poaItem = PoaRules.checkPoaTransactionPermissions(
        snapshot, updateTime, poaReference, attorneyAddress,
        poaAddress, TxType.DELETE_ADDRESS, poaAddress, 1L,
        checkOnly
    );

    checkAddressCanBeDeleted(snapshot, updateTime, poaAddress);
    return null;
  }

}
