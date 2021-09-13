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
package io.setl.bc.pychain.tx.updatestate.contracts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.bc.pychain.tx.create.CommitToContract.newCommitmentUnsigned;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.time.Instant;

import org.junit.Test;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData.AssetIn;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;

@SuppressWarnings("unlikely-arg-type")
public class NominateTest extends BaseTestClass {


  @Test
  public void updatestate() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final Entity client1 = new Entity();
    final Entity client2 = new Entity();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(
        state1.getChainId(),            //  chainId
        "",
        party1Nonce++,            //  nonce
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,            //  height
        "",
        6
    );           //  timestamp

    TokensNominateContractData testData = new TokensNominateContractData(thisnewTX.getContractAddress(), CONTRACT_NAME_TOKENS_NOMINATE, "namespace",
        "inputclass",
        "outputclass", 1L, 1L, Instant.now().getEpochSecond() + 1000, new String[]{"time"}, party1Address, "", ""
    );

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    StateSnapshot s0 = state1.createSnapshot();

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Fudge holdings

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    addDefaultAddressEntry(assetBalances, client1.address, 3);
    addDefaultAddressEntry(assetBalances, client2.address, 3);
    addDefaultAddressEntry(assetBalances, party1Address, 3);

    String namespace = "namespace";
    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
    if (namespaceEntry == null) {
      namespaceTree.add(new NamespaceEntry(namespace, party1Address, ""));
      namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
    }

    namespaceEntry.setAsset(new Asset("outputclass", null));

    final String inputAssetID = "namespace|inputclass";
    final String outputAssetID = "namespace|outputclass";

    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(client1.address);
    toAddressEntry.setAssetBalance(inputAssetID, new Balance(10000L));

    toAddressEntry = assetBalances.findAndMarkUpdated(client2.address);
    toAddressEntry.setAssetBalance(inputAssetID, new Balance(10000L));

    toAddressEntry = assetBalances.findAndMarkUpdated(party1Address);
    toAddressEntry.setAssetBalance(outputAssetID, new Balance(20000L));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // New nominate
    // Unsigned 'AssetIn'

    NominateCommitData commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());
    commitData.setAssetIn(new AssetIn(100L, null, null));
    int clientNonce1 = 0;

    CommitToContractTx commitTx = newCommitmentUnsigned(
        16,
        clientNonce1,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    assertTrue(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    toAddressEntry = assetBalances.find(client1.address);

    assertEquals(new Balance(9900L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(100L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(client2.address);
    assertEquals(new Balance(10000L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(0L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(party1Address);
    assertEquals(new Balance(100L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(19900L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    // New nominate
    // Signed 'AssetIn'

    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    AssetIn thisIn = new AssetIn(200L, client2.publicHex, "");
    String sigMessage = thisIn.stringToHashToSign(thisnewTX.getContractAddress(), commitTx.getAuthoringAddress(), commitTx.getNonce());
    thisIn.signature = verifier.createSignatureB64(sigMessage, client2.privateHex);
    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(thisIn);

    thisIn = new AssetIn(300L, client1.publicHex, "");
    sigMessage = thisIn.stringToHashToSign(thisnewTX.getContractAddress(), commitTx.getAuthoringAddress(), commitTx.getNonce());
    thisIn.signature = verifier.createSignatureB64(sigMessage, client1.privateKey);
    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(thisIn);

    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(400L, client1.publicHex, ""));
    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(500L, "", ""));

    assertTrue(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    toAddressEntry = assetBalances.find(client1.address);

    assertEquals(new Balance(8700L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(1300L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(client2.address);
    assertEquals(new Balance(9800L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(200L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(party1Address);
    assertEquals(new Balance(1500L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(18500L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

  }


  @Test
  public void updatestate2() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    Entity client1 = new Entity();
    Entity client2 = new Entity();

    int party1Nonce = 0;
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    NewContractTx thisnewTX = new NewContractTx(
        state1.getChainId(),            //  chainId
        "",
        party1Nonce++,            //  nonce
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,            //  height
        "",
        6
    );           //  timestamp

    final String inputAssetID = "namespace|inputclass";
    final String outputAssetID = "namespace|outputclass";

    TokensNominateContractData testData = new TokensNominateContractData(thisnewTX.getContractAddress(), CONTRACT_NAME_TOKENS_NOMINATE, "namespace",
        "inputclass",
        "outputclass", 5L, 15L, Instant.now().getEpochSecond() + 1000, new String[]{"time"}, party1Address, "", ""
    );

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    StateSnapshot s0 = state1.createSnapshot();

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Fudge holdings

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    addDefaultAddressEntry(assetBalances, client1.address, 3);
    addDefaultAddressEntry(assetBalances, client2.address, 3);
    addDefaultAddressEntry(assetBalances, party1Address, 3);

    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(client1.address);
    toAddressEntry.setAssetBalance(inputAssetID, new Balance(10000L));

    toAddressEntry = assetBalances.findAndMarkUpdated(client2.address);
    toAddressEntry.setAssetBalance(inputAssetID, new Balance(10000L));

    toAddressEntry = assetBalances.findAndMarkUpdated(party1Address);
    toAddressEntry.setAssetBalance(outputAssetID, new Balance(30000L));

    String namespace = "namespace";
    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
    if (namespaceEntry == null) {
      namespaceTree.add(new NamespaceEntry(namespace, "SomeOtherAddress", ""));
      namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
    }

    namespaceEntry.setAsset(new Asset("outputclass", null));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // New nominate
    // Unsigned 'AssetIn'

    NominateCommitData commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());
    commitData.setAssetIn(new AssetIn(100L, null, null));
    final MessageSignerVerifier verifier = MessageVerifierFactory.get();
    int clientNonce1 = 0;

    CommitToContractTx commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    assertTrue(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    toAddressEntry = assetBalances.find(client1.address);

    assertEquals(new Balance(9900L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(300L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(client2.address);
    assertEquals(new Balance(10000L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(0L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(party1Address);
    assertEquals(new Balance(100L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(29700L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    s0.commit();
    StateSnapshot s1 = s0.createSnapshot();
    assetBalances = s1.getAssetBalances();

    // Try depleting 'In' asset to check the balance is deleted

    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());
    commitData.setAssetIn(new AssetIn(9900L, null, null));

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    assertTrue(UpdateState.update(commitTx, s1, commitTx.getTimestamp(), commitTx.getPriority(), false));

    toAddressEntry = assetBalances.find(client1.address);

    assertFalse(toAddressEntry.getClassBalance().containsKey(inputAssetID));

    assetBalances = s0.getAssetBalances();

    // New nominate
    // Signed 'AssetIn'

    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    AssetIn thisIn = new AssetIn(200L, client2.publicHex, "");
    String sigMessage = thisIn.stringToHashToSign(thisnewTX.getContractAddress(), commitTx.getAuthoringAddress(), commitTx.getNonce());
    thisIn.signature = verifier.createSignatureB64(sigMessage, client2.privateHex);
    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(thisIn);

    thisIn = new AssetIn(300L, client1.publicHex, "");
    sigMessage = thisIn.stringToHashToSign(thisnewTX.getContractAddress(), commitTx.getAuthoringAddress(), commitTx.getNonce());
    thisIn.signature = verifier.createSignatureB64(sigMessage, client1.privateKey);
    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(thisIn);

    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(400L, client1.publicHex, ""));
    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(500L, "", ""));

    assertTrue(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    toAddressEntry = assetBalances.find(client1.address);

    assertEquals(new Balance(8700L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(3900L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(client2.address);
    assertEquals(new Balance(9800L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(600L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(party1Address);
    assertEquals(new Balance(1500L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(25500L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    // New nominate
    // Some to fail :

    // Too much Input
    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(50000L, "", ""));
    assertFalse(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    // Too much Output

    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(28000L, "", ""));
    assertFalse(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    // Bad Address

    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(8000L, "aaaaaa", ""));
    assertFalse(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    // Bad Sig

    commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());

    commitTx = newCommitmentUnsigned(
        16,
        clientNonce1++,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    ((NominateCommitData) commitTx.getCommitmentData()).setAssetIn(new AssetIn(8000L, client1.publicHex, "fred"));
    assertFalse(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));


  }


  @Test
  public void updatestate3() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    Entity client1 = new Entity();
    Entity client2 = new Entity();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(
        state1.getChainId(),            //  chainId
        "",
        party1Nonce++,            //  nonce
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,            //  height
        "",
        6
    );           //  timestamp

    TokensNominateContractData testData = new TokensNominateContractData(thisnewTX.getContractAddress(), CONTRACT_NAME_TOKENS_NOMINATE, "namespace",
        "inputclass",
        "outputclass", 1L, 1L, Instant.now().getEpochSecond() + 1000, new String[]{"time"}, party1Address, "", ""
    );

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    StateSnapshot s0 = state1.createSnapshot();

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Fudge holdings

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    addDefaultAddressEntry(assetBalances, client1.address, 3);
    addDefaultAddressEntry(assetBalances, client2.address, 3);
    addDefaultAddressEntry(assetBalances, party1Address, 3);

    String namespace = "namespace";
    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
    if (namespaceEntry == null) {
      namespaceTree.add(new NamespaceEntry(namespace, party1Address, ""));
      namespaceEntry = namespaceTree.findAndMarkUpdated(namespace);
    }

    namespaceEntry.setAsset(new Asset("outputclass", null));

    final String inputAssetID = "namespace|inputclass";
    final String outputAssetID = "namespace|outputclass";

    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(client1.address);
    toAddressEntry.setAssetBalance(inputAssetID, new Balance(10000L));

    toAddressEntry = assetBalances.findAndMarkUpdated(client2.address);
    toAddressEntry.setAssetBalance(inputAssetID, new Balance(10000L));

    toAddressEntry = assetBalances.findAndMarkUpdated(party1Address);
    toAddressEntry.setAssetBalance(outputAssetID, new Balance(20000L));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // New nominate
    // Unsigned 'AssetIn'
    // Nominate the whole amount of namespace|inputclass from client1.address

    NominateCommitData commitData = new NominateCommitData("namespace", "inputclass", "", "", thisnewTX.getContractAddress());
    commitData.setAssetIn(new AssetIn(10000L, null, null));
    int clientNonce1 = 0;

    CommitToContractTx commitTx = newCommitmentUnsigned(
        16,
        clientNonce1,
        client1.publicHex,
        client1.address,
        thisnewTX.getContractAddress(),
        commitData,
        ""
    );

    assertTrue(UpdateState.update(commitTx, s0, commitTx.getTimestamp(), commitTx.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    toAddressEntry = assetBalances.find(client1.address);

    // Balance has become null.
    assertNull(toAddressEntry.getClassBalance().get(inputAssetID));
    assertEquals(new Balance(10000L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(client2.address);
    assertEquals(new Balance(10000L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(0L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));

    toAddressEntry = assetBalances.find(party1Address);
    assertEquals(new Balance(10000L), toAddressEntry.getClassBalance().getOrDefault(inputAssetID, BALANCE_ZERO));
    assertEquals(new Balance(10000L), toAddressEntry.getClassBalance().getOrDefault(outputAssetID, BALANCE_ZERO));


  }


}
