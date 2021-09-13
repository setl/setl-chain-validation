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

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import javax.annotation.Nullable;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.IndexedList;

/**
 * Lightning strike access through provided merkle store, by index.
 *
 * @author aanten
 */
// S1192 - define constants for repeated strings, but these strings are error messages
@SuppressWarnings("squid:S1192")
public class IpfsDirectAccessor<T> implements IndexedList<T> {

  private static final byte[] emptyArray = new byte[0];

  private final MerkleStoreReader<Object> ms;

  private long entryCount;

  private int levels;

  private Hash rootHash;


  /**
   * New instance.
   *
   * @param ms         Reader to access the Merkle store
   * @param rootHash   the root hash of the Merkle tree
   * @param entryCount the number of entries in the base list
   */
  public IpfsDirectAccessor(MerkleStoreReader<Object> ms, Hash rootHash, long entryCount) {
    this.entryCount = entryCount;
    this.rootHash = rootHash;
    this.ms = ms;
    if (entryCount == 0) {
      return;
    }
    levels = entryCount == 1 ? 2 : (65 - Long.numberOfLeadingZeros(entryCount - 1));
  }


  /**
   * Get the path from the root to the specified item index. If the index is invalid, will return null.
   *
   * @param index the item's index
   *
   * @return the path
   */
  @Nullable
  public TreePath<T> buildTreePath(long index) {
    // if index is outside the valid range, we can't do anything.
    if (index < 0 || entryCount <= index) {
      return null;
    }

    TreePath<T> treePath = new TreePath<>();
    treePath.setRoot(rootHash);

    // for compatibility with python - always hash 2 or more levels
    verify(index < entryCount, "Index %d >= entries %d", index, entryCount);

    // the entry will be in the bottom level of the tree, so work down through (levels-1) levels.
    Hash id = rootHash;
    requireNonNull(id, "No root hash found");

    for (int i = 0; i < levels - 1; i++) {
      Object o = ms.get(id);
      verifyNotNull(o, "No item found at level %d:%s", i, id);
      verify(o instanceof byte[][], "Expecting branch at level %d", i);

      byte[][] oo = (byte[][]) o;
      Hash left = new Hash(oo[0]);
      Hash right = oo.length > 1 ? new Hash(oo[1]) : null;
      treePath.addBranch(id, left, right);

      // Get next level's hash from Merkle node
      long lIndex = index >> (levels - 2 - i);
      id = (lIndex & 1) == 0 ? left : right;
      requireNonNull(id, "Corrupt tree structure");
    }

    Object o = ms.get(id);
    verifyNotNull(o, "No item found:%s", id);
    verify(!(o instanceof byte[][]), "Expecting leaf at level %d", levels - 1);
    treePath.setLeaf(id, (T) o);
    return treePath;
  }


  @Override
  public long getEntryCount() {
    return entryCount;
  }


  /**
   * getHashEntry.
   *
   * @param level 0 indicates leaf
   * @param index index within given level
   *
   * @return The hash at the given position
   */
  // S1168 - we do intend to return null
  @SuppressWarnings("squid:S1168")
  public byte[] getHashEntry(int level, long index) {
    // if no entries, then the hash is obviously null.
    if (entryCount == 0) {
      return null;
    }

    // for compatibility with python - always hash 2 or more levels
    if (index >= maxEntries(level, entryCount)) {
      return emptyArray;
    }

    // if we are looking for the root hash, can quickly return that.
    if (level == levels - 1 && index == 0) {
      return rootHash.get();
    }

    // We are counting up from the leaves,
    int targetLevel = levels - level - 2;
    Hash cHash = rootHash;
    for (int i = 0; i < targetLevel; i++) {
      Object o = ms.get(cHash);

      verifyNotNull(o, "No item found at level %d:%s", i, cHash);
      verify(o instanceof byte[][], "Expecting branch at level %d", i);

      byte[][] oo = (byte[][]) o;

      long lIndex = index >> (targetLevel - i);
      cHash = new Hash(oo[(int) (lIndex & 1)]);
    }

    // read the target level
    Object o = ms.get(cHash);
    verifyNotNull(o, "No item found:%s", cHash);
    verify(o instanceof byte[][], "Expecting branch at level %d", targetLevel);
    byte[][] oo = (byte[][]) o;

    if ((index & 1) == 0) {
      return oo[0];
    }

    return oo.length > 1 ? oo[1] : null;
  }


  @Override
  public T getHashableEntry(long index) {
    // if no entries, then the entry is obviously null
    if (entryCount == 0) {
      return null;
    }

    // for compatibility with python - always hash 2 or more levels
    verify(index < entryCount, "Index %d >= entries %d", index, entryCount);

    // the entry will be in the bottom level of the tree, so work down through (levels-1) levels.
    Hash cHash = rootHash;
    for (int i = 0; i < levels - 1; i++) {
      Object o = ms.get(cHash);
      verifyNotNull(o, "No item found at level %d:%s", i, cHash);
      verify(o instanceof byte[][], "Expecting branch at level %d", i);

      byte[][] oo = (byte[][]) o;

      // Get next level's hash from Merkle node
      long lIndex = index >> (levels - 2 - i);
      cHash = new Hash(oo[(int) (lIndex & 1)]);
    }

    Object o = ms.get(cHash);
    verifyNotNull(o, "No item found:%s", cHash);
    verify(!(o instanceof byte[][]), "Expecting leaf at level %d", levels - 1);
    return (T) o;
  }


  public Hash getRootHash() {
    return rootHash;
  }


  private long maxEntries(int level, final long entryCount0) {
    long count = entryCount0;
    for (int i = 0; i < level; i++) {
      count >>= 1;
      if ((count & 1) == 1) {
        count++;
      }
    }
    return count;
  }


  /**
   * Set the number of entries in the merkle tree base list, and the root hash.
   *
   * @param l    the number of entries
   * @param hash the base hash
   */
  public void setEntryCount(long l, Hash hash) {
    this.entryCount = l;
    levels = l == 1 ? 2 : (65 - Long.numberOfLeadingZeros(l - 1));
    this.rootHash = hash;
  }

}
