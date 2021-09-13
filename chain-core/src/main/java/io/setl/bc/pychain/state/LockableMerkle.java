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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import java.util.Collection;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * @author Simon Greatrix on 18/12/2018.
 */
public class LockableMerkle<V> implements SnapshotMerkle<V> {

  protected final SnapshotMerkle<V> instance;

  protected final BooleanSupplier isLocked;


  public LockableMerkle(SnapshotMerkle<V> instance, BooleanSupplier isLocked) {
    this.instance = instance;
    this.isLocked = isLocked;
  }


  protected void checkLocked() {
    if (isLocked.getAsBoolean()) {
      throw new IllegalStateException("Merkle instance is locked against changes.");
    }
  }


  @Override
  public String computeRootHash() {
    return instance.computeRootHash();
  }


  @Override
  @Deprecated
  public Object[][] debugGetChangedObjectArray() {
    return instance.debugGetChangedObjectArray();
  }


  @Override
  @Deprecated
  public MPWrappedArray debugGetWrappedMerkleArray() {
    return instance.debugGetWrappedMerkleArray();
  }


  @Override
  public V find(String key) {
    return instance.find(key);
  }


  @Override
  public Hash getHash() {
    return instance.getHash();
  }


  @Override
  public Class<V> getLeafType() {
    return instance.getLeafType();
  }


  @Nullable
  @Override
  public Map<String, Object> getPartialTreeMap(Collection<String> keys, Function<V, Map<String, Object>> leaf2Map) {
    return instance.getPartialTreeMap(keys, leaf2Map);
  }


  @Override
  public boolean itemExists(String key) {
    return instance.itemExists(key);
  }


  @Override
  public boolean remove(String key) {
    checkLocked();
    return instance.remove(key);
  }


  @Override
  public Stream<V> stream() {
    return instance.stream();
  }


  @Override
  public void update(String key, V object) {
    checkLocked();
    instance.update(key, object);
  }
}
