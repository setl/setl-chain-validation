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

import java.util.stream.Stream;

import io.setl.bc.pychain.state.entry.MEntry;

/**
 * A mutable Merkle which does not yet exist in state.
 *
 * @author Simon Greatrix on 10/10/2019.
 */
public class MutableMerkleInitial<V extends MEntry> extends AbstractMutableMerkle<V> {

  private final Class<V> leafType;

  private int changeSequence = 0;


  public MutableMerkleInitial(Class<V> leafType) {
    this.leafType = leafType;
  }


  @Override
  public void commit(int version, long blockHeight, StateChangeListener changeListener) {
    // We have no parent object to commit to. It probably does not seed saying, but this approach is not thread-safe. If another thread had created a parent
    // object in state or a parent snapshot, then we should commit to that object.
    throw new IllegalStateException("Initial Merkles cannot be committed directly");
  }


  @Override
  protected NamedObject<V> findEntryInState(String key) {
    return null;
  }


  @Override
  public Class<V> getLeafType() {
    return leafType;
  }


  /**
   * We only create the Merkle in state if this has at least one insert.
   *
   * @return true if there is at least one insert
   */
  boolean hasInsert() {
    for (NamedObject<?> n : changes.values()) {
      if (!n.isDeleted) {
        return true;
      }
    }

    // Everything changed was also deleted
    return false;
  }


  @Override
  protected boolean itemExistsInState(String key) {
    return false;
  }


  @Override
  int nextSequence() {
    return changeSequence++;
  }


  @Override
  public Stream<V> stream() {
    return changes.values().stream().filter(io -> !io.isDeleted).map(io -> io.object);
  }


  void writeTo(int version, long blockHeight, SnapshotMerkle<?> snapshotMerkle, StateChangeListener listener) {
    // Check type compatibility.
    SnapshotMerkle<V> snapshot = SnapshotMerkle.cast(snapshotMerkle, getLeafType());

    boolean setHeight = (version >= VERSION_USE_UPDATE_HEIGHT);
    for (NamedObject<V> n : changes.values()) {
      if (!n.isDeleted) {
        if (setHeight) {
          n.object.setBlockUpdateHeight(blockHeight);
        }
        listener.add(n.object);
        snapshot.update(n.name, n.object);
      }
    }
  }

}
