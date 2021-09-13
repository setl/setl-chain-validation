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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.PoaNewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.CommitToContract;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaCommitToContract;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class PoaExchangeCommitTest extends BaseTestClass {

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
    final String protocol = "prot";
    final String metadata = "meta";

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

    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

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

    // --------------------------------------------------------------------------------
    // OK, clientAddress1 has issueAmount of namespace1|classname1 and
    //     clientAddress2 has issueAmount of namespace2|classname2

    // Client 1 Creates a contract to swap 2 of Asset 1 (Out) for 1 of Asset 2 (in).
    String poaAddress = clientAddress1;
    String poaPubKey = clientPublic1;

    final String poaReference = "poaRef1";

    final String poaCommitReference = "poaRef2";
    final long poaStartDate = 0L;
    final long poaEndDate = Instant.now().getEpochSecond() + 999L;
    int party1Nonce = 0;

    NewContractTx thisnewTX = new PoaNewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        attorneyPubKey,
        attorneyAddress,
        clientAddress1,
        poaReference,
        null,
        "",
        protocol,
        metadata,
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

    // No PoA
    StateSnapshot s1 = s0.createSnapshot();
    assertFalse(UpdateState.update(thisnewTX, s1, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_AS_ISSUER.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, poaStartDate, poaEndDate, itemsData, protocol,
            metadata, "");
    assertTrue(UpdateState.update(thisAddTX, s1, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Bad Tx Type in POA
    assertFalse(UpdateState.update(thisnewTX, s1, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Grant POA, good assets

    itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, poaStartDate, poaEndDate, itemsData, protocol,
            metadata,
            "");
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // New Contract OK...
    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...
    // Commit by Asset holder

    inputs = new Object[]{new NominateAsset(namespace2, classname2, blocksize1)};

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

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - blocksize2));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - blocksize1));

    // Now try to commit using a PoA

    thisCommit = PoaCommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        attorneyPubKey,
        attorneyAddress,
        clientAddress2,
        poaCommitReference,
        thisnewTX.getContractAddress(),
        commitData,
        protocol,
        metadata,
        "");

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Grant POA, good assets

    itemsData = new Object[]{
        new Object[]{TxType.COMMIT_TO_CONTRACT.getId(), (blocksize1), new String[]{fullAssetID2}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party2Nonce, clientPublic2, clientAddress2, poaCommitReference, attorneyAddress, poaStartDate, poaEndDate, itemsData,
            protocol, metadata, "");

    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // now OK...

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Check balances.

    assetBalances = s0.getAssetBalances();

    client1 = assetBalances.find(clientAddress1);
    client2 = assetBalances.find(clientAddress2);

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - (blocksize2 * 2)));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1 * 2));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2 * 2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - (blocksize1 * 2)));

  }


  @Test
  public void updatestate2() throws Exception {

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
    final String protocol = "prot";
    final String metadata = "meta";

    String namespace1 = "NS1";
    String classname1 = "Class1";
    final String fullAssetID1 = namespace1 + "|" + classname1;

    String namespace2 = "NS2";
    String classname2 = "Class2";

    final String fullAssetID2 = namespace2 + "|" + classname2;

    final String fullAssetID12 = namespace1 + "|" + classname2;
    final String fullAssetID21 = namespace2 + "|" + classname1;

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

    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    // Establish Assets :

    StateSnapshot s0 = state1.createSnapshot();

    // Asset 1

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace1, classname2, "", "");

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

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace1,
        classname2,
        chainID,
        new Object[]{new Object[]{clientAddress1, issueAmount}},
        issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Asset 2

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, publicKey1, party1Address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname1, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, publicKey1, party1Address, namespace2, classname2, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        publicKey1,
        party1Address,
        namespace2,
        classname1,
        chainID,
        new Object[]{
            new Object[]{clientAddress2, issueAmount},
            new Object[]{clientAddress3, issueAmount},
        },
        issueAmount * 2, "", "", "");

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
    StateSnapshot s1 = s0.createSnapshot();

    // --------------------------------------------------------------------------------
    // OK, clientAddress1 has issueAmount of namespace1|classname1 and namespace1|classname2 and
    //     clientAddress2 has issueAmount of namespace2|classname1 and namespace2|classname2

    // Client 1 Creates a contract to swap 2 of Asset 1 (Out) for 1 of Asset 2 (in).
    String poaAddress = clientAddress1;
    String poaPubKey = clientPublic1;

    final String poaReference = "poaRef1";

    final String poaCommitReference = "poaRef2";
    final long poaStartDate = 0L;
    final long poaEndDate = Instant.now().getEpochSecond() + 999L;
    int party1Nonce = 0;

    NewContractTx thisnewTX = new PoaNewContractTx(state1.getChainId(),
        "",
        party1Nonce++,
        true,
        attorneyPubKey,
        attorneyAddress,
        clientAddress1,
        poaReference,
        null,
        "",
        protocol,
        metadata,
        0,
        "",
        6);

    Object[] inputs = new Object[]{
        new NominateAsset(namespace2, classname1, blocksize1, clientAddress2, null, null, null),
        new NominateAsset(namespace2, classname2, blocksize1, clientAddress2, null, null, null)};
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

    // No PoA
    assertFalse(UpdateState.update(thisnewTX, s1, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_AS_ISSUER.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, poaStartDate, poaEndDate, itemsData, protocol,
            metadata, "");
    assertTrue(UpdateState.update(thisAddTX, s1, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Bad Tx Type in POA
    assertFalse(UpdateState.update(thisnewTX, s1, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Grant POA, good assets
    s1 = s0.createSnapshot();

    itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID12}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, poaStartDate, poaEndDate, itemsData, protocol,
            metadata,
            "");
    assertTrue(UpdateState.update(thisAddTX, s1, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // New Contract Not OK, PoA needs Output Asset (fullAssetID2) too...
    assertFalse(UpdateState.update(thisnewTX, s1, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Reset snapshot, makes replacing the PoA easier.

    itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2, fullAssetID21}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, poaStartDate, poaEndDate, itemsData, protocol,
            metadata,
            "");

    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    MutableMerkle<PoaEntry> poaList = s0.getPowerOfAttorneys();
    PoaEntry addressPOA = poaList.find(poaAddress);
    PoaEntry poaDetailEntry = poaList.find(addressPOA.getFullReference(poaReference));
    PoaDetail thisDetail = poaDetailEntry.getPoaDetail();
    List<PoaItem> items = thisDetail.getItem(TxType.NEW_CONTRACT);
    assertEquals(1, items.size());
    assertTrue(items.get(0).getAmount().equalTo(1));
    assertEquals(4, items.get(0).getAssets().size());
    assertTrue(items.get(0).getAssets().contains("Crud"));
    assertTrue(items.get(0).getAssets().contains(fullAssetID1));
    assertTrue(items.get(0).getAssets().contains(fullAssetID2));
    assertTrue(items.get(0).getAssets().contains(fullAssetID21));

    // New Contract OK...
    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Check remaining PoAs

    poaList = s0.getPowerOfAttorneys();
    addressPOA = poaList.find(poaAddress);
    assertNull(addressPOA); // Will have been deleted as it was entirely consumed

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // OK, Contract is established, now commit...
    // Commit by Asset holder

    inputs = new Object[]{
        new NominateAsset(namespace2, classname1, blocksize1),
        new NominateAsset(namespace2, classname2, blocksize1)
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

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - blocksize2));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - blocksize1));

    // Now try to commit using a PoA

    thisCommit = PoaCommitToContract.newCommitmentUnsigned(
        chainID,
        party2Nonce++,
        attorneyPubKey,
        attorneyAddress,
        clientAddress2,
        poaCommitReference,
        thisnewTX.getContractAddress(),
        commitData,
        protocol,
        metadata,
        "");

    assertFalse(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Grant POA, good assets

    itemsData = new Object[]{
        new Object[]{TxType.COMMIT_TO_CONTRACT.getId(), (blocksize1 * 2), new String[]{fullAssetID2}}, // Surplus quantity
        new Object[]{TxType.COMMIT_TO_CONTRACT.getId(), (blocksize1), new String[]{fullAssetID21}}
    };

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party2Nonce, clientPublic2, clientAddress2, poaCommitReference, attorneyAddress, poaStartDate, poaEndDate, itemsData,
            protocol, metadata, "");

    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // now OK...

    assertTrue(UpdateState.update(thisCommit, s0, thisCommit.getTimestamp(), thisCommit.getPriority(), false));

    // Check balances.

    assetBalances = s0.getAssetBalances();

    client1 = assetBalances.find(clientAddress1);
    client2 = assetBalances.find(clientAddress2);

    assertTrue(client1.getAssetBalance(fullAssetID1).equalTo(issueAmount - (blocksize2 * 2)));
    assertTrue(client1.getAssetBalance(fullAssetID2).equalTo(blocksize1 * 2));

    assertTrue(client2.getAssetBalance(fullAssetID1).equalTo(blocksize2 * 2));
    assertTrue(client2.getAssetBalance(fullAssetID2).equalTo(issueAmount - (blocksize1 * 2)));

    // Check remaining PoAs

    poaList = s0.getPowerOfAttorneys();
    addressPOA = poaList.find(clientAddress2);
    poaDetailEntry = poaList.find(addressPOA.getFullReference(poaCommitReference));
    thisDetail = poaDetailEntry.getPoaDetail();
    items = thisDetail.getItem(TxType.COMMIT_TO_CONTRACT);
    assertEquals(2, items.size());
    assertTrue(items.get(0).getAmount().equalTo(1));
    assertTrue(items.get(0).getAssets().contains(fullAssetID2));
    assertTrue(items.get(1).getAmount().equalTo(0));
    assertTrue(items.get(1).getAssets().contains(fullAssetID21));


  }

}
