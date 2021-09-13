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
import io.setl.common.Hex;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapBackedMerkleStore<T> implements MerkleStore<T> {

  private static final Logger logger = LoggerFactory.getLogger(MapBackedMerkleStore.class);

  final Map<Hash, T> theMap;


  public MapBackedMerkleStore() {
    this(new HashMap<>());
  }


  public MapBackedMerkleStore(Map<Hash, T> theMap) {
    this.theMap = theMap;
  }


  /**
   * Copy all the entries in this store to another store.
   *
   * @param destination the destination store.
   */
  public void copyTo(MerkleStore<T> destination) {
    for (Entry<Hash, T> entry : theMap.entrySet()) {
      destination.put(entry.getKey(), entry.getValue());
    }
  }


  @Override
  public T get(@Nonnull Hash hash) {
    T r = theMap.get(hash);
    if (r == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Not found:{}", Hex.encode(hash.get()));
      }
    }
    return r;
  }


  @Override
  public void put(@Nonnull Hash hash, T data) {
    theMap.put(hash, data);
    if (logger.isDebugEnabled()) {
      logger.debug("Put:{} -> {}", Hex.encode(hash.get()), data);
    }
  }
}
