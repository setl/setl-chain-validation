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

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.p2p.message.StateResponse;
import io.setl.bc.pychain.peer.PeerAddress;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Provide a source of blocks from the blockchain. Blocks are delivered sequentially from a given height(sequential block index) asynchronously.
 */
public interface BlockProvider {

  /**
   * Start listening for new blocks. Historic blocks will not be requested.
   *
   * @param listener the listener
   */
  void addBlockListener(Consumer<Block> listener);

  /**
   * Start listening for blocks on or after startBlock. Where blockchain height is {@literal >=} startBlock, blocks startBlock to blockchainheight-1 will be
   * sent
   * immediately.
   *
   * @param startBlock the earliest block the listener is interested in
   * @param listener   the listener
   */
  void addBlockListener(int startBlock, Consumer<Block> listener);

  /**
   * Start listening for blocks on or after startBlock until endBlock. Where blockchain height is {@literal >=} startBlock, blocks startBlock to
   * blockchainheight-1
   * will
   * be sent immediately.
   *
   * @param startBlock the earliest block the listener is interested in
   * @param endBlock   the last block the listener is interested in
   * @param listener   the listener
   */
  void addBlockListener(int startBlock, int endBlock, Consumer<Block> listener);

  /**
   * Listen for block events.
   *
   * @param listener the listener
   */
  void addEventListener(Consumer<BlockEvent> listener);

  /**
   * Start listening for state response messages. It's used to observe blockchain height.
   *
   * @param listener the listener
   */
  void addStateListener(BiConsumer<StateResponse, PeerAddress> listener);

  /**
   * Get the current chain height. This requires the server to receive a reply to a
   * state request message.
   *
   * @return the current height.
   */
  int currentHeight();

  /**
   * Remove a block listener.
   *
   * @param listener the listener to remove
   */
  void removeBlockListener(Consumer<Block> listener);

  /**
   * Remove an event listener.
   *
   * @param listener the listener to remove
   */
  void removeEventListener(Consumer<BlockEvent> listener);

  /**
   * Remove an state listener.
   *
   * @param listener the listener to remove
   */
  void removeStateListener(BiConsumer<StateResponse, PeerAddress> listener);

  /**
   * Request a specific block.
   *
   * @param height   the specific height of the block
   * @param listener a listener that will be informed of the block (May be null)
   */
  void requestBlock(int height, @Nullable Consumer<Block> listener);

  /**
   * Start the block provider.
   */
  void start();

  /**
   * Stop the block provider. No further blocks will be delivered, and all listeners will be disconnected/released.
   */
  void stop();
}
