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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POAS;
import static io.setl.common.CommonPy.TxType.COMMIT_TO_CONTRACT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;
import io.setl.util.CollectionUtils;
import java.time.Instant;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaAddTest extends BaseTestClass {

  ObjectEncodedState state1;

  String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";


  @Test
  public void updatestate() throws Exception {

    state1 = fileStateLoaded.loadStateFromFile(stateFile);

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

    PoaAddTx thisTX = PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    endDate = thisTX.getTimestamp() + 999;

    thisTX = PoaAdd.poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertFalse(UpdateState.update(thisTX, s0, 1L, thisTX.getPriority(), false));
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    PoaEntry addressPOA = s0.getPowerOfAttorneys().find(fromAddress);

    PoaHeader thisHeader = addressPOA.getReference(thisTX.getReference());
    assertNotNull(thisHeader);
    assertEquals(thisHeader.startDate, startDate);
    assertEquals(thisHeader.expiryDate, endDate);
    assertEquals(thisHeader.reference, thisTX.getReference());

    PoaEntry poaDetailEntry = s0.getPowerOfAttorneys().find(addressPOA.getFullReference(thisTX.getReference()));

    PoaDetail thisDetail = poaDetailEntry.getPoaDetail();
    assertEquals(thisDetail.getReference(), thisTX.getReference());
    assertEquals(thisDetail.getAttorneyAddress(), thisTX.getAttorneyAddress());
    assertEquals(thisDetail.getIssuerAddress(), thisTX.getFromAddress());
    assertEquals(thisDetail.getStartTime(), startDate);
    assertEquals(thisDetail.getEndTime(), endDate);
    assertNull(thisDetail.getItem(TxType.WFL_HOLD1));
    PoaItem thisItem = thisDetail.getItem(TxType.CREATE_MEMO).get(0);
    assertEquals(thisItem.getAmount(), amount);
    assertEquals(TxType.CREATE_MEMO, thisItem.getTxType());
    assertEquals(thisItem.getAssets(), CollectionUtils.set("Asset1", "Asset2"));
  }

  @Test
  public void updatestate2() throws Exception {

    state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    String fromPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String fromAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    String attorneyAddress = "157XPPiV4Nc4xdrFUDJeec2W2RrSTe3fHo";

    int chainID = 16;
    long nonce = 123456;
    final String reference = "toKey2";
    final Long amount = 1000L;
    final long startDate = 0L;
    long endDate = 0L;
    final Object[] itemsData = new Object[]{
        new Object[]{3, 1, new String[]{"CompanyX|Rights"}},
        new Object[]{3, 1, new String[]{"BankX|GBP"}},
        new Object[]{0x92, amount, new String[]{"CompanyX|Rights"}},
        new Object[]{0x92, amount, new String[]{"BankX|GBP"}},
    };
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    PoaAddTx thisTX = PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, attorneyAddress, startDate, Instant.now().getEpochSecond() + 999, itemsData, protocol,
            metadata, poa);

    // Fail , Bad Time
    assertFalse(UpdateState.update(thisTX, s0, 0, thisTX.getPriority(), false));

    thisTX = PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, attorneyAddress, startDate, Instant.now().getEpochSecond() + 999, itemsData, protocol,
            metadata, poa);

    // Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    assertNotSame(SuccessType.PASS, UpdateStateUtils.checkPoaTransactionPermissions(s0, Instant.now().getEpochSecond(), reference, attorneyAddress,
        fromAddress, COMMIT_TO_CONTRACT, new String[]{"CompanyX|Rights"}, amount, false).success);

    // For real...
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertSame(SuccessType.PASS, UpdateStateUtils.checkPoaTransactionPermissions(s0, Instant.now().getEpochSecond(), reference, attorneyAddress,
        fromAddress, COMMIT_TO_CONTRACT, new String[]{"CompanyX|Rights"}, amount, false).success);


    // Address permissions
    s0 = state1.createSnapshot();

    AbstractTx thisAbsTX = RegisterAddress.registerAddressUnsigned(chainID, 0, fromPubKey, fromAddress, "", "", "");
    assertTrue(UpdateState.update(thisAbsTX, s0, thisAbsTX.getTimestamp(), thisAbsTX.getPriority(), false));

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    thisTX = PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, attorneyAddress, startDate, Instant.now().getEpochSecond() + 999, itemsData, protocol,
            metadata, poa);

    // No perms, fail
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(fromAddress);
    fromAddressEntry.setAddressPermissions(AP_POAS);

    // Has perms, OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));


  }

}
