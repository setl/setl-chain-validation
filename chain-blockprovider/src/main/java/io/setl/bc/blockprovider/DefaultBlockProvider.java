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
package io.setl.bc.blockprovider;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.BlockVerifier;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.BlockCommitted;
import io.setl.bc.pychain.p2p.message.BlockFinalized;
import io.setl.bc.pychain.p2p.message.EmptyProposal;
import io.setl.bc.pychain.p2p.message.Message;
import io.setl.bc.pychain.p2p.message.StateRequest;
import io.setl.bc.pychain.p2p.message.StateResponse;
import io.setl.bc.pychain.p2p.message.UnknownMessage;
import io.setl.bc.pychain.peer.BlockChainListener;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.common.CommonPy.P2PType;
import io.setl.util.TimeBasedUuid;

/**
 * Default implementation of a {@link BlockProvider}.
 */
@Component
public class DefaultBlockProvider implements BlockChainListener, BlockProvider, InitializingBean, DisposableBean {

  /** We wait this many milliseconds for a block to turn up before re-requesting. */
  private static final int BLOCK_REQUEST_TIMEOUT = 4500;

  private static final Logger logger = LoggerFactory.getLogger(DefaultBlockProvider.class);

  /** Listeners for blocks. */
  private final List<BlockListener> blockListeners = new CopyOnWriteArrayList<>();

  /** The ID of the block chain. */
  private final int chainId;

  /** Listeners for events. */
  private final List<Consumer<BlockEvent>> eventListeners = new CopyOnWriteArrayList<>();

  /**
   * The maximum number of blocks we will wait for simultaneously.
   * For classic mesh network, this will be 10, for Kafka-Style, this will be 1.
   * The reason being that it makes the block fetch / catchup process very much faster without
   * having to change much of the existing code. (Set in the Constructor)
   */
  private final int maxPendingBlocks;

  /** Message factory for creating block request messages. */
  private final MsgFactory msgFactory = new MsgFactory();

  /** Peer manager for sending and receiving messages. */
  private final PeerManager peerManager;

  /**
   * The blocks this has requested from the peers and not yet received. Only blocks that were at or below the network height at the time of request will be
   * considered pending.
   */
  private final SortedMap<Integer, Long> pendingBlocks = Collections.synchronizedSortedMap(new TreeMap<>());

  /** Synchronization lock. */
  private final Object stateHeightLock = new Object();

  /** Listeners for state events. */
  private final List<BiConsumer<StateResponse, PeerAddress>> stateListeners = new CopyOnWriteArrayList<>();

  /** Clock for tracking expiry of requests. */
  @VisibleForTesting
  Clock systemClock = Clock.systemUTC();

  /** Was the "start" method used to start the peer manager?. */
  private boolean didStartPeerManager = false;

  /** If true, at least one of the listeners does not know the first block it is will be interested in. */
  private boolean hasUnsetListeners = false;

  /** The node we were last contacted by. This is the node we request blocks from. */
  @SuppressWarnings("squid:S3077") // 'Non-primitive fields should not be "volatile' : Simon G has verified this use of 'volatile'.
  private volatile PeerAddress lastContact;

  /** Network height. Set to -1 until known. */
  private volatile int stateHeight = -1;


  /**
   * New instance.
   *
   * @param peerManager the peer manager
   * @param chainId     the chain this provides blocks for
   */
  @Autowired
  public DefaultBlockProvider(PeerManager peerManager, @Value("${chainid}") int chainId) {
    this.peerManager = peerManager;
    this.chainId = chainId;

    // This is important as, at the moment, Only one block at a time is requested from the Kafka Block Fetcher
    // We could request more but, given it is pretty fast anyway, this helps to avoid the Block consumers being given blocks too quickly.
    if (peerManager.hasBlockFetcher()) {
      maxPendingBlocks = 1;
    } else {
      maxPendingBlocks = 10;
    }
  }


  @Override
  public void addBlockListener(Consumer<Block> listener) {
    addBlockListener(-1, Integer.MAX_VALUE, listener);
  }


  @Override
  public void addBlockListener(int startBlock, Consumer<Block> listener) {
    addBlockListener(startBlock, Integer.MAX_VALUE, listener);
  }


  @Override
  public void addBlockListener(int startBlock, int endBlock, Consumer<Block> listener) {
    // negative start block means not interested in history
    if (startBlock <= -1) {
      // start from the state height.
      synchronized (stateHeightLock) {
        startBlock = stateHeight;
      }
    }
    if (endBlock < startBlock) {
      return;
    }

    // Trigger requires the listeners in order, so we sync to avoid a listener being added whilst it is running.
    synchronized (blockListeners) {
      blockListeners.add(new BlockListener(startBlock, endBlock, listener));
      trigger();
    }

    if (startBlock == -1) {
      synchronized (stateHeightLock) {
        hasUnsetListeners = true;
      }
    }
  }


  @Override
  public void addEventListener(Consumer<BlockEvent> listener) {
    eventListeners.add(listener);
  }


  @Override
  public void addStateListener(BiConsumer<StateResponse, PeerAddress> listener) {
    stateListeners.add(listener);
  }


  @Override
  public void afterPropertiesSet() {
    peerManager.addListener(this);

    // We need the peer status to start requesting blocks, so we immediately request it.
    logger.debug("Requesting peer status");
    peerManager.broadcast(new StateRequest(chainId));
  }


  @Override
  public int currentHeight() {
    return stateHeight;
  }


  @Override
  public void destroy() {
    stop();
  }


  @Override
  public void eventReceived(PeerAddress peerAddress, Message eventMessage) {
    try {
      P2PType messageType = eventMessage.getType();
      if (messageType == P2PType.UNKNOWN) {
        logger.error("Received unknown message type {}", ((UnknownMessage) eventMessage).getTypeId());
        return;
      }

      switch (messageType) {
        case PROPOSAL:
          break;
        case BLOCK_FINALIZED:
          lastContact = peerAddress;
          handleBlockFinalized((BlockFinalized) eventMessage);
          break;
        case EMPTY_PROPOSAL:
          lastContact = peerAddress;
          handleEmptyProposal((EmptyProposal) eventMessage);
          break;
        case BLOCK_COMMITTED:
          lastContact = peerAddress;
          handleBlockCommitted((BlockCommitted) eventMessage);
          break;
        case STATE_REQUEST:
          // Respond to state request with current state
          break;
        case STATE_RESPONSE:
          lastContact = peerAddress;
          handleStateResponse(peerAddress, (StateResponse) eventMessage);
          break;
        default:
          if (logger.isDebugEnabled()) {
            logger.debug("{}:Ignored", messageType.name());
          }
          break;
      }

    } catch (Exception e) {
      logger.error("Error in eventReceived:handleBlockchainEvent", e);
    }
  }


  public List<BlockListener> getBlockListeners() {
    return List.copyOf(blockListeners);
  }


  public List<Consumer<BlockEvent>> getEventListeners() {
    return List.copyOf(eventListeners);
  }


  public PeerAddress getLastContact() {
    return lastContact;
  }


  public Map<Integer, Long> getPendingBlocks() {
    return Map.copyOf(pendingBlocks);
  }


  public List<BiConsumer<StateResponse, PeerAddress>> getStateListeners() {
    return List.copyOf(stateListeners);
  }


  private void handleBlockCommitted(BlockCommitted eventMessage) {
    int blockHeight = eventMessage.getHeight();
    logger.debug("Received \"Block Committed\" at Block Height {}", blockHeight);
    synchronized (stateHeightLock) {
      // Note State Height is block height + 1
      stateHeight = blockHeight + 1;
    }
    trigger();
    heartbeat(stateHeight, false);
  }


  private synchronized void handleBlockFinalized(BlockFinalized eventMessage) {
    // Result of block request
    Block block = eventMessage.getBlock();
    logger.debug("Received \"Block Finalized\" height {}.", block.getHeight());
    // Calculate the block hash
    try {
      String blockHash = new BlockVerifier().computeHashAsHex(block);
      block.setBlockHash(Hash.fromHex(blockHash));
      logger.debug("Finalized block has hash:{}", blockHash);
    } catch (Exception e) {
      logger.error("Error when verifying block:", e);
      return;
    }

    // Block is good so we accept it
    pendingBlocks.remove(block.getHeight());

    logger.debug("Forwarding block {} ({}) to listeners", block.getHeight(), block.getBlockHash());
    blockListeners.forEach(l -> l.accept(block));
    blockListeners.removeIf(BlockListener::isFinished);

    // <= so that it still triggers if pendingBlocks.size() == 0 && MAX_PENDING_BLOCKS == 1
    if (pendingBlocks.size() <= maxPendingBlocks / 2) {
      trigger();
    }
  }


  private void handleEmptyProposal(EmptyProposal eventMessage) {
    int messageHeight = eventMessage.getHeight();
    logger.debug("Received \"Empty Proposal\" at height {}", messageHeight);
    if (!pendingBlocks.isEmpty()) {
      trigger();
    }
    heartbeat(messageHeight, true);
  }


  @SuppressWarnings("squid:S1172") // 'Unused method parameters' : No, I like it here. (NPP)
  private void handleStateResponse(PeerAddress source, StateResponse message) {
    synchronized (stateHeightLock) {
      stateHeight = message.getHeight();
      if (hasUnsetListeners) {
        // state X is created from state X-1 and block X-1, so the next block is block X.
        blockListeners.forEach(bl -> bl.startAt(stateHeight));
        hasUnsetListeners = false;
      }
      stateHeightLock.notifyAll();
    }
    logger.debug("Received \"State Response\" at height {}", stateHeight);
    stateListeners.forEach(l -> l.accept(message, source));
    trigger();
  }


  private void heartbeat(int height, boolean isEmpty) {
    BlockEvent blockEvent = new BlockEvent(chainId, height, isEmpty);
    eventListeners.forEach(c -> c.accept(blockEvent));
  }


  public boolean isHasUnsetListeners() {
    return hasUnsetListeners;
  }


  @Override
  public void removeBlockListener(Consumer<Block> listener) {
    blockListeners.removeIf(l -> l.consumer.equals(listener));
  }


  @Override
  public void removeEventListener(Consumer<BlockEvent> listener) {
    eventListeners.remove(listener);
  }


  @Override
  public void removeStateListener(BiConsumer<StateResponse, PeerAddress> listener) {
    stateListeners.remove(listener);
  }


  @Override
  public void requestBlock(int height, Consumer<Block> listener) {
    if (listener == null) {
      listener = a -> {
        // do nothing
      };
    }
    addBlockListener(height, height, listener);
  }


  @Override
  public void start() {
    logger.info("Start called");

    String nodeName = TimeBasedUuid.create().toString();
    peerManager.init(chainId, nodeName, false);
    peerManager.start();
    didStartPeerManager = true;
  }


  @Override
  public void stop() {
    logger.info("Stop called");
    blockListeners.clear();
    eventListeners.clear();

    // Peer manager does not implement a "removeListener" method, so we can't call that. However, we will stop it if we started it.
    if (didStartPeerManager) {
      peerManager.stop();
    }
  }


  /**
   * An event has happened that may trigger the requesting of a new block. We want the blocks in order, so the oldest must be requested first. We also want to
   * limit the number of concurrent requests and retry requests if they are not answered in a reasonable time frame.
   */
  private void trigger() {
    // What is the newest (highest) block agreed by the validation network?
    int blockHeight;
    synchronized (stateHeightLock) {
      if (stateHeight < 1) {
        logger.debug("Ignoring block request trigger event because network height is still unknown or zero.");
        return;
      }
      // State X is combined with block X to produce state X+1. Hence if the state is at Y, the last block is Y-1.
      blockHeight = stateHeight - 1;
    }

    logger.debug("Block request triggered for block height:{}", blockHeight);

    // What time are we making requests
    long now = systemClock.millis();

    // Requests made prior to this time have expired.
    long expired = now - BLOCK_REQUEST_TIMEOUT;

    synchronized (blockListeners) {
      // check for fast exit conditions: no listeners, or already have the maximum number of blocks requested.
      if (blockListeners.isEmpty()) {
        logger.debug("No current active listeners");
        return;
      }

      // Once pendingBlocks.size() >= MAX_PENDING_BLOCKS, we will never re-request blocks if none arrive.
      // Must flush pendingBlocks of expired requests BEFORE this check.

      pendingBlocks.values().removeIf(t -> t < expired);

      // Still too many : exit...

      if (pendingBlocks.size() >= maxPendingBlocks) {
        logger.debug("Maximum number of pending blocks reached. No more requested.");
        return;
      }

      // abandon expired requests
      pendingBlocks.values().removeIf(t -> t < expired);

      TreeSet<Integer> interestingBlocks = triggerFindInterestingBlocks(blockHeight);

      List<int[]> requestRanges = triggerBuildRanges(interestingBlocks);

      // make the requests
      for (int[] range : requestRanges) {
        int start = range[0];
        int additional = range[1] - start;
        logger.debug("Requesting blocks from {}, additional {}", start, additional);
        peerManager.send(lastContact, msgFactory.blockRequest(chainId, start, additional));
        for (int h = range[0]; h <= range[1]; h++) {
          pendingBlocks.put(h, now);
        }
      }
    }
  }


  private List<int[]> triggerBuildRanges(TreeSet<Integer> requestedBlocks) {
    // Convert the individual heights to block ranges.
    List<int[]> ranges = new ArrayList<>(requestedBlocks.size());

    // start with a range that definitely cannot be merged.
    int[] range = {-2, -2};

    // loop over all the individual heights
    for (int request : requestedBlocks) {
      if (range[1] + 1 == request) {
        // extend current range if we can
        range[1] = request;
      } else {
        // start new range
        range = new int[]{request, request};
        ranges.add(range);
      }
    }

    return ranges;
  }


  /** Construct a set of interesting blocks to request. */
  private TreeSet<Integer> triggerFindInterestingBlocks(int blockHeight) {
    synchronized (blockListeners) {
      int maxCanRequest = maxPendingBlocks - pendingBlocks.size();
      TreeSet<Integer> requestedBlocks = new TreeSet<>();
      int height = -1;
      while (maxCanRequest > 0) {
        // find the lowest numbered block the listeners are interested in.
        final int testHeight = height + 1;
        height = blockListeners.stream().mapToInt(bl -> bl.nextInteresting(testHeight)).min().orElse(Integer.MAX_VALUE);
        if (height > blockHeight) {
          // cannot request this yet
          break;
        }

        // If not currently pending, we will request the block.
        if (!pendingBlocks.containsKey(height)) {
          requestedBlocks.add(height);
          maxCanRequest--;
        }
      }
      return requestedBlocks;
    }
  }

}
