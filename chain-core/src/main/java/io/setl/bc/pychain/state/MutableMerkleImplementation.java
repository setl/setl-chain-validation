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

import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.setl.bc.pychain.state.entry.MEntry;

/**
 * A memory based mutable wrapper around a Merkle.
 *
 * @author aanten
 */
public class MutableMerkleImplementation<ValueType extends MEntry> extends AbstractMutableMerkle<ValueType> {

  /** The actual state list. */
  private final SnapshotMerkle<ValueType> wrapped;

  /** Sequence number for changes. */
  private int changeSequence = 0;


  /**
   * New instance which wraps the specified state list.
   *
   * @param wrappedState : State upon which to operate.
   */
  public MutableMerkleImplementation(Merkle<ValueType> wrappedState) {
    wrapped = (SnapshotMerkle<ValueType>) wrappedState;
  }


  /**
   * Apply changes to underlying state.
   */
  public void commit(int version, long blockHeight, StateChangeListener changeListener) {
    // In general the intention is to produce an outcome identical to updating the wrapped list directly. However, mixtures of adding and deleting the same
    // key are optimized to a single add, a single delete, or a no-op. This will not produce the same index as performing all the additions and deletions.
    List<NamedObject<ValueType>> addOrDeletes = new ArrayList<>();
    boolean setHeight = (version >= VERSION_USE_UPDATE_HEIGHT);

    // immediately apply all updates, as they do not change the index ordering
    for (NamedObject<ValueType> io : changes.values()) {
      if (setHeight) {
        io.object.setBlockUpdateHeight(blockHeight);
      }
      if (!io.isNew && !io.isDeleted) {
        changeListener.update(io.object);
        wrapped.update(io.name, io.object);
      } else {
        // It is an add or delete. If it is both then it was added and immediately deleted for a no-op.
        if (!(io.isNew && io.isDeleted)) {
          addOrDeletes.add(io);
        }
      }
    }

    // Sort into natural order, which is increasing change sequence.
    Collections.sort(addOrDeletes);

    // Process additions and deletes in sequence order.
    for (NamedObject<ValueType> io : addOrDeletes) {
      // existing entries are updates or deletes
      if (io.isDeleted) {
        // This is a delete
        changeListener.remove(io.object.getKey(), getLeafType());
        wrapped.remove(io.object.getKey());
      } else {
        // This is an add
        changeListener.add(io.object);
        wrapped.update(io.name, io.object);
      }
    }

    // clear applied changes and cache
    reset();
  }


  @Override
  protected NamedObject<ValueType> findEntryInState(String key) {
    // Find item in wrapped data.
    ValueType thisObject = wrapped.find(key);
    if (thisObject == null) {
      // Item does not exist
      return null;
    }
    return new NamedObject<>(-1, false, key, thisObject);
  }


  @Override
  public Class<ValueType> getLeafType() {
    return wrapped.getLeafType();
  }


  @Override
  protected boolean itemExistsInState(String key) {
    return wrapped.itemExists(key);
  }


  @Override
  int nextSequence() {
    return changeSequence++;
  }


  @Override
  public Stream<ValueType> stream() {
    Stream<ValueType> parentStream = wrapped.stream().filter(value -> !changes.containsKey(value.getKey()));
    Stream<ValueType> changedStream = changes.values().stream().filter(io -> !io.isDeleted).map(io -> io.object);
    return Stream.concat(changedStream, parentStream);
  }
}
