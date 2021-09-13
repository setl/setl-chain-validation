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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_ADDRESSES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RegisterAddressTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX;

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");

    // Fail on too old
    assertFalse(UpdateState.update(thisTX, s0, 1L, thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, "crud", "", "", "");

    // Fail on bad address
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");

    // OK
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);

    assertNotNull(fromAddressEntry); // Address Exists
    assertEquals(fromAddressEntry.getAddress(), address); // Address is correct
    assertTrue(fromAddressEntry.getClassBalance().isEmpty()); // There are no assets

    //
    s0.setConfigValue("authorisebyaddress", 1);

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, ++addressNonce, pubKey, address, toAddress2, "", "");

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    fromAddressEntry.setAddressPermissions(AP_ADDRESSES);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

  }


  @Test
  public void updatestate_xChain() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    int chainID2 = 20;

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();

    final String sigPubKey1 = "mary had a little lamb";
    final Long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final Long sigAmount2 = 87654321L;

    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    String pubKey = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX;

    thisTX = RegisterAddress.registerAddressUnsigned(chainID2, addressNonce, pubKey, address, "", "", "");

    // Fail : From unknown chain

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);

    assertNull(fromAddressEntry); // Address Exists

    // Register Chain

    AbstractTx thisTXxc = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, chainID2, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTXxc, s0, thisTXxc.getTimestamp(), thisTXxc.getPriority(), false));

    // OK

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);

    assertNotNull(fromAddressEntry); // Address Exists
    assertEquals(fromAddressEntry.getAddress(), address); // Address is correct
    assertTrue(fromAddressEntry.getClassBalance().isEmpty()); // There are no assets


  }


}
