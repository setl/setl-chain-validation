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
package io.setl.bc.pychain.peer.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.setl.bc.pychain.p2p.message.Message;
import io.setl.bc.pychain.peer.BlockChainListener;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.peer.TransactionListener;

/**
 * @author Simon Greatrix on 2019-04-02.
 */
public class BasicPeerManager implements PeerManager {

  private final List<BlockChainListener> blockChainListeners = new CopyOnWriteArrayList<>();

  private final List<TransactionListener> transactionListeners = new CopyOnWriteArrayList<>();


  @Override
  public void addListener(BlockChainListener blockChainListener) {
    blockChainListeners.add(blockChainListener);
  }


  @Override
  public void addPeer(String name, int port) {

  }


  @Override
  public void addPeer(String name, int port, Map<String, Object> properties) {

  }


  @Override
  public void addTransactionListener(TransactionListener l) {
    transactionListeners.add(l);
  }


  @Override
  public void broadcast(Message message) {

  }


  @Override
  public void consensusFinished() {

  }


  @Override
  public void consensusStarted() {

  }


  @Override
  public List<Object> getActiveConnectionSnapshot() {
    return null;
  }


  @Override
  public void init(List<Integer> chainsId, String uniqueNodeIdentifier, boolean broadcastTransactions) {

  }


  @Override
  public void init(int chainId, String uniqueNodeIdentifier, boolean broadcastTransactions) {

  }


  @Override
  public boolean send(PeerAddress addr, Object[] msg) {
    return false;
  }


  @Override
  public boolean send(PeerAddress addr, byte[] msg) {
    return false;
  }


  @Override
  public void setListenPort(int port) {

  }


  @Override
  public void start() {

  }


  @Override
  public void stop() {

  }


  @Override
  public void pause() {

  }


  @Override
  public void resume() {

  }

}
