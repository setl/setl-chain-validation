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

import static io.setl.common.CommonPy.EncumbranceConstants.ISSUER_LOCK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class LockHoldingTest extends BaseTestClass {

  final long halfLockAmount = 100;

  final long issueAmount = 1000;

  final long issueTransferAmount = 100;

  final long lockAmount = 200;

  final long transferAmount = 400;

  String administratorAddress;

  String administratorPukKey;


  String beneficiaryAddress;

  String beneficiaryPukKey;

  int chainID = 16;

  String classname = "Class1";

  String issuerAddress;

  int issuerNonce = 0;

  String issuerPubKey;

  String namespace = "NS1";

  final String fullAssetID = namespace + "|" + classname;

  String reference = "reference";

  StateSnapshot state1;

  String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

  String subjectAddress;

  int subjectNonce = 0;

  String subjectPukKey;


  private void initEncumberTest() {

    issuerPubKey = getRandomPublicKey();
    issuerAddress = AddressUtil.publicKeyToAddress(issuerPubKey, AddressType.NORMAL);

    subjectPukKey = getRandomPublicKey();
    subjectAddress = AddressUtil.publicKeyToAddress(subjectPukKey, AddressType.NORMAL);

    beneficiaryPukKey = getRandomPublicKey();
    beneficiaryAddress = AddressUtil.publicKeyToAddress(beneficiaryPukKey, AddressType.NORMAL);

    administratorPukKey = getRandomPublicKey();
    administratorAddress = AddressUtil.publicKeyToAddress(administratorPukKey, AddressType.NORMAL);

  }


  @Test
  public void lockHoldingSimple() throws Exception {

    initEncumberTest();

    /*
    byte[][] pubpriv = KeyGen.generatePublicPrivateKeyPair();
    sendTx.log("Priv:" + ByteUtil.bytesToHex(pubpriv[1]));
    sendTx.log("Pub:" + ByteUtil.bytesToHex(pubpriv[0]));
    */

    state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertFalse(s0.isAssetLocked(namespace));
    assertFalse(s0.isAssetLocked(fullAssetID));

    EncumbranceDetail[] administrators =
        new EncumbranceDetail[]{new EncumbranceDetail(administratorAddress, 20L, 40L), new EncumbranceDetail(beneficiaryAddress, 40L, 0L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(beneficiaryAddress, 202L, 404L),
        new EncumbranceDetail(beneficiaryAddress, 0L, 0L)};
    final EncumbranceDetail[] issuer = new EncumbranceDetail[]{new EncumbranceDetail(issuerAddress, 0L, Long.MAX_VALUE)};

    Map<String, Object> txMap = new TreeMap<>();
    // txMap.put("reference", reference);
    txMap.put("administrators", administrators);
    // txMap.put("beneficiaries", beneficiaries);

    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, subjectNonce++, subjectPukKey, subjectAddress, namespace, classname, "", lockAmount, "", "", "");

    s0.commit();
    s0 = state1.createSnapshot();
    AddressEncumbrances thisAddressEncumbrance = state1.getEncumbrances().find(subjectAddress);
    assertNull(thisAddressEncumbrance);

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // Fail Checkonly : Not the Issuer
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    s0.commit();
    s0 = state1.createSnapshot();
    thisAddressEncumbrance = state1.getEncumbrances().find(subjectAddress);
    assertNull(thisAddressEncumbrance);

    // OK, except that only the issuer can lock !
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // try again as issuer :

    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, lockAmount, "", "", "");

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK, now that the issuer is locking !
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);

    assertNotNull(thisAddressEncumbrance);

    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount);

    // Check that the Encumbrance (Lock) entry looks as expected.
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    EncumbranceEntry testEntry =
        new EncumbranceEntry(
            ISSUER_LOCK,
            lockAmount,
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(issuer)));

    testEntry.priority = thisEntry.priority; // Fix.
    assertEquals(thisEntry, testEntry);
    s0 = state1.createSnapshot();

    // Fail on Timestamp
    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, lockAmount, "", "", "");
    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Fail Address not match Public Key
    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, "dross", namespace, classname, subjectAddress, lockAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // UnLockHolding :-

    thisTX = io.setl.bc.pychain.tx.create.UnlockHolding
        .unlockHoldingUnsigned(chainID, subjectNonce++, subjectPukKey, subjectAddress, namespace, classname, subjectAddress, lockAmount, "", "", "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // Fail Checkonly : Not the Issuer
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // Fail : Not the Issuer
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Lock still in place

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount);

    thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    assertEquals(thisEntry.amount, lockAmount);

    // try again as issuer :

    thisTX = io.setl.bc.pychain.tx.create.UnlockHolding
        .unlockHoldingUnsigned(chainID, subjectNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, halfLockAmount, "", "", "");

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly : Is the Issuer
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // Check Lock still in place

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount);

    thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    assertEquals(thisEntry.amount, lockAmount);

    // OK : Is the Issuer
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Lock has reduced

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount - halfLockAmount);

    thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    assertEquals(thisEntry.amount, lockAmount - halfLockAmount);

    // remove the rest

    thisTX = io.setl.bc.pychain.tx.create.UnlockHolding
        .unlockHoldingUnsigned(chainID, subjectNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, halfLockAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Lock has gone

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);

    assertNull(thisAddressEncumbrance);
  }


  @Test
  public void lockHoldingVsEncumbrance() throws Exception {

    initEncumberTest();

    /*
    byte[][] pubpriv = KeyGen.generatePublicPrivateKeyPair();
    sendTx.log("Priv:" + ByteUtil.bytesToHex(pubpriv[1]));
    sendTx.log("Pub:" + ByteUtil.bytesToHex(pubpriv[0]));
    */

    state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertFalse(s0.isAssetLocked(namespace));
    assertFalse(s0.isAssetLocked(fullAssetID));

    EncumbranceDetail[] administrators =
        new EncumbranceDetail[]{new EncumbranceDetail(administratorAddress, 20L, 40L), new EncumbranceDetail(beneficiaryAddress, 40L, 0L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(beneficiaryAddress, 202L, 404L),
        new EncumbranceDetail(beneficiaryAddress, 0L, 0L)};
    final EncumbranceDetail[] issuer = new EncumbranceDetail[]{new EncumbranceDetail(issuerAddress, 0L, Long.MAX_VALUE)};

    // Pre-Existing Encumbrance

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", reference);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);

    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, subjectNonce++, subjectPukKey, subjectAddress, namespace, classname, "", lockAmount, txMap, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // 'Lock' Encumbrance

    txMap = new TreeMap<>();
    // txMap.put("reference", reference);
    txMap.put("administrators", administrators);
    // txMap.put("beneficiaries", beneficiaries);

    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, lockAmount, "", "", "");

    s0.commit();
    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);
    assertEquals(1, thisAddressEncumbrance.getEncumbranceList().size());

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    s0.commit();

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);
    assertEquals(1, thisAddressEncumbrance.getEncumbranceList().size());
    assertNotNull(thisAddressEncumbrance.getEncumbranceList().get(fullAssetID));

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);

    assertNotNull(thisAddressEncumbrance);
    assertEquals(1, thisAddressEncumbrance.getEncumbranceList().size());
    assertNotNull(thisAddressEncumbrance.getEncumbranceList().get(fullAssetID));

    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount * 2);

    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    assertEquals(0, thisEntry.priority); // Issuer Lock is 'high' priority

    thisEntry = thisAssetEncumbrances.getByReference(reference).get(0);
    assertEquals(10, thisEntry.priority); // Pre-Existing Encumbrance priority

    thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    EncumbranceEntry testEntry =
        new EncumbranceEntry(
            ISSUER_LOCK,
            lockAmount,
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(issuer)));

    testEntry.priority = thisEntry.priority; // Fix.
    assertEquals(thisEntry, testEntry);

    // Fail on Timestamp
    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, lockAmount, "", "", "");
    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Fail Address not match Public Key
    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, "dross", namespace, classname, subjectAddress, lockAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Add an 'issuer' Lock

    thisTX = io.setl.bc.pychain.tx.create.LockHolding
        .lockholdingUnsigned(chainID, issuerNonce++, issuerPubKey, issuerAddress, namespace, classname, subjectAddress, lockAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    s0.commit();

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount * 3);

    thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    assertEquals(0, thisEntry.priority); // Issuer Encumbrance priority 'high'

    thisEntry = thisAssetEncumbrances.getByReference(reference).get(0);
    assertEquals(10, thisEntry.priority); // Pre-Existing Encumbrance priority


  }

}
