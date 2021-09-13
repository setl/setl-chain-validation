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

import static com.google.common.base.Strings.isNullOrEmpty;

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_ADDRESSES;
import static io.setl.common.CommonPy.VersionConstants.VERSION_SET_ADDRESS_METADATA;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.RegisterAddressTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;
import io.setl.common.CommonPy.TxType;

public class RegisterAddress extends TxProcessor<RegisterAddressTx> {

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
  public static ReturnTuple updatestate(RegisterAddressTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    return new RegisterAddress().update(thisTX, stateSnapshot, updateTime, priority, checkOnly);
  }


  /** The address that is creating the address. An address can register itself. */
  private String fromAddress;

  /** True if this transaction came from another chain. */
  private boolean isCrossChain = false;

  /** The address to be created. */
  private String newAddress;


  @Override
  protected Class<RegisterAddressTx> fulfills() {
    return RegisterAddressTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, RegisterAddressTx txi) {
    // the author of the transaction
    fromAddress = txi.getFromAddress();

    // the address to register
    newAddress = txi.getToAddress();
    if (isNullOrEmpty(newAddress)) {
      // If no 'to' address is specified, the address is trying to register itself
      newAddress = fromAddress;
    }
  }


  /**
   * A register address transaction is not applicable if it is at the wrong priority, or for an unknown chain.
   */
  @Override
  protected boolean isNotApplicable(RegisterAddressTx txi, StateSnapshot snapshot, long updateTime, int priority) {
    if (priority != TxType.REGISTER_ADDRESS.getPriority()) {
      return true;
    }

    if (txi.getChainId() == snapshot.getChainId()) {
      // it IS applicable, so it IS NOT not applicable (it's a double negative)
      return false;
    }

    // It is not for the local chain, but it could still be applicable by being from a known foreign chain. If we don't know the chain, it is not applicable.
    isCrossChain = true;
    return snapshot.getXChainSignNodesValue(txi.getChainId()) == null;
  }


  @Override
  protected ReturnTuple tryApply(RegisterAddressTx txi, StateSnapshot snapshot, long updateTime) {
    // All we have to do is create the address and set its meta-data
    int version = snapshot.getVersion();
    AddressEntry newAddressEntry = addDefaultAddressEntry(snapshot.getMerkle(AddressEntry.class), newAddress, version);
    if (version >= VERSION_SET_ADDRESS_METADATA) {
      newAddressEntry.setAddressMetadata(txi.getMetadata());
    }

    return null;
  }


  @Override
  protected ReturnTuple tryApplyPoa(RegisterAddressTx txi, StateSnapshot snapshot, long updateTime) {
    // An address cannot grant a POA before they exist, so there is no way to use a POA to create the address.
    throw new UnsupportedOperationException();
  }


  @Override
  protected ReturnTuple tryChecks(RegisterAddressTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // If the address already exists we simply report success.
    MutableMerkle<AddressEntry> addressEntries = snapshot.getMerkle(AddressEntry.class);
    if (addressEntries.itemExists(newAddress)) {
      return ReturnTuple.pass(checkOnly);
    }

    // If addresses must be registered before use, and the address is not self-registering, then the from address must already be registered
    if (snapshot.getStateConfig().getMustRegister() && !fromAddress.equals(newAddress)) {
      // the from address must already exist
      MerkleRules.checkExists("From Address", addressEntries, fromAddress);
    }

    // We do no further checks on cross chain TXs
    if (isCrossChain) {
      return null;
    }

    // A local chain TX may require on-chain permissions
    AddressRules.checkAddressPermissions(snapshot, fromAddress, TxType.REGISTER_ADDRESS, AP_ADDRESSES);
    return null;
  }


  @Override
  protected ReturnTuple tryChecksPoa(RegisterAddressTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) {
    // An address cannot grant a POA before they exist, so there is no way to use a POA to create the address.
    throw new UnsupportedOperationException();
  }

}
