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
package io.setl.bc.pychain.state.index;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.KeyToIndex;
import io.setl.bc.pychain.state.ipfs.IpfsWalker;
import io.setl.bc.pychain.state.ipfs.MerkleStoreReader;
import java.util.function.Consumer;

/**
 * Maintains the key-to-index mapping in a memory map.
 *
 * @param <T> the value type
 */
public class MemoryKeyToIndex<T> extends DefaultKeyToIndex<T> implements KeyToIndex<T> {

  /**
   * New instance.
   *
   * @param hash the root hash
   * @param ms   reader on the Merkle store containing the reset of the tree
   */
  public MemoryKeyToIndex(Hash hash, MerkleStoreReader<Object> ms) {
    IpfsWalker.walk(hash, ms, new Consumer<Object>() {
      long index = 0;


      @Override
      public void accept(Object t) {
        long i = index++;
        T key = (T) ((Object[]) t)[1];
        map.put(key, i);
      }
    });
  }
}
