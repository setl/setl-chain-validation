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
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaAssetClassDelete;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import org.junit.Test;

public class PoaAssetClassDeleteTest extends BaseTestClass {


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

    /*
    byte[][] pubpriv = KeyGen.generatePublicPrivateKeyPair();
    sendTx.log("Priv:" + ByteUtil.bytesToHex(pubpriv[1]));
    sendTx.log("Pub:" + ByteUtil.bytesToHex(pubpriv[0]));
    */

    String poaPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String poaAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";
    final Object[] itemsData = new Object[]{new Object[]{TxType.DELETE_ASSET_CLASS.getId(), 1, new String[]{"Crud", fullAssetID, "Crud2"}}};
    final Object[] itemsDataWildcard1 = new Object[]{new Object[]{TxType.DELETE_ASSET_CLASS.getId(), 1, new String[]{"Crud", "*", "Crud2"}}};
    final Object[] itemsDataWildcard2 = new Object[]{
        new Object[]{TxType.DELETE_ASSET_CLASS.getId(), 1, new String[]{"Crud", namespace + "|" + "*", "Crud2"}}};
    final Object[] itemsDataWrongWildcard = new Object[]{
        new Object[]{TxType.DELETE_ASSET_CLASS.getId(), 1, new String[]{"Crud", namespace + "|x" + "*", "Crud2"}}};
    final Object[] itemsDataWrongAsset = new Object[]{new Object[]{TxType.DELETE_ASSET_CLASS.getId(), 1, new String[]{"Crud", fullAssetID + "x", "Crud2"}}};
    final Object[] itemsDataWrongTx = new Object[]{new Object[]{TxType.DO_NOTHING.getId(), 1, new String[]{"Crud", fullAssetID, "Crud2"}}};
    final Object[] itemsDataNoAmount = new Object[]{new Object[]{TxType.DELETE_ASSET_CLASS.getId(), 0, new String[]{"Crud", fullAssetID, "Crud2"}}};
    final String protocol = "prot";
    final String poa = "poa";
    int addressNonce = 0;


    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");

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

    // Grant POA

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);

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

    // Fail to delete again.
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertFalse(thisTX.isGood());

    // Try a bad one : itemsDataNoAmount

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsDataNoAmount, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceEntry = namespaceTree.find(namespace);
    assertTrue(namespaceEntry.containsAsset(classname));

    // Try a bad one : itemsDataWrongTx

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsDataWrongTx, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceEntry = namespaceTree.find(namespace);
    assertTrue(namespaceEntry.containsAsset(classname));

    // Try a bad one : itemsDataWrongAsset

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsDataWrongAsset, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceEntry = namespaceTree.find(namespace);
    assertTrue(namespaceEntry.containsAsset(classname));

    // Try a bad one : itemsDataWrongWildcard

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsDataWrongWildcard, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceEntry = namespaceTree.find(namespace);
    assertTrue(namespaceEntry.containsAsset(classname));

    // Try a good one : itemsDataWildcard1

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsDataWildcard1, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceEntry = namespaceTree.find(namespace);
    assertFalse(namespaceEntry.containsAsset(classname));

    // Try a good one : itemsDataWildcard2

    s0 = state1.createSnapshot();
    namespaceTree = s0.getNamespaces();

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsDataWildcard2, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA Asset Class Register
    //thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, metadata, "");
    thisTX = PoaAssetClassDelete
        .poaAssetClassDeleteUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceEntry = namespaceTree.find(namespace);
    assertFalse(namespaceEntry.containsAsset(classname));

  }


}
