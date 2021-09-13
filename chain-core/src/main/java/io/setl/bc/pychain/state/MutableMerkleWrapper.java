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

import io.setl.bc.pychain.state.entry.MEntry;
import java.util.stream.Stream;

// otherwise a more performant map could be better ?



/**
 * Created by nicholaspennington on 28/07/2017.
 */
public class MutableMerkleWrapper<ValueType extends MEntry> extends AbstractMutableMerkle<ValueType> {

  private AbstractMutableMerkle<ValueType> wrapped;


  /**
   * MutableMerkleWrapper.
   * <p>Constructor.
   * This Class provides snapshot functionality for the Data Items stored in an underlying snapshot.
   * The MutableMerkleImplementation Class manages the process of getting and conditionally updating / saving data to state.</p>
   * <p>It was considered useful to be able to conditionally update snapshots in the same way that they conditionally update state.</p>
   *
   * @param baseWrapper : State upon which to operate.
   */
  public MutableMerkleWrapper(AbstractMutableMerkle<ValueType> baseWrapper) {
    wrapped = baseWrapper;
  }


  /**
   * MutableMerkleWrapper.
   * <p>Constructor.
   * This Class provides snapshot functionality for the Data Items stored in an underlying snapshot.
   * The MutableMerkleImplementation Class manages the process of getting and conditionally updating / saving data to state.</p>
   * <p>It was considered useful to be able to conditionally update snapshots in the same way that they conditionally update state.</p>
   *
   * @param baseWrapper : State upon which to operate.
   */
  public MutableMerkleWrapper(MutableMerkle<ValueType> baseWrapper) {
    this((AbstractMutableMerkle<ValueType>) baseWrapper);
  }


  @Override
  public void commit(int version, long blockHeight, StateChangeListener changeListener) {
    wrapped.bulkChange(changes);
    reset();
  }


  @Override
  protected NamedObject<ValueType> findEntryInState(String key) {
    return wrapped.findEntry(key, false);
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
    return wrapped.nextSequence();
  }


  @Override
  public Stream<ValueType> stream() {
    Stream<ValueType> parentStream = wrapped.stream().filter(value -> !changes.containsKey(value.getKey()));
    Stream<ValueType> changedStream = changes.values().stream().filter(io -> !io.isDeleted).map(io -> io.object);
    return Stream.concat(changedStream, parentStream);
  }
}
