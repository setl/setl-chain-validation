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

import static com.google.common.base.Verify.verify;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.IndexedList;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.SnapshotMerkle;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.hashtree.ChangeConsumer;
import io.setl.bc.pychain.state.hashtree.HashTree;
import io.setl.bc.pychain.state.hashtree.IncrementalMerkleHashCalculator;
import io.setl.bc.pychain.state.hashtree.MemoryBackedHashTree;
import io.setl.bc.pychain.state.ipfs.KeyToHashIndex.HashAndIndex;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Hex;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

public class IpfsList<V extends MEntry> implements Merkle<V>, SnapshotMerkle<V> {

  private static final byte[] DELETEDENTRY = new byte[]{0x7e};

  private static final byte[] EMPTYARRAY = new byte[0];

  private static final SerialiseToByte hashSerialiser = HashSerialisation.getInstance();



  public static class OverridingHashTree implements HashTree {

    private final IpfsDirectAccessor<Object[]> accessor;

    private final HashTree hashTree;


    public OverridingHashTree(IpfsDirectAccessor<Object[]> accessor, HashTree overridingHashTree) {
      this.hashTree = overridingHashTree;
      this.accessor = accessor;
    }


    @Override
    public byte[] getHashEntry(int level, long i) {

      byte[] o = hashTree.getHashEntry(level, i);
      if (o == DELETEDENTRY) {
        return EMPTYARRAY;
      }
      if (o.length != 0) {
        return o;
      }
      o = accessor.getHashEntry(level, i);
      return o == null ? EMPTYARRAY : o;
    }


    @Override
    public byte[] getTopHashEntry() {
      return accessor.getRootHash().get();
    }


    @Override
    public void removeHash(int level, long entry) {
      hashTree.setHash(level, entry, DELETEDENTRY);
    }


    @Override
    public void setHash(int level, long entry, byte[] hash) {
      hashTree.setHash(level, entry, hash);
    }


    @Override
    public void trimTree(long fromIndex) {
      //Nothing to do
    }
  }



  private final IpfsDirectAccessor<Object[]> accessor;

  private final EntryDecoder<V> decoder;

  private final Class<V> leafType;

  private final MerkleStore<Object> ms;

  private final KeyToHashIndex<String> theIndex;

  private final Map<Long, V> unhashedChanges = new HashMap<>();

  private Hash hash;


  public IpfsList(Hash hash, MerkleStore<Object> ms, EntryDecoder<V> decoder, Class<V> leafType) {
    this(hash, ms, decoder, null, leafType);
  }


  /**
   * IPFS implementation of Merkle.java interface.
   *
   * @param hash : Root hash of merkle
   * @param ms   : Store from which to retrieve data
   */
  public IpfsList(Hash hash, MerkleStore<Object> ms, EntryDecoder<V> decoder, KeyToHashIndex<String> theIndex, Class<V> leafType) {
    this.leafType = leafType;
    // Note Hash maybe null when merkle size is zero (blank hash in python!)

    // TODO - Inject the implementations of theIndex
    if (theIndex == null) {
      this.theIndex = new MemoryKeyToHashIndex<>(hash, ms);
    } else {
      this.theIndex = theIndex;
    }

    accessor = new IpfsDirectAccessor<>(ms, hash, this.theIndex.getEntryCount());
    this.decoder = decoder;
    this.hash = hash;
    this.ms = ms;
  }


  private void applyChanges() {
    if (unhashedChanges.isEmpty()) {
      // Nothing to do.
      return;
    }
    // Flush index changes to ensure persisted
    theIndex.flush();

    HashTree memoryHashTree = new MemoryBackedHashTree();
    HashTree hashTree = new OverridingHashTree(accessor, memoryHashTree);

    final long indexEntryCount = theIndex.getEntryCount();

    // Create consumer to write changes
    ChangeConsumer cc = (index, contentHash, payload, isLeaf) -> {
      ms.put(contentHash, payload);
      if (isLeaf) {
        theIndex.set((String) payload[1], contentHash);
        V v = unhashedChanges.get(index);
        if (v != null) {
          GlobalHashCache.put(contentHash, v);
        }
      }
    };

    // Create accessor, which overrides current values with any changed
    // values
    IndexedList<Object[]> indexedList = new IndexedList<Object[]>() {
      @Override
      public long getEntryCount() {

        return indexEntryCount;
      }


      @Override
      public Object[] getHashableEntry(long i) {

        V e = unhashedChanges.get(i);
        if (e != null) {
          return e.encode(i);
        } else {
          return IpfsList.this.getHashableEntry(i);
        }
      }
    };

    byte[] returnHash = new IncrementalMerkleHashCalculator().computeHashesFromChanges(hashTree, indexedList,
        hashSerialiser, unhashedChanges.keySet(), cc
    );

    this.hash = new Hash(returnHash);
    accessor.setEntryCount(indexEntryCount, hash);

    unhashedChanges.clear();
  }


  @Override
  public String computeRootHash() {
    getHash();
    if (getEntryCount() == 0) {
      return "";
    }
    return Hex.encode(hash.get());
  }


  public IpfsList<V> copy() {
    return new IpfsList<>(hash, ms, decoder, theIndex.copy(), leafType);
  }


  @Deprecated
  @Override
  public Object[][] debugGetChangedObjectArray() {
    return new Object[0][];
  }


  @Deprecated
  @Override
  public MPWrappedArray debugGetWrappedMerkleArray() {
    int l = (int) getEntryCount();
    Object[] r = new Object[l];
    for (int i = 0; i < l; i++) {
      r[i] = getHashableEntry(i);
    }
    return new MPWrappedArrayImpl(r);
  }


  @Override
  public V find(String key) {
    HashAndIndex hashAndIndex = theIndex.get(key);
    if (hashAndIndex == null) {
      return null;
    }
    return GlobalHashCache.get(hashAndIndex.getHash(), h -> get(hashAndIndex.getIndex()));
  }


  public long findIndex(String key) {
    return theIndex.find(key);
  }


  @Override
  public void forEach(Consumer<? super V> c) {
    IpfsWalker.walk(hash, ms, a -> c.accept(decoder.decode(new MPWrappedArrayImpl((Object[]) a))));
  }


  /**
   * Get an entry from this list by its index.
   *
   * @param index the entry's index
   *
   * @return the entry
   */
  public V get(long index) {
    if (index < 0) {
      return null;
    }
    return decoder.decode(new MPWrappedArrayImpl(getHashableEntry(index)));
  }


  /**
   * Get the number of entries in this list.
   *
   * @return the entry count
   */
  public long getEntryCount() {
    if (accessor == null) {
      return 0L;
    }
    return accessor.getEntryCount();
  }


  @Override
  public Hash getHash() {
    // Apply any outstanding changes - which will persist and recompute the entire merkle structure
    // as required.
    applyChanges();
    return hash;
  }


  public Object[] getHashableEntry(long index) {
    return accessor.getHashableEntry(index);
  }


  @Override
  public Class<V> getLeafType() {
    return leafType;
  }


  /**
   * Get the tree that contains only the specified leaves.
   *
   * @param keys the leaf keys to match
   *
   * @return the tree, or null if no leaves matched
   */
  @Nullable
  public PartialTree<V> getPartialTree(Collection<String> keys) {
    boolean foundOne = false;
    PartialTree<V> partialTree = new PartialTree<>();
    for (String k : keys) {
      long index = findIndex(k);
      if (index >= 0) {
        TreePath<Object[]> treePath = accessor.buildTreePath(index);
        if (treePath != null) {
          // found the index
          foundOne = true;
          V leaf = decoder.decode(new MPWrappedArrayImpl(treePath.getLeafValue()));
          partialTree.addPath(treePath, leaf);
        }
      }
    }
    return foundOne ? partialTree : null;
  }


  /**
   * Get the tree that contains only the specified items, using the provided convertor to transform the leaves into an external form.
   *
   * @param keys     the leaf keys to match
   * @param leaf2Map convert leaf values to name-value maps.
   *
   * @return the tree, or null if no leaves matched
   */
  @Nullable
  public Map<String, Object> getPartialTreeMap(Collection<String> keys, Function<V, Map<String, Object>> leaf2Map) {
    PartialTree<V> partialTree = getPartialTree(keys);
    if (partialTree == null) {
      return null;
    }
    return partialTree.toMap(leaf2Map, l -> hashSerialiser.serialise(l.encode(-1)));
  }


  @Override
  public boolean itemExists(String key) {
    return findIndex(key) != -1;
  }


  /**
   * Remove an entry from this list.
   *
   * @param index the index of the entry to remove. (Specify &lt;0 to search for it by key)
   * @param key   the key of the entry to remove
   *
   * @return the new list size
   */
  public long remove(long index, String key) {
    long lastIndex = theIndex.getEntryCount() - 1;

    if (index < 0) {
      index = findIndex(key);
    }

    if (index < lastIndex) {
      applyChanges();
      // If not removing the last item, move the last item over the one being deleted
      V item = get(lastIndex);
      theIndex.set(key, index);
      unhashedChanges.put(index, item);
    }
    // Delete the key to the deleted item
    theIndex.remove(key);

    unhashedChanges.put(lastIndex, null);

    return lastIndex;
  }


  @Override
  public boolean remove(String key) {
    return remove(-1, key) != -1;
  }


  @Override
  public Stream<V> stream() {
    return StreamSupport.stream(new IpfsWalker<V>(ms, hash, decoder), false);
  }


  @Override
  public void update(String key, V object) {
    update(findIndex(key), object);
  }


  /**
   * Update the given entry in this list.
   *
   * @param givenIndex the index of the updated entry (specify &lt;0 to add)
   * @param object     the new value
   */
  public void update(long givenIndex, V object) {
    long index = givenIndex;
    long size = theIndex.getEntryCount();

    // If index indicates 'Add', give it the right index
    if (index < 0) {
      index = size;
    }

    verify(index <= size, "index %s exceeds size %s", index, size);

    //Assumption that key may not change - so only need to add new
    if (index == size) {
      theIndex.put(object.getKey(), index);
    }

    unhashedChanges.put(index, object);
    theIndex.put(object.getKey(), index);
  }


  /**
   * Walk the Merkle tree, and return the number of entries in the base list.
   *
   * @return the number of entries
   */
  public long walk() {
    long[] r = new long[1];
    IpfsWalker.walk(hash, ms, t -> r[0]++);
    return r[0];
  }
}
