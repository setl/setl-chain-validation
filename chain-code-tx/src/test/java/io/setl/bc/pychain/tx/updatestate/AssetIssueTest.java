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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.LockedAsset.Type;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.AssetIssueTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import org.junit.Test;

public class AssetIssueTest extends BaseTestClass {

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

    AssetIssueTx assissTX;
    AbstractTx thisTX;

    // ----

    // Namespace not exist

    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    ReturnTuple rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Namespace `NS1` has not been registered.");

    // Register Namespace
    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Class not exist
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Class `Class1` is not registered in namespace `NS1`.");

    // Wrong Chain ID always fails gracefully
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID + 1, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Unmatched Priority always fails gracefully
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority() + 1, false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Register Class
    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Address
    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad timestamp
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = UpdateState.doUpdate(assissTX, s0, 1, assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Tx Timestamp invalid.");

    // Check only
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), true);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Check Only.");

    // Now OK
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // OK on from == to
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, address,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // OK on Zero amount
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        0, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Some more to fail ...

    // Fail on -ve amount
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        -issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Invalid amount");

    // Fail on bad address
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, toAddress2, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = UpdateState.doUpdate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "`From` Address and Public key do not match.");

    // Lock Namespace
    s0.setAssetLockValue(namespace, LockedAsset.Type.FULL);

    // NS Locked
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Namespace is locked : NS1");

    // Lock Namespace
    s0.setAssetLockValue(namespace, Type.NO_LOCK);
    s0.setAssetLockValue(fullAssetID, Type.FULL);

    // NS Locked
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Asset is locked : NS1|Class1");

    s0.setAssetLockValue(fullAssetID, Type.NO_LOCK);

    // Require address to exist
    s0.setConfigValue("registeraddresses", 1);
    assertTrue(s0.getStateConfig().getMustRegister());

    // Fail to send to Address 3
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress3,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Target Address `" + toAddress3 + "` does not exist in state. MustRegister. Tx Hash " + assissTX.getHash());

    //
    // Reset.
    //

    s0 = state1.createSnapshot();

    // Register Namespace
    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce, pubKey2, toAddress2, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Class
    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce, pubKey2, toAddress2, namespace, classname, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Address
    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey2, toAddress2, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Not own the namespace
    assissTX = io.setl.bc.pychain.tx.create.AssetIssue.assetIssueUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
        issueAmount, "", "", "");
    rTup = AssetIssue.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Namespace `NS1` is not controlled by the  Tx Address `" + address + "`");

    // Reset.
    s0 = state1.createSnapshot();

  }


}