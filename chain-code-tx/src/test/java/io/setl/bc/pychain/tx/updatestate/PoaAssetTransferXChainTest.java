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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultNonceEntry;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.XChainTypes.CHAIN_ADDRESS_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaAssetTransferXChain;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaAssetTransferXChainTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String xcStateFile = "src/test/resources/test-states/genesis/20/4e905d26446940930b87cf1b0ea817ba150548410390d0fdb3266ea5e3ad0a2d";
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
    String pubKey4 = getRandomPublicKey();
    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    int addressNonce = 0;

    String poaAddress = AddressUtil.publicKeyToAddress(poaPubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();
    final String toAddress4 = AddressUtil.publicKeyToAddress(pubKey4, AddressType.NORMAL);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    final ObjectEncodedState xcState = fileStateLoaded.loadStateFromFile(xcStateFile);

    StateSnapshot s0 = state1.createSnapshot();

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set xChain.
    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, xchainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set xChain.
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

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, toAddress2, issueAmount, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    // Verify initial balances.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(poaAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, -issueAmount);

    // Grant POA

    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_X_CHAIN.getId(), (issueAmount * 10L), new String[]{"Crud", fullAssetID, "Crud2"}}};

    PoaAddTx thisAddTX;

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey4, toAddress4, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Some to fail :
    // Same chain transfer.
    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID, toAddress3, transferAmount, "", "",
        "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID, toAddress3, -1, "", "", "");
    // -ve amount.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID + 99, toAddress3, transferAmount, "",
        "", "");
    // bad chain.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, "dross", toAddress2, poaReference, namespace, classname, chainID, toAddress3, transferAmount, "", "", "");
    // bad address.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID, "moredross", transferAmount, "", "",
        "");
    // to bad address.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID, toAddress2, transferAmount, "", "",
        "");
    // to bad address.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress4, poaReference, namespace, classname, chainID, toAddress2, transferAmount, "", "",
        "");
    // to from address (not exist).
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // addDefaultNonceEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
    // version 'VERSION_SET_FULL_ADDRESS' (4).

    addDefaultNonceEntry(assetBalances, toAddress4, 3);
    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce, attorneyPubKey, attorneyAddress, toAddress4, poaReference, namespace, classname, chainID, toAddress2, 0, "", "", "");
    // it works for a zero amount.
    assertTrue(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Same chain transfer.

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce++, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID, toAddress3, transferAmount, "", "",
        "");

    // No POA
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add POA
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey2, toAddress2, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    thisTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, "", "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    thisTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce++, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, chainID, toAddress3, transferAmount, "", "",
        "");

    // Locked namespace
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    AbstractTx unlockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, "", "", "");
    assertTrue(UpdateState.update(unlockTX, s0, unlockTX.getTimestamp(), unlockTX.getPriority(), false));

    // Locked asset
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    unlockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, "", "");
    assertTrue(UpdateState.update(unlockTX, s0, unlockTX.getTimestamp(), unlockTX.getPriority(), false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check from and to balances.

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount - transferAmount);

    fromAddressEntry = assetBalances.find(toAddress3);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Check Chain Address not exist.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    assertNull(fromAddressEntry);

    // Send to xChain, Chain address will be populated.
    thisTX = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID, addressNonce++, attorneyPubKey, attorneyAddress, toAddress2, poaReference, namespace, classname, xchainID, toAddress3, transferAmount, "", "",
        "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check From Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount - transferAmount - transferAmount);

    // Check To Address has not changed
    fromAddressEntry = assetBalances.find(toAddress3);
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
    fromAddressEntry = assetBalances.find(toAddress3);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Check Chain address has decremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, chainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, -transferAmount);

  }
}
