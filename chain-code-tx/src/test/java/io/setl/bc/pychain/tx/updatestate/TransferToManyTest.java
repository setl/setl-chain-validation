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

import static io.setl.bc.pychain.tx.updatestate.BaseTestClass.getRandomAddress;
import static io.setl.bc.pychain.tx.updatestate.BaseTestClass.getRandomPublicKey;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.XChainTypes.CHAIN_ADDRESS_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.create.TransferToMany;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;




@SuppressWarnings("unlikely-arg-type")
public class TransferToManyTest {

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

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();
    String pubKey3 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = AddressUtil.publicKeyToAddress(pubKey3, AddressType.NORMAL);
    final String toAddress2 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot xcState = fileStateLoaded.loadStateFromFile(xcStateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();
    final StateSnapshot xc0 = xcState.createSnapshot();

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set xChain.
    AbstractTx thisTX = AddXChain
        .addXChainUnsigned(chainID, addressNonce++, pubKey, address, xchainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set xChain.
    thisTX = AddXChain.addXChainUnsigned(xchainID, addressNonce++, pubKey, address, chainID, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Place initial holding ...
    // issueAmount to toAddress1

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress1, issueAmount, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    // Transfer to many, issuance...

    thisTX = TransferToMany.transferToManyUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, chainID, new Object[]{new Object[]{toAddress1, issueTransferAmount}},
        issueTransferAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad check amount :

    thisTX = TransferToMany.transferToManyUnsigned(
        chainID, addressNonce++, pubKey, address, namespace, classname, chainID, new Object[]{new Object[]{toAddress1, issueTransferAmount}},
        -1, "", "", "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Issued amount and issuer -ve balance.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress1);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount + issueTransferAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, -issueAmount - issueTransferAmount);

    // Ordinary transfer to many.
    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        pubKey3,
        toAddress3,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount,
        "", "",
        "");

    // Fail on bad 'From Address' : Nothing there
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Ordinary transfer to many.
    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        pubKey2,
        toAddress1,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, -1L}},
        transferAmount,
        "", "",
        "");

    // Fail on bad 'Transfer Amount' : Nothing there
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Ordinary transfer to many.
    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        pubKey2,
        toAddress1,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount,
        "", "",
        "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // Fail on Lock. Lock both Asset and Namespace.
    AbstractTx lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    // OK. CheckOnly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK real.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

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
    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        pubKey2,
        toAddress1,
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

    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce,
        pubKey2,
        "DuffAddress",
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount,
        "", "",
        "");

    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Amount check :
    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce,
        pubKey2,
        "DuffAddress",
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, transferAmount}},
        transferAmount + 1,
        "", "",
        "");

    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Now to Zero out 'toAddress1' and to chech that the Balance is removed.

    s0.commit();
    s0 = state1.createSnapshot();
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress1);
    fromBalances = fromAddressEntry.getClassBalance();

    thisTX = TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        pubKey2,
        toAddress1,
        namespace,
        classname,
        xchainID,
        new Object[]{new Object[]{toAddress2, fromBalances.get(fullAssetID)}},
        fromBalances.get(fullAssetID),
        "", "",
        "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress1);
    fromBalances = fromAddressEntry.getClassBalance();

    assertNull(fromBalances.get(fullAssetID));

  }

}
