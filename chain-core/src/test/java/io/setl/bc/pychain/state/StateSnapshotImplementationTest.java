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
package io.setl.bc.pychain.state;

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultNonceEntry;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_DEFAULT_AUTHORISE_BY_ADDRESS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.ContractEntryTestClass;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.LockedAsset.Type;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryList;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.test.BlockBuilder;
import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import io.setl.common.TypeSafeMap;
import io.setl.crypto.KeyGen;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StateSnapshotImplementationTest {

  public MessageDigest digest;

  public FileStateLoader fileStateLoaded;

  public SerialiseToByte hashSerialiser;


  @Test
  public void addContractEvent() throws Exception {

    AbstractState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String address = "add1";
    String function = "func1";
    String eventName = "eName1";
    String eventData = "eData1";
    String address1 = "add11";
    String function1 = "func11";
    String eventName1 = "eName11";
    String eventData1 = "eData11";

    final String eventKey = address + "|" + eventName;
    final String eventKey1 = address1 + "|" + eventName1;

//    AbstractStateTest.addContractEvent(state1, address, function, eventName, eventData);
    w0.addContractEvent(address1, function1, eventName1, eventData1);
    w0.addContractEvent(address, function, eventName1, eventData1);

    w0.commit();
    s0.commit();

    assertTrue(s0.getContractEvents().size() == 2);
    assertTrue(w0.getContractEvents().size() == 2);

    // Do again to exercise the s0 functions.

    state1 = getGenesis20();
    s0 = state1.createSnapshot();

//    AbstractStateTest.addContractEvent(state1, address, function, eventName, eventData);
    s0.addContractEvent(address1, function1, eventName1, eventData1);
    s0.addContractEvent(address, function, eventName1, eventData1);

    s0.commit();

//    assertTrue(state1.getContractEventTree().get(eventKey) == null);
//    assertTrue(state1.getContractEventTree().get(eventKey1).getEventAddress().equals(address1));
//    assertTrue(state1.getContractEventTree().get(eventKey1).getEventName().equals(eventName1));
//    assertTrue(state1.getContractEventTree().get(eventKey1).getEventFunction().equals(function1));
//    assertTrue(state1.getContractEventTree().get(eventKey1).getStringValue().equals(eventData1));

  }


  @Test
  public void addContractEventTime() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String address = "add1";
    Long eventTime = 98765430L;
    String address1 = "add1";
    Long eventTime1 = 1234565L;
    Long eventTime2 = 4444445L;

    AbstractStateTest.addContractEventTime(state1, address, eventTime);
    w0.addContractEventTime(address1, eventTime1);
    w0.addContractEventTime(address, eventTime2);
    w0.removeContractEventTime(address, eventTime);

    assertTrue(eventsAt(state1,eventTime).contains(address));

    w0.commit();
    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(eventsAt(state1,eventTime1).contains(address1));
    assertTrue(eventsAt(state1,eventTime2).contains(address));
    assertTrue(eventsAt(state1,eventTime) == null);

    state1 = getGenesis20();
    s0 = state1.createSnapshot();
    AbstractStateTest.addContractEventTime(state1, address, eventTime);
    s0.addContractEventTime(address1, eventTime1);
    s0.addContractEventTime(address, eventTime2);
    s0.removeContractEventTime(address, eventTime);

    assertTrue(eventsAt(state1,eventTime).contains(address));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(eventsAt(state1,eventTime1).contains(address1));
    assertTrue(eventsAt(state1,eventTime2).contains(address));
    assertTrue(eventsAt(state1,eventTime) == null);
  }

  private Set<String> eventsAt(State state, long time) {
    return state.getContractTimeEvents().getEventDetailsBefore(time).get(time);
  }

  @Test
  public void deleteContractEvent() throws Exception {

    AbstractState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String address = "add1";
    String function = "func1";
    String eventName = "eName1";
    String eventData = "eData1";

    w0.addContractEvent(address, function, eventName, eventData);

    w0.commit();
    assertEquals(1,s0.getContractEvents().size());
  }


  @Test
  public void getAddressPermissions() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String address = "add1";

    assertTrue(w0.getAddressPermissions(null) == AP_DEFAULT_AUTHORISE_BY_ADDRESS);
    assertTrue(s0.getAddressPermissions(null) == AP_DEFAULT_AUTHORISE_BY_ADDRESS);
    assertTrue(!w0.canUseTx(address, TxType.REGISTER_ASSET_CLASS));
    assertTrue(!s0.canUseTx(address, TxType.REGISTER_ASSET_CLASS));

    MutableMerkle<AddressEntry> assetBalances = w0.getAssetBalances();
    // AddressEntry addressEntry = new AddressEntry(0, address, 0, 0, 0);
    AddressEntry addressEntry = addDefaultAddressEntry(assetBalances, address, 3);
    // addressEntry.makeClassBalance();
    addressEntry.setAddressPermissions(42);
    // assetBalances.add(addressEntry);

    assertTrue(!w0.canUseTx(address, TxType.REGISTER_ASSET_CLASS));
    assertTrue(!s0.canUseTx(address, TxType.REGISTER_ASSET_CLASS));

    assertTrue(w0.getAddressPermissions(address) == 42);
    assertTrue(s0.getAddressPermissions(address) == AP_DEFAULT_AUTHORISE_BY_ADDRESS);

    w0.commit();

    assertTrue(w0.getAddressPermissions(address) == 42);
    assertTrue(s0.getAddressPermissions(address) == 42);

  }


  @Test
  public void getAssetBalances() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    Iterator<AddressEntry> iterator = state1.getAssetBalances().iterator();
    String addressKey = iterator.next().getKey();

    AddressEntry addressEntry = w0.getAssetBalances().findAndMarkUpdated(addressKey);

    String newItemName = "newAddress";
    String newClassKey = "XXX";

    Map<String, Balance> newBalances = addressEntry.makeClassBalance();
    newBalances.put(newClassKey, new Balance(42L));
    addDefaultNonceEntry(w0.getAssetBalances(), newItemName, 3);

    assertTrue(s0.getAssetBalances().find(newItemName) == null);
    assertTrue(!s0.getAssetBalances().find(addressKey).getClassBalance().containsKey(newClassKey));

    w0.commit();

    assertTrue(s0.getAssetBalances().find(newItemName) != null);
    assertTrue(s0.getAssetBalances().find(addressKey).getClassBalance().containsKey(newClassKey));

    assertTrue(state1.getAssetBalances().find(newItemName) == null);
    assertTrue(!state1.getAssetBalances().find(addressKey).getClassBalance().containsKey(newClassKey));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getAssetBalances().find(newItemName) != null);
    assertTrue(state1.getAssetBalances().find(addressKey).getClassBalance().containsKey(newClassKey));

  }


  @Test
  public void getAssetLockValue() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String existingKey = "abc|def";
    String newKey = "zxc|qwe";
    LockedAsset existingValue = new LockedAsset(existingKey, LockedAsset.Type.NO_LOCK, -1);

    ((KeyedIndexedEntryList<LockedAsset>) state1.getLockedAssets()).update(existingKey, existingValue);

    w0.setAssetLockValue(existingKey, LockedAsset.Type.FULL);
    w0.setAssetLockValue(newKey, LockedAsset.Type.FULL);

    assertTrue(w0.getAssetLockValue(null, LockedAsset.Type.FULL) == LockedAsset.Type.FULL); // Default.
    assertTrue(w0.getAssetLockValue(null, Type.NO_LOCK) == Type.NO_LOCK); // Default.
    assertTrue(w0.getAssetLockValue(existingKey) == LockedAsset.Type.FULL);
    assertTrue(w0.getAssetLockValue(newKey) == LockedAsset.Type.FULL);
    assertTrue(s0.getAssetLockValue(existingKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(s0.getAssetLockValue(newKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(state1.getAssetLockValue(existingKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(state1.getLockedAssets().find(newKey) == null);
    assertTrue(state1.getAssetLockValue(newKey) == LockedAsset.Type.NO_LOCK);

    w0.commit();

    assertTrue(s0.getAssetLockValue(existingKey) == LockedAsset.Type.FULL);
    assertTrue(s0.getAssetLockValue(newKey) == LockedAsset.Type.FULL);
    assertTrue(state1.getAssetLockValue(existingKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(state1.getLockedAssets().find(newKey) == null);
    assertTrue(state1.getAssetLockValue(newKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(!state1.isAssetLocked(newKey));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getAssetLockValue(existingKey) == LockedAsset.Type.FULL);
    assertTrue(state1.getLockedAssets().find(newKey) != null);
    assertTrue(state1.getAssetLockValue(newKey) == LockedAsset.Type.FULL);
    assertTrue(state1.isAssetLocked(newKey));

  }


  @Test
  public void getChainId() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    assertTrue(w0.getChainId() == 20);
    assertTrue(s0.getChainId() == 20);
    assertTrue(state1.getChainId() == 20);

  }


  @Test
  public void getConfigValue() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    Setting<String> newItemKey = new Setting<>("somethig new", TypeSafeMap::asString);
    String newItemValue = "value new";

    w0.setConfigValue(newItemKey, newItemValue);

    assertTrue(w0.getStateConfig().getConfigValue(newItemKey).equals(newItemValue));
    assertTrue(w0.getConfigValue(newItemKey).equals(newItemValue));
    assertTrue(!s0.getConfigValue(newItemKey, "").equals(newItemValue));

    w0.commit();

    assertTrue(s0.getStateConfig().getConfigValue(newItemKey).equals(newItemValue));
    assertTrue(s0.getConfigValue(newItemKey, "").equals(newItemValue));
  }


  @Test
  public void getContracts() throws Exception {

    AbstractState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String itemKey1 = "itemName";
    String propertyName1 = "XXX";

    // Add initial Contract Entry to State (itemKey1)

    state1.getMutationHelper().getContracts().update(itemKey1,
        new ContractEntry(-1L, itemKey1, new MPWrappedMap<>(new Object[]{"__function", CONTRACT_NAME_DVP_UK})));

    // Put Item Key 2 to the Snapshot Wrap version of ItemKey 1.

    ContractEntryTestClass itemEntry = new ContractEntryTestClass(w0.getContracts().findAndMarkUpdated(itemKey1));

    itemEntry.getDictionary().put(propertyName1, "");

    // Add Contract Entry to Snapshot Wrap (newItemName)

    String itemKey2 = "newAddress";

    assertTrue(s0.getContracts().find(itemKey2) == null);

    w0.getContracts().add(new ContractEntry(-1L, itemKey2, new MPWrappedMap<>(new Object[]{"__function", CONTRACT_NAME_DVP_UK})));

    // Assert that 'itemKey2' has not appeared in Snapshot
    assertTrue(s0.getContracts().find(itemKey2) == null);

    // Assert that 'propertyName1' has not appeared in Snapshot version of itemKey1
    assertTrue(!(new ContractEntryTestClass(s0.getContracts().find(itemKey1))).getDictionary().containsKey(propertyName1));

    // Commit Wrap to Snapshot
    w0.commit();

    assertTrue(s0.getContracts().find(itemKey2) != null);
    assertTrue((new ContractEntryTestClass(s0.getContracts().find(itemKey1))).getDictionary().containsKey(propertyName1));

    assertTrue(state1.getContracts().find(itemKey2) == null);
    assertTrue(!(new ContractEntryTestClass(state1.getContracts().find(itemKey1))).getDictionary().containsKey(propertyName1));

    state1 = (AbstractState) s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getContracts().find(itemKey2) != null);
    assertTrue((new ContractEntryTestClass(state1.getContracts().find(itemKey1))).getDictionary().containsKey(propertyName1));

  }


  @Test
  public void getEffectiveTXList() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    assertTrue(w0.getEffectiveTXList().size() == 0);
    NullTx nullTX = new NullTx(20, 4, "hash", 1, false, "publicKey", "fromAddress", "Signature", 0, "POA", Instant.now().getEpochSecond());
    w0.addEffectiveTX(nullTX);

    assertTrue(w0.getEffectiveTXList().size() == 1);
    assertTrue(((NullTx) w0.getEffectiveTXList().get(0)) == nullTX);

    assertTrue(s0.getEffectiveTXList().size() == 0);

    w0.commit();

    assertTrue(s0.getEffectiveTXList().size() == 1);
    assertTrue(((NullTx) s0.getEffectiveTXList().get(0)) == nullTX);

    s0.commit();

    state1 = getGenesis20();
    s0 = state1.createSnapshot();

    assertTrue(s0.getEffectiveTXList().size() == 0);
    nullTX = new NullTx(20, 4, "hash", 1, false, "publicKey", "fromAddress", "Signature", 0, "POA", Instant.now().getEpochSecond());
    s0.addEffectiveTX(nullTX);

    assertTrue(s0.getEffectiveTXList().size() == 1);
    assertTrue(((NullTx) s0.getEffectiveTXList().get(0)) == nullTX);

    assertTrue(s0.getEffectiveTxListEncoded().length == 1);
    assertTrue(Arrays.equals((Object[]) s0.getEffectiveTxListEncoded()[0], nullTX.encodeTx()));
  }


  @Test
  public void getEncumbrances() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String itemKey = "itemName";
    String newClassKey = "XXX";
    ((AbstractState) state1).getMutationHelper().getEncumbrances().update(itemKey, new AddressEncumbrances(itemKey));

    AddressEncumbrances itemEntry = w0.getEncumbrances().findAndMarkUpdated(itemKey);

    String newItemName = "newAddress";

    itemEntry.setAddress(newClassKey);

    assertTrue(s0.getEncumbrances().find(newItemName) == null);

    w0.getEncumbrances().add(new AddressEncumbrances(newItemName));

    assertTrue(s0.getEncumbrances().find(newItemName) == null);
    assertTrue(!s0.getEncumbrances().find(itemKey).getAddress().equals(newClassKey));

    w0.commit();

    assertTrue(s0.getEncumbrances().find(newItemName) != null);
    assertTrue(s0.getEncumbrances().find(itemKey).getAddress().equals(newClassKey));

    assertTrue(state1.getEncumbrances().find(newItemName) == null);
    assertTrue(!state1.getEncumbrances().find(itemKey).getAddress().equals(newClassKey));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getEncumbrances().find(newItemName) != null);
    assertTrue(state1.getEncumbrances().find(itemKey).getAddress().equals(newClassKey));

  }


  ObjectEncodedState getGenesis20() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/20/e00f2e0ddc1e76879ce56437a2d153c7f039a44a784e9c659a5c581971c33999";
    return new ObjectEncodedState(fileStateLoaded.loadStateFromFile(stateFile));
  }


  @Test
  public void getNamespaces() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    Iterator<NamespaceEntry> iterator = state1.getNamespaces().iterator();
    String nsKey = iterator.next().getKey();

    NamespaceEntry namespaceEntry = w0.getNamespaces().findAndMarkUpdated(nsKey);
    namespaceEntry.setAddress("fred1");
    w0.getNamespaces().add(new NamespaceEntry("newnamespace", "newReturnAddress", "thisMetadata"));

    assertTrue(s0.getNamespaces().find("newnamespace") == null);
    assertTrue(!s0.getNamespaces().find(nsKey).getAddress().equals("fred1"));

    w0.commit();

    assertTrue(s0.getNamespaces().find("newnamespace") != null);
    assertTrue(s0.getNamespaces().find(nsKey).getAddress().equals("fred1"));

    assertTrue(state1.getNamespaces().find("newnamespace") == null);
    assertTrue(!state1.getNamespaces().find(nsKey).getAddress().equals("fred1"));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getNamespaces().find("newnamespace") != null);
    assertTrue(state1.getNamespaces().find(nsKey).getAddress().equals("fred1"));

  }


  @Test
  public void getPowerOfAttorneys() throws Exception {
    /*
     * Crappy test, skeleton only.
     * TODO : Improve getPowerOfAttorneys() test.
     * */
    AbstractState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    String existingItemKey = "existing name";
    String updatedValue = "something changed";
    String newItemKey = "something new";

    // Add 'existing' POA
    state1.getMutationHelper().getPowerOfAttorneys().update(existingItemKey,
        new PoaEntry(new MPWrappedArrayImpl(new Object[]{0, existingItemKey})));

    // Update 'existing' item
    PoaEntry updatedItemEntry = w0.getPowerOfAttorneys().findAndMarkUpdated(existingItemKey);
    updatedItemEntry.setReference(updatedValue, 0, Long.MAX_VALUE);

    // Add new item
    w0.getPowerOfAttorneys().add(new PoaEntry(new MPWrappedArrayImpl(new Object[]{1, newItemKey})));

    // New item not propagated?
    assertTrue(state1.getPowerOfAttorneys().find(newItemKey) == null);
    assertTrue(s0.getPowerOfAttorneys().find(newItemKey) == null);

    // Changed item not propagated?
    assertFalse(state1.getPowerOfAttorneys().find(existingItemKey).hasReference(updatedValue));
    assertFalse(s0.getPowerOfAttorneys().find(existingItemKey).hasReference(updatedValue));

    // Commit changes from wrap to snapshot
    w0.commit();

    assertTrue(s0.getPowerOfAttorneys().find(newItemKey) != null);
    assertTrue(s0.getPowerOfAttorneys().find(existingItemKey).hasReference(updatedValue));

    assertTrue(state1.getPowerOfAttorneys().find(newItemKey) == null);
    assertFalse(state1.getPowerOfAttorneys().find(existingItemKey).hasReference(updatedValue));

    // Commit to state.
    s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getPowerOfAttorneys().find(newItemKey) != null);
    assertTrue(state1.getPowerOfAttorneys().find(existingItemKey).hasReference(updatedValue));

  }


  /**
   * Does what it says on the tin...
   *
   * @return :
   */
  public String getRandomAddress() {

    KeyPair keyPair = KeyGen.Type.ED25519.generate();
    String address = AddressUtil.publicKeyToAddress(keyPair.getPublic(), AddressType.NORMAL);

    return address;

  }


  @Test
  public void getSignNodes() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    Iterator<SignNodeEntry> iterator = state1.getSignNodes().iterator();
    String sigKey = iterator.next().getKey();

    SignNodeEntry signode0 = w0.getSignNodes().findAndMarkUpdated(sigKey);
    signode0.setReturnAddress("fred1");
    w0.getSignNodes().add(new SignNodeEntry("hexPublicKey1", "newReturnAddress", 1000, 0, 0, 0));

    assertTrue(s0.getSignNodes().find("hexPublicKey1") == null);

    w0.commit();

    assertTrue(s0.getSignNodes().find("hexPublicKey1") != null);
    assertTrue(s0.getSignNodes().find(sigKey).getReturnAddress().equals("fred1"));

    assertTrue(state1.getSignNodes().find("hexPublicKey1") == null);
    assertTrue(!state1.getSignNodes().find(sigKey).getReturnAddress().equals("fred1"));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getSignNodes().find("hexPublicKey1") != null);
    assertTrue(state1.getSignNodes().find(sigKey).getReturnAddress().equals("fred1"));

  }


  @Test
  public void getTimestamp() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    assertTrue(s0.getTimestamp() == state1.getTimestamp());
    assertTrue(w0.getTimestamp() == state1.getTimestamp());

    assertTrue(s0.getBlockHash().equals(state1.getBlockHash()));
    assertTrue(w0.getBlockHash().equals(state1.getBlockHash()));

    assertTrue(s0.getHeight() == state1.getHeight());
    assertTrue(w0.getHeight() == state1.getHeight());

  }


  @Test
  public void getVersion() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    assertTrue(w0.getVersion() == 3);
    assertTrue(s0.getVersion() == 3);
    assertTrue(state1.getVersion() == 3);
  }


  @Test
  public void getXChainSignNodesValue() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    int existingKey = 999;
    int newKey = 888888;
    XChainDetails existingValue = new XChainDetails(existingKey, 12, Collections.emptySortedMap(), 0, 0);
    XChainDetails updatedValue = new XChainDetails(existingKey, 13, Collections.emptySortedMap(), 0, 0);
    XChainDetails newValue = new XChainDetails(newKey, 100, Collections.emptySortedMap(), 0, 0);
    s0.setXChainSignNodesValue(existingKey, existingValue);

    w0.setXChainSignNodesValue(existingKey, updatedValue);
    w0.setXChainSignNodesValue(newKey, newValue);

    assertEquals(updatedValue, w0.getXChainSignNodesValue(existingKey));
    assertEquals(newValue, w0.getXChainSignNodesValue(newKey));
    assertEquals(existingValue, s0.getXChainSignNodesValue(existingKey));
    assertNull(s0.getXChainSignNodesValue(newKey));
    assertTrue(state1.getXChainSignNodesValue(newKey) == null);

    w0.commit();

    assertEquals(updatedValue, s0.getXChainSignNodesValue(existingKey));
    assertEquals(newValue, s0.getXChainSignNodesValue(newKey));

    s0.commit();

    assertEquals(updatedValue, s0.getXChainSignNodesValue(existingKey));
    assertEquals(newValue, s0.getXChainSignNodesValue(newKey));
  }


  @Test
  public void isCorrupted() throws Exception {

    ObjectEncodedState state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    assertTrue(!w0.isCorrupted());
    assertTrue(!s0.isCorrupted());

    w0.setCorrupted(true);
    assertTrue(w0.isCorrupted());

    w0.setCorrupted(false);

    assertTrue(!w0.isCorrupted());

    w0.setCorrupted(true, "bill");

    assertTrue(w0.isCorrupted());
    assertTrue(!s0.isCorrupted());

    s0.setCorrupted(true, "fred");
    assertTrue(s0.isCorrupted());

    s0.setCorrupted(false, "");

    assertTrue(!s0.isCorrupted());

    try {
      w0.commit();
      fail("State was not corrupt");
    } catch (StateSnapshotCorruptedException e) {
      // correct
    }
  }


  @Test
  public void removeAssetLockValue() throws Exception {

    State state1 = getGenesis20();

    String existingKey = "abc|def";
    LockedAsset existingValue = new LockedAsset(existingKey, LockedAsset.Type.FULL, -1);

    ((KeyedIndexedEntryList<LockedAsset>) state1.getLockedAssets()).update(existingKey, existingValue);

    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    w0.removeAssetLockValue(existingKey);

    assertTrue(!w0.isAssetLocked(existingKey));
    assertTrue(w0.getAssetLockValue(existingKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(s0.getAssetLockValue(existingKey) == LockedAsset.Type.FULL);
    assertTrue(state1.getAssetLockValue(existingKey) == LockedAsset.Type.FULL);

    w0.commit();

    assertTrue(s0.getAssetLockValue(existingKey) == LockedAsset.Type.NO_LOCK);
    assertTrue(state1.getAssetLockValue(existingKey) == LockedAsset.Type.FULL);

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getAssetLockValue(existingKey) == LockedAsset.Type.NO_LOCK);

  }


  @Test
  public void removeXChainSignodesValue() throws Exception {

    State state1 = getGenesis20();
    StateSnapshot s0 = state1.createSnapshot();
    StateSnapshot w0 = s0.createSnapshot();

    int existingKey = 999;
    XChainDetails existingValue = new XChainDetails(existingKey, 12, Collections.emptySortedMap(), 123, 1234);
    s0.setXChainSignNodesValue(existingKey, existingValue);
    state1 = s0.finalizeBlock(new BlockBuilder().build());
    s0 = state1.createSnapshot();
    w0 = s0.createSnapshot();

    assertEquals(existingValue, w0.getXChainSignNodesValue(existingKey));
    assertEquals(existingValue, s0.getXChainSignNodesValue(existingKey));

    w0.removeXChainSignNodesValue(existingKey);

    assertNull(w0.getXChainSignNodesValue(existingKey));
    assertEquals(existingValue, s0.getXChainSignNodesValue(existingKey));

    w0.commit();

    assertTrue(s0.getXChainSignNodesValue(existingKey) == null);
    assertTrue(state1.getXChainSignNodesValue(existingKey).equals(existingValue));

    state1 = s0.finalizeBlock(new BlockBuilder().build());

    assertTrue(state1.getXChainSignNodesValue(existingKey) == null);

  }


  @Before
  public void setUp() throws Exception {

    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();

    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
  }


  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }


}
