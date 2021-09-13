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

import static io.setl.bc.pychain.state.entry.LockedAsset.Type.FULL;
import static io.setl.bc.pychain.state.entry.LockedAsset.Type.NO_LOCK;
import static io.setl.bc.pychain.tx.updatestate.BaseTestClass.getRandomAddress;
import static io.setl.bc.pychain.tx.updatestate.BaseTestClass.getRandomPublicKey;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassDelete;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.security.MessageDigest;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetClassDeleteTest {

  MessageDigest digest;

  FileStateLoader fileStateLoaded;

  SerialiseToByte hashSerialiser;


  @Before
  public void setUp() throws Exception {

    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();

    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
  }


  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }


  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String metadata = "this metadata";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;

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

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertFalse(namespaceEntry.containsAsset(classname));

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    assertTrue("", namespaceEntry.containsAsset(classname));

    Asset classDetails = namespaceEntry.getAsset(classname);

    assertEquals("", classname, classDetails.getAssetId());
    assertEquals("", metadata, classDetails.getMetadata());

    s0.commit();

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    s0.setAssetLockValue(fullAssetID, FULL);

    thisTX = AssetClassDelete.assetClassDeleteUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");
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

    namespaceEntry = namespaceTree.find(namespace);

    assertFalse(namespaceEntry.containsAsset(classname));
    assertSame(s0.getAssetLockValue(fullAssetID, NO_LOCK), NO_LOCK);

    // Fail to delete again.
    thisTX = AssetClassDelete.assetClassDeleteUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertFalse(thisTX.isGood());

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
