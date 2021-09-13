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

import java.util.stream.Stream;
import javax.annotation.Nullable;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.dbstore.DBStoreException;

/**
 * A store of binary data which is keyed upon a hash value.
 *
 * @author Simon Greatrix on 2019-02-12.
 */
public interface HashStore {

  /**
   * Scan all data in the store.
   *
   * @return stream of all data
   */
  Stream<byte[]> contents();


  /**
   * Flush all records relating to a finalised state to persistent storage.
   *
   * @param state the state
   */
  void flush(AbstractState state);


  /**
   * Get a value from the hash store by its hash key.
   *
   * @param key the key
   *
   * @return the value, or null if not found
   */
  @Nullable
  byte[] get(Hash key);


  /**
   * Insert a new value into the store.
   *
   * @param data the data to store
   *
   * @return the hash to retrieve the data with in future
   */
  Hash insert(byte[] data);


  /**
   * Shutdown the storage, ensuring all data is written.
   */
  void shutdown() throws DBStoreException;

}
