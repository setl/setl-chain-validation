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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POAS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.PoaDeleteTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import io.setl.util.CollectionUtils;
import java.time.Instant;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaDeleteTest extends BaseTestClass {

  StateSnapshot state1;

  String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";


  @Test
  public void updatestate() throws Exception {

    state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    String fromPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String fromAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    String returnAddress = "157XPPiV4Nc4xdrFUDJeec2W2RrSTe3fHo";

    int chainID = 16;
    long nonce = 123456;
    final String reference = "toKey2";
    final Long amount = 65748L;
    final long startDate = 0L;
    long endDate = 0L;
    final Object[] itemsData = new Object[]{new Object[]{TxType.CREATE_MEMO.getId(), amount, new String[]{"Asset1", "Asset2"}}};
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    PoaAddTx poaAddTx = io.setl.bc.pychain.tx.create.PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertFalse(UpdateState.update(poaAddTx, s0, poaAddTx.getTimestamp(), poaAddTx.getPriority(), false));

    endDate = poaAddTx.getTimestamp() + 999;

    poaAddTx = PoaAdd.poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertTrue(UpdateState.update(poaAddTx, s0, poaAddTx.getTimestamp(), poaAddTx.getPriority(), false));

    s0.commit();

    PoaEntry addressPOA = s0.getPowerOfAttorneys().find(fromAddress);

    PoaHeader thisHeader = addressPOA.getReference(poaAddTx.getReference());
    assertNotNull(thisHeader);
    assertEquals(thisHeader.startDate, startDate);
    assertEquals(thisHeader.expiryDate, endDate);
    assertEquals(thisHeader.reference, poaAddTx.getReference());

    PoaEntry poaDetailEntry = s0.getPowerOfAttorneys().find(addressPOA.getFullReference(poaAddTx.getReference()));

    PoaDetail thisDetail = poaDetailEntry.getPoaDetail();
    assertEquals(thisDetail.getReference(), poaAddTx.getReference());
    assertEquals(thisDetail.getAttorneyAddress(), poaAddTx.getAttorneyAddress());
    assertEquals(thisDetail.getIssuerAddress(), poaAddTx.getFromAddress());
    assertEquals(thisDetail.getStartTime(), startDate);
    assertEquals(thisDetail.getEndTime(), endDate);
    assertNull(thisDetail.getItem(TxType.WFL_HOLD3));
    PoaItem thisItem = thisDetail.getItem(TxType.CREATE_MEMO).get(0);
    assertEquals(thisItem.getAmount(), amount);
    assertEquals(TxType.CREATE_MEMO, thisItem.getTxType());
    assertEquals(thisItem.getAssets(), CollectionUtils.set("Asset1", "Asset2"));

    PoaDeleteTx thisTX = io.setl.bc.pychain.tx.create.PoaDelete
        .poaDeleteUnsigned(chainID, nonce, fromPubKey, fromAddress, "", reference, protocol, metadata, poa);

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    addressPOA = s0.getPowerOfAttorneys().find(fromAddress);

    assertNull(addressPOA);

    addressPOA = s0.getPowerOfAttorneys().find(fromAddress + "|" + reference);

    assertNull(addressPOA);

  }


  @Test
  public void updatestate2() throws Exception {

    state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    String fromPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String fromAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    String returnAddress = "157XPPiV4Nc4xdrFUDJeec2W2RrSTe3fHo";

    String emptyPubKey = getRandomPublicKey();

    final String emptyAddress = AddressUtil.publicKeyToAddress(emptyPubKey, AddressType.NORMAL);

    int chainID = 16;
    long nonce = 123456;
    final String reference = "toKey2";
    final Long amount = 65748L;
    final long startDate = 0L;
    long endDate = 0L;
    final Object[] itemsData = new Object[]{new Object[]{TxType.CREATE_MEMO.getId(), amount, new String[]{"Asset1", "Asset2"}}};
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    PoaAddTx poaAddTx = io.setl.bc.pychain.tx.create.PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    endDate = poaAddTx.getTimestamp() + 999;

    poaAddTx = PoaAdd.poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertTrue(UpdateState.update(poaAddTx, s0, poaAddTx.getTimestamp(), poaAddTx.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // Some bad deletes ...

    // Bad Chain

    PoaDeleteTx thisTX = io.setl.bc.pychain.tx.create.PoaDelete
        .poaDeleteUnsigned(chainID + 1, nonce, fromPubKey, fromAddress, "", reference, protocol, metadata, poa);

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Timestamp
    thisTX = io.setl.bc.pychain.tx.create.PoaDelete
        .poaDeleteUnsigned(chainID, nonce, fromPubKey, fromAddress, "", reference, protocol, metadata, poa);
    thisTX.setTimestamp(1);

    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Bad From Address

    thisTX = io.setl.bc.pychain.tx.create.PoaDelete
        .poaDeleteUnsigned(chainID, nonce, fromPubKey, "badFrom", "", reference, protocol, metadata, poa);

    // PoaDelete class checks 'fromAddress' returns null if is is bad
    assertNull(thisTX);

    thisTX = new PoaDeleteTx(chainID, null, nonce, false, fromPubKey, "badFrom", "", reference, protocol, metadata, "", 1, "", 1L);
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // No POAs

    thisTX = io.setl.bc.pychain.tx.create.PoaDelete
        .poaDeleteUnsigned(chainID, nonce, emptyPubKey, emptyAddress, "", reference, protocol, metadata, poa);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(fromAddress);
    if (fromAddressEntry == null) {
      fromAddressEntry = addDefaultAddressEntry(assetBalances, fromAddress, s0.getVersion());
    }
    fromAddressEntry.setAddressPermissions(AP_POAS);

    // Good delete, Check only

    thisTX = io.setl.bc.pychain.tx.create.PoaDelete
        .poaDeleteUnsigned(chainID, nonce, fromPubKey, fromAddress, "", reference, protocol, metadata, poa);

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // POA Still Exists

    PoaEntry addressPOA = s0.getPowerOfAttorneys().find(fromAddress);

    PoaHeader thisHeader = addressPOA.getReference(poaAddTx.getReference());
    assertNotNull(thisHeader);
    assertEquals(thisHeader.startDate, startDate);
    assertEquals(thisHeader.expiryDate, endDate);
    assertEquals(thisHeader.reference, poaAddTx.getReference());

    PoaEntry poaDetailEntry = s0.getPowerOfAttorneys().find(addressPOA.getFullReference(poaAddTx.getReference()));

    PoaDetail thisDetail = poaDetailEntry.getPoaDetail();
    assertEquals(thisDetail.getReference(), poaAddTx.getReference());
    assertEquals(thisDetail.getAttorneyAddress(), poaAddTx.getAttorneyAddress());
    assertEquals(thisDetail.getIssuerAddress(), poaAddTx.getFromAddress());
    assertEquals(thisDetail.getStartTime(), startDate);
    assertEquals(thisDetail.getEndTime(), endDate);
    assertNull(thisDetail.getItem(TxType.WFL_HOLD3));
    PoaItem thisItem = thisDetail.getItem(TxType.CREATE_MEMO).get(0);
    assertEquals(thisItem.getAmount(), amount);
    assertEquals(TxType.CREATE_MEMO, thisItem.getTxType());
    assertEquals(thisItem.getAssets(), CollectionUtils.set("Asset1", "Asset2"));

    // Good delete, final.

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    addressPOA = s0.getPowerOfAttorneys().find(fromAddress);

    assertNull(addressPOA);

    addressPOA = s0.getPowerOfAttorneys().find(fromAddress + "|" + reference);

    assertNull(addressPOA);

  }

}
