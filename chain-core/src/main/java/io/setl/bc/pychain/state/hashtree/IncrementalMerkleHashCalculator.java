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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.IndexedList;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Sha256Hash;
import io.setl.utils.ByteUtil;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculator of existing merkle hash tree using set of changed indexes.
 *
 * @author aanten
 */
public class IncrementalMerkleHashCalculator {

  private static final Logger logger = LoggerFactory.getLogger(IncrementalMerkleHashCalculator.class);


  // For future, non binary implementation
  static double log(int x, int base) {

    return (Math.log(x) / Math.log(base));
  }


  /**
   * Update a Merkle hash tree from the base list and a set of changed indices.
   *
   * @param hashTree              the hash tree
   * @param indexedList           the base list
   * @param hashSerialiser        the serialiser that converts data to a digestible byte stream.
   * @param initialChangedIndices the changed items in the base list
   * @param cc                    change consumer for changed tree nodes
   *
   * @return the root hash
   */
  @SuppressWarnings("squid:S3776")
  public byte[] computeHashesFromChanges(HashTree hashTree, IndexedList<Object[]> indexedList,
      SerialiseToByte hashSerialiser, Set<Long> initialChangedIndices, ChangeConsumer cc) {
    byte[] returnHash = hashTree.getTopHashEntry();

    // Exit if no data set.
    int entryCount = (int) indexedList.getEntryCount();
    if (entryCount == 0) {
      return returnHash;
    }

    // for compatibility with python - always hash 2 or more levels
    int levels = (entryCount == 1 ? 2 : (33 - Integer.numberOfLeadingZeros(entryCount - 1)));
    long itemCount = indexedList.getEntryCount();
    int hashedCount = 0;

    MessageDigest digest = Sha256Hash.newDigest();

    // Start at the base (level zero) where we hash the data items.
    Set<Long> changedIndices = new HashSet<>();
    for (long thisIndex : initialChangedIndices) {

      byte[] bytes;
      Object[] thisEntry;

      if (thisIndex < itemCount) {
        thisEntry = indexedList.getHashableEntry(thisIndex);
        bytes = hashSerialiser.serialise(thisEntry);

        byte[] hash = digest.digest(bytes);

        if (cc != null) {
          cc.accept(thisIndex, new Hash(hash), thisEntry, true);
        }

        hashedCount++;
        hashTree.setHash(0, thisIndex, hash);

        if (logger.isTraceEnabled()) {
          logger.info("Setting hash {},{}={}", 0, thisIndex, ByteUtil.bytesToHex(hash));
        }

      } else {
        hashTree.removeHash(0, thisIndex);
      }

      // Trim this entry.
      changedIndices.add(thisIndex / 2);
    }
    itemCount = ((itemCount + 1) / 2);

    // For each subsequent level
    for (int thisLevel = 1; thisLevel < levels; thisLevel++) {

      Set<Long> nextChanges = new HashSet<>();

      // Above base, hash the hashes.

      for (long thisIndex : changedIndices) {

        if (thisIndex < itemCount) {
          long lowerIndex = thisIndex * 2;

          // Hash concatenated lower hashes (Allows for missing items OK).

          byte[] t0 = hashTree.getHashEntry(thisLevel - 1, lowerIndex++);

          digest.update(t0);

          byte[] t1 = hashTree.getHashEntry(thisLevel - 1, lowerIndex);

          digest.update(t1);

          // Hash previous hashes

          byte[] hash = digest.digest();

          //
          if (cc != null) {
            cc.accept(-1, new Hash(hash), t1.length > 1 ? new byte[][]{t0, t1} : new byte[][]{t0},
                false);
          }
          if (thisLevel == levels - 1) {
            //Last level
            returnHash = hash;
          }

          hashedCount += 2;

          // Set hash in tree
          hashTree.setHash(thisLevel, thisIndex, hash);

          if (logger.isTraceEnabled()) {
            logger.trace("Setting hash {},{}={}", thisLevel, thisIndex, ByteUtil.bytesToHex(hash));
          }

        } else {
          hashTree.removeHash(thisLevel, thisIndex);
          if (thisLevel == levels - 1) {
            //Last level
            returnHash = hashTree.getHashEntry(thisLevel, 0);
            if (logger.isTraceEnabled()) {
              logger.trace("New tophash:{}", ByteUtil.bytesToHex(returnHash));
            }
          }
        }

        // Trim this entry.
        nextChanges.add(thisIndex / 2);
      } // For

      changedIndices = nextChanges;
      itemCount = ((itemCount + 1) / 2);

    }

    // Trim excess layers

    hashTree.trimTree(levels);

    if (logger.isTraceEnabled()) {
      logger.info("Hashed:{}/{}", hashedCount, entryCount);
    }

    return returnHash;
  }

}
