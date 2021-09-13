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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.PoaAssetIssueEncumberTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

public class PoaAssetIssueAndEncumberTest extends BaseTestClass {

  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void updatestate1() throws Exception {

    // Test PoaIssueAssetWithEncumbrance for 'default' encumbrance (given 'reference' only, no admin or beneficiaries).
    // Just testing for Poa parts as the other code functionality is tested in the non-poa transaction tests.

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String metadata = "meta";
    final String protocol = "prot";
    final String poa = "poa";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    String pubKey3 = getRandomPublicKey();

    final String poaPubKey = pubKey3;
    final String poaAddress = AddressUtil.publicKeyToAddress(pubKey3, AddressType.NORMAL);
    final String reference = "poaRef1";
    final String reference2 = "poaRef2";

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    PoaAssetIssueEncumberTx assissTX;
    AbstractTx thisTX;

    //

    Map<String, Object> txDict = new TreeMap<>();
    txDict.put("reference", reference);
    // txMap.put("administrators", administrators);
    // txMap.put("beneficiaries", beneficiaries);

    // ----

    // Register Namespace
    thisTX = io.setl.bc.pychain.tx.create.NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Class
    thisTX = io.setl.bc.pychain.tx.create.AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Address
    thisTX = io.setl.bc.pychain.tx.create.RegisterAddress.registerAddressUnsigned(chainID, addressNonce, poaPubKey, poaAddress, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Register Address
    thisTX = io.setl.bc.pychain.tx.create.RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Now OK
    assissTX = io.setl.bc.pychain.tx.create.PoaAssetIssueAndEncumber
        .poaAssetIssueAndEncumberUnsigned(chainID, addressNonce, poaPubKey, poaAddress, address, reference, namespace, classname, toAddress2,
            issueAmount, txDict, "", "", "");

    // POA not exist

    ReturnTuple rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "No POAs for POA Address " + address);

    // Grant POA

    Object[] itemsData = new Object[]{new Object[]{TxType.ISSUE_ASSET.getId(), issueAmount, new String[]{fullAssetID}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey, address, reference, poaAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Wrong PoA
    // 'No POA item for this effective TX Type : 28'

    rTup = AssetIssueAndEncumber.updatestate(assissTX, s0, assissTX.getTimestamp(), assissTX.getPriority(), false);
    assertNotEquals(rTup.success, SuccessType.PASS);
    assertEquals(rTup.status, "No POA item for this effective TX Type : " + TxType.ISSUE_AND_ENCUMBER_ASSET);

    // Correct Poa

    itemsData = new Object[]{new Object[]{TxType.ISSUE_AND_ENCUMBER_ASSET.getId(), issueAmount, new String[]{fullAssetID}}};

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey, address, reference2, poaAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // POA exist

    assissTX = io.setl.bc.pychain.tx.create.PoaAssetIssueAndEncumber
        .poaAssetIssueAndEncumberUnsigned(chainID, addressNonce, poaPubKey, poaAddress, address, reference2, namespace, classname, toAddress2,
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
    assertEquals(0L, thisDetail.startTime);
    assertEquals(thisDetail.endTime, Long.MAX_VALUE);

    thisDetail = thisEntry.getBeneficiaries().get(0);
    assertEquals(thisDetail.address, address);
    assertEquals(0L, thisDetail.startTime);
    assertEquals(thisDetail.endTime, Long.MAX_VALUE);


  }


}
