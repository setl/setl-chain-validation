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
package io.setl.bc.pychain.peer;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.common.CommonPy.P2PType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.setl.bc.pychain.p2p.message.Message;

/**
 * Provide messaging service to/from other peers.
 *
 * @author aanten
 */
public interface PeerManager extends TransactionFlowHandler {


  void addListener(BlockChainListener blockChainListener);

  default void addPeer(String name, int port) {
    addPeer(name, port, Collections.emptyMap());
  }

  void addPeer(String name, int port, Map<String, Object> properties);

  void addTransactionListener(TransactionListener l);

  void broadcast(Message message);

  void consensusFinished();

  void consensusStarted();

  List<Object> getActiveConnectionSnapshot();

  void init(List<Integer> chainsId, String uniqueNodeIdentifier, boolean broadcastTransactions);

  default void init(int chainsId, String uniqueNodeIdentifier, boolean broadcastTransactions) {
    init(Arrays.asList(chainsId), uniqueNodeIdentifier, broadcastTransactions);
  }

  default boolean send(PeerAddress addr, Message message) {
    return send(addr, message.encode());
  }

  default boolean doStateBroadcast() {
    return false;
  }

  boolean send(PeerAddress addr, Object[] msg);

  boolean send(PeerAddress addr, byte[] msg);

  void setListenPort(int port);

  void start();

  void stop();

  default void persistBlock(Block block, int chainId, int height, boolean iDidPropose) {
    // Default is do nothing. Kafka will do something.
    return;
  }

  default boolean getBlock(int chainId, int fromHeight) {
    // Default is do nothing. Kafka might care.
    return false;
  }

  default boolean hasBlockFetcher() {
    // This should indicate the difference between classic network Block requests, with possibly inconsistent response and latency (false)
    // with a more reliable (hopefully) Block Fetcher (Kafka topic based, for example) implementation.
    // The main difference is that the Nodes (e.g. Wallet node Block Provider) tend to request batches of blocks from the network when using
    // the 'classic' approach, but single blocks when using the 'BlockFetcher' approach.
    // This property is intended to make it easier to tell which approach is implemented in the Peer Manager.
    //
    // Default is no. Kafka might do.
    return false;
  }

}
