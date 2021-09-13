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
package io.setl.bc.pychain.validator.merkletree;

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryList;
import io.setl.bc.pychain.validator.ValidationException;
import io.setl.bc.pychain.validator.Validator;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.bc.store.IpfsStore;
import io.setl.utils.ByteUtil;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the full Merkle Tree.
 *
 * @author Valerio Trigari 23/11/2018
 */
public class MerkleTreeValidator implements Validator {

  private static final int MAX_LEAVES = 64;

  private static final Logger logger = LoggerFactory.getLogger(MerkleTreeValidator.class);

  private DBStore dbStore;

  private MessageDigest digest;

  private int levelIdx = 0;

  private int[] nodesIdx;

  private SerialiseToByte serialiser;

  private StateReader stateReader;


  /**
   * MerkleTreeValidator constructor.
   * <p>Instantiate a new MerkleTreeValidator</p>
   *
   * @param dbStore     :
   * @param stateReader :
   * @param digest      :
   * @param serialiser  :
   */
  public MerkleTreeValidator(DBStore dbStore, StateReader stateReader, MessageDigest digest, SerialiseToByte serialiser) {
    this.dbStore = dbStore;
    this.stateReader = stateReader;
    this.digest = digest;
    this.serialiser = serialiser;
  }


  private void compareHashes(byte[] loadedHash, byte[] computedHash, int level, int node) throws MerkleTreeValidationException {
    if (!MessageDigest.isEqual(loadedHash, computedHash)) {
      String message = String.format("Hash value mismatch: loaded %s, computed %s", ByteUtil.bytesToHex(loadedHash), ByteUtil.bytesToHex(computedHash));
      throw new MerkleTreeValidationException(message, level, node);
    }
  }


  private byte[] computeCombinedHash(byte[][] nodeArray) {
    digest.update(nodeArray[0]);
    digest.update(nodeArray.length > 1 ? nodeArray[1] : new byte[0]);
    return digest.digest();
  }


  private void traverseAndValidateMerkleTree(byte[] loadedHash) throws MerkleTreeValidationException {
    Object loadedNodeObj = ((IpfsStore) stateReader).get(new Hash(loadedHash));

    if (loadedNodeObj instanceof byte[][]) {
      // Node
      byte[][] node = (byte[][]) loadedNodeObj;

      byte[] computedHash = computeCombinedHash(node);
      compareHashes(loadedHash, computedHash, levelIdx, nodesIdx[levelIdx]);

      // Left branch
      // Move to the next level
      levelIdx += 1;

      if (levelIdx >= MAX_LEAVES) {
        throw new MerkleTreeValidationException(String.format("Number of leaves exceed maximum value allowed of %d", MAX_LEAVES), levelIdx, nodesIdx[levelIdx]);
      }

      traverseAndValidateMerkleTree(node[0]);
      // Move to the next node on the right, on the same level
      nodesIdx[levelIdx] += 1;

      // Right branch
      if (node.length > 1) {
        traverseAndValidateMerkleTree(node[1]);
        // Move to the next node on the right, on the same level
        nodesIdx[levelIdx] += 1;
        // Go back to the previous level
        levelIdx -= 1;
      }
    } else {
      // Leaf
      byte[] leaf = serialiser.serialise(loadedNodeObj);
      byte[] computedHash = digest.digest(leaf);
      compareHashes(loadedHash, computedHash, levelIdx, nodesIdx[levelIdx]);
    }
  }

  /*
   * Monolithic Merkle Tree Validation
   */


  @Override
  public void validate(int height) throws ValidationException {
    try {
      logger.info("Validate Merkle Trees for height: {}", height);
      AbstractState state = stateReader.readState(dbStore.getStateHash(height));

      logger.info("Validating Sign Nodes");
      validateMerkleTree(state.getSignNodes());

      for(Class<? extends MEntry> type : state.getMerkleTypes()) {
        logger.info("Validating merkle for {}",type);
        validateMerkleTree(state.getMerkle(type));
      }

      logger.info("Validation complete");
    } catch (DBStoreException | NoStateFoundException e) {
      logger.error("Validation failed", e);
      throw new ValidationException();
    }
  }

  /*
   * IPFS Merkle Tree Validation
   */


  private void validateIpfsMerkleTree(Merkle<?> listState) throws MerkleTreeValidationException {
    // Top level
    byte[] loadedHash = listState.getHash().get();

    if (loadedHash == null) {
      return;
    }

    // Other levels
    traverseAndValidateMerkleTree(loadedHash);
  }


  private void validateMerkleTree(Merkle<? extends MEntry> listState) throws ValidationException {
    // Initialize level and node indices
    levelIdx = 0;
    nodesIdx = new int[MAX_LEAVES];

    if (listState instanceof KeyedIndexedEntryList<?>) {
      validateMonolithicMerkleTree(listState);
    } else {
      validateIpfsMerkleTree(listState);
    }
  }


  private void validateMonolithicMerkleTree(Merkle<? extends MEntry> listState) throws MerkleTreeValidationException {
    // Leaves
    Map<Integer, byte[]> leaves = new HashMap<>();

    int index = 0;
    for (MEntry entry : listState) {
      Object[] leafObj = entry.encode(index);
      byte[] serialisedLeaf = serialiser.serialise(leafObj);
      byte[] computedHash = digest.digest(serialisedLeaf);
      leaves.put(index, computedHash);
      index++;
    }
    int nodes = leaves.size();
    if (nodes == 0) {
      return;
    }

    Map<Integer, Map<Integer, byte[]>> merkleTree = new HashMap<>();

    int level = 0;
    merkleTree.put(level++, leaves);

    final int branches = 2;

    // Other levels
    while (nodes > 0) {
      nodes = (nodes + 1) / 2;

      Map<Integer, byte[]> currentLevel = new HashMap<>();

      for (int node = 0; node < nodes; node++) {
        for (int branch = 0; branch < branches; branch++) {
          byte[] currentNode = merkleTree.get(level - 1).get((node * branches) + branch);
          digest.update(currentNode != null ? currentNode : new byte[0]);
        }

        byte[] computedHash = digest.digest();
        currentLevel.put(node, computedHash);
      }

      merkleTree.put(level++, currentLevel);

      if (nodes == 1) {
        break;
      }
    }

    byte[] computedTopHash = merkleTree.get(merkleTree.size() - 1).get(0);
    byte[] loadedTopHash = listState.getHash().get();

    compareHashes(loadedTopHash, computedTopHash, 0, 0);
  }



}
