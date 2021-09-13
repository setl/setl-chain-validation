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
package io.setl.bc.pychain.state.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.HashStore;

/**
 * An in-memory hash store for holding state during testing.
 *
 * @author Simon Greatrix on 27/12/2019.
 */
public class MemoryHashStore implements HashStore {

  ConcurrentHashMap<Hash, byte[]> store = new ConcurrentHashMap<>();


  @Override
  public Stream<byte[]> contents() {
    return store.values().stream();
  }


  @Override
  public void flush(AbstractState state) {
    // do nothing
  }


  @Override
  public byte[] get(Hash key) {
    return store.get(key);
  }


  @Override
  public Hash insert(byte[] data) {
    Digest digest = Digest.create(Digest.TYPE_SHA_512_256);
    Hash hash = digest.digest(data);
    store.putIfAbsent(hash, data);
    return hash;
  }


  @Override
  public void shutdown() throws DBStoreException {
    store.clear();
  }

}
