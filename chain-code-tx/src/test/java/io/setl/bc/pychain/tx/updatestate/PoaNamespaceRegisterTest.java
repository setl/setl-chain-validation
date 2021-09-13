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

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import org.junit.Test;

public class PoaNamespaceRegisterTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;

    String attorneyPubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String poaPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String poaAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";
    final long startDate = 0L;
    long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";
    final Object[] itemsData = new Object[]{new Object[]{TxType.REGISTER_NAMESPACE.getId(), 1, new String[]{namespace, fullAssetID}}};

    String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    String toAddress2 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    AbstractTx thisTX = io.setl.bc.pychain.tx.create.PoaNamespaceRegister
        .poaNamespaceRegisterUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, "", "", "");

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

    assertNull("", namespaceEntry);

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertEquals("", namespaceEntry.getAddress(), poaAddress);

    s0.commit();

    namespaceEntry = state1.getNamespaces().find(namespace);
    assertNotNull("", namespaceEntry);
    assertEquals("", namespaceEntry.getAddress(), poaAddress);

    PoaEntry thisEntry = state1.getPowerOfAttorneys().find(poaAddress + "|" + poaReference);
    assertNull(thisEntry);
  }

}
