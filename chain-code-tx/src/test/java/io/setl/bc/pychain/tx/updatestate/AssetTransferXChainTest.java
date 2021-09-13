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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.AssetTransferXChain;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class AssetTransferXChainTest extends BaseTestClass {

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

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();
    String pubKey4 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();
    final String toAddress4 = AddressUtil.publicKeyToAddress(pubKey4, AddressType.NORMAL);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot s0 = state1.createSnapshot();

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set xChain.
    AbstractTx thisTX = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, xchainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set xChain.
    ObjectEncodedState xcState = fileStateLoaded.loadStateFromFile(xcStateFile);
    StateSnapshot xc0 = xcState.createSnapshot();

    thisTX = AddXChain.addXChainUnsigned(xchainID, addressNonce++, pubKey, address, chainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Place initial holding ...
    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, issueAmount, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    // Verify initial balances.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(-issueAmount));

    // Some to fail :
    // Same chain transfer.
    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey2, toAddress2, namespace, classname, chainID, toAddress3, transferAmount, "", "", "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey2, toAddress2, namespace, classname, chainID, toAddress3, -1, "", "", "");
    // -ve amount.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey2, toAddress2, namespace, classname, chainID + 99, toAddress3, transferAmount, "", "", "");
    // bad chain.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey2, "dross", namespace, classname, chainID, toAddress3, transferAmount, "", "", "");
    // bad address.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey2, toAddress2, namespace, classname, chainID, "moredross", transferAmount, "", "", "");
    // to bad address.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey2, toAddress2, namespace, classname, chainID, toAddress2, transferAmount, "", "", "");
    // to bad address.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey4, toAddress4, namespace, classname, chainID, toAddress2, transferAmount, "", "", "");
    // to from address (not exist).
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // addDefaultNonceEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
    // version 'VERSION_SET_FULL_ADDRESS' (4).

    addDefaultNonceEntry(assetBalances, toAddress4, 3);
    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce, pubKey4, toAddress4, namespace, classname, chainID, toAddress2, 0, "", "", "");
    // it works for a zero amount.
    assertTrue(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Same chain transfer.

    thisTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    thisTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, chainID, toAddress3, transferAmount, "", "", "");

    // Locked namespace
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    AbstractTx unlockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "");
    assertTrue(UpdateState.update(unlockTX, s0, unlockTX.getTimestamp(), unlockTX.getPriority(), false));

    // Locked asset
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    unlockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");
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
    assertTrue(newValue.equalTo(issueAmount - transferAmount));

    fromAddressEntry = assetBalances.find(toAddress3);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount));

    // Check Chain Address not exist.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    assertNull(fromAddressEntry);

    // Send to xChain, Chain address will be populated.
    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, xchainID, toAddress3, transferAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check From Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - transferAmount - transferAmount));

    // Check To Address has not changed
    fromAddressEntry = assetBalances.find(toAddress3);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount));

    // Check Chain address has incremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount));

    // Check xcTo Address has changed
    assetBalances = xc0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress3);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount));

    // Check Chain address has decremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, chainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertTrue(newValue.equalTo(-transferAmount));

    // Zero out toAddress2, test balance is removed

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();

    thisTX = AssetTransferXChain.assetTransferXChainUnsigned(chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, xchainID, toAddress3,
        fromBalances.get(fullAssetID),
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    assertNull(fromBalances.get(fullAssetID));

  }
}
