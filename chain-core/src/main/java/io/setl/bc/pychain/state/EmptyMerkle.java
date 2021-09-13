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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;

/**
 * An empty merkle. This is returned from State when no Merkle for a given leaf type has been created.
 *
 * @author Simon Greatrix on 24/03/2020.
 */
public class EmptyMerkle<V> implements Merkle<V> {

  private final Class<V> leafType;


  public EmptyMerkle(Class<V> leafType) {
    this.leafType = leafType;
  }


  @Override
  public String computeRootHash() {
    return null;
  }


  @Override
  public Object[][] debugGetChangedObjectArray() {
    throw new UnsupportedOperationException();
  }


  @Override
  public MPWrappedArray debugGetWrappedMerkleArray() {
    throw new UnsupportedOperationException();
  }


  @Override
  public V find(String key) {
    return null;
  }


  @Override
  public Hash getHash() {
    return Hash.NULL_HASH;
  }


  @Override
  public Class<V> getLeafType() {
    return leafType;
  }


  @Nullable
  @Override
  public Map<String, Object> getPartialTreeMap(
      Collection<String> keys, Function<V, Map<String, Object>> leaf2Map
  ) {
    return Collections.emptyMap();
  }


  @Override
  public boolean itemExists(String key) {
    return false;
  }


  @Override
  public Stream<V> stream() {
    return Stream.empty();
  }

}
