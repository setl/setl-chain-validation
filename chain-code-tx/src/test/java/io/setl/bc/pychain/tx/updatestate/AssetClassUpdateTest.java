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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetClassUpdate;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.time.Instant;
import org.junit.Test;

public class AssetClassUpdateTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String metadata = "this metadata";
    final String newMetadata = "this new metadata";
    final int chainID = 16;

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    String toAddress2 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");
    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    // Namespace Exists, but class does not.

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertFalse(namespaceEntry.containsAsset(classname));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");
    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    // Class Exists.

    assertTrue("", namespaceEntry.containsAsset(classname));

    Asset classDetails = namespaceEntry.getAsset(classname);

    assertEquals("", classname, classDetails.getAssetId());
    assertEquals("", metadata, classDetails.getMetadata());

    s0.commit();
    s0 = state1.createSnapshot();

    thisTX = AssetClassUpdate.assetClassUpdateUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, newMetadata, "");
    final AbstractTx saveTX = thisTX;

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);

    classDetails = namespaceEntry.getAsset(classname);

    assertEquals("", classname, classDetails.getAssetId());
    assertEquals("", newMetadata, classDetails.getMetadata());
    assertEquals("", newMetadata, namespaceEntry.getClassMetadata(classname));
    assertNotEquals(metadata, namespaceEntry.getClassMetadata(classname));

    // Reset to known state.
    s0 = state1.createSnapshot();

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    //
    thisTX = saveTX;

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);
    fromAddressEntry.setAddressPermissions(AP_CLASSES);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Reset
    s0 = state1.createSnapshot();


  }

}
