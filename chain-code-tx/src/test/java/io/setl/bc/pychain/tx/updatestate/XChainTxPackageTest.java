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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetClassUpdate;
import io.setl.bc.pychain.tx.create.AssetTransferXChain;
import io.setl.bc.pychain.tx.create.Bond;
import io.setl.bc.pychain.tx.create.ExerciseEncumbrance;
import io.setl.bc.pychain.tx.create.IssuerTransfer;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.NamespaceTransfer;
import io.setl.bc.pychain.tx.create.UnBond;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.XChainParameters;
import io.setl.common.Pair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class XChainTxPackageTest extends BaseTestClass {

  @Test
  @SuppressWarnings({"squid:S2159"}) // Suppress '.equals on different types' warning.
  public void assetTest() throws Exception {

    // Create State that could be updated

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Add xChain. Signatures are not checked in the update code so not necessary.

    final int baseChainID = 16;
    final int otherChainID = 18;
    final Balance issueAmount = new Balance(1000);
    final Balance transferAmount = new Balance(400);
    final long exerciseAmount = 200;
    final String namespace = "NS1";
    final String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;

    String sigPubKey1 = "mary had a little lamb";
    long sigAmount1 = 12345678L;
    String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    long sigAmount2 = 87654321L;
    int addressNonce = 0;

    final String fromPubKey = getRandomPublicKey();
    final String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);
    final String toAddress1 = "toAddress1";
    final String toAddress2 = "toAddress2";
    final String toAddress3 = "toAddress3";

    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set Chain +2 with Signode data.

    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1,
            XChainParameters.AcceptAnyTx + XChainParameters.ExternalNamespacePriority, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // So we can use snapshots relative to this...
    s0.commit();
    s0 = state1.createSnapshot();

    // Ok now create a simple Tx to test

    AbstractTx xChainTX1 = AssetTransferXChain.assetTransferXChainUnsigned(
        otherChainID, addressNonce++, fromPubKey, fromAddress, namespace, classname, baseChainID, toAddress1, transferAmount, "", "", "");
    xChainTX1.setUpdated(true);

    int otherBlockHeight = 0;

    XChainTxPackageTx xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry addressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    Map<String, Balance> balances = addressEntry.getClassBalance();

    assertNotNull(balances);
    Balance newValue = balances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(transferAmount, newValue);

    xChainTX1 = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        otherChainID, addressNonce++, fromPubKey, fromAddress,
        namespace,
        classname,
        baseChainID,
        new Object[]{new Object[]{toAddress2, transferAmount}, new Object[]{toAddress3, issueAmount}},
        transferAmount,
        "", "", "");
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    addressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    balances = addressEntry.getClassBalance();
    assertNotNull(balances);
    newValue = balances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(transferAmount, newValue);

    addressEntry = assetBalances.findAndMarkUpdated(toAddress3);
    balances = addressEntry.getClassBalance();
    assertNotNull(balances);
    newValue = balances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(issueAmount, newValue);

    //
    xChainTX1 = IssuerTransfer.issuerTransferUnsigned(
        otherChainID, addressNonce++, fromPubKey, fromAddress, namespace, classname, toAddress2, baseChainID, toAddress3, transferAmount, "", "", "");

    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    addressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    balances = addressEntry.getClassBalance();
    assertNotNull(balances);
    newValue = balances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(transferAmount, newValue);

    addressEntry = assetBalances.findAndMarkUpdated(toAddress3);
    balances = addressEntry.getClassBalance();
    assertNotNull(balances);
    newValue = balances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(issueAmount.add(transferAmount), newValue);

    //
    xChainTX1 = ExerciseEncumbrance.exerciseEncumbranceUnsigned(
        otherChainID, addressNonce + 1, fromPubKey, fromAddress, namespace, classname,
        toAddress1,
        "reference",
        baseChainID,
        toAddress2,
        exerciseAmount,
        "",
        "",
        "");

    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    addressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    balances = addressEntry.getClassBalance();
    assertNotNull(balances);
    newValue = balances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(transferAmount.add(exerciseAmount), newValue);

    // Some to fail :

  }


  @Test
  public void bondTest() throws Exception {

    // Create State that could be updated

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    // Add xChain. Signatures are not checked in the update code so not necessary.

    final int baseChainID = 16;
    final int otherChainID = 18;

    final String sigPubKey1 = "mary had a little lamb";
    final long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final long sigAmount2 = 87654321L;
    final Long sigAmount3 = 424242L;

    String sigPubKey3 = getRandomPublicKey();
    final String sigPubAddress3 = AddressUtil.publicKeyToAddress(sigPubKey3, AddressType.NORMAL);

    final String fromPubKey = getRandomPublicKey();
    final String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);

    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    int addressNonce = 0;

    StateSnapshot s0 = state1.createSnapshot();

    // Set Chain +2 with Signode data.

    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1,
            XChainParameters.AcceptAnyTx + XChainParameters.ExternalNamespacePriority, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // So we can use snapshots relative to this...
    s0.commit();
    s0 = state1.createSnapshot();
    int blockHeight = 0;

    // Ok now create a simple Tx to test

    AbstractTx xChainTX1 = Bond.bondUnsigned(otherChainID, addressNonce++, fromPubKey, fromAddress, sigPubKey3, fromAddress, sigAmount3, "");
    xChainTX1.setUpdated(true);

    XChainTxPackageTx xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            blockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    XChainDetails chainDetails1 = s0.getXChainSignNodesValue(otherChainID);
    assertNotNull(chainDetails1);
    assertEquals(new Balance(sigAmount3), chainDetails1.getSignNodes().get(sigPubKey3));

    xChainTX1 = Bond.bondUnsigned(otherChainID, addressNonce++, fromPubKey, fromAddress, sigPubKey3, fromAddress, sigAmount3, "");
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            blockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    chainDetails1 = s0.getXChainSignNodesValue(otherChainID);
    assertNotNull(chainDetails1);
    assertEquals(new Balance(sigAmount3 * 2), chainDetails1.getSignNodes().get(sigPubKey3));

    xChainTX1 = UnBond.unBondUnsigned(otherChainID, addressNonce++, sigPubKey3, sigPubAddress3, fromAddress, sigAmount3, "", "", "");
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            blockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    chainDetails1 = s0.getXChainSignNodesValue(otherChainID);
    assertNotNull(chainDetails1);
    assertEquals(new Balance(sigAmount3), chainDetails1.getSignNodes().get(sigPubKey3));

    // Should eliminate a Signode entry, length down to 2.

    xChainTX1 = UnBond.unBondUnsigned(otherChainID, addressNonce + 1, sigPubKey3, sigPubAddress3, fromAddress, sigAmount3, "", "", "");
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            blockHeight,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    chainDetails1 = s0.getXChainSignNodesValue(otherChainID);
    assertNotNull(chainDetails1);
    assertEquals(2, chainDetails1.getSignNodes().size());

  }


  @Test
  public void classTest() throws Exception {

    // Create State that could be updated

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Add xChain. Signatures are not checked in the update code so not necessary.

    final int baseChainID = 16;
    final int otherChainID = 18;
    final String namespace = "NS1";
    final String classname = "Class1";
    final String metadata = "Meta";  // Base64(MsgPack(""))
    final String metadata2 = "Meta2";  // Base64(MsgPack(""))
    final String poa = "";

    String sigPubKey1 = "mary had a little lamb";
    long sigAmount1 = 12345678L;
    String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    long sigAmount2 = 87654321L;
    int addressNonce = 0;

    final String fromPubKey = getRandomPublicKey();
    final String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);

    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set Chain +2 with Signode data.

    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1,
            XChainParameters.AcceptAnyTx + XChainParameters.ExternalNamespacePriority, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // So we can use snapshots relative to this...
    s0.commit();
    s0 = state1.createSnapshot();

    // Ok now create a simple Tx to test

    AbstractTx xChainTX1 = NamespaceRegister.namespaceRegisterUnsigned(
        otherChainID,
        addressNonce,
        fromPubKey,
        fromAddress,
        namespace,
        metadata,
        poa);

    xChainTX1.setUpdated(true);

    int otherBlockHeight = 0;

    XChainTxPackageTx xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);
    assertNull("", namespaceEntry);

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);
    assertNotNull("", namespaceEntry);
    assertEquals("", fromAddress, namespaceEntry.getAddress());

    xChainTX1 = AssetClassRegister.assetClassRegisterUnsigned(otherChainID, addressNonce++, fromPubKey, fromAddress, namespace, classname, metadata, poa);
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);
    assertNotNull("", namespaceEntry);
    assertTrue("", namespaceEntry.containsAsset(classname));
    assertEquals(classname, namespaceEntry.getAsset(classname).getAssetId());
    assertEquals(metadata, namespaceEntry.getAsset(classname).getMetadata());

    // Update will fail : ExternalClassPriority

    xChainTX1 = AssetClassUpdate.assetClassUpdateUnsigned(otherChainID, addressNonce++, fromPubKey, fromAddress, namespace, classname, metadata2, poa);
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight++,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);
    assertNotNull("", namespaceEntry);
    assertTrue("", namespaceEntry.containsAsset(classname));
    assertEquals(classname, namespaceEntry.getAsset(classname).getAssetId());
    assertNotEquals(metadata2, namespaceEntry.getAsset(classname).getMetadata()); // Not ExternalClassPriority

    // Update will succeed : Set ExternalClassPriority property.

    thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1,
            XChainParameters.AcceptAnyTx + XChainParameters.ExternalNamespacePriority + XChainParameters.ExternalClassPriority, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    xChainTX1 = AssetClassUpdate.assetClassUpdateUnsigned(otherChainID, addressNonce + 1, fromPubKey, fromAddress, namespace, classname, metadata2, poa);
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            otherBlockHeight,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);
    assertNotNull("", namespaceEntry);
    assertTrue("", namespaceEntry.containsAsset(classname));
    assertEquals(classname, namespaceEntry.getAsset(classname).getAssetId());
    assertEquals(metadata2, namespaceEntry.getAsset(classname).getMetadata()); // Not ExternalClassPriority

  }


  @Test
  public void namespaceTest() throws Exception {

    // Create State that could be updated

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Add xChain. Signatures are not checked in the update code so not necessary.

    final int baseChainID = 16;
    final int otherChainID = 18;
    final String namespace = "NS1";
    final String metadata = "Meta";  // Base64(MsgPack(""))
    final String poa = "";

    String sigPubKey1 = "mary had a little lamb";
    long sigAmount1 = 12345678L;
    String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    long sigAmount2 = 87654321L;
    int addressNonce = 0;

    final String fromPubKey = getRandomPublicKey();
    final String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);
    final String toAddress1 = "toAddress1";

    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set Chain +2 with Signode data.

    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1,
            XChainParameters.AcceptAnyTx + XChainParameters.ExternalNamespacePriority, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // So we can use snapshots relative to this...
    s0.commit();
    s0 = state1.createSnapshot();

    // Ok now create a simple Tx to test

    AbstractTx xChainTX1 = NamespaceRegister.namespaceRegisterUnsigned(
        otherChainID,
        addressNonce,
        fromPubKey,
        fromAddress,
        namespace,
        metadata,
        poa);

    xChainTX1.setUpdated(true);

    XChainTxPackageTx xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            0,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);
    assertNull("", namespaceEntry);

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);
    assertNotNull("", namespaceEntry);
    assertEquals("", fromAddress, namespaceEntry.getAddress());

    xChainTX1 = NamespaceTransfer.namespaceTransferUnsigned(otherChainID, addressNonce + 1, fromPubKey, fromAddress, namespace, toAddress1, "");
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            1,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);
    assertNotNull("", namespaceEntry);
    assertEquals("", toAddress1, namespaceEntry.getAddress());

  }


  @Test
  public void processXChainTxs() throws Exception {

    // Create State that could be updated

    TransactionProcessor transactionProcessor = DefaultProcessor.getInstance();

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Add xChain. Signatures are not checked in the update code so not necessary.

    final int baseChainID = 16;
    final int otherChainID = 18;
    final Balance transferAmount = new Balance(400);
    final String namespace = "NS1";
    final String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;

    final String sigPubKey1 = getRandomPublicKey();

    long sigAmount1 = 12345678L;

    final String sigPubKey2 = getRandomPublicKey();

    long sigAmount2 = 87654321L;
    int addressNonce = 0;

    final String fromPubKey = getRandomPublicKey();
    final String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);
    final String toAddress1 = "toAddress1";

    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set Chain +2 with Signode data.

    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1, XChainParameters.AcceptAnyTx, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // So we can use snapshots relative to this...
    s0.commit();
    s0 = state1.createSnapshot();

    // Ok now create a simple Tx to test

    AbstractTx xChainTX1 = AssetTransferXChain
        .assetTransferXChainUnsigned(otherChainID, addressNonce + 1, fromPubKey, fromAddress, namespace, classname, baseChainID, toAddress1, transferAmount,
            "", "", "");
    xChainTX1.setUpdated(true);

    XChainTxPackageTx xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            0,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    xCPackage1.setUpdated(true);

    if (!transactionProcessor.processTransactions(s0, new Txi[]{xCPackage1}, Instant.now().getEpochSecond(), true)) {
      throw new IllegalStateException("State processing failed.");
    }

    // Verify changes

    // Check Asset Balances in State and Snapshot

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    assertNotNull(fromBalances);
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(transferAmount, newValue);

    fromAddressEntry = state1.getAssetBalances().find(toAddress1);
    assertNull(fromAddressEntry);

    // Check xChain details in Snapshot (changed)

    XChainDetails xChainDetails = s0.getXChainSignNodesValue(otherChainID);
    assertEquals(0, xChainDetails.getBlockHeight());

    // OK check that it can not be re-applied

    s0.commit();

    if (transactionProcessor.processTransactions(s0, new Txi[]{xCPackage1}, Instant.now().getEpochSecond(), true)) {
      throw new IllegalStateException("State processing failed.");
    }

    // Now do another block...

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            1,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    xCPackage1.setUpdated(true);

    if (!transactionProcessor.processTransactions(s0, new Txi[]{xCPackage1}, Instant.now().getEpochSecond(), true)) {
      throw new IllegalStateException("State processing failed.");
    }

    // Verify changes

    // Check Asset Balances in State and Snapshot

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    fromBalances = fromAddressEntry.getClassBalance();
    assertNotNull(fromBalances);
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(transferAmount.multiplyBy(2), newValue);

    // Check xChain details in Snapshot (changed)

    xChainDetails = s0.getXChainSignNodesValue(otherChainID);
    assertEquals(1, xChainDetails.getBlockHeight());


  }


  @Test
  public void updatestate() throws Exception {

    // Oh man, this is gonna be big....

    // Create State that could be updated

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Add xChain. Signatures are not checked in the update code so not necessary.

    final int baseChainID = 16;
    final int otherChainID = 18;
    final Balance transferAmount = new Balance(400);
    final String namespace = "NS1";
    final String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;

    String sigPubKey1 = "mary had a little lamb";
    long sigAmount1 = 12345678L;
    String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    long sigAmount2 = 87654321L;
    int addressNonce = 0;

    final String fromPubKey = getRandomPublicKey();
    final String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);
    final String toAddress1 = "toAddress1";

    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set Chain +2 with Signode data.

    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(baseChainID, addressNonce++, fromPubKey, fromAddress, otherChainID, -1, XChainParameters.AcceptAnyTx, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // So we can use snapshots relative to this...
    s0.commit();
    s0 = state1.createSnapshot();

    // Ok now create a simple Tx to test

    AbstractTx xChainTX1 = AssetTransferXChain
        .assetTransferXChainUnsigned(otherChainID, addressNonce++, fromPubKey, fromAddress, namespace, classname, baseChainID, toAddress1, transferAmount,
            "", "", "");
    xChainTX1.setUpdated(true);

    XChainTxPackageTx xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            0,
            otherChainID,
            "basehash",
            new Object[0],
            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    assertNotNull(fromBalances);
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(transferAmount, newValue);

    // Reapply should fail :
    assertFalse(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    // Try again with one that fails (Send asset to Originating Address).

    s0 = state1.createSnapshot();

    // Require Registered addresses

    s0.setConfigValue("registeraddresses", 1L);

    // Ok now create a simple Tx to test

    xChainTX1 = AssetTransferXChain
        .assetTransferXChainUnsigned(baseChainID + 2, addressNonce + 1, fromPubKey, fromAddress, namespace, classname, baseChainID, toAddress1, transferAmount,
            "", "", "");
    xChainTX1.setUpdated(true);

    xCPackage1 =
        new XChainTxPackageTx(
            baseChainID,
            "",
            false,
            "version",
            0,
            otherChainID,
            "basehash",
            new Object[0],

            new Pair[]{new Pair<>(xChainTX1.getHash(), 20)},
            new Object[0],
            new Object[]{xChainTX1.encodeTx()},
            xChainTX1.getTimestamp(),
            0,
            xChainTX1.getTimestamp());

    assertTrue(UpdateState.update(xCPackage1, s0, xCPackage1.getTimestamp(), xCPackage1.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    assertNull(fromAddressEntry);

    fromAddressEntry = assetBalances.findAndMarkUpdated(fromAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    assertNotNull(fromBalances);
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(transferAmount, newValue);

  }


}
