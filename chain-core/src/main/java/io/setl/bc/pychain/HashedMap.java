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
package io.setl.bc.pychain;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.util.CopyOnWriteMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Simon Greatrix on 18/12/2018.
 */
public class HashedMap<K extends Comparable<? super K>, V> extends CopyOnWriteMap<K, V> {

  protected Hash hash = null;

  protected int hashType = 0;


  public HashedMap() {
    super(new TreeMap<>(), null);
  }


  public HashedMap(Map<K, V> initialValues) {
    super(new TreeMap<>(initialValues), null);
  }


  /**
   * New instance which is a copy of another.
   *
   * @param original the original instance
   */
  public HashedMap(HashedMap<K,V> original) {
    super(original);
    hash = original.hash;
    hashType = original.hashType;
  }


  @Override
  protected Map<K, V> createNewMap(int suggestedSize) {
    hash = null;
    return new TreeMap<>();
  }


  /**
   * Get the hash of this map.
   *
   * @return the hash
   */
  public Hash getHash(int type) {
    if (hash == null || hashType != type) {
      Digest digest = Digest.create(type);
      hash = digest.digest(toBytes());
      hashType = type;
    }
    return hash;
  }


  protected byte[] toBytes() {
    try {
      return MsgPack.writer().writeValueAsBytes(this);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize", e);
    }
  }
}

