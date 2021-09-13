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
package io.setl.bc.pychain.state.monolithic;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.IndexedList;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.SnapshotMerkle;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.hashtree.HashTree;
import io.setl.bc.pychain.state.hashtree.IncrementalMerkleHashCalculator;
import io.setl.bc.pychain.state.hashtree.MemoryBackedHashTree;
import io.setl.bc.pychain.state.index.DefaultKeyToIndex;
import io.setl.bc.pychain.state.ipfs.MerkleStoreWriter;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Hex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class KeyedIndexedEntryList<ValueType extends MEntry>
    extends PyWrappedImmutableIndexedList implements Merkle<ValueType>, SnapshotMerkle<ValueType> {

  private static final SerialiseToByte hashSerialiser = HashSerialisation.getInstance();

  private static final Logger logger = LoggerFactory.getLogger(KeyedIndexedEntryList.class);

  private final EntryDecoder<ValueType> decoder;

  private final Class<ValueType> leafType;

  protected List<ValueType> entriesList;

  private HashTree hashTree = new MemoryBackedHashTree();

  private DefaultKeyToIndex<String> keyToIndex = new DefaultKeyToIndex<>();

  private MerkleStoreWriter<Object[]> msw;

  private Set<Long> unhashedChanges = new HashSet<>();


  /**
   * Create a new list.
   *
   * @param v       the data in the list
   * @param version the encoding version
   * @param decoder the decoder for individual list entries
   */
  public KeyedIndexedEntryList(MPWrappedArray v, int version, EntryDecoder<ValueType> decoder, Class<ValueType> leafType) {
    super(v, version);
    this.decoder = decoder;
    this.leafType = leafType;
    entriesList = decodeToEntryList();

    buildHashTree();

    for (long i = 0, l = getEntryCount(); i < l; i++) {
      keyToIndex.put(entriesList.get((int) i).getKey(), i);
    }
  }


  protected KeyedIndexedEntryList(EntryDecoder<ValueType> decoder, Class<ValueType> leafType) {
    this.decoder = decoder;
    this.leafType = leafType;
    entriesList = decodeToEntryList();
    buildHashTree();
  }


  private void buildHashTree() {
    Set<Long> changes = new HashSet<>();

    for (long i = 0, l = getEntryCount(); i < l; i++) {
      changes.add(i);
    }

    new IncrementalMerkleHashCalculator().computeHashesFromChanges(hashTree, this, hashSerialiser,
        changes, (i, a, b, c) -> {
          if (msw != null) {
            msw.put(a, b);
          }
        }
    );
  }


  @Override
  public String computeRootHash() {
    getHash();
    if (getEntryCount() == 0) {
      return "";
    }
    return Hex.encode(hashTree.getTopHashEntry());
  }


  /**
   * Debug method for retrieving the list contents. Not for use outside of testing.
   *
   * @return all entries
   *
   * @deprecated do not use
   */
  @Deprecated
  @Override
  public Object[][] debugGetChangedObjectArray() {
    if (!unhashedChanges.isEmpty()) {
      throw new IllegalStateException("Unhashed changes - access blocked");
    }
    return encodeEntryListToObjectArray(entriesList);
  }


  /**
   * Verify the loaded entries encode and decode to the same entries. Not for use outside of testing.
   */
  public void debugVerifyLoadedEntries() {
    Object[][] dec = encodeEntryListToObjectArray(decodeToEntryList());
    for (int i = 0; i < dec.length; i++) {
      Object[] a = super.getHashableEntry(i);
      Object[] b = dec[i];
      if (a.length != b.length) {
        logger.error("{} loaded length={} modified={}", this.getClass().getName(), a.length,
            b.length
        );
        throw new IllegalStateException("Persisted/modified mismatch on " + this.getClass().getName());
      }
    }
  }


  protected List<ValueType> decodeToEntryList() {

    List<ValueType> r = new ArrayList<>();
    for (int i = 0, l = wrappedMerkleArray.size(); i < l; i++) {
      MPWrappedArray va = wrappedMerkleArray.asWrapped(i);
      r.add(decoder.decode(va));
    }
    return r;
  }


  /**
   * Encode this list as nested arrays of primitives.
   *
   * @return the arrays containing the encoded data.l
   */
  public Object[] encode() {
    final int objecttype = 0;
    final int mversion = 0;
    final int datatype = 0;
    final int maxOffset = 0;

    return new Object[]{objecttype, mversion, datatype, maxOffset, encodeEntryListToObjectArray(entriesList)};
  }


  /**
   * Convert list back to Object[] format.
   */
  private Object[][] encodeEntryListToObjectArray(List<ValueType> e) {
    Object[][] r = new Object[e.size()][];
    int i = 0;
    for (ValueType a : e) {
      r[i] = a.encode(i);
      i++;
    }
    return r;
  }


  @Override
  public ValueType find(String key) {
    return get(findIndex(key));
  }


  public long findIndex(String key) {
    return keyToIndex.find(key);
  }


  /**
   * Get the entry in this via its index.
   *
   * @param index the entry's index
   *
   * @return the entry's value
   */
  public ValueType get(long index) {
    if (index < 0 || index >= entriesList.size()) {
      return null;
    }
    return entriesList.get((int) index);
  }


  @Override
  public long getEntryCount() {
    return entriesList.size();
  }


  /**
   * Calculate the SHA of this list.
   *
   * @return the SHA
   */
  public Hash getHash() {
    if (!unhashedChanges.isEmpty()) {
      Set<Long> changedIndices = unhashedChanges;
      IndexedList<Object[]> indexedList = this;
      unhashedChanges = new HashSet<>();
      new IncrementalMerkleHashCalculator().computeHashesFromChanges(hashTree, indexedList, hashSerialiser, changedIndices, null);
    }
    return new Hash(hashTree.getTopHashEntry());
  }


  @Override
  public Object[] getHashableEntry(long i) {
    if (!unhashedChanges.isEmpty()) {
      throw new IllegalStateException("Unhashed changes - access blocked");
    }
    ValueType a = entriesList.get((int) i);
    return a.encode((int) i);
  }


  @Override
  public Class<ValueType> getLeafType() {
    return leafType;
  }


  @Nullable
  public Map<String, Object> getPartialTreeMap(Collection<String> keys, Function<ValueType, Map<String, Object>> leaf2Map) {
    throw new UnsupportedOperationException("Not implemented");
  }


  @Override
  public boolean itemExists(String key) {
    return findIndex(key) != -1;
  }


  /**
   * Visit the entries in this list to update the backing Merkle Tree structure.
   *
   * @param msw a Merkle store writer instance (optional)
   */
  public void iterate(MerkleStoreWriter<Object[]> msw) {
    Set<Long> changes = new HashSet<>();

    for (long i = 0, l = getEntryCount(); i < l; i++) {
      changes.add(i);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("{} Changed entities {}/{}", this.getClass().getName(), changes.size(), getEntryCount());
    }

    new IncrementalMerkleHashCalculator().computeHashesFromChanges(hashTree, this, hashSerialiser,
        changes, (i, a, b, c) -> {
          if (msw != null) {
            msw.put(a, b);
          }
          logger.info("New hash:{}={}", a, b.getClass().getName());
        }
    );

  }


  @Override
  public boolean remove(String key) {
    return remove(-1, key) != -1;
  }


  /**
   * Remove an entry from this list.
   *
   * @param index The index of the object being updated
   * @param key   The removed key value. Provided as a performance benefit.
   *
   * @return the index of the entry which was moved into the vacated space.
   */
  public long remove(long index, String key) {

    if (index < 0) {
      index = findIndex(key);
      if (index < 0) {
        return -1;
      }
    }

    // TODO: remove this check when we are confident it never fails
    long keyIndex = findIndex(key);
    if (keyIndex != index) {
      throw new IllegalArgumentException("Index specified as " + index + " but should have been " + keyIndex + " for key \"" + key + "\"");
    }

    int size = entriesList.size();
    int lastIndex = size - 1;

    ValueType item;

    if (index < lastIndex) {
      // If not removing the last item, move the last item over the one being deleted
      item = entriesList.get(lastIndex);
      entriesList.set((int) index, item);
      keyToIndex.put(item.getKey(), index);

      unhashedChanges.add(index);
    }

    // Delete the key to the deleted item
    keyToIndex.remove(key);

    // Delete the last item.
    entriesList.remove(lastIndex);

    unhashedChanges.add((long) lastIndex);

    return lastIndex;
  }


  public void setMerkleStoreWriter(MerkleStoreWriter<Object[]> msw) {
    this.msw = msw;
  }


  @Override
  public Stream<ValueType> stream() {
    return entriesList.stream();
  }


  @Override
  public void update(String key, ValueType item) {
    update(findIndex(key), item);
  }


  /**
   * update(). <p>Add or Update an entry in this data set</p>
   *
   * @param givenIndex The index of the object being updated
   */
  public void update(long givenIndex, ValueType item) {
    long size = (long) entriesList.size();
    long index = givenIndex;

    // If index indicates 'Add', give it the right index
    if (index < 0) {
      index = size;
    }

    // If index exists, Set the item.
    if (index < size) {

      entriesList.set((int) index, item);

    } else {
      // Else add it (if a valid index)
      if (index == size) {

        entriesList.add(item);
        keyToIndex.put(item.getKey(), index);

      } else {
        throw new IllegalArgumentException(String.format("index %d exceeds size %d", index, size));
      }
    }

    unhashedChanges.add(index);
  }
}
