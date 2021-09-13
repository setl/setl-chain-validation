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

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.entry.HashWithType;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * An indexed, transactional, and searchable list.
 *
 * @param <V> value type
 *
 * @author andy
 */
public interface Merkle<V> extends KeyedList<String, V> {

  /**
   * Cast this Merkle to one that has the required explicit leaf type.
   *
   * @param type the specific leaf type
   * @param <V2> the leaf type
   *
   * @return this Merkle with the required leaf type
   */
  static <V2> Merkle<V2> cast(Merkle<?> instance, Class<V2> type) {
    if( instance==null ) {
      return null;
    }
    if (type.isAssignableFrom(instance.getLeafType())) {
      // As we have checked the leaf type, this is not really an unchecked cast
      @SuppressWarnings("unchecked")
      Merkle<V2> castInstance = (Merkle<V2>) instance;
      return castInstance;
    }
    throw new ClassCastException("Cannot convert leaf type " + instance.getLeafType() + " to " + type);
  }


  /**
   * Apply any outstanding changes, and compute/recompute the root hash as required.
   */
  String computeRootHash();

  /**
   * For testing only.
   *
   * @return changed objects
   *
   * @deprecated for testing only
   */
  @Deprecated
  Object[][] debugGetChangedObjectArray();

  /**
   * For testing only.
   *
   * @return merkle array
   *
   * @deprecated for testing only
   */
  @Deprecated
  MPWrappedArray debugGetWrappedMerkleArray();

  /**
   * Get the hash.
   *
   * @return the hash
   */
  Hash getHash();

  /**
   * Get the hash and the algorithm ID.
   *
   * @return hash and type
   */
  default HashWithType getHashWithType() {
    return new HashWithType(getHash(), Digest.TYPE_SHA_256);
  }

  /**
   * Get the type of the leaves.
   *
   * @return the leaf type
   */
  Class<V> getLeafType();

  /**
   * Get the tree that contains only the specified items, using the provided convertor to transform the leaves into an external form.
   *
   * @param keys     the leaf keys to match
   * @param leaf2Map convert leaf values to name-value maps.
   *
   * @return the tree, or null if no leaves matched
   */
  @Nullable
  Map<String, Object> getPartialTreeMap(Collection<String> keys, Function<V, Map<String, Object>> leaf2Map);
}
