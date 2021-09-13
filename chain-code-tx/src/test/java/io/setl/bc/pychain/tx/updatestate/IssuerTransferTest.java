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
import static io.setl.common.CommonPy.VersionConstants.VERSION_SET_FULL_ADDRESS;
import static io.setl.common.CommonPy.XChainTypes.CHAIN_ADDRESS_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.IssuerTransfer;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class IssuerTransferTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String xcStateFile = "src/test/resources/test-states/genesis/20/4e905d26446940930b87cf1b0ea817ba150548410390d0fdb3266ea5e3ad0a2d";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    int xchainID = 20;
    final long issueAmount = 2000;
    final long transferAmount = 400;
    final String sigPubKey1 = "mary had a little lamb";
    final Long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final Long sigAmount2 = 87654321L;

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();
    final String toAddress4 = getRandomAddress();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    ObjectEncodedState xcState = fileStateLoaded.loadStateFromFile(xcStateFile);

    StateSnapshot s0 = state1.createSnapshot();
    final StateSnapshot xc0 = xcState.createSnapshot();

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set xChain.
    AbstractTx thisTX = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, xchainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set xChain.
    thisTX = AddXChain.addXChainUnsigned(xchainID, addressNonce++, pubKey, address, chainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Place initial holding ...

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Verify initial balances.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, -issueAmount);

    // Same chain transfer.

    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, chainID, toAddress3, transferAmount, "", "", "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));
    thisTX.setTimestamp(Instant.now().getEpochSecond());

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    newValue = fromAddressEntry.getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount - transferAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress3);
    newValue = fromAddressEntry.getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Same chain transfer : Target address exists, but does not have balance structure (legacy).

    addDefaultNonceEntry(assetBalances, toAddress4, VERSION_SET_FULL_ADDRESS - 1);

    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, chainID, toAddress4, transferAmount, "", "", "");

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    newValue = fromAddressEntry.getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount - (transferAmount * 2));

    fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress4);
    newValue = fromAddressEntry.getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, transferAmount);

    // Send to xChain, Chain address will be populated.

    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, xchainID, toAddress3, transferAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check From Address
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount - (transferAmount * 3));

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

    // Some fails
    // Adderss vs public key
    assetBalances = s0.getAssetBalances();

    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce, pubKey, "CrudAddress", namespace, classname, toAddress2, chainID, toAddress3, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Namespace
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce, pubKey, address, "BadNamespace", classname, toAddress2, chainID, toAddress3, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Class
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce, pubKey, address, namespace, "Bad Class", toAddress2, chainID, toAddress3, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on amount.
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, chainID, toAddress3, issueAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on -ve amount.
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, chainID, toAddress3, -transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on non-existent toChain.
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, chainID + xchainID, toAddress3, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on from == to.
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, address, chainID, toAddress3, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on not issuer.
    thisTX = IssuerTransfer.issuerTransferUnsigned(
        chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, address, chainID, toAddress3, issueAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Zero out Address2, test balance is removed

    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    assertTrue(fromBalances.get(fullAssetID).greaterThan(0L));

    thisTX = IssuerTransfer.issuerTransferUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, chainID, toAddress3,
        fromBalances.get(fullAssetID), "",
        "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    assertNull(fromBalances.get(fullAssetID));

  }

}
