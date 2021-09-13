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
import static io.setl.bc.pychain.state.entry.LockedAsset.Type.FULL;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.time.Instant;
import org.junit.Test;

public class AssetClassRegisterTest extends BaseTestClass {


  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String metadata = "this metadata";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    long issueAmount = 1000;
    long issueTransferAmount = 100;
    long transferAmount = 400;

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    String toAddress2 = getRandomAddress();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    s0.setAssetLockValue(namespace, FULL);
    s0.setAssetLockValue(fullAssetID, FULL);

    // Asset lock there....
    assertTrue(s0.isAssetLocked(namespace));
    assertTrue(s0.isAssetLocked(fullAssetID));

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset lock has gone....
    assertFalse(s0.isAssetLocked(namespace));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertFalse(namespaceEntry.containsAsset(classname));

    // Fail on timestamp.
    thisTX.setTimestamp(1L);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    //
    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);
    if (fromAddressEntry == null) {
      fromAddressEntry = addDefaultAddressEntry(assetBalances, address, s0.getVersion());
    }
    fromAddressEntry.setAddressPermissions(AP_CLASSES);

    // OK now ...

    // Asset lock still there....
    assertTrue(s0.isAssetLocked(fullAssetID));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertTrue("", namespaceEntry.containsAsset(classname));

    // Asset lock has gone....
    assertFalse(s0.isAssetLocked(fullAssetID));

    Asset classDetails = namespaceEntry.getAsset(classname);

    assertEquals("", classname, classDetails.getAssetId());
    assertEquals("", metadata, classDetails.getMetadata());

    // Some bad Txs

    // Empty Namespace

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, "", classname, metadata, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Empty Class

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", metadata, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad From Address Class

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, getRandomAddress(), namespace, classname, metadata, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));


  }


}
