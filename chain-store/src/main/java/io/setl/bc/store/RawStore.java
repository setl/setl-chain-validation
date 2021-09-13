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
package io.setl.bc.store;

import java.io.IOException;

/**
 * A simple key value store.
 * Created by aanten on 01/06/2017.
 */
public interface RawStore {

  /**
   * Delete the value stored against key.
   *
   * @param key The key
   */
  void delete(byte[] key);

  /**
   * Ensure all updates have been written to persistent storage, if such a concept is relevant to this store.
   */
  default void flush() {
    // do nothing
  }

  /**
   * Get value stored against key.
   *
   * @param key The key
   *
   * @return The data, or null if not present
   */
  byte[] get(byte[] key);

  /**
   * Save or replace data stored against key.
   *
   * @param key  The key
   * @param data The value
   */
  void put(byte[] key, byte[] data);


  /**
   * Save data stored against key, provided no such data is already present.
   *
   * @param key  The key
   * @param data The value
   */
  void putIfAbsent(byte[] key, byte[] data);

  /**
   * Safely shutdown this store.
   */
  void shutdown() throws IOException;
}
