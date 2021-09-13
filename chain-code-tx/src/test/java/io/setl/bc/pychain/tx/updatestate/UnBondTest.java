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
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_BONDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class UnBondTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/20/e00f2e0ddc1e76879ce56437a2d153c7f039a44a784e9c659a5c581971c33999";
    String namespace = "SYS";
    String classname = "STAKE";
    final String protocol = "";
    final String metadata = "";
    final String fullAssetID = namespace + "|" + classname;
    final int chainID = 20;
    int masterNonce = 0;
    final long bondAmount = 1000;


    String pubKey = getRandomPublicKey();
    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String returnAddress = address; // TODO ??? not the same as address ???
    int addressNonce = 0;

    String masterPubKey = "a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58";
    String masterAddress = "1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb";
    AbstractTx thisTX;

    /*
     * SYS|STAKE controlling address = '1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb'
     * SYS|STAKE controlling publicKey = 'a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58'
     */

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    thisTX = AssetIssue.assetIssueUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, classname, address, bondAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.Bond.bondUnsigned(chainID, addressNonce++, pubKey, address, pubKey, address, bondAmount, "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<SignNodeEntry> sigNodes = s0.getSignNodes();
    SignNodeEntry sigEntry = sigNodes.findAndMarkUpdated(pubKey);

    assertNotNull(sigEntry);
    assertEquals(sigEntry.getBalance(), bondAmount);
    assertEquals(pubKey, sigEntry.getHexPublicKey());
    assertEquals(address, sigEntry.getReturnAddress());

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(new Balance(0L), newValue);

    s0.commit();
    s0 = state1.createSnapshot();
    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    thisTX = io.setl.bc.pychain.tx.create.UnBond.unBondUnsigned(chainID, addressNonce++, pubKey, address, returnAddress, bondAmount, protocol, metadata, "");
    final AbstractTx saveTX = thisTX;

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertNotNull(sigEntry);
    assertEquals(new Balance(0L), sigEntry.getBalance());

    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, bondAmount);

    s0 = state1.createSnapshot();
    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(returnAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    //
    thisTX = saveTX;

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);
    fromAddressEntry.setAddressPermissions(AP_BONDING);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();
    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    // Fail on Timestamp
    thisTX = io.setl.bc.pychain.tx.create.UnBond.unBondUnsigned(chainID, addressNonce, pubKey, address, returnAddress, bondAmount, protocol, metadata, "");
    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Fail Address not match Public Key
    thisTX = io.setl.bc.pychain.tx.create.UnBond.unBondUnsigned(chainID, addressNonce, masterPubKey, address, returnAddress, bondAmount, protocol,
        metadata, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on -ve amount
    thisTX = io.setl.bc.pychain.tx.create.UnBond.unBondUnsigned(chainID, addressNonce, pubKey, address, returnAddress, -1L, protocol, metadata, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Insufficient balance
    thisTX = io.setl.bc.pychain.tx.create.UnBond.unBondUnsigned(chainID, addressNonce, pubKey, address, returnAddress, bondAmount, protocol, metadata, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

  }

  @Test
  public void updatestate2() throws Exception {

    // Test return to arbitrary address.

    String stateFile = "src/test/resources/test-states/genesis/20/e00f2e0ddc1e76879ce56437a2d153c7f039a44a784e9c659a5c581971c33999";
    String namespace = "SYS";
    String classname = "STAKE";
    final String protocol = "";
    final String metadata = "";
    final String fullAssetID = namespace + "|" + classname;
    final int chainID = 20;
    int masterNonce = 0;
    final long issueAmount = 1000;

    String pubKey = getRandomPublicKey();
    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    String retPubKey = getRandomPublicKey();
    String returnAddress = AddressUtil.publicKeyToAddress(retPubKey, AddressType.NORMAL);

    int addressNonce = 0;

    String masterPubKey = "a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58";
    String masterAddress = "1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb";
    AbstractTx thisTX;

    /*
     * SYS|STAKE controlling address = '1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb'
     * SYS|STAKE controlling publicKey = 'a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58'
     */

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    thisTX = AssetIssue.assetIssueUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, classname, address, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.Bond.bondUnsigned(chainID, addressNonce++, pubKey, address, pubKey, address, issueAmount, "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<SignNodeEntry> sigNodes = s0.getSignNodes();
    SignNodeEntry sigEntry = sigNodes.findAndMarkUpdated(pubKey);

    assertNotNull(sigEntry);
    assertEquals(sigEntry.getBalance(), issueAmount);
    assertEquals(pubKey, sigEntry.getHexPublicKey());
    assertNotEquals(returnAddress, sigEntry.getReturnAddress());

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(returnAddress);
    if (fromAddressEntry == null) {
      fromAddressEntry = addDefaultAddressEntry(assetBalances, returnAddress, s0.getVersion());
    }
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(new Balance(0L), newValue);

    s0.commit();
    s0 = state1.createSnapshot();
    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);
    assetBalances = s0.getAssetBalances();

    MutableMerkle<SignNodeEntry> signNodes = s0.getSignNodes();
    signNodes.findAndMarkUpdated(pubKey);

    fromAddressEntry = assetBalances.findAndMarkUpdated(returnAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(new Balance(0L), newValue);

    thisTX = io.setl.bc.pychain.tx.create.UnBond.unBondUnsigned(chainID, addressNonce++, pubKey, address, returnAddress, issueAmount, protocol, metadata, "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertNotNull(sigEntry);
    assertEquals(new Balance(0L), sigEntry.getBalance());

    fromAddressEntry = assetBalances.findAndMarkUpdated(returnAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    // Check signode deleted

    signNodes = s0.getSignNodes();
    SignNodeEntry fromSignodeEntry = signNodes.findAndMarkUpdated(pubKey);
    assertNull(fromSignodeEntry);

  }

}
