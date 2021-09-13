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
import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * @author Simon Greatrix on 2019-03-07.
 */
public class MemoryKeyToHashIndex<K> implements KeyToHashIndex<K> {

  static class MyHashAndIndex implements HashAndIndex {

    Hash hash;

    long index;


    @Override
    public Hash getHash() {
      return hash;
    }


    @Override
    public long getIndex() {
      return index;
    }
  }



  private final HashMap<K, MyHashAndIndex> map = new HashMap<>();


  /**
   * New instance.
   *
   * @param hash the Merkle tree root hash
   * @param ms   the Merkle store containing the objects
   */
  public MemoryKeyToHashIndex(Hash hash, MerkleStore<Object> ms) {
    IpfsWalker.walk(hash, ms, new BiConsumer<Hash, Object[]>() {
      long index = 0;


      @Override
      public void accept(Hash hash, Object[] value) {
        long i = index++;
        K key = (K) value[1];
        MyHashAndIndex hai = new MyHashAndIndex();
        hai.hash = hash;
        hai.index = i;
        map.put(key, hai);
      }
    });
  }


  private MemoryKeyToHashIndex(MemoryKeyToHashIndex<K> original) {
    map.putAll(original.map);
  }


  public MemoryKeyToHashIndex<K> copy() {
    return new MemoryKeyToHashIndex<>(this);
  }


  @Override
  public long find(K key) {
    MyHashAndIndex hai = map.get(key);
    return (hai != null) ? hai.index : -1;
  }


  @Override
  public HashAndIndex get(K key) {
    return map.get(key);
  }


  @Override
  public long getEntryCount() {
    return map.size();
  }


  @Override
  public void put(K key, long value) {
    MyHashAndIndex hai = new MyHashAndIndex();
    hai.index = value;
    map.put(key, hai);
  }


  @Override
  public void remove(K key) {
    map.remove(key);
  }


  @Override
  public void set(K key, Hash hash) {
    MyHashAndIndex hai = map.get(key);
    hai.hash = hash;
  }


  @Override
  public void set(K key, long index) {
    MyHashAndIndex hai = map.get(key);
    hai.index = index;
  }
}
