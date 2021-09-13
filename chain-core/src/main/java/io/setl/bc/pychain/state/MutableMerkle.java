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

import java.util.Set;

public interface MutableMerkle<V> extends KeyedList<String, V> {

  /**
   * Cast the MutableMerkle to one with the specified leaf type.
   *
   * @param instance the instance to cast
   * @param type     the required leaf type
   * @param <V2>     the required leaf type
   *
   * @return this Merkle, having been cast
   */
  static <V2> MutableMerkle<V2> cast(MutableMerkle<?> instance, Class<V2> type) {
    if (type.isAssignableFrom(instance.getLeafType())) {
      // As we have checked the leaf type, this cast is not really unchecked
      @SuppressWarnings("unchecked")
      MutableMerkle<V2> castInstance = (MutableMerkle<V2>) instance;
      return castInstance;
    }
    throw new ClassCastException("Cannot convert leaf type " + instance.getLeafType() + " to " + type);
  }

  /**
   * Add the specified entry to this.
   *
   * @param entry the entry to add
   */
  void add(V entry);

  /**
   * Apply changes to underlying state.
   *
   * @param version        the state version
   * @param blockHeight    the chain height
   * @param changeListener to inform of state changes
   */
  void commit(int version, long blockHeight, StateChangeListener changeListener);

  /**
   * Delete the named entry.
   *
   * @param key the name to delete
   */
  void delete(String key);

  /**
   * Find an entry and mark it as updated.
   *
   * @param key the entry's name
   *
   * @return the entry
   */
  V findAndMarkUpdated(String key);

  /**
   * Get the type of the leaves.
   *
   * @return the leaf type
   */
  Class<V> getLeafType();

  /**
   * Get the set of all entry keys that have been marked as updated.
   *
   * @return the set of keys
   */
  Set<String> getUpdatedKeys();

  /**
   * Test if an entry with the given names exists in this.
   *
   * @param key the key to check
   *
   * @return true if it exists
   */
  boolean itemExists(String key);

  /**
   * Reset this mutable list to match the underlying list.
   */
  void reset();

}
