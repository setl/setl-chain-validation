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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.CommitToContract;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.util.Map;
import org.junit.Test;

public class ExchangeCommitTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int addressNonce = 0;
    final long issueAmount = 1000;

    final Long blocksize1 = 1L;
    final Long blocksize2 = 2L;

    final Long minBlocks = 0L;
    final Long maxBlocks = 0L;
    final Long startDate = 0L;
    final Long expiryDate = Long.MAX_VALUE;

    String namespace1 = "NS1";
    String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    String namespace2 = "NS2";
    String classname2 = "Class2";
    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final String clientPublic1 = getRandomPublicKey();
    final String clientAddress1 = AddressUtil.publicKeyToAddress(clientPublic1, AddressType.NORMAL);

    final String clientPublic2 = getRandomPublicKey();
    final String clientAddress2 = AddressUtil.publicKeyToAddress(clientPublic2, AddressType.NORMAL);
    int clientNonce2 = 0;

    final String clientPublic3 = getRandomPublicKey();
    final String clientAddress3 = AddressUtil.publicKeyToAddress(clientPublic3, AddressType.NORMAL);
    int clientNonce3 = 0;

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace1,
        classname1,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asse 2

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname2,
        chainID,
        new Object[]{
            new Object[]{clientAddress2, issueAmount},
            new Object[]{clientAddress3, issueAmount},
        },
        issueAmount * 2, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit

    s0.commit();
    s0 = state1.createSnapshot();

    // OK, clientAddress1 has issueAmount of namespace1|classname1 and
    //     clientAddress2 has issueAmount of namespace2|classname2

    // Client 1 Creates a contract to swap 2 of Asset 1 (Out) for 1 of Asset 2 (in).
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        clientPublic1,
        clientAddress1,
        null,
        "",
        0,
        "",
        6);

    Object[] inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1, clientAddress2, null, null, null)};
    Object[] outputs = new Object[]{new NominateAsset(namespace1, classname1, blocksize2)};

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        clientAddress1, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...
    int party2Nonce = 0;

    inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1)};

    ExchangeCommitData commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    CommitToContractTx thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Try to commit from the 'Wrong' commit address ; The contract specified an acceptable input address.

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic3,
        clientAddress3,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Check balances

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    AddressEntry client1 = assetBalances.find(clientAddress1);
    Map<String, Balance> client1Balances = client1.getClassBalance();

    AddressEntry client2 = assetBalances.find(clientAddress2);
    Map<String, Balance> client2Balances = client2.getClassBalance();

    assertTrue(true);

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - blocksize2));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - blocksize1));

    //

  }


  @Test
  public void updatestate2() throws Exception {
    // 2 Assets in, 2 assets out

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int addressNonce = 0;
    final long issueAmount = 1000;

    final Long blocksize1 = 1L;
    final Long blocksize2 = 2L;
    final Long blocksize3 = 3L;
    final Long blocksize4 = 4L;

    final Long blockMultiple = 10L;

    final Long startDate = 0L;
    final Long expiryDate = Long.MAX_VALUE;

    final String namespace1 = "NS1";
    final String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    final String namespace2 = "NS1";
    final String classname2 = "Class2";
    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String namespace3 = "NS1";
    final String classname3 = "Class3";
    final String fullAssetID3 = namespace3 + "|" + classname3;

    final String namespace4 = "NS1";
    final String classname4 = "Class4";
    final String fullAssetID4 = namespace4 + "|" + classname4;

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final String clientPublic1 = getRandomPublicKey();
    final String clientAddress1 = AddressUtil.publicKeyToAddress(clientPublic1, AddressType.NORMAL);

    final String clientPublic2 = getRandomPublicKey();
    final String clientAddress2 = AddressUtil.publicKeyToAddress(clientPublic2, AddressType.NORMAL);
    int clientNonce2 = 0;

    final String clientPublic3 = getRandomPublicKey();
    final String clientAddress3 = AddressUtil.publicKeyToAddress(clientPublic3, AddressType.NORMAL);
    int clientNonce3 = 0;

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace1,
        classname1,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 2

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname2,
        chainID,
        new Object[]{new Object[]{clientAddress2, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 3

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace3, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace3, classname3, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace3,
        classname3,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 4

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace4, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace4, classname4, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace4,
        classname4,
        chainID,
        new Object[]{new Object[]{clientAddress2, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit

    s0.commit();
    s0 = state1.createSnapshot();

    // OK, clientAddress1 has issueAmount of namespace1|classname1 and namespace3|classname3
    //     clientAddress2 has issueAmount of namespace2|classname2 and namespace4|classname4

    // Client 1 Creates a contract
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        clientPublic1,
        clientAddress1,
        null,
        "",
        0,
        "",
        6);

    Object[] inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2),
        new NominateAsset(namespace4, classname4, blocksize4),
    };

    Object[] outputs = new Object[]{
        new NominateAsset(namespace1, classname1, blocksize1),
        new NominateAsset(namespace3, classname3, blocksize3),
    };

    Long minBlocks = 0L;
    Long maxBlocks = 0L;

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        clientAddress1, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    ExchangeCommitData commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    int party2Nonce = 0;

    CommitToContractTx thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    AddressEntry client1 = assetBalances.find(clientAddress1);
    Map<String, Balance> client1Balances = client1.getClassBalance();

    AddressEntry client2 = assetBalances.find(clientAddress2);
    Map<String, Balance> client2Balances = client2.getClassBalance();

    assertTrue(true);

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - (blocksize1 * blockMultiple)));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize2 * blockMultiple));
    assertTrue(client1.getAssetBalance(fullAssetID3).equalTo(issueAmount - (blocksize3 * blockMultiple)));
    assertTrue(client1.getAssetBalance(fullAssetID4).equalTo(blocksize4 * blockMultiple));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize1 * blockMultiple));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - (blocksize2 * blockMultiple)));
    assertTrue(client2.getAssetBalance(fullAssetID3).equalTo(blocksize3 * blockMultiple));
    assertTrue(client2.getAssetBalance(fullAssetID4).equalTo(issueAmount - (blocksize4 * blockMultiple)));
  }


  @Test
  public void updatestate3() throws Exception {
    // 2 Assets in, 2 assets out
    // Test max / min blocks, Start & End Date

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int addressNonce = 0;
    final long issueAmount = 1000;

    final Long blocksize1 = 1L;
    final Long blocksize2 = 2L;
    final Long blocksize3 = 3L;
    final Long blocksize4 = 4L;

    final Long minBlocks = 2L;
    final Long maxBlocks = 3L;
    final Long startDate = 100L;
    final Long expiryDate = Long.MAX_VALUE - 1;

    final String namespace1 = "NS1";
    final String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    final String namespace2 = "NS1";
    final String classname2 = "Class2";
    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String namespace3 = "NS1";
    final String classname3 = "Class3";
    final String fullAssetID3 = namespace3 + "|" + classname3;

    final String namespace4 = "NS1";
    final String classname4 = "Class4";
    final String fullAssetID4 = namespace4 + "|" + classname4;

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final String clientPublic1 = getRandomPublicKey();
    final String clientAddress1 = AddressUtil.publicKeyToAddress(clientPublic1, AddressType.NORMAL);

    final String clientPublic2 = getRandomPublicKey();
    final String clientAddress2 = AddressUtil.publicKeyToAddress(clientPublic2, AddressType.NORMAL);
    int clientNonce2 = 0;

    final String clientPublic3 = getRandomPublicKey();
    final String clientAddress3 = AddressUtil.publicKeyToAddress(clientPublic3, AddressType.NORMAL);
    int clientNonce3 = 0;

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace1,
        classname1,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 2

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname2,
        chainID,
        new Object[]{new Object[]{clientAddress2, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 3

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace3, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace3, classname3, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace3,
        classname3,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 4

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace4, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace4, classname4, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace4,
        classname4,
        chainID,
        new Object[]{new Object[]{clientAddress2, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit

    s0.commit();
    s0 = state1.createSnapshot();

    // OK, clientAddress1 has issueAmount of namespace1|classname1 and namespace3|classname3
    //     clientAddress2 has issueAmount of namespace2|classname2 and namespace4|classname4

    // Client 1 Creates a contract
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        clientPublic1,
        clientAddress1,
        null,
        "",
        0,
        "",
        6);

    Object[] inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2),
        new NominateAsset(namespace4, classname4, blocksize4),
    };

    Object[] outputs = new Object[]{
        new NominateAsset(namespace1, classname1, blocksize1),
        new NominateAsset(namespace3, classname3, blocksize3),
    };

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        clientAddress1, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...

    Long blockMultiple = maxBlocks + 1;

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    ExchangeCommitData commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    int party2Nonce = 0;

    CommitToContractTx thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    // Should fail, too many blocks...

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // try again with too few blocks

    blockMultiple = minBlocks - 1;

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Fail : Before startdate...

    blockMultiple = maxBlocks;

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    thisCommit.setTimestamp(startDate - 1);

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Fail : after enddate...

    blockMultiple = maxBlocks;

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    thisCommit.setTimestamp(Long.MAX_VALUE);

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // OK, One to work now...

    blockMultiple = maxBlocks;

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    AddressEntry client1 = assetBalances.find(clientAddress1);
    Map<String, Balance> client1Balances = client1.getClassBalance();

    AddressEntry client2 = assetBalances.find(clientAddress2);
    Map<String, Balance> client2Balances = client2.getClassBalance();

    assertTrue(true);

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - (blocksize1 * blockMultiple)));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize2 * blockMultiple));
    assertTrue(client1.getAssetBalance(fullAssetID3).equalTo(issueAmount - (blocksize3 * blockMultiple)));
    assertTrue(client1.getAssetBalance(fullAssetID4).equalTo(blocksize4 * blockMultiple));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize1 * blockMultiple));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - (blocksize2 * blockMultiple)));
    assertTrue(client2.getAssetBalance(fullAssetID3).equalTo(blocksize3 * blockMultiple));
    assertTrue(client2.getAssetBalance(fullAssetID4).equalTo(issueAmount - (blocksize4 * blockMultiple)));
  }


  @Test
  public void updatestate4() throws Exception {
    // 2 Assets in, 2 assets out
    // Test Issuer as contractor

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int addressNonce = 0;
    final long issueAmount = 1000;

    final Long blocksize1 = 1L;
    final Long blocksize2 = 2L;
    final Long blocksize3 = 3L;
    final Long blocksize4 = 4L;

    final Long blockMultiple = 10L;

    final Long startDate = 0L;
    final Long expiryDate = Long.MAX_VALUE;

    final String namespace1 = "NS1";
    final String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    final String namespace2 = "NS1";
    final String classname2 = "Class2";
    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String namespace3 = "NS1";
    final String classname3 = "Class3";
    final String fullAssetID3 = namespace3 + "|" + classname3;

    final String namespace4 = "NS1";
    final String classname4 = "Class4";
    final String fullAssetID4 = namespace4 + "|" + classname4;

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final String clientPublic1 = getRandomPublicKey();
    final String clientAddress1 = AddressUtil.publicKeyToAddress(clientPublic1, AddressType.NORMAL);

    final String clientPublic2 = getRandomPublicKey();
    final String clientAddress2 = AddressUtil.publicKeyToAddress(clientPublic2, AddressType.NORMAL);
    int clientNonce2 = 0;

    final String clientPublic3 = getRandomPublicKey();
    final String clientAddress3 = AddressUtil.publicKeyToAddress(clientPublic3, AddressType.NORMAL);
    int clientNonce3 = 0;

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 2

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname2,
        chainID,
        new Object[]{new Object[]{clientAddress2, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 3

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace3, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace3, classname3, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 4

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace4, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace4, classname4, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace4,
        classname4,
        chainID,
        new Object[]{new Object[]{clientAddress2, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit

    s0.commit();
    s0 = state1.createSnapshot();

    // OK, clientAddress2 has issueAmount of namespace2|classname2 and namespace4|classname4

    // Issuer (public1) Creates a contract
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,
        "",
        6);

    Object[] inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2),
        new NominateAsset(namespace4, classname4, blocksize4),
    };

    Object[] outputs = new Object[]{
        new NominateAsset(namespace1, classname1, blocksize1),
        new NominateAsset(namespace3, classname3, blocksize3),
    };

    Long minBlocks = 0L;
    Long maxBlocks = 0L;

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        party1Address, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...

    inputs = new Object[]{
        new NominateAsset(namespace2, classname2, blocksize2 * blockMultiple),
        new NominateAsset(namespace4, classname4, blocksize4 * blockMultiple),
    };

    ExchangeCommitData commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    int party2Nonce = 0;

    CommitToContractTx thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    AddressEntry client2 = assetBalances.find(clientAddress2);

    AddressEntry issuer = assetBalances.find(party1Address);

    assertTrue(true);

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize1 * blockMultiple));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - (blocksize2 * blockMultiple)));
    assertTrue(client2.getAssetBalance(fullAssetID3).equalTo(blocksize3 * blockMultiple));
    assertTrue(client2.getAssetBalance(fullAssetID4).equalTo(issueAmount - (blocksize4 * blockMultiple)));

    assertTrue(issuer.getAssetBalance(fullAssetID1).equalTo(-blocksize1 * blockMultiple));
    assertTrue(issuer.getAssetBalance(fullAssetID2).equalTo(-issueAmount + (blocksize2 * blockMultiple)));
    assertTrue(issuer.getAssetBalance(fullAssetID3).equalTo(-blocksize3 * blockMultiple));
    assertTrue(issuer.getAssetBalance(fullAssetID4).equalTo(-issueAmount + (blocksize4 * blockMultiple)));

  }


  @Test
  public void updatestate5() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int addressNonce = 0;
    final long issueAmount = 1000;

    final Long blocksize1 = 10L;
    final Long blocksize2 = 20L;

    final Long minBlocks = 0L;
    final Long maxBlocks = 0L;
    final Long startDate = 0L;
    final Long expiryDate = Long.MAX_VALUE;

    String namespace1 = "NS1";
    String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    String namespace2 = "NS2";
    String classname2 = "Class2";
    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final String clientPublic1 = getRandomPublicKey();
    final String clientAddress1 = AddressUtil.publicKeyToAddress(clientPublic1, AddressType.NORMAL);

    final String clientPublic2 = getRandomPublicKey();
    final String clientAddress2 = AddressUtil.publicKeyToAddress(clientPublic2, AddressType.NORMAL);
    int clientNonce2 = 0;

    final String clientPublic3 = getRandomPublicKey();
    final String clientAddress3 = AddressUtil.publicKeyToAddress(clientPublic3, AddressType.NORMAL);
    int clientNonce3 = 0;

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace1,
        classname1,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asse 2

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname2,
        chainID,
        new Object[]{
            new Object[]{clientAddress2, issueAmount},
            new Object[]{clientAddress3, issueAmount},
        },
        issueAmount * 2, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit

    s0.commit();
    s0 = state1.createSnapshot();

    // OK, clientAddress1 has issueAmount of namespace1|classname1 and
    //     clientAddress2 has issueAmount of namespace2|classname2

    // Client 1 Creates a contract to swap 20 of Asset 1 (Out) for 10 of Asset 2 (in).
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        clientPublic1,
        clientAddress1,
        null,
        "",
        0,
        "",
        6);

    Object[] inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1, clientAddress2, null, null, null)};
    Object[] outputs = new Object[]{new NominateAsset(namespace1, classname1, blocksize2)};

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        clientAddress1, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // New 'Checkpoint'.

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...
    int party2Nonce = 0;

    inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1)};

    ExchangeCommitData commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    CommitToContractTx thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    // Try with locked Namespace
    s0.setAssetLockValue(namespace2, LockedAsset.Type.FULL);

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    s0.setAssetLockValue(namespace2, LockedAsset.Type.NO_LOCK);
    // Try with locked Asset
    s0.setAssetLockValue(fullAssetID2, LockedAsset.Type.FULL);

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));
    s0.setAssetLockValue(fullAssetID2, LockedAsset.Type.NO_LOCK);

    // Bad Timestamp
    assertFalse(UpdateState.update(thisCommit, s0, 1L, thisCommit.getPriority(), false));

    // No Contract
    MutableMerkle<ContractEntry> contractsList = s0.getContracts();
    contractsList.delete(thisnewTX.getContractAddress());

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    s0 = state1.createSnapshot();

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(clientAddress2);
    fromAddressEntry.setAddressPermissions(AP_CLASS);

    // Wrong permission
    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    fromAddressEntry.setAddressPermissions(AP_COMMIT);
    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    s0 = state1.createSnapshot();

    // OK
    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Try to commit from the 'Wrong' commit address ; The contract specified an acceptable input address.

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic3,
        clientAddress3,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Check balances

    assetBalances = s0.getAssetBalances();

    AddressEntry client1 = assetBalances.find(clientAddress1);
    Map<String, Balance> client1Balances = client1.getClassBalance();

    AddressEntry client2 = assetBalances.find(clientAddress2);
    Map<String, Balance> client2Balances = client2.getClassBalance();

    assertTrue(true);

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - blocksize2));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - blocksize1));

    //

  }


  @Test
  public void updatestate6() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int addressNonce = 0;
    final long issueAmount = 1000;

    final Long blocksize1 = 1000L;
    final Long blocksize2 = 1000L;

    final Long minBlocks = 0L;
    final Long maxBlocks = 0L;
    final Long startDate = 0L;
    final Long expiryDate = Long.MAX_VALUE;

    String namespace1 = "NS1";
    String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    String namespace2 = "NS2";
    String classname2 = "Class2";
    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    final String clientPublic1 = getRandomPublicKey();
    final String clientAddress1 = AddressUtil.publicKeyToAddress(clientPublic1, AddressType.NORMAL);

    final String clientPublic2 = getRandomPublicKey();
    final String clientAddress2 = AddressUtil.publicKeyToAddress(clientPublic2, AddressType.NORMAL);
    int clientNonce2 = 0;

    final String clientPublic3 = getRandomPublicKey();
    final String clientAddress3 = AddressUtil.publicKeyToAddress(clientPublic3, AddressType.NORMAL);
    int clientNonce3 = 0;

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace1,
        classname1,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asse 2

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname2,
        chainID,
        new Object[]{
            new Object[]{clientAddress2, issueAmount},
            new Object[]{clientAddress3, issueAmount},
        },
        issueAmount * 2, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit

    s0.commit();
    s0 = state1.createSnapshot();

    // OK, clientAddress1 has issueAmount of namespace1|classname1 and
    //     clientAddress2 has issueAmount of namespace2|classname2

    // Client 1 Creates a contract to swap 2 of Asset 1 (Out) for 1 of Asset 2 (in).
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        clientPublic1,
        clientAddress1,
        null,
        "",
        0,
        "",
        6);

    Object[] inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1, clientAddress2, null, null, null)};
    Object[] outputs = new Object[]{new NominateAsset(namespace1, classname1, blocksize2)};

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        clientAddress1, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...
    int party2Nonce = 0;

    inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1)};

    ExchangeCommitData commitData = new ExchangeCommitData(
        thisnewTX.getContractAddress(),
        inputs,
        null,
        "",
        ""
    );

    CommitToContractTx thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic2,
        clientAddress2,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Try to commit from the 'Wrong' commit address ; The contract specified an acceptable input address.

    thisCommit = CommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        clientPublic3,
        clientAddress3,
        thisnewTX.getContractAddress(),
        commitData,
        "");

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Check balances

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    AddressEntry client1 = assetBalances.find(clientAddress1);
    Map<String, Balance> client1Balances = client1.getClassBalance();

    AddressEntry client2 = assetBalances.find(clientAddress2);
    Map<String, Balance> client2Balances = client2.getClassBalance();

    assertNull(client1.getClassBalance().get(fullAssetID1));
    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - blocksize2));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2));
    assertNull(client2.getClassBalance().get(fullAssetID2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - blocksize1));

    //

  }


}
