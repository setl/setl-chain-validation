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

import static io.setl.common.Balance.BALANCE_ZERO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaAssetIssue;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaAssetIssueTest extends BaseTestClass {

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
    final String poaPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    final String poaAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    final String poaReference = "poaRef1";
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String metadata = "meta";
    final String protocol = "prot";
    final String poa = "poa";

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = PoaAssetIssue
        .poaAssetIssueUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, address, poaReference, namespace, classname, toAddress2, issueAmount, "", "",
            "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Grant POA

    Object[] itemsData = new Object[]{new Object[]{TxType.ISSUE_ASSET.getId(), issueAmount, new String[]{fullAssetID}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, pubKey, address, poaReference, poaAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();

    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(newValue, issueAmount);

  }

}
