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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.IterableSerializer;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * A transactional, searchable and mutable list.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @author andy
 */
@JsonSerialize(as = Iterable.class)
public interface KeyedList<K, V> extends Iterable<V> {

  /**
   * Find element using given key.
   *
   * @param key The key to search for
   *
   * @return The found value, or null if not found
   */
  V find(K key);

  /**
   * Does state contain an entry with the given name?.
   *
   * @param key the key to check
   *
   * @return true if an entry with that key exists
   */
  boolean itemExists(String key);

  default Iterator<V> iterator() {
    return stream().iterator();
  }

  Stream<V> stream();
}
