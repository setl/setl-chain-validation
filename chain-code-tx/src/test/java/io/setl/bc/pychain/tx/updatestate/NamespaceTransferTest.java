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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_NAMESPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.NamespaceTransfer;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class NamespaceTransferTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/20/e00f2e0ddc1e76879ce56437a2d153c7f039a44a784e9c659a5c581971c33999";
    final String namespace = "testNS";
    final String classname = "testClass";
    final int chainID = 20;
    final long issueAmount = 1000;

    String pubKey = getRandomPublicKey();
    final String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    int addressNonce = 0;

    final String masterPubKey = "a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58";
    final String masterAddress = "1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb";
    int masterNonce = 0;

    final String toAddress = "ABO66opxVprUsUl8drBjgAiNCN5YhPLQdw";

    AbstractTx thisTX;

    /*
     * SYS|STAKE controlling address = '1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb'
     * SYS|STAKE controlling publicKey = 'a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58'
     */

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, classname, address, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    thisTX = NamespaceTransfer.namespaceTransferUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, toAddress, "");
    final AbstractTx saveTX = thisTX;

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<NamespaceEntry> namespaceTree = s0.getNamespaces();
    NamespaceEntry namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertEquals("", toAddress, namespaceEntry.getAddress()); // Has moved
    assertTrue("", namespaceEntry.containsAsset(classname)); // Still contains the class

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress);
    Map<String, Balance> issuanceBalances = fromAddressEntry.getClassBalance();

    assertEquals("", issuanceBalances.get(namespace + "|" + classname), -issueAmount);

    // Fail on Timestamp
    thisTX = NamespaceTransfer.namespaceTransferUnsigned(chainID, masterNonce, masterPubKey, masterAddress, namespace, toAddress, "");
    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Fail Address not match Public Key
    thisTX = NamespaceTransfer.namespaceTransferUnsigned(chainID, masterNonce, masterPubKey, "duffAddress", namespace, toAddress, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Not Own
    thisTX = NamespaceTransfer.namespaceTransferUnsigned(chainID, masterNonce, masterPubKey, masterAddress, namespace, toAddress, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Reset to known state.
    s0 = state1.createSnapshot();

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertEquals("", masterAddress, namespaceEntry.getAddress()); // Not moved

    //
    thisTX = saveTX;

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(masterAddress);
    fromAddressEntry.setAddressPermissions(AP_NAMESPACES);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    namespaceTree = s0.getNamespaces();
    namespaceEntry = namespaceTree.find(namespace);

    assertNotNull("", namespaceEntry);
    assertEquals("", toAddress, namespaceEntry.getAddress()); // Has moved
    assertTrue("", namespaceEntry.containsAsset(classname)); // Still contains the class

  }

}
