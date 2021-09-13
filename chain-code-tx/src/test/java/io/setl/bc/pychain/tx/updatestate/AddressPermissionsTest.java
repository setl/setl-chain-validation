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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.bc.pychain.tx.create.AddressPermissions.addressPermissionsUnsigned;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_ADMIN;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.AddressPermissionsTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import java.time.Instant;
import org.junit.Test;

public class AddressPermissionsTest extends BaseTestClass {


  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String metadata = "this metadata";
    int chainID = 16;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    long permissions = AP_NAMESPACE;

    AbstractTx thisTX = addressPermissionsUnsigned(chainID, addressNonce++, pubKey, address, toAddress1, permissions, "", "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Config value not set
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.setConfigValue("authorisebyaddress", 1);

    // No permissions for address
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    AddressEntry addressEntry = assetBalances.findAndMarkUpdated(address);

    if (addressEntry == null) {

      addDefaultAddressEntry(assetBalances, address, 3);

      addressEntry = assetBalances.findAndMarkUpdated(address);
    }

    addressEntry.setAddressPermissions(AP_ADMIN);

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    assertEquals(s0.getAddressPermissions(toAddress1), permissions);

    long newpermissions = AP_NAMESPACE + AP_CLASSES;

    thisTX = addressPermissionsUnsigned(chainID, addressNonce++, pubKey, address, toAddress1, newpermissions, "", "");

    // Fail on timestamp.
    thisTX.setTimestamp(1L);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // Bad Address
    AddressPermissionsTx apTX = addressPermissionsUnsigned(chainID, addressNonce++, pubKey, "Bad1", toAddress1, newpermissions, "", "");
    assertNotSame(AddressPermissions.updatestate(apTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false).success, SuccessType.PASS);

    // Bad ToAddress
    apTX = addressPermissionsUnsigned(chainID, addressNonce++, pubKey, address, "Bad2", newpermissions, "", "");
    assertNotSame(AddressPermissions.updatestate(apTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false).success, SuccessType.PASS);

    assertEquals(s0.getAddressPermissions(toAddress1), permissions);

  }


}
