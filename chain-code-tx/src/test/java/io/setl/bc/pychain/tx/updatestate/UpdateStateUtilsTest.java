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

import static io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.tidyPoaReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.AbstractMutableMerkle;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import org.junit.Test;

public class UpdateStateUtilsTest extends BaseTestClass {


  @Test
  public void tidyPoaReferenceTest() throws Exception {

    String poaPubKey = getRandomPublicKey();
    String poaAddress = AddressUtil.publicKeyToAddress(poaPubKey, AddressType.NORMAL);

    String attorneyPubKey = getRandomPublicKey();
    String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String poaReference1 = "thisPoaRef1";
    final String poaReference2 = "thisPoaRef2";
    final long transferAmount = 400;
    final long startDate = 0L;
    long endDate = Instant.now().getEpochSecond() + 999L;
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int addressNonce = 0;
    int chainID = 16;

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    // Null Entry, should return with no Error.
    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference1, poaAddress);

    // Add POA
    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_FROM_MANY.getId(), (transferAmount * 3), new String[]{fullAssetID}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference1, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();
    MutableMerkle<PoaEntry> poaList = s0.getPowerOfAttorneys();
    PoaEntry addressPOA = poaList.find(poaAddress);
    final String fullPoaRef1 = addressPOA.getFullReference(poaReference1);
    final String fullPoaRef2 = addressPOA.getFullReference(poaReference2);

    // Null Reference Entry, should return with no Error.
    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference2, poaAddress);

    // Check nothing changed
    assertEquals(1, ((AbstractMutableMerkle) s0.getPowerOfAttorneys()).getChangedEntriesCount());
    assertNotNull(poaList.find(poaAddress));
    assertNotNull(poaList.find(fullPoaRef1));

    // OK Pretend it's expired
    tidyPoaReference(s0, endDate + 1, poaReference1, poaAddress);

    // Expecting 2 changed entries and previous entries return Null.
    assertEquals(2, ((AbstractMutableMerkle) s0.getPowerOfAttorneys()).getChangedEntriesCount());
    assertNull(poaList.find(poaAddress));
    assertNull(poaList.find(fullPoaRef1));

    // Reset
    s0 = state1.createSnapshot();
    poaList = s0.getPowerOfAttorneys();
    addressPOA = poaList.find(poaAddress);

    // OK, if Detail is missing then reference is deleted also.
    poaList.delete(fullPoaRef1);
    assertNotNull(poaList.find(poaAddress));
    assertNull(poaList.find(fullPoaRef1));

    // Check
    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference1, poaAddress);
    assertEquals(2, ((AbstractMutableMerkle) s0.getPowerOfAttorneys()).getChangedEntriesCount());
    assertNull(poaList.find(poaAddress));
    assertNull(poaList.find(fullPoaRef1));

    // Reset
    s0 = state1.createSnapshot();
    poaList = s0.getPowerOfAttorneys();
    addressPOA = poaList.find(poaAddress);

    // If Poa remaining, should be no change...
    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference1, poaAddress);
    assertEquals(1, ((AbstractMutableMerkle) s0.getPowerOfAttorneys()).getChangedEntriesCount());
    assertNotNull(poaList.find(poaAddress));
    assertNotNull(poaList.find(fullPoaRef1));

    PoaItem thisItem = poaList.findAndMarkUpdated(fullPoaRef1).getPoaDetail().getItem(TxType.TRANSFER_ASSET_FROM_MANY).get(0);
    thisItem.consume(thisItem.getAmount().subtract(100));

    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference1, poaAddress);
    assertEquals(2, ((AbstractMutableMerkle) s0.getPowerOfAttorneys()).getChangedEntriesCount());
    assertNotNull(poaList.find(poaAddress));
    assertNotNull(poaList.find(fullPoaRef1));

    thisItem.consume(100);
    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference1, poaAddress);
    assertNull(poaList.find(poaAddress));
    assertNull(poaList.find(fullPoaRef1));

    // Reset
    s0 = state1.createSnapshot();
    poaList = s0.getPowerOfAttorneys();
    addressPOA = poaList.find(poaAddress);

    // Add second POA and check

    itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_FROM_MANY.getId(), (transferAmount * 3), new String[]{fullAssetID}}};

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference2, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Consume first POA
    thisItem = poaList.findAndMarkUpdated(fullPoaRef1).getPoaDetail().getItem(TxType.TRANSFER_ASSET_FROM_MANY).get(0);
    thisItem.consume(thisItem.getAmount());

    // Tidy
    tidyPoaReference(s0, Instant.now().getEpochSecond(), poaReference1, poaAddress);

    // First Detail is gone, Address and Second Detail remain.
    assertEquals(3, ((AbstractMutableMerkle) s0.getPowerOfAttorneys()).getChangedEntriesCount());
    assertNotNull(poaList.find(poaAddress));
    assertNull(poaList.find(fullPoaRef1));
    assertNotNull(poaList.find(fullPoaRef2));

  }
}
