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
import java.util.Map.Entry;
import java.util.Set;
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
import io.setl.bc.pychain.state.ipfs.GlobalHashCache;

/**
 * A memory based implementation. All changes are stored in memory, prior to being committed.
 *
 * @author aanten
 */
class StateSnapshotImplementation extends AbstractStateSnapshot {

  StateChangeListener changeListener;

  final AbstractState wrapped;


  /**
   * StateSnapshotImplementation constructor.
   *
   * @param baseState : State object to base snapshot on.
   */
  StateSnapshotImplementation(AbstractState baseState, StateChangeListener changeListener) {
    wrapped = baseState;
    this.changeListener = changeListener;
    signNodeList = new MutableMerkleImplementation<>(baseState.getSignNodes());

    reset();
  }


  @Override
  public void commit() throws StateSnapshotCorruptedException {
    if (isCorrupted) {
      throw new StateSnapshotCorruptedException(corruptedMessage);
    }
    if (wrapped.isInitialised) {
      throw new StateSnapshotCorruptedException("State is no longer mutable");
    }
  }



  /**
   * Perform the actual write to state. This is only done when the block is finalized.
   */
  private void commitNow(int blockHeight) {
    GlobalHashCache.reset();

    int version = wrapped.getVersion();

    signNodeList.commit(version, blockHeight, changeListener);

    Set<Class<? extends MEntry>> parentMerkles = wrapped.getMerkleTypes();
    for (Entry<Class<? extends MEntry>, AbstractMutableMerkle<?>> e : merkles.entrySet()) {
      Class<? extends MEntry> name = e.getKey();
      AbstractMutableMerkle<?> merkle = e.getValue();
      if (parentMerkles.contains(name)) {
        // Existing Merkle
        merkle.commit(version, blockHeight, changeListener);
      } else {
        // New Merkle. Just write it directly into the parent if it has at least one insert.
        MutableMerkleInitial<?> initial = (MutableMerkleInitial<?>) merkle;
        if (initial.hasInsert()) {
          SnapshotMerkle<?> snapshotMerkle = wrapped.createMerkle(name);
          initial.writeTo(version, blockHeight, snapshotMerkle, changeListener);
        }
      }
    }

    wrapped.setConfigMap(configMap, false);
    wrapped.setXChainSignNodes(xChainSignNodesMap, removedXChainSignNodesMap, false);
    wrapped.setPrivilegedKeys(privilegedKeys, removedPrivilegedKeys, false);

    for (NamedDatum<?> datum : namedData.values()) {
      datum.commit();
    }

    reset();
  }


  @Override
  protected <X extends MEntry> AbstractMutableMerkle<X> createMerkle(Class<X> leafType) {
    if (merkleExists(leafType)) {
      return new MutableMerkleImplementation<>(wrapped.getMerkle(leafType));
    }
    return new MutableMerkleInitial<>(leafType);
  }


  @Override
  public State finalizeBlock(Block block) throws StateSnapshotCorruptedException {
    changeListener.initialise(wrapped);
    changeListener.start(getVersion(), getHeight());
    try {
      commit();
      commitNow(getHeight());
    } catch (StateSnapshotCorruptedException e) {
      changeListener.fail(e);
      throw e;
    } catch (RuntimeException e) {
      changeListener.fail(null);
      throw e;
    }
    wrapped.finalizeBlock(block);
    changeListener.complete(wrapped);
    return wrapped;
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
    return contractEvents.values();
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
    if (removedXChainSignNodesMap.contains(keyName)) {
      return null;
    }

    XChainDetails thisValue = xChainSignNodesMap.get(keyName);

    if (thisValue == null) {
      thisValue = wrapped.getXChainSignNodes().get(keyName);
    }

    return thisValue;
  }


  @Override
  public boolean merkleExists(Class<? extends MEntry> name) {
    return merkles.containsKey(name) || wrapped.getMerkleTypes().contains(name);
  }

}
