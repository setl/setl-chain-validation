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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultNonceEntry;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;

import java.util.Map;

import org.junit.Test;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.AssetTransferTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.AssetTransfer;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;

public class AssetTransferTest extends BaseTestClass {

  io.setl.bc.pychain.tx.updatestate.AssetTransfer instance = new io.setl.bc.pychain.tx.updatestate.AssetTransfer();


  @Test(expected = TxFailedException.class)
  public void shouldRejectInvalidToAddress() throws TxFailedException {
    String pubKey = getRandomPublicKey();
    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    AssetTransferTx txi = AssetTransfer.assetTransferUnsigned(20, 30, pubKey, address, "ns", "asset", "random value",
        2000L, "", "", ""
    );
    StateSnapshot snapshot = mock(StateSnapshot.class);
    instance.tryChecks(txi, snapshot, 1000000, true);
  }


  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.TransferToMany.transferToManyUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        namespace,
        classname,
        chainID,
        new Object[]{new Object[]{toAddress2, issueTransferAmount}},
        issueTransferAmount, "", "", ""
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);

    // State version < VERSION_USE_UPDATE_HEIGHT
    if (s0.getVersion() < VERSION_USE_UPDATE_HEIGHT) {
      assertTrue(fromAddressEntry.getBlockUpdateHeight() < 0);
    } else {
      assertEquals(fromAddressEntry.getBlockUpdateHeight(), s0.getVersion());
    }

    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();

    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertTrue(newValue.equalTo(issueAmount + issueTransferAmount));

    // New Asset Transfer
    thisTX = AssetTransfer.assetTransferUnsigned(chainID, addressNonce, pubKey2, "Bad Address", namespace, classname, toAddress3, transferAmount, "", "", "");
    assertNotSame(io.setl.bc.pychain.tx.updatestate.AssetTransfer.updatestate(
        (AssetTransferTx) thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false).success, SuccessType.PASS);

    // From = To
    thisTX = AssetTransfer.assetTransferUnsigned(chainID, addressNonce, pubKey2, toAddress2, namespace, classname, toAddress2, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // -ve amount
    thisTX = AssetTransfer.assetTransferUnsigned(chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, toAddress3, -transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    thisTX = AssetTransfer.assetTransferUnsigned(chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, toAddress3, transferAmount, "", "", "");

    // TImestamp
    assertFalse(UpdateState.update(thisTX, s0, 1L, thisTX.getPriority(), false));

    //

    AbstractTx lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    //

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Unlock Namespace

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    //
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();

    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertTrue(newValue.equalTo(issueAmount + issueTransferAmount - transferAmount));

    fromAddressEntry = assetBalances.find(toAddress3);
    fromBalances = fromAddressEntry.getClassBalance();

    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertTrue(newValue.equalTo(transferAmount));

    // Refresh.
    s0 = state1.createSnapshot();

    String pubKey3 = getRandomPublicKey();
    final String address3 = AddressUtil.publicKeyToAddress(pubKey3, AddressType.NORMAL);

    // Send asset to an address with no Balances Map.

    thisTX = AssetTransfer.assetTransferUnsigned(chainID, addressNonce++, pubKey3, address3, namespace, classname, toAddress3, transferAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add Balances map and try again.

    addDefaultNonceEntry(assetBalances, address3, 3);
    fromAddressEntry = assetBalances.find(address3);
    fromBalances = fromAddressEntry.getClassBalance();
    assertNull(fromBalances);

    thisTX = AssetTransfer.assetTransferUnsigned(chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, address3, transferAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address3);
    fromBalances = fromAddressEntry.getClassBalance();

    newValue = fromBalances.get(fullAssetID);
    assertTrue(newValue.equalTo(transferAmount));

    // Zero out Address2, test balance is removed

    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();

    thisTX = AssetTransfer
        .assetTransferUnsigned(chainID, addressNonce++, pubKey2, toAddress2, namespace, classname, address3, fromBalances.get(fullAssetID), "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry = assetBalances.find(toAddress2);
    fromBalances = fromAddressEntry.getClassBalance();
    assertNull(fromBalances.get(fullAssetID));
  }

}
