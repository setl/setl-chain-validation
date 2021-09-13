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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_DELETE_ADDRESS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import java.time.Instant;
import java.util.EnumSet;

import org.junit.Test;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.NamespaceRegisterTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddressDelete;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;

public class AddressDeleteTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX;

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);

    assertNotNull(fromAddressEntry); // Address Exists
    assertEquals(fromAddressEntry.getAddress(), address); // Address is correct
    assertTrue(fromAddressEntry.getClassBalance().isEmpty()); // There are no assets

    // Now delete

    thisTX = AddressDelete.addressDeleteUnsigned(chainID, addressNonce, pubKey, address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);

    assertNull(fromAddressEntry); // Address Not Exist

    // Reset
    s0 = state1.createSnapshot();

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // Fail on Timestamp

    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Bad address
    thisTX = AddressDelete.addressDeleteUnsigned(chainID, addressNonce, pubKey, "BadAddress", "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check address still exists
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);

    assertNotNull(fromAddressEntry); // Address Exists
    assertEquals(fromAddressEntry.getAddress(), address); // Address is correct
    assertTrue(fromAddressEntry.getClassBalance().isEmpty()); // There are no assets

    s0.setConfigValue("authorisebyaddress", 1);

    thisTX = AddressDelete.addressDeleteUnsigned(chainID, addressNonce, pubKey, address, "", "", "");

    // Insufficient Address permissions (By TX ID)
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromAddressEntry.setAddressPermissions(AP_TX_LIST);
    fromAddressEntry.setAuthorisedTx(EnumSet.of(TxType.DO_NOTHING));

    // Still Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry.setAuthorisedTx(EnumSet.of(TxType.DELETE_ADDRESS));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);
    assertNull(fromAddressEntry); // Address Exists

    // Reset
    s0 = state1.createSnapshot();

    s0.setConfigValue("authorisebyaddress", 1);

    // Insufficient Address permissions (By Category)
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromAddressEntry.setAddressPermissions(AP_DELETE_ADDRESS);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);
    assertNull(fromAddressEntry); // Address Exists

    // Reset
    s0 = state1.createSnapshot();
    assetBalances = s0.getAssetBalances();

    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(address);
    if (toAddressEntry == null) {
      toAddressEntry = addDefaultAddressEntry(assetBalances, address, s0.getVersion());
    }
    toAddressEntry.setAssetBalance("Test|Class", new Balance(1));

    // Not OK, Remaining balance
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    toAddressEntry.setAssetBalance("Test|Class", new Balance(0));

    // Now OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);
    assertNull(fromAddressEntry); // Address Exists

    // Reset
    s0 = state1.createSnapshot();

    // Create a namespace, which should prevent the address from being deleted. Creating the namespace implicitly creates the address
    NamespaceRegisterTx register = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce, pubKey, address, "address-delete-test", "", "");
    assertTrue(UpdateState.update(register, s0, register.getTimestamp(), register.getTxType().getPriority(), false));
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
  }

}
