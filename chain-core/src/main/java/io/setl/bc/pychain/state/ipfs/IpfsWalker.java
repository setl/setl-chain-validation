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

import static com.google.common.base.Verify.verifyNotNull;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.bc.pychain.state.entry.MEntry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Walk a Content-addressable storage structure.
 *
 * @author aanten
 */
public class IpfsWalker<T extends MEntry> implements Spliterator<T> {

  /**
   * Walk a Content addressable storage structure from the given root hash.
   *
   * @param hash Root hash of merkle
   * @param ms   Store from which to retrieve data
   * @param tgt  Leaf node consumer
   */
  public static <T> void walk(Hash hash, MerkleStoreReader<Object> ms, Consumer<T> tgt) {
    walk(hash, ms, (BiConsumer<Hash, T>) (h, t) -> tgt.accept(t));
  }


  /**
   * Walk a Content addressable storage structure from the given root hash.
   *
   * @param hash Root hash of merkle
   * @param ms   Store from which to retrieve data
   * @param tgt  Leaf node consumer
   */
  public static <T> void walk(Hash hash, MerkleStoreReader<Object> ms, BiConsumer<Hash, T> tgt) {
    // Hash maybe null when merkle size is zero (blank hash in python!)
    if (hash.get() != null) {
      walkHashesAndBuildLeaf(ms, hash, tgt);
    }
  }


  private static <T> void walkHashesAndBuildLeaf(MerkleStoreReader<Object> ms, Hash hash, BiConsumer<Hash, T> tgt) {
    Object o = ms.get(hash);
    verifyNotNull(o, "No item found: %s", hash);

    if (o instanceof byte[][]) {
      byte[][] oo = (byte[][]) o;
      // Merkle node
      walkHashesAndBuildLeaf(ms, new Hash(oo[0]), tgt);
      if (oo.length > 1) {
        walkHashesAndBuildLeaf(ms, new Hash(oo[1]), tgt);
      }
    } else {
      // Leaf node
      tgt.accept(hash, (T) o);
    }
  }


  final EntryDecoder<T> decoder;

  final MerkleStoreReader<Object> storeReader;

  final Deque<Hash> toVisit = new ArrayDeque<>();

  T nextValue = null;


  /**
   * New instance.
   *
   * @param storeReader the Merkle store which stores the IPFS tree
   * @param root        the root of the tree
   * @param decoder     the decoder for leaf entries
   */
  public IpfsWalker(MerkleStoreReader<Object> storeReader, Hash root, EntryDecoder<T> decoder) {
    this.storeReader = storeReader;
    this.decoder = decoder;
    toVisit.push(root);
  }


  @Override
  public int characteristics() {
    return NONNULL | IMMUTABLE;
  }


  @Override
  public long estimateSize() {
    // Size is unknown
    return Long.MAX_VALUE;
  }


  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    while (nextValue == null && !toVisit.isEmpty()) {
      visitNode();
    }
    if (nextValue == null) {
      // no more entries
      return false;
    }

    // found an entry, visit it
    action.accept(nextValue);
    nextValue = null;
    return true;
  }


  @Override
  public Spliterator<T> trySplit() {
    if (nextValue != null || toVisit.isEmpty()) {
      return null;
    }

    visitNode();

    if (nextValue != null) {
      // this was a leaf, so cannot split
      return null;
    }

    return new IpfsWalker<>(storeReader, toVisit.pop(), decoder);
  }


  private void visitNode() {
    Hash hash = toVisit.pop();
    Object o = storeReader.get(hash);
    verifyNotNull(o, "No item found: %s", hash);

    if (o instanceof byte[][]) {
      byte[][] ba = (byte[][]) o;
      toVisit.push(new Hash(ba[0]));
      if (ba.length > 1) {
        toVisit.push(new Hash(ba[1]));
      }
    } else {
      nextValue = decoder.decode(new MPWrappedArrayImpl((Object[]) o));
    }
  }
}
