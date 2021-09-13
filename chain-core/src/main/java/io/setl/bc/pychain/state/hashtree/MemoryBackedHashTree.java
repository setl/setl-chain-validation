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
package io.setl.bc.pychain.state.hashtree;

import io.setl.utils.ByteUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryBackedHashTree implements HashTree {

  private static final byte[] emptyArray = new byte[0];

  private static final Logger logger = LoggerFactory.getLogger(MemoryBackedHashTree.class);

  private Map<Integer, Map<Long, byte[]>> hashTree;


  public MemoryBackedHashTree() {

    hashTree = new HashMap<>();
  }


  /**
   * Debug method for testing. Verifies that the hashes of the entries match expectations.
   *
   * @param compHashes expected hashes of entries
   */
  public void debugCompareFull(byte[][][] compHashes) {
    if (logger.isInfoEnabled()) {
      for (int i = 0; i < compHashes.length; i++) {
        byte[][] cc = compHashes[i];
        for (int j = 0; j < cc.length; j++) {
          if (!Arrays.equals(cc[j], getHashEntry(i, j))) {
            logger.info("ERROR:level={}, entry={}, full:{}!=delta:{}", i, j,
                ByteUtil.bytesToHex(cc[j]), ByteUtil.bytesToHex(getHashEntry(i, j)));
          }
        }
      }
    }
  }


  @Override
  public byte[] getHashEntry(int level, long index) {

    Map<Long, byte[]> thisLayer;

    if ((thisLayer = hashTree.get(level)) == null) {
      return emptyArray;
    }
    byte[] rVal = thisLayer.get(index);

    return rVal == null ? emptyArray : rVal;
  }


  @Override
  public byte[] getTopHashEntry() {
    if (hashTree.isEmpty()) {
      return null;
    }
    return getHashEntry(hashTree.size() - 1, 0);
  }


  @Override
  public void removeHash(int level, long entry) {

    Map<Long, byte[]> thisLayer;

    if ((thisLayer = hashTree.get(level)) != null) {
      thisLayer.remove(entry);
    }
  }


  @Override
  public void setHash(int level, long index, byte[] hash) {
    Map<Long, byte[]> hashLayer = hashTree.computeIfAbsent(level, k -> new HashMap<>());
    hashLayer.put(index, hash);
  }


  @Override
  public void trimTree(long fromIndex) {

    int treeHeight = hashTree.size() - 1;

    while (treeHeight >= fromIndex) {
      hashTree.remove(treeHeight);
      treeHeight--;
    }

  }
  
}
