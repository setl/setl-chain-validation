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
import static io.setl.common.CommonPy.XChainTypes.CHAIN_ADDRESS_FORMAT;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.TransferToManyTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaTransferToMany;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaTransferToManyTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String xcStateFile = "src/test/resources/test-states/genesis/20/4e905d26446940930b87cf1b0ea817ba150548410390d0fdb3266ea5e3ad0a2d";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    int xchainID = 20;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;
    final String sigPubKey1 = "mary had a little lamb";
    final Long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final Long sigAmount2 = 87654321L;

    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    String poaPubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();
    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    int addressNonce = 0;

    String poaAddress = AddressUtil.publicKeyToAddress(poaPubKey, AddressType.NORMAL);
    final String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress2 = getRandomAddress();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set xChain.
    AbstractTx thisTX = io.setl.bc.pychain.tx.create.AddXChain
        .addXChainUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, xchainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set xChain.
    ObjectEncodedState xcState = fileStateLoaded.loadStateFromFile(xcStateFile);
    StateSnapshot xc0 = xcState.createSnapshot();

    thisTX = AddXChain.addXChainUnsigned(xchainID, addressNonce++, poaPubKey, poaAddress, chainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Place initial holding ...
    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, toAddress1, issueAmount, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    // Grant POA

    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_TO_MANY.getId(), (issueAmount * 10L), new String[]{"Crud", fullAssetID, "Crud2"}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Transfer to many, issuance...
    TransferToManyTx transferTX;

    // Bad Poa Address
    transferTX = io.setl.bc.pychain.tx.create.PoaTransferToMany.poaTransferToManyUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, "BadAddress", poaReference, namespace, classname, chainID,
        new Object[]{new Object[]{toAddress1, issueTransferAmount}},
        issueTransferAmount, "", "", "");

    ReturnTuple rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Invalid POA address BadAddress");

    // OK
    transferTX = io.setl.bc.pychain.tx.create.PoaTransferToMany.poaTransferToManyUnsigned(
        chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, chainID,
        new Object[]{new Object[]{toAddress1, issueTransferAmount}},
        issueTransferAmount, "", "", "");

    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Check Issued amount and issuer -ve balance.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount + issueTransferAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(poaAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, -issueAmount - issueTransferAmount);

    // Ordinary transfer to many.

    transferTX = io.setl.bc.pychain.tx.create.PoaTransferToMany.poaTransferToManyUnsigned(
        chainID,
        addressNonce++,
        attorneyPubKey, attorneyAddress,
        toAddress1,
        poaReference,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount,
        "", "",
        "");

    // Fail on No POA.
    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "No POAs for POA Address " + toAddress1);

    // Grant POA

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey2, toAddress1, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    transferTX.setTimestamp(1L);
    // Fail on timestamp.
    rTup = UpdateState.doUpdate(transferTX, s0, Instant.now().getEpochSecond(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Tx Timestamp invalid.");

    // OK on bad priority.
    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority() + 1, false);
    assertSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Fail on Lock.
    AbstractTx lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Asset is locked : NS1|Class1");

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    // OK. CheckOnly
    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), true);
    assertSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Check Only.");

    // OK real.
    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Check From Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress1);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount + issueTransferAmount - transferAmount);

    // Check To Address
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Check Chain Address not exist.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    assertNull(fromAddressEntry);

    // Ordinary transfer to many, xChain
    thisTX = io.setl.bc.pychain.tx.create.PoaTransferToMany.poaTransferToManyUnsigned(
        chainID,
        addressNonce++,
        attorneyPubKey, attorneyAddress,
        toAddress1,
        poaReference,
        namespace,
        classname,
        xchainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount,
        "", "",
        "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check From Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress1);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount + issueTransferAmount - transferAmount - transferAmount);

    // Check To Address has not changed
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Check Chain address has incremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Check xcTo Address has changed
    assetBalances = xc0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Check Chain address has decremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, chainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, -transferAmount);

    // Some fails :
    // Address vs Public key
    assetBalances = s0.getAssetBalances();

    transferTX = io.setl.bc.pychain.tx.create.PoaTransferToMany.poaTransferToManyUnsigned(
        chainID,
        addressNonce,
        attorneyPubKey, "DuffAddress",
        toAddress1,
        poaReference,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount,
        "", "",
        "");

    rTup = UpdateState.doUpdate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "`From` Address and Public key do not match.");

    // Amount check :
    transferTX = PoaTransferToMany.poaTransferToManyUnsigned(
        chainID,
        addressNonce,
        attorneyPubKey, attorneyAddress,
        toAddress1,
        poaReference,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount + 1,
        "", "",
        "");

    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Address payments do not equal Tx Total amount.");

    // Amount check :
    transferTX = PoaTransferToMany.poaTransferToManyUnsigned(
        chainID,
        addressNonce,
        attorneyPubKey, attorneyAddress,
        toAddress1,
        poaReference,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, Long.MAX_VALUE}},
        Long.MAX_VALUE,
        "", "",
        "");

    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "No remaining allowance for [NS1|Class1] in PoA `poaRef1`");

    // Must register check :

    String randomAddress = getRandomAddress();

    transferTX = PoaTransferToMany.poaTransferToManyUnsigned(
        chainID,
        addressNonce,
        attorneyPubKey, attorneyAddress,
        toAddress1,
        poaReference,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{randomAddress, 1}},
        1,
        "", "",
        "");

    s0.setConfigValue("registeraddresses", 1);

    rTup = TransferToMany.updatestate(transferTX, s0, transferTX.getTimestamp(), transferTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Target Address `" + randomAddress + "` does not exist in state. MustRegister. Tx Hash " + transferTX.getHash());

  }
}
