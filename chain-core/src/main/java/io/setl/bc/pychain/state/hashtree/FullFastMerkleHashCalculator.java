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

import io.setl.bc.pychain.state.IndexedList;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Sha256Hash;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Full in-memory merkle hashtree calculator. Complete hashtree is calculated upon each invocation.
 *
 * @author aanten
 */
public class FullFastMerkleHashCalculator {

  private static final byte[] emptyArray = new byte[0];

  private static final Logger logger = LoggerFactory.getLogger(FullFastMerkleHashCalculator.class);


  /**
   * Compute a hash tree.
   *
   * @param ke             the base list containing the tree's leaves
   * @param hashSerialiser the serialiser to convert data into a digestible form
   *
   * @return the hashes of the tree nodes, by level and index.
   */
  public byte[][][] computeHashTreeFast(IndexedList<Object[]> ke, SerialiseToByte hashSerialiser) {
    MessageDigest digest = Sha256Hash.newDigest();

    int l = (int) ke.getEntryCount();
    if (l == 0) {
      return null;
    }
    // for compatibility with python - always hash 2 or more levels
    int levels = l == 1 ? 2 : (33 - Integer.numberOfLeadingZeros(l - 1));

    byte[][][] newHashList = new byte[levels][][];

    int hashCount = l;
    for (int i = 0; i < levels; i++) {
      newHashList[i] = new byte[hashCount][];
      if (i == 0) {
        for (int j = 0; j < hashCount; j++) {
          try {
            Object[] he = ke.getHashableEntry(j);
            byte[] bytes = hashSerialiser.serialise(he);
            byte[] hash = digest.digest(bytes);
            newHashList[i][j] = hash;
            digest.reset();
          } catch (Exception e) {
            logger.error("compHashes exception", e);
          }
        }
      } else {
        byte[][] prev = newHashList[i - 1];
        for (int j = 0, ii = 0; j < hashCount; j++) {
          digest.update(prev[ii++]);
          if (ii < prev.length) {
            digest.update(prev[ii++]);
          } else {
            digest.update(emptyArray);
          }
          // Hash previous hashes
          newHashList[i][j] = digest.digest();
        }
      }
      hashCount = hashCount / 2 + (hashCount % 2);
    }
    return newHashList;
  }
}
