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
package io.setl.bc.pychain.node;

import static io.setl.bc.logging.LoggingConstants.MARKER_CONSENSUS;
import static io.setl.bc.logging.LoggingConstants.MARKER_STORAGE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.BlockWriter;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.BlockRequest;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.validator.ValidatorHandler;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;

/**
 * BlockManager
 */
@Component
public class BlockManager {

  private static final Logger logger = LoggerFactory.getLogger(BlockManager.class);

  private final NodeConfiguration config;
  private final BlockReader blockReader;
  private final BlockWriter blockWriter;
  private final StateManager stateManager;
  private final PeerManager peerManager;
  private final DBStore dbStore;
  private final ValidatorHandler validatorHandler;
  private final PriorityExecutor priorityExecutor;
  private final MsgFactory msgFactory;

  private Block lastBlock = null;
  private final Object lastBlockLock = new Object();


  /**
   * Constructor.
   *
   * @param config           the config.
   * @param blockReader      the block reader.
   * @param blockWriter      the block writer.
   * @param stateManager     the state manager.
   * @param peerManager      the peer manager.
   * @param dbStore          the db store.
   * @param validatorHandler the validator handler.
   * @param priorityExecutor the priority executor.
   * @param msgFactory       the message factory.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public BlockManager(
      NodeConfiguration config,
      BlockReader blockReader,
      BlockWriter blockWriter,
      StateManager stateManager,
      PeerManager peerManager,
      DBStore dbStore,
      ValidatorHandler validatorHandler,
      PriorityExecutor priorityExecutor,
      MsgFactory msgFactory
  ) {
    this.config = config;
    this.blockReader = blockReader;
    this.blockWriter = blockWriter;
    this.stateManager = stateManager;
    this.peerManager = peerManager;
    this.dbStore = dbStore;
    this.validatorHandler = validatorHandler;
    this.priorityExecutor = priorityExecutor;
    this.msgFactory = msgFactory;
  }


  /**
   * Persists block asynchronously.
   *
   * @param block     the block to persist.
   * @param blockHash the block hash.
   */
  public void persistBlockAsync(Block block, Hash blockHash) {
    synchronized (lastBlockLock) {
      // Cache the block as the last block
      lastBlock = block;
    }
    priorityExecutor.submit(Priorities.BLOCK_WRITER, () -> {
      logger.info(MARKER_CONSENSUS, "  commit,Persisting block:{}", blockHash);
      persistBlock(block, blockHash);
      logger.info(MARKER_CONSENSUS, "  commit,Block persisted:{}", blockHash);
    });
  }


  /**
   * Persists block.
   *
   * @param block     the block to persist.
   * @param blockHash the block hash.
   */
  public void persistBlock(Block block, Hash blockHash) {
    try {
      // persist to store.
      dbStore.setBlockHash(block.getHeight(), blockHash.toHexString(), block.getTimeStamp());
      blockWriter.writeBlock(block, blockHash);
      validatorHandler.setHeight(block.getHeight());
    } catch (Exception e) {
      logger.error(MARKER_STORAGE, "Could not persist block", e);
    }
  }


  /**
   * Handles the request for a block.
   *
   * @param blockRequest the block request.
   * @param peerAddress  the peer address.
   */
  public void handleBlockRequest(BlockRequest blockRequest, PeerAddress peerAddress) {

    if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
      logger.info(MARKER_CONSENSUS, "Block request {} {}", blockRequest.getFirstHeight(), blockRequest.getAdditionalBlockCount());
    }

    // Check that we are on the right chain id
    if (blockRequest.getChainId() != config.getChainId()) {
      return;
    }

    final StateDetail csd = stateManager.getCurrentStateDetail();
    final int firstHeight = blockRequest.getFirstHeight();
    if (firstHeight < 0 || firstHeight > csd.getHeight()) {
      return;
    }

    for (int i = 0, l = 1 + blockRequest.getAdditionalBlockCount(); i < l; i++) {
      try {
        int rBlock = firstHeight + i;

        logger.info(MARKER_CONSENSUS, "Loading block: {}", rBlock);
        Block block = null;
        synchronized (lastBlockLock) {
          // If the cached last block is the block that is being requested use it
          if (lastBlock != null && lastBlock.getHeight() == rBlock) {
            block = lastBlock;
          }
        }

        if (block == null) {
          // If the block isn't the last block and we have gone past the height drop out
          if (rBlock >= csd.getHeight()) {
            return;
          }

          block = blockReader.readBlock(stateManager.getBlockHash(rBlock));
        }

        logger.info(MARKER_CONSENSUS, "Sending block: {}", rBlock);
        peerManager.send(peerAddress, msgFactory.blockFinalized(block));
      } catch (Exception e) {
        logger.error(MARKER_CONSENSUS, "blockRequest:", e);
      }
    }
  }

}
