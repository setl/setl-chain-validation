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
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaLockHoldingTest extends BaseTestClass {

  final long issueAmount = 1000;

  final long issueTransferAmount = 100;

  final long lockAmount = 200;

  final long transferAmount = 400;

  int chainID = 16;

  String classname = "Class1";

  String namespace = "NS1";

  final String fullAssetID = namespace + "|" + classname;

  String reference = "reference";

  StateSnapshot state1;

  String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

  private String administratorPukKey;

  private String beneficiaryPukKey;

  private String issuerAddress;

  private int issuerNonce = 0;

  private String issuerPubKey;

  private String subjectAddress;

  private int subjectNonce = 0;

  private String subjectPubKey;


  private void initPoaTest() {

    issuerPubKey = getRandomPublicKey();
    issuerAddress = AddressUtil.publicKeyToAddress(issuerPubKey, AddressType.NORMAL);

    subjectPubKey = getRandomPublicKey();
    subjectAddress = AddressUtil.publicKeyToAddress(subjectPubKey, AddressType.NORMAL);

    beneficiaryPukKey = getRandomPublicKey();

    administratorPukKey = getRandomPublicKey();
  }


  @Test
  public void updatestate() throws Exception {

    initPoaTest();

    final String poaAddress = "demoAddress2";
    final String poaReference = "thisReference";
    final String protocol = "Proto";  // Base64(MsgPack(""))
    final String metadata = "Meta";  // Base64(MsgPack(""))
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poa = "poa";

    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Set Up.

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

    final EncumbranceDetail[] issuer = new EncumbranceDetail[]{new EncumbranceDetail(issuerAddress, 0L, Long.MAX_VALUE)};

    thisTX = io.setl.bc.pychain.tx.create.PoaLockHolding
        .poaLockholdingUnsigned(chainID, subjectNonce++, attorneyPubKey, attorneyAddress, issuerAddress,
            poaReference, namespace, classname, subjectAddress, lockAmount, "", "", "");

    s0.commit();
    s0 = state1.createSnapshot();
    // Bad Tx ID.

    Object[] itemsData = new Object[]{new Object[]{TxType.REGISTER_ASSET_CLASS.getId(), 1, new String[]{fullAssetID}}};
    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, issuerNonce, issuerPubKey, issuerAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();
    // Bad Asset

    itemsData = new Object[]{new Object[]{TxType.LOCK_ASSET_HOLDING.getId(), 1, new String[]{"Dross"}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, issuerNonce, issuerPubKey, issuerAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();
    // Bad Amount

    itemsData = new Object[]{new Object[]{TxType.LOCK_ASSET_HOLDING.getId(), 1, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, issuerNonce, issuerPubKey, issuerAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();
    // OK.

    itemsData = new Object[]{new Object[]{TxType.LOCK_ASSET_HOLDING.getId(), lockAmount, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, issuerNonce, issuerPubKey, issuerAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();

    itemsData = new Object[]{
        new Object[]{
            TxType.LOCK_ASSET_HOLDING.getId(),
            lockAmount,
            new String[]{fullAssetID}},
        new Object[]{
            TxType.UNLOCK_ASSET_HOLDING.getId(),
            lockAmount,
            new String[]{fullAssetID}
        }
    };

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, issuerNonce, issuerPubKey, issuerAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    AddressEncumbrances thisAddressEncumbrance = state1.getEncumbrances().find(subjectAddress);
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
    thisAddressEncumbrance = state1.getEncumbrances().find(subjectAddress);
    assertNull(thisAddressEncumbrance);

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    thisAddressEncumbrance = s0.getEncumbrances().find(subjectAddress);

    assertNotNull(thisAddressEncumbrance);

    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);

    assertNotNull(thisAssetEncumbrances);
    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount);

    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(ISSUER_LOCK).get(0);
    assertNotNull(thisEntry);
    EncumbranceEntry testEntry =
        new EncumbranceEntry(
            ISSUER_LOCK,
            lockAmount,
            new ArrayList<EncumbranceDetail>(),
            new ArrayList<EncumbranceDetail>(Arrays.asList(issuer)));
    testEntry.priority = thisEntry.priority; // Fix.
    assertEquals(thisEntry, testEntry);

    // Now test UnLock

    thisTX = io.setl.bc.pychain.tx.create.PoaUnlockHolding
        .poaUnlockHoldingUnsigned(chainID, subjectNonce++, attorneyPubKey, attorneyAddress, issuerAddress,
            poaReference, namespace, classname, subjectAddress, lockAmount, "", "", "");

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
  }

}
