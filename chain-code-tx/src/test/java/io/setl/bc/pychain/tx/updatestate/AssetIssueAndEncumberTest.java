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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.LockedAsset.Type;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.AssetIssueEncumberTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

public class AssetIssueAndEncumberTest extends BaseTestClass {

  String reference = "reference";


  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void updatestate1() throws Exception {

    // Test IssueAssetWithEncumbrance for 'default' encumbrance (given 'reference' only, no admin or beneficiaries).
    // Test also the set of normal AssetIssue cases from the AssetIssue test.

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    AssetIssueEncumberTx assissTX;
    AbstractTx thisTX;

    //

    Map<String, Object> txDict = new TreeMap<>();
    txDict.put("reference", reference);
    // txMap.put("administrators", administrators);
    // txMap.put("beneficiaries", beneficiaries);

    // ----

    // Namespace not exist

    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    ReturnTuple rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Namespace `NS1` has not been registered.");

    // Register Namespace
    thisTX = io.setl.bc.pychain.tx.create.NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Class not exist
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Class `Class1` is not registered in namespace `NS1`.");

    // Wrong Chain ID always fails gracefully
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID + 1, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Unmatched Priority always fails gracefully
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority() + 1, false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Register Class. NB: this implicitly creates address
    thisTX = io.setl.bc.pychain.tx.create.AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Address
    thisTX = io.setl.bc.pychain.tx.create.RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad timestamp
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = UpdateState.doUpdate(assissTX, s0, 1, assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Tx Timestamp invalid.");

    // Check only
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), true);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Check Only.");

    // Now OK
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Check Encumbrance is in place...
    // Transaction was added with 'null' administrators and beneficiaries, to the 'from' address is used by default.

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(toAddress2);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), issueAmount);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(reference).get(0);

    assertEquals(thisEntry.reference, reference);
    assertEquals(thisEntry.getAdministrators().size(), 1);
    assertEquals(thisEntry.getBeneficiaries().size(), 1);
    EncumbranceDetail thisDetail = thisEntry.getAdministrators().get(0);
    assertEquals(thisDetail.address, address);
    assertEquals(0, thisDetail.startTime);
    assertEquals(thisDetail.endTime, Long.MAX_VALUE);

    thisDetail = thisEntry.getBeneficiaries().get(0);
    assertEquals(thisDetail.address, address);
    assertEquals(0, thisDetail.startTime);
    assertEquals(thisDetail.endTime, Long.MAX_VALUE);

    // OK on from == to
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, address,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Fail on Zero amount
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            0, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Invalid amount");

    // Some more to fail ...

    // Fail on -ve amount
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            -issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Invalid amount");

    // Fail on bad address
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, toAddress2, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = UpdateState.doUpdate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "`From` Address and Public key do not match.");

    // Lock Namespace
    s0.setAssetLockValue(namespace, LockedAsset.Type.FULL);

    // NS Locked
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Namespace is locked : NS1");

    // Lock Namespace
    s0.setAssetLockValue(namespace, Type.NO_LOCK);
    s0.setAssetLockValue(fullAssetID, Type.FULL);

    // NS Locked
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Asset is locked : NS1|Class1");

    s0.setAssetLockValue(fullAssetID, Type.NO_LOCK);

    // Require address to exist
    s0.setConfigValue("registeraddresses", 1);
    assertTrue(s0.getStateConfig().getMustRegister());

    // Fail to send to Address 3
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress3,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
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
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotSame(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Namespace `NS1` is not controlled by the  Tx Address `" + address + "`");

    // Reset.
    s0 = state1.createSnapshot();

  }


  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void updatestate2() throws Exception {

    // Test IssueAssetWithEncumbrance for 'default' encumbrance (given 'reference' only, no admin or beneficiaries).
    // Test also the set of normal AssetIssue cases from the AssetIssue test.

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

    AssetIssueEncumberTx assissTX;
    AbstractTx thisTX;

    //

    String administratorAddress = getRandomAddress();
    String beneficiaryAddress = getRandomAddress();

    EncumbranceDetail[] administrators =
        new EncumbranceDetail[]{new EncumbranceDetail(administratorAddress, 20L, 40L), new EncumbranceDetail(beneficiaryAddress, 40L, 0L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(beneficiaryAddress, 202L, 404L),
        new EncumbranceDetail(beneficiaryAddress, 0L, 0L)};

    Map<String, Object> txDict = new TreeMap<>();
    txDict.put("reference", reference);
    txDict.put("administrators", administrators);
    txDict.put("beneficiaries", beneficiaries);

    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");

    // Register Namespace
    thisTX = io.setl.bc.pychain.tx.create.NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Class
    thisTX = io.setl.bc.pychain.tx.create.AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Address
    thisTX = io.setl.bc.pychain.tx.create.RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check only
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    ReturnTuple rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), true);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "Check Only.");

    // Now OK
    assissTX = io.setl.bc.pychain.tx.create.AssetIssueAndEncumber
        .assetIssueAndEncumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");
    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "");

    // Check Encumbrance is in place...
    // Transaction was added with specified administrators and beneficiaries.

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(toAddress2);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), issueAmount);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(reference).get(0);

    assertEquals(thisEntry.reference, reference);
    assertEquals(thisEntry.getAdministrators().size(), 2);
    assertEquals(thisEntry.getBeneficiaries().size(), 2);
    EncumbranceDetail thisDetail = thisEntry.getAdministrators().get(0);
    assertEquals(thisDetail.address, administratorAddress);
    assertEquals(20L, thisDetail.startTime);
    assertEquals(40L, thisDetail.endTime);
    thisDetail = thisEntry.getAdministrators().get(1);
    assertEquals(thisDetail.address, beneficiaryAddress);
    assertEquals(40L, thisDetail.startTime);
    assertEquals(0L, thisDetail.endTime);

    /*
       EncumbranceDetail[] administrators =
        new EncumbranceDetail[]{new EncumbranceDetail(administratorAddress, 20L, 40L), new EncumbranceDetail(beneficiaryAddress, 40L, 0L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(beneficiaryAddress, 202L, 404L),
        new EncumbranceDetail(beneficiaryAddress, 0L, 0L)};

     */

    thisDetail = thisEntry.getBeneficiaries().get(0);
    assertEquals(thisDetail.address, beneficiaryAddress);
    assertEquals(202L, thisDetail.startTime);
    assertEquals(404L, thisDetail.endTime);
    thisDetail = thisEntry.getBeneficiaries().get(1);
    assertEquals(thisDetail.address, beneficiaryAddress);
    assertEquals(0L, thisDetail.startTime);
    assertEquals(0L, thisDetail.endTime);

  }

}