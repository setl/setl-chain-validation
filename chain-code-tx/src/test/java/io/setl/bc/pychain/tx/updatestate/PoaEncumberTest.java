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
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaEncumberTest extends BaseTestClass {

  final long exerciseAmount = 100;

  final long issueAmount = 1000;

  final long issueTransferAmount = 100;

  final long lockAmount = 200;

  final long transferAmount = 400;

  byte[] bPrivKey;

  byte[] bPubKey;

  int chainID = 16;

  String classname = "Class1";

  String namespace = "NS1";

  final String fullAssetID = namespace + "|" + classname;

  String reference = "reference";

  StateSnapshot state1;

  String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

  private Entity administrator = new Entity();

  private Entity beneficiary = new Entity();

  private Entity issuer = new Entity();
  
  private Entity subject = new Entity();

  private Entity attorney = new Entity();
  
  private int issuerNonce = 0;


  private int subjectNonce = 0;


  private void initPoaEncumberTest() {

  }


  @Test
  public void updatestate() throws Exception {

    initPoaEncumberTest();

    final String poaAddress = "demoAddress2";
    final String poaReference = "thisReference";
    final String protocol = "Proto";  // Base64(MsgPack(""))
    final String metadata = "Meta";  // Base64(MsgPack(""))
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poa = "poa";

    state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, classname, subject.address, issueAmount,
        "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertFalse(s0.isAssetLocked(namespace));
    assertFalse(s0.isAssetLocked(fullAssetID));

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(administrator.address, 20L, 40L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(beneficiary.address, 202L, 404L),
        new EncumbranceDetail(beneficiary.address, 0L, 0L)};

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", reference);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);

    thisTX = io.setl.bc.pychain.tx.create.PoaEncumber
        .encumberUnsigned(chainID, subjectNonce++, attorney.publicHex, attorney.address, subject.address,
            poaReference, namespace, classname, "", lockAmount, txMap, "", "", "");

    s0.commit();
    s0 = state1.createSnapshot();
    // Bad Tx ID.

    Object[] itemsData = new Object[]{new Object[]{TxType.REGISTER_ASSET_CLASS.getId(), 1, new String[]{fullAssetID}}};
    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, subject.publicHex, subject.address, poaReference, attorney.address, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();
    // Bad Asset

    itemsData = new Object[]{new Object[]{TxType.ENCUMBER_ASSET.getId(), 1, new String[]{"Dross"}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, subject.publicHex, subject.address, poaReference, attorney.address, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();
    // Bad Amount

    itemsData = new Object[]{new Object[]{TxType.ENCUMBER_ASSET.getId(), 1, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, subject.publicHex, subject.address, poaReference, attorney.address, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();
    // OK.

    itemsData = new Object[]{new Object[]{TxType.ENCUMBER_ASSET.getId(), lockAmount, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, subject.publicHex, subject.address, poaReference, attorney.address, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();

    itemsData = new Object[]{new Object[]{TxType.ENCUMBER_ASSET.getId(), lockAmount, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, subject.publicHex, subject.address, poaReference, attorney.address, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    AddressEncumbrances thisAddressEncumbrance = state1.getEncumbrances().find(subject.address);
    assertNull(thisAddressEncumbrance);

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    s0.commit();
    s0 = state1.createSnapshot();
    thisAddressEncumbrance = state1.getEncumbrances().find(subject.address);
    assertNull(thisAddressEncumbrance);

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    thisAddressEncumbrance = s0.getEncumbrances().find(subject.address);

    assertNotNull(thisAddressEncumbrance);

    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount);

    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(reference).get(0);
    assertNotNull(thisEntry);
    EncumbranceEntry testEntry =
        new EncumbranceEntry(
            reference,
            lockAmount,
            new ArrayList<EncumbranceDetail>(Arrays.asList(beneficiaries)),
            new ArrayList<EncumbranceDetail>(Arrays.asList(administrators)));
    testEntry.priority = thisEntry.priority; // Fix.
    assertEquals(thisEntry, testEntry);


  }

}
