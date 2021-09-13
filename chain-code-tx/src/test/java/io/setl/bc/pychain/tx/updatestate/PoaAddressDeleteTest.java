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

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaAddressDelete;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import org.junit.Test;

public class PoaAddressDeleteTest extends BaseTestClass {

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
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String address2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    final Object[] itemsData = new Object[]{new Object[]{TxType.DELETE_ADDRESS.getId(), 1, new String[]{"Crud", "*", "Crud2"}}};

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX;

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);

    assertNotNull(fromAddressEntry); // Address Exists
    assertEquals(fromAddressEntry.getAddress(), address); // Address is correct
    assertTrue(fromAddressEntry.getClassBalance().isEmpty()); // There are no assets

    // Now delete

    thisTX = PoaAddressDelete.poaAddressDeleteUnsigned(chainID, addressNonce++, pubKey2, address2, address, poaReference, "", "", "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Grant POA

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey, address, poaReference, address2, startDate, endDate, itemsData, "", "", "");
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));
    assertTrue(s0.getPowerOfAttorneys().itemExists(address));

    // Try again.

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);

    assertNull(fromAddressEntry); // Address Not Exist
    assertFalse(s0.getPowerOfAttorneys().itemExists(address)); // PoAs for address have been deleted
  }

}
