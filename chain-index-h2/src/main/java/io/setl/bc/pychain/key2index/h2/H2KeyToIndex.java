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
package io.setl.bc.pychain.key2index.h2;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.KeyToIndex;
import io.setl.bc.pychain.state.ipfs.IpfsConstants;
import io.setl.bc.pychain.state.ipfs.IpfsWalker;
import io.setl.bc.pychain.state.ipfs.MerkleStoreReader;
import java.util.function.Consumer;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * H2 Bdb (MVStore) based implementation. Uses two separate maps, map  - used only to store the root hash of the current mapI mapI - the map of key to index
 *
 * <p>Upon construction, the current hash is checked, if not that requested, the entire index is rebuilt.
 *
 * @author aanten
 */
public class H2KeyToIndex<K> implements KeyToIndex<K> {

  private static final Logger logger = LoggerFactory.getLogger(H2KeyToIndex.class);

  private static final String FILE_NAME = IpfsConstants.BDBKEYTOINDEX_FILENAME;
  private static final MVStore store = MVStore.open(FILE_NAME);
  private static final Integer ACTIVEHASH = -1;

  private MVMap<K, Long> mapI = null;


  /**
   * Construct and rebuild as necessary, the key to index mapping for the given name and hash.
   *
   * @param name : The unique name of this map. Usually classname of List, eg "AssetBalanceIpfs"
   * @param hash : The required root hash
   * @param ms : The source store used to populate if necessary.
   */
  public H2KeyToIndex(String name,
      Hash hash,
      MerkleStoreReader<Object> ms) {
    create(name, hash, ms);
  }


  /**
   * Actually implement the constructor.
   *
   * @param name : The identifying name
   * @param hash : The hash
   * @param ms : The Merkle Store
   */
  private void create(String name,
      Hash hash,
      MerkleStoreReader<Object> ms) {

    MVMap<Integer, Object> map = store.openMap(name);
    mapI = store.openMap(name + "_I");

    if (!hash.isNull()) {

      Object ah = map.get(ACTIVEHASH);

      if (ah != null) {

        if (ah.equals(hash)) {
          logger.info("{} State up to date", name);
          return;

        } else {
          logger.info("{} Current state {} is not requested state {}", name, ah, hash);
          mapI.clear();
          map.clear();
        }
      }

      //  add a key+index pair to the mapI
      //
      IpfsWalker.walk(hash, ms, new Consumer<Object>() {
        long index = 0;

        @Override
        public void accept(Object obj) {
          long i = index++;

          K key = (K) ((Object[]) obj)[1];

          mapI.put(key, i);
        }

      });

    } else {
      mapI.clear();
      map.clear();
    }

    // Record the hash of active data
    map.put(ACTIVEHASH, hash);

    // Flush changes
    store.commit();
  }


  /**
   * Report the mapI key, or -1, as a long.
   *
   * @param key : The key of the item sought
   * @return : The value which matches the key
   */
  @Override
  public long find(K key) {
    Long retVal = mapI.get(key);

    if (retVal == null) {
      return -1;
    }

    return retVal;
  }


  /**
   * Insert a key+value (index) pair into the mapI.
   *
   * @param key : The key to be associated with the value
   * @param value : The value to be associated with the key
   */
  @Override
  public void put(K key,long value) {
    mapI.put(key, value);
  }


  /**
   * Report the number of items in the current mapI.
   *
   * @return : the number of items in the mapI, expressed as a long
   */
  public long getEntryCount() {

    return mapI.sizeAsLong();
  }

  @Override
  public void remove(K key) {
    mapI.remove(key);
  }

}
