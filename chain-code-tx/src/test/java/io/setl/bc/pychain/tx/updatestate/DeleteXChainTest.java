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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_XCHAINING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.DeleteXChain;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DeleteXChainTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    String metadata = "this metadata";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    long issueAmount = 1000;
    long issueTransferAmount = 100;
    long transferAmount = 400;
    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    final String sigPubKey1 = "mary had a little lamb";
    final Long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final Long sigAmount2 = 87654321L;

    String pubKey = getRandomPublicKey();
    int addressNonce = 0;
    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set Chain 1 with empty Signode data. Fail.
    thisTX = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, chainID + 1, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set Chain 2 with Signode data.

    thisTX = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, chainID + 2, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set with no Signode data to 'Update' Params. Check Signodes persist.

    thisTX = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, chainID + 2, 0, newChainParams, null, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    s0 = state1.createSnapshot();

    XChainDetails chainDetails1 = s0.getXChainSignNodesValue(chainID + 1);
    XChainDetails chainDetails2 = s0.getXChainSignNodesValue(chainID + 2);
    XChainDetails chainDetails3 = s0.getXChainSignNodesValue(chainID + 3);

    assertNull(chainDetails1);
    assertNotNull(chainDetails2);
    assertNull(chainDetails3);

    // Set with no Signnode data to 'Update' Params. Check Signodes persist.

    thisTX = DeleteXChain.deleteXChainUnsigned(chainID, addressNonce++, pubKey, address, chainID + 2, "meta", "poa");
    final AbstractTx saveTX = thisTX;

    s0 = state1.createSnapshot();

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);
    fromAddressEntry.setAddressPermissions(AP_XCHAINING);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Reset
    s0 = state1.createSnapshot();

    //  More tests.

    thisTX = saveTX;

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    chainDetails2 = s0.getXChainSignNodesValue(chainID + 2);

    Map<String, Balance> gotSigList = chainDetails2.getSignNodes();
    assertNotNull(gotSigList);
    assertEquals(0, gotSigList.size());
    assertEquals(chainDetails2.getStatus(), -1L);
  }

}
