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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_DEFAULT_AUTHORISE_BY_ADDRESS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.CTEventsWrapper;
import io.setl.bc.pychain.state.entry.ContractTimeEvents;
import io.setl.bc.pychain.state.entry.DataName;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamedDatum;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.common.CommonPy.TxType;
import io.setl.common.MutableInt;
import io.setl.common.Pair;

/**
 * Common functionality for state snapshots.
 *
 * @author Simon Greatrix on 30/04/2018.
 */
public abstract class AbstractStateSnapshot implements StateSnapshot {

  protected TreeMap<String, Object> configMap;

  protected HashMap<Pair<String, String>, EventData> contractEvents = new HashMap<>();

  /** Map of contract events to the addresses of contracts which experienced those events. */
  protected EnumMap<ContractLifeCycle, SortedSet<String>> contractLifecycleEvents = new EnumMap<>(ContractLifeCycle.class);

  protected CTEventsWrapper contractTimeEvents;

  /**
   * Map of contract address to users affected by events on those contracts. Some contract events (especially "delete") make it impossible to discover who
   * was affected after the fact, so we preserve the information as we get notified of events.
   */
  protected TreeMap<String, SortedSet<String>> contractUsers = new TreeMap<>();

  protected String corruptedMessage;

  protected ArrayList<Txi> effectiveTXList;

  protected boolean isCorrupted;

  protected TreeMap<Class<? extends MEntry>, AbstractMutableMerkle<?>> merkles = new TreeMap<>(Comparator.comparing(Class::getName));

  protected HashMap<DataName<?>, NamedDatum<?>> namedData = new HashMap<>();

  protected TreeMap<String, PrivilegedKey> privilegedKeys;

  protected HashSet<Long> removedXChainSignNodesMap;

  protected AbstractMutableMerkle<SignNodeEntry> signNodeList;

  protected StateConfig stateConfig = null;

  protected TreeMap<Long, XChainDetails> xChainSignNodesMap;

  Set<String> removedPrivilegedKeys = new HashSet<>();


  @Override
  public boolean addContractEvent(String contractAddress, String contractFunction, String eventName, String eventData) {
    EventData event = new EventData(contractAddress, contractFunction, eventName, eventData);
    contractEvents.put(event.getId(), event);
    return true;
  }


  /**
   * Add a Time/Address pair to the contract Time Events.
   *
   * @param address   the contract's address
   * @param eventTime the time event
   *
   * @return true if the event did not already exist
   */
  @Override
  public boolean addContractEventTime(String address, long eventTime) {
    return getDatum(ContractTimeEvents.NAME).add(address, eventTime);
  }


  @Override
  public boolean addEffectiveTX(Txi txData) {
    if (txData != null) {
      effectiveTXList.add(txData);
    }
    return true;
  }


  @Override
  public void addLifeCycleEvent(ContractLifeCycle event, String contractAddress, Set<String> affectedUsers) {
    SortedSet<String> addresses = contractLifecycleEvents.computeIfAbsent(event, k -> new TreeSet<>());
    addresses.add(contractAddress);

    Set<String> users = contractUsers.computeIfAbsent(contractAddress, k -> new TreeSet<>());
    users.addAll(affectedUsers);
  }


  @Override
  public String commitIfNotCorrupt() {
    if (isCorrupted) {
      return corruptedMessage;
    }
    try {
      commit();
      return null;
    } catch (StateSnapshotCorruptedException e) {
      // should not happen as we checked for corruption earlier
      return e.getMessage();
    }
  }


  protected abstract <X extends MEntry> AbstractMutableMerkle<X> createMerkle(Class<X> leafType);


  @Override
  public StateSnapshot createSnapshot() {
    return new StateSnapshotWrapper(this);
  }


  @Override
  public long getAddressPermissions(String address) {
    AddressEntry thisAddress = getAssetBalances().find(address);

    if (thisAddress == null) {
      return AP_DEFAULT_AUTHORISE_BY_ADDRESS;
    }

    return thisAddress.getAddressPermissions();
  }


  @Override
  public LockedAsset.Type getAssetLockValue(String keyName) {
    return getAssetLockValue(keyName, LockedAsset.Type.NO_LOCK);
  }


  @Override
  public <Q> Q getConfigValue(Setting<Q> keyName) {
    String label = keyName.getLabel();
    if (!configMap.containsKey(label)) {
      return getWrapped().getConfigValue(keyName);
    }
    Object thisObject = configMap.get(label);
    Q thisValue = keyName.cast(thisObject);
    return (thisValue != null) ? thisValue : getWrapped().getConfigValue(keyName);
  }


  @Override
  public Object[] getContractEventsEncoded() {
    Collection<EventData> events = getContractEvents();
    if (events.isEmpty()) {
      return new Object[0];
    }
    Object[] rVal = new Object[events.size()];

    final MutableInt i = new MutableInt(-1);
    events.forEach(thisEvent -> rVal[i.increment()] = thisEvent.encode());
    return rVal;
  }


  @Override
  public EnumMap<ContractLifeCycle, SortedSet<String>> getContractLifeCycleEvents() {
    return contractLifecycleEvents;
  }


  @Override
  public SortedMap<String, SortedSet<String>> getContractUsers() {
    return contractUsers;
  }


  @Override
  public List<Txi> getEffectiveTXList() {
    /* Only includes Effective transactions added to this Snapshot.
     * Because this is cumulative throughout block processing, it is not necessary to contrive to return all existing TXs. */
    return effectiveTXList;
  }


  @Override
  public Object[] getEffectiveTxListEncoded() {
    if (effectiveTXList.isEmpty()) {
      return new Object[0];
    }
    Object[] rVal = new Object[effectiveTXList.size()];

    final MutableInt i = new MutableInt(-1);
    effectiveTXList.forEach(thisTx -> rVal[i.increment()] = thisTx.encodeTx());
    return rVal;
  }


  @Override
  @Nonnull
  public <X extends MEntry> MutableMerkle<X> getMerkle(Class<X> leafType) {
    MutableMerkle<?> merkle = merkles.computeIfAbsent(leafType, this::createMerkle);
    return MutableMerkle.cast(merkle, leafType);
  }


  @Override
  public Set<Class<? extends MEntry>> getMerkleTypes() {
    return Collections.unmodifiableSet(merkles.keySet());
  }


  @Override
  public MutableMerkle<SignNodeEntry> getSignNodes() {
    return signNodeList;
  }


  @Override
  public StateConfig getStateConfig() {
    return stateConfig;
  }


  protected abstract StateBase getWrapped();


  @Override
  public boolean canUseTx(String address, TxType txID) {
    AddressEntry thisAddress = getAssetBalances().find(address);
    if (thisAddress == null) {
      return false;
    }

    return thisAddress.canUseTx(txID);
  }


  @Override
  public boolean isAssetLocked(String assetName) {
    LockedAsset.Type thisValue = getAssetLockValue(assetName, LockedAsset.Type.NO_LOCK);
    return (LockedAsset.Type.NO_LOCK != thisValue);
  }


  @Override
  public boolean isCorrupted() {
    return isCorrupted;
  }


  protected abstract boolean merkleExists(Class<? extends MEntry> name);


  public void removeAssetLockValue(String keyName) {
    getLockedAssets().delete(keyName);
  }


  @Override
  public void removeContractEventTime(String address, long eventTime) {
    getDatum(ContractTimeEvents.NAME).remove(address, eventTime);
  }


  @Override
  public void removePendingEventTimeAddresses(long eventTime, Set<String> addressSet) {
    ((CTEventsWrapper) getDatum(ContractTimeEvents.NAME)).removePendingEventTimeAddresses(eventTime, addressSet);
  }


  @Override
  public void removeXChainSignNodesValue(Number keyName) {
    removedXChainSignNodesMap.add(keyName.longValue());
  }


  /**
   * Reset Snapshot.
   */
  protected void reset() {
    isCorrupted = false;

    signNodeList.reset();
    configMap = new TreeMap<>();
    xChainSignNodesMap = new TreeMap<>();
    removedXChainSignNodesMap = new HashSet<>();

    stateConfig = new StateConfig(this);
    effectiveTXList = new ArrayList<>();
    contractLifecycleEvents.clear();
    contractUsers.clear();
    privilegedKeys = new TreeMap<>();
    removedPrivilegedKeys = new HashSet<>();

    for (NamedDatum<?> datum : namedData.values()) {
      datum.reset();
    }

    merkles.clear();
  }


  @Override
  public void setAssetLockValue(String keyName, LockedAsset.Type keyValue) {
    MutableMerkle<LockedAsset> lockedAssets = getLockedAssets();
    LockedAsset lockedAsset = lockedAssets.findAndMarkUpdated(keyName);
    if (lockedAsset == null) {
      lockedAssets.add(new LockedAsset(keyName, keyValue, getHeight()));
    } else {
      lockedAsset.setType(keyValue, getHeight());
    }
  }


  @Override
  public void setConfigValue(String keyName, Object keyValue) {
    configMap.put(keyName, keyValue);
    stateConfig.resetPerformanceCache();
  }


  @Override
  public void setCorrupted(boolean corrupted) {
    isCorrupted = corrupted;
    corruptedMessage = "No message";
  }


  @Override
  public void setCorrupted(boolean corrupted, String message) {
    isCorrupted = corrupted;
    corruptedMessage = message;
  }


  @Override
  public void setPrivilegedKey(String keyName, PrivilegedKey key) {
    if (key == null) {
      removedPrivilegedKeys.add(keyName);
      privilegedKeys.remove(keyName);
    } else {
      privilegedKeys.put(keyName, key);
      removedPrivilegedKeys.remove(keyName);
    }
  }


  @Override
  public void setXChainSignNodesValue(Number number, XChainDetails keyValue) {
    Long keyName = number.longValue();
    xChainSignNodesMap.put(keyName, keyValue);
    removedXChainSignNodesMap.remove(keyName);
  }

}
