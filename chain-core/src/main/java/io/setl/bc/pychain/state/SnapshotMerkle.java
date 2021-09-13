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

/**
 * A Merkle that can be mutated via a snapshot.
 *
 * @param <V> value type
 *
 * @author Simon Greatrix
 */
public interface SnapshotMerkle<V> extends Merkle<V> {

  /**
   * Cast this SnapshotMerkle to one that has the required explicit leaf type.
   *
   * @param type the specific leaf type
   * @param <V2> the leaf type
   *
   * @return this Merkle with the required leaf type
   */
  static <V2> SnapshotMerkle<V2> cast(SnapshotMerkle<?> instance, Class<V2> type) {
    if (type.isAssignableFrom(instance.getLeafType())) {
      // As we have checked the leaf type, this is not really an unchecked cast
      @SuppressWarnings("unchecked")
      SnapshotMerkle<V2> castInstance = (SnapshotMerkle<V2>) instance;
      return castInstance;
    }
    throw new ClassCastException("Cannot convert leaf type " + instance.getLeafType() + " to " + type);
  }


  /**
   * Provide mechanism to update state ONLY from a state snapshot.
   *
   * @param key The removed key value.
   *
   * @return true if the entry was found and removed.
   */
  boolean remove(String key);

  /**
   * Provide mechanism to update state ONLY from a state snapshot.
   *
   * @param key    The key for the object being updated
   * @param object The updated value
   */
  void update(String key, V object);
}
