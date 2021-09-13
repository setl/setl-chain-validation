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
package io.setl.bc.pychain.state.yaml;

import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.SnapshotMerkle;
import io.setl.bc.pychain.state.entry.MEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * @author Simon Greatrix on 13/02/2018.
 */
public abstract class YamlBaseList<V extends MEntry> implements SnapshotMerkle<V> {

  private HashMap<String, V> map = new HashMap<>();


  @Override
  public String computeRootHash() {
    // Not implemented
    return "--hash--";
  }


  /**
   * Unsupported.
   *
   * @return nothing
   *
   * @deprecated do not use
   */
  @Deprecated
  @Override
  public Object[][] debugGetChangedObjectArray() {
    throw new NotImplementedException();
  }


  /**
   * Unsupported.
   *
   * @return nothing
   *
   * @deprecated do not use
   */
  @Deprecated
  @Override
  public MPWrappedArray debugGetWrappedMerkleArray() {
    throw new NotImplementedException();
  }


  @Override
  public V find(String key) {
    return map.get(key);
  }


  @Override
  public Hash getHash() {
    throw new NotImplementedException();
  }


  @Nullable
  @Override
  public Map<String, Object> getPartialTreeMap(Collection<String> keys, Function<V, Map<String, Object>> leaf2Map) {
    throw new NotImplementedException();
  }


  @Override
  public boolean itemExists(String key) {
    return map.containsKey(key);
  }


  @Override
  public boolean remove(String key) {
    return map.remove(key) != null;
  }


  @Override
  public Stream<V> stream() {
    return map.values().stream();
  }


  @Override
  public void update(String key, V object) {
    map.put(key, object);
  }

}
