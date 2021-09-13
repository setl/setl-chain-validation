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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_LOCKING;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.time.Instant;
import org.junit.Test;

public class LockAssetTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertFalse(s0.isAssetLocked(namespace));
    assertFalse(s0.isAssetLocked(fullAssetID));

    s0.commit();
    s0 = state1.createSnapshot();

    thisTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "", "");
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

    assertFalse(s0.isAssetLocked(namespace));
    assertTrue(s0.isAssetLocked(fullAssetID));

    // Reset to known state.
    s0 = state1.createSnapshot();

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    //
    thisTX = saveTX;

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(address);
    fromAddressEntry.setAddressPermissions(AP_LOCKING);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    assertFalse(s0.isAssetLocked(namespace));
    assertTrue(s0.isAssetLocked(fullAssetID));

    // Unlock full Asset
    thisTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertTrue(s0.isAssetLocked(namespace));
    assertTrue(s0.isAssetLocked(fullAssetID));

    s0.commit();
  }

}
