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
import static org.junit.Assert.assertTrue;

import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_BONDING;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import org.junit.Test;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.Bond;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;

public class BondTest extends BaseTestClass {


  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/20/e00f2e0ddc1e76879ce56437a2d153c7f039a44a784e9c659a5c581971c33999";
    String namespace = "SYS";
    String classname = "STAKE";
    final int chainID = 20;
    int masterNonce = 0;
    final long issueAmount = 1000;
    final String fullAssetID = namespace + (classname.length() > 0 ? "|" + classname : "");

    String pubKey = getRandomPublicKey();
    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    String masterPubKey = "a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58";
    String masterAddress = "1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb";
    AbstractTx thisTX;

    /*
     * SYS|STAKE controlling address = '1KwDq7FzTNWcwqB5EVfMD9oqpBMoTaAMLb'
     * SYS|STAKE controlling publicKey = 'a6ba227d1140d0d250bd9365340873d7738eee1f3ec106dc659acbe46f65fe58'
     */

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    thisTX = AssetIssue.assetIssueUnsigned(chainID, masterNonce++, masterPubKey, masterAddress, namespace, classname, address, issueAmount, "", "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    int addressNonce = 0;

    thisTX = Bond.bondUnsigned(chainID, addressNonce++, pubKey, address, pubKey, address, issueAmount, "");
    final AbstractTx saveTX = thisTX;

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    MutableMerkle<SignNodeEntry> sigNodes = s0.getSignNodes();
    SignNodeEntry sigEntry = sigNodes.findAndMarkUpdated(pubKey);

    assertNotNull(sigEntry);
    assertTrue(sigEntry.getBalance().equalTo(issueAmount));
    assertEquals(pubKey, sigEntry.getHexPublicKey());
    assertEquals(address, sigEntry.getReturnAddress());

    // Fail on Timestamp
    thisTX = Bond.bondUnsigned(chainID, addressNonce, pubKey, address, pubKey, address, issueAmount, "");
    thisTX.setTimestamp(1);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    // Fail Address not match Public Key
    thisTX = Bond.bondUnsigned(chainID, addressNonce, pubKey, masterAddress, pubKey, address, issueAmount, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on -ve amount
    thisTX = Bond.bondUnsigned(chainID, addressNonce, pubKey, address, pubKey, address, -1L, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Fail on duff address
    thisTX = Bond.bondUnsigned(chainID, addressNonce, pubKey, "duffAddress", pubKey, address, issueAmount, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Insufficient balance
    thisTX = Bond.bondUnsigned(chainID, addressNonce, pubKey, address, pubKey, address, issueAmount, "");
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

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
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromAddressEntry.setAddressPermissions(AP_BONDING);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check

    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);

    assertNotNull(sigEntry);
    assertTrue(sigEntry.getBalance().equalTo(issueAmount));
    assertEquals(pubKey, sigEntry.getHexPublicKey());
    assertEquals(address, sigEntry.getReturnAddress());

    // Reset
    s0 = state1.createSnapshot();

    // Lock namespace

    s0.setAssetLockValue(namespace, LockedAsset.Type.FULL);

    // Bond Fails.
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.setAssetLockValue(namespace, LockedAsset.Type.NO_LOCK);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Reset
    s0 = state1.createSnapshot();

    // Lock namespace + class

    s0.setAssetLockValue(fullAssetID, LockedAsset.Type.FULL);

    // Bond Fails.
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Unlock
    s0.setAssetLockValue(fullAssetID, LockedAsset.Type.NO_LOCK);

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check bonding

    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);

    assertNotNull(sigEntry);
    assertTrue(sigEntry.getBalance().equalTo(issueAmount));
    assertEquals(pubKey, sigEntry.getHexPublicKey());
    assertEquals(address, sigEntry.getReturnAddress());

    // Reset
    s0 = state1.createSnapshot();

    // Test vs Encumbrance

    // Set Encumbrance

    Balance existingBalance = BALANCE_ZERO;
    assetBalances = s0.getAssetBalances();
    AddressEntry thisAddressEntry = assetBalances.findAndMarkUpdated(address);

    if (thisAddressEntry != null) {
      existingBalance = thisAddressEntry.getAssetBalance(fullAssetID);
    }

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().findAndMarkUpdated(address);

    if (thisAddressEncumbrance == null) {
      thisAddressEncumbrance = new AddressEncumbrances(address);
      s0.getEncumbrances().add(thisAddressEncumbrance);
    }

    String administratorAddress = getRandomAddress();
    String beneficiaryAddress = getRandomAddress();

    EncumbranceDetail[] administrators =
        {new EncumbranceDetail(administratorAddress, 0L, 0L), new EncumbranceDetail(beneficiaryAddress, 0L, 0L)};
    EncumbranceDetail[] beneficiaries = {
        new EncumbranceDetail(beneficiaryAddress, 0L, 0L),
        new EncumbranceDetail(beneficiaryAddress, 0L, 0L)
    };

    EncumbranceEntry newEntry = new EncumbranceEntry("reference", Integer.MAX_VALUE, Arrays.asList(beneficiaries), Arrays.asList(administrators));

    thisAddressEncumbrance.setEncumbranceEntry(fullAssetID, existingBalance, s0.getTimestamp(), newEntry, false, true);

    // Insufficient unEncumbered balance
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Reset
    s0 = state1.createSnapshot();

    s0.setConfigValue("authorisebyaddress", 1);

    // Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(address);
    fromAddressEntry.setAddressPermissions(AP_TX_LIST);
    fromAddressEntry.setAuthorisedTx(Collections.singleton(TxType.DO_NOTHING));

    // Still Insufficient Address permissions
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry.setAuthorisedTx(EnumSet.of(TxType.GRANT_VOTING_POWER));

    // OK.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check

    sigNodes = s0.getSignNodes();
    sigEntry = sigNodes.findAndMarkUpdated(pubKey);

    assertNotNull(sigEntry);
    assertTrue(sigEntry.getBalance().equalTo(issueAmount));
    assertEquals(pubKey, sigEntry.getHexPublicKey());
    assertEquals(address, sigEntry.getReturnAddress());

  }


}
