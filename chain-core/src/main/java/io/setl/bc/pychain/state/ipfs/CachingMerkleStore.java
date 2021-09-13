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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.setl.bc.pychain.Hash;

public class CachingMerkleStore<T> implements MerkleStore<T> {

  private final LoadingCache<Hash, T> cache;

  private final MerkleStore<T> ms;


  /**
   * CachingMerkleStore.
   *
   * @param merkleStore :
   * @param maxSize :
   */
  public CachingMerkleStore(MerkleStore<T> merkleStore, int maxSize) {
    this.ms = merkleStore;
    this.cache = CacheBuilder.newBuilder().maximumSize(maxSize).build(new CacheLoader<Hash, T>() {
      @Override
      public T load(Hash hash) {
        return CachingMerkleStore.this.ms.get(hash);
      }
    });
  }


  @Override
  public T get(Hash hash) {
    return cache.getUnchecked(hash);
  }


  @Override
  public void put(Hash hash, T data) {
    cache.put(hash, data);
    ms.put(hash, data);
  }


}
