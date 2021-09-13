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
 * Map key to index. Used to locate the position within a state list of a specific key
 *
 * @author aanten
 */
public interface KeyToIndex<K> {

  /**
   * Find the index of the specified key.
   * @param key The key.
   * @return The index.
   */
  long find(K key);

  void remove(K key);

  /**
   * Get the total number of entries.
   * @return The number of entries.
   */
  long getEntryCount();

  /**
   * Updated the specified key to the given index.
   * @param key The key.
   * @param value The new index.
   */
  void put(K key, long value);


  /**
   * Hint to underlying implementation that it is a good time to flush to storage.
   */
  default void flush() {
    // Nothing
  }
}
