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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.state.entry.DataName;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamedDatum;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.common.Pair;

/**
 * Created by nicholaspennington on 28/07/2017.
 */
public class StateSnapshotWrapper extends AbstractStateSnapshot {

  AbstractStateSnapshot wrapped;


  /**
   * StateSnapshotWrapper.
   * <p>Provide the ability to conditionally update snapshots in the same way that they can conditionally update state.</p>
   *
   * @param baseState :
   */
  StateSnapshotWrapper(AbstractStateSnapshot baseState) {
    wrapped = baseState;
    signNodeList = new MutableMerkleWrapper<>(wrapped.signNodeList);

    reset();
  }


  /**
   * Apply changes to underlying Snapshot.
   */
  @Override
  public void commit() throws StateSnapshotCorruptedException {
    if (this.isCorrupted) {
      throw new StateSnapshotCorruptedException(corruptedMessage);
    }

    int version = wrapped.getVersion();
    long blockHeight = wrapped.getHeight(); // Block height is assumed to be state height.

    signNodeList.commit(version, blockHeight, NoOpChangeListener.INSTANCE);

    Set<Class<? extends MEntry>> parentMerkles = wrapped.getMerkleTypes();
    for (Entry<Class<? extends MEntry>, AbstractMutableMerkle<?>> e : merkles.entrySet()) {
      Class<? extends MEntry> name = e.getKey();
      AbstractMutableMerkle<?> merkle = e.getValue();
      if (parentMerkles.contains(name)) {
        // Existing Merkle.
        merkle.commit(version, blockHeight, NoOpChangeListener.INSTANCE);
      } else {
        // New Merkle. Just write it directly into the parent if it has at least one insert.
        MutableMerkleInitial<?> initial = (MutableMerkleInitial<?>) merkle;
        if (initial.hasInsert()) {
          wrapped.merkles.put(name, merkle);
        }
      }
    }

    //config
    configMap.forEach((key, value) -> wrapped.setConfigValue(key, value));

    xChainSignNodesMap.forEach((key, value) -> wrapped.setXChainSignNodesValue(key, value));

    removedXChainSignNodesMap.forEach(key -> wrapped.removeXChainSignNodesValue(key));

    if (!this.effectiveTXList.isEmpty()) {
      wrapped.getEffectiveTXList().addAll(this.effectiveTXList);
    }

    // Merge any contract life cycle events into the wrapped snap shot
    final Map<ContractLifeCycle, SortedSet<String>> parentLifeCycleEvents = wrapped.getContractLifeCycleEvents();
    getContractLifeCycleEvents().forEach(
        (event, set) -> {
          parentLifeCycleEvents.computeIfAbsent(event, e -> new TreeSet<>()).addAll(set);
        }
    );
    getContractLifeCycleEvents().clear();

    // Merge any user-to-contract associations into the wrapped snap shot
    final Map<String, SortedSet<String>> parentContractUsers = wrapped.getContractUsers();
    getContractUsers().forEach(
        (user, contracts) -> {
          parentContractUsers.computeIfAbsent(user, u -> new TreeSet<>()).addAll(contracts);
        }
    );
    getContractUsers().clear();

    if (!this.contractEvents.isEmpty()) {
      this.wrapped.contractEvents.putAll(this.contractEvents);
      contractEvents.clear();
    }

    for (NamedDatum<?> datum : namedData.values()) {
      datum.commit();
    }

    removedPrivilegedKeys.forEach(k -> wrapped.setPrivilegedKey(k, null));
    privilegedKeys.forEach((k, p) -> wrapped.setPrivilegedKey(k, p));

    reset();
  }


  @Override
  protected <X extends MEntry> AbstractMutableMerkle<X> createMerkle(Class<X> leafType) {
    if (merkleExists(leafType)) {
      return new MutableMerkleWrapper<>(wrapped.getMerkle(leafType));
    }
    return new MutableMerkleInitial<>(leafType);
  }


  @Override
  public State finalizeBlock(Block block) {
    throw new IllegalStateException("Only top-level snapshots can be finalized.");
  }


  @Override
  public Hash getBlockHash() {
    return wrapped.getBlockHash();
  }


  @Override
  public int getChainId() {
    return wrapped.getChainId();
  }


  @Override
  public Collection<EventData> getContractEvents() {
    HashMap<Pair<String, String>, EventData> allEvents = new HashMap<>();
    wrapped.getContractEvents().forEach(e -> allEvents.put(e.getId(), e));
    allEvents.putAll(contractEvents);
    return allEvents.values();
  }


  @Override
  public <N extends NamedDatum<N>> N getDatum(DataName<N> name) {
    NamedDatum<?> datum = namedData.computeIfAbsent(name, n -> wrapped.getDatum(n).snapshot());
    return name.getType().cast(datum);
  }


  @Override
  public int getHeight() {
    return wrapped.getHeight();
  }


  @Override
  public PrivilegedKey getPrivilegedKey(String keyName) {
    if (keyName == null || keyName.isEmpty()) {
      return null;
    }

    // try by address
    if (privilegedKeys.containsKey(keyName)) {
      return privilegedKeys.get(keyName);
    }

    // Try by name
    for (PrivilegedKey pk : privilegedKeys.values()) {
      if (keyName.equals(pk.getName())) {
        return pk;
      }
    }

    // try parent
    return wrapped.getPrivilegedKey(keyName);
  }


  @Override
  public long getTimestamp() {
    return wrapped.getTimestamp();
  }


  @Override
  public int getVersion() {
    return wrapped.getVersion();
  }


  @Override
  protected StateBase getWrapped() {
    return wrapped;
  }


  @Nullable
  @Override
  public XChainDetails getXChainSignNodesValue(Number number) {
    Long keyName = number.longValue();
    if (this.removedXChainSignNodesMap.contains(keyName)) {
      return null;
    }

    XChainDetails thisValue = this.xChainSignNodesMap.get(keyName);

    if (thisValue == null) {
      thisValue = wrapped.getXChainSignNodesValue(keyName);
    }

    return thisValue;
  }


  @Override
  public boolean merkleExists(Class<? extends MEntry> leafType) {
    return merkles.containsKey(leafType) || wrapped.merkleExists(leafType);
  }

}
