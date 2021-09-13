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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class UnEncumberTest extends BaseTestClass {

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
    final long lockAmount = 200;
    final long unlockAmount = 100;

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

    assertFalse(s0.isAssetLocked(namespace));
    assertFalse(s0.isAssetLocked(fullAssetID));

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(getRandomAddress(), 20L, 40L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(getRandomAddress(), 202L, 404L)};

    final String reference = "reference";

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", reference);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);

    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", lockAmount, txMap, "", "", "");

    ReturnTuple rTup = Encumber.updatestate((EncumberTx)thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);
    assertEquals(rTup.success, SuccessType.PASS);

    s0.commit();

    AddressEncumbrances thisAddressEncumbrance = state1.getEncumbrances().find(address);

    assertNotNull(thisAddressEncumbrance);

    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount);

    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference("reference").get(0);
    EncumbranceEntry testEntry =
        new EncumbranceEntry(
            reference,
            lockAmount,
            new ArrayList<EncumbranceDetail>(Arrays.asList(beneficiaries)),
            new ArrayList<EncumbranceDetail>(Arrays.asList(administrators)));
    testEntry.priority = thisEntry.priority; // Fix.
    assertEquals(thisEntry, testEntry);

    s0 = state1.createSnapshot();

    // OK, Now Un-Encumber

    thisTX = io.setl.bc.pychain.tx.create.UnEncumber
        .unencumberUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", reference, unlockAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisAddressEncumbrance = s0.getEncumbrances().find(address);
    assertEquals(thisAddressEncumbrance.getEncumbranceTotal(address, fullAssetID, 0), lockAmount - unlockAmount);

    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getByReference("reference").get(0);
    assertNotNull(thisEntry);

    thisTX = io.setl.bc.pychain.tx.create.UnEncumber
        .unencumberUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", reference, (lockAmount - unlockAmount), "", "", "");

    //

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));
    thisTX.setTimestamp(Instant.now().getEpochSecond());

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    // assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    thisAddressEncumbrance = s0.getEncumbrances().find(address);
    assertEquals(thisAddressEncumbrance.getEncumbranceTotal(address, fullAssetID, 0), lockAmount - unlockAmount);

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertEquals(new Balance(0L), thisAddressEncumbrance.getEncumbranceTotal(address, fullAssetID, 0));

    thisEntry = null;

    if (!thisAssetEncumbrances.getByReference("reference").isEmpty()) {
      thisEntry = thisAssetEncumbrances.getByReference("reference").get(0);
    }

    assertNull(thisEntry);

    // Fail on Timestamp
    thisTX = io.setl.bc.pychain.tx.create.UnEncumber
        .unencumberUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, "", reference, unlockAmount, "", "", "");
    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Fail Address not match Public Key
    thisTX = io.setl.bc.pychain.tx.create.UnEncumber
        .unencumberUnsigned(chainID, addressNonce, pubKey, "duff", namespace, classname, "", reference, unlockAmount, "", "", "");
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

  }

}
