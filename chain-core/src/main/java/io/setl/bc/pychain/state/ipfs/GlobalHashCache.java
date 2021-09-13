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
package io.setl.bc.pychain.state.ipfs;

import io.setl.bc.pychain.Hash;
import io.setl.util.LRUCache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author Simon Greatrix on 2019-03-07.
 */
public class GlobalHashCache {

  private static final ConcurrentHashMap<Hash, Object> LAST_BLOCK = new ConcurrentHashMap<>();

  private static final LRUCache<Hash, Object> LRU_CACHE = new LRUCache<>(20_000, null);


  /**
   * Get the object identified by the given Hash.
   *
   * @param hash   the hash
   * @param source mapping of hashes to values
   * @param <V>    the required data type
   *
   * @return the datum
   */
  public static <V> V get(Hash hash, Function<Hash, ? extends V> source) {
    @SuppressWarnings("unchecked")
    V v = (V) LAST_BLOCK.get(hash);
    if (v != null) {
      LRU_CACHE.put(hash, v);
      return v;
    }
    return LRU_CACHE.get(hash, source);
  }


  public static void put(Hash hash, Object value) {
    LAST_BLOCK.put(hash, value);
  }


  public static void reset() {
    LAST_BLOCK.clear();
  }
}
