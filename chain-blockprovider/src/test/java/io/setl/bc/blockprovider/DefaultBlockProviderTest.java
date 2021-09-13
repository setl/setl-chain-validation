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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.noMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.Mockito;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.BlockCommitted;
import io.setl.bc.pychain.p2p.message.BlockFinalized;
import io.setl.bc.pychain.p2p.message.EmptyProposal;
import io.setl.bc.pychain.p2p.message.Encodable;
import io.setl.bc.pychain.p2p.message.StateResponse;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.state.test.BlockBuilder;
import io.setl.common.MutableObject;
import io.setl.common.Pair;

/**
 * @author Simon Greatrix on 01/04/2021.
 */
public class DefaultBlockProviderTest {

  static class TestConsumer implements Consumer<Block> {

    List<Block> received = new ArrayList<>();


    @Override
    public void accept(Block block) {
      received.add(block);
    }

  }



  private final Hash hash = new Hash(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});

  MsgFactory msgFactory = new MsgFactory();

  PeerAddress peerAddress = () -> "test";

  PeerManager peerManager = mock(PeerManager.class);

  DefaultBlockProvider provider = new DefaultBlockProvider(peerManager, 100);


  @Test
  public void addBlockListener1() {
    // Check add leads to events being received.
    TestConsumer consumer = new TestConsumer();
    assertFalse(provider.isHasUnsetListeners());
    assertTrue(provider.getBlockListeners().isEmpty());
    provider.addBlockListener(consumer);
    assertFalse(provider.getBlockListeners().isEmpty());
    assertTrue(provider.isHasUnsetListeners());

    // state response should initialise the listener
    StateResponse stateResponse = new StateResponse(100, 19, new Encodable[0], hash, hash, 100L);
    provider.eventReceived(peerAddress, stateResponse);
    assertSame(peerAddress, provider.getLastContact());
    assertFalse(provider.isHasUnsetListeners());

    // When the next block is issued, it will be requested
    stateResponse = new StateResponse(100, 20, new Encodable[0], hash, hash, 100L);
    provider.eventReceived(peerAddress, stateResponse);

    Object[] msg = msgFactory.blockRequest(100, 19, 0);
    verify(peerManager).send(any(), aryEq(msg));

    // block finalized should go to listener
    Block block = new BlockBuilder().withHeight(19).build();
    BlockFinalized blockFinalized = new BlockFinalized(block);
    provider.eventReceived(peerAddress, blockFinalized);
    assertEquals(1, consumer.received.size());

    provider.removeBlockListener(consumer);
    assertTrue(provider.getBlockListeners().isEmpty());
  }


  @Test
  public void addBlockListener2() {
    TestConsumer consumer = new TestConsumer();
    assertFalse(provider.isHasUnsetListeners());
    provider.addBlockListener(30, consumer);
    assertFalse(provider.isHasUnsetListeners());

    // state response does nothing
    StateResponse stateResponse = new StateResponse(100, 20, new Encodable[0], hash, hash, 100L);
    provider.eventReceived(peerAddress, stateResponse);
    assertSame(peerAddress, provider.getLastContact());
    assertFalse(provider.isHasUnsetListeners());
    assertEquals(20,provider.currentHeight());

    // listener receives block finalized
    Block block = new BlockBuilder().withHeight(20).build();
    BlockFinalized blockFinalized = new BlockFinalized(block);
    provider.eventReceived(peerAddress, blockFinalized);
    assertEquals(0, consumer.received.size());

    // state 20 is created by block 19, so we are not interested yet.
    verify(peerManager).hasBlockFetcher();
    verify(peerManager, noMoreInteractions()).send(any(), any(Object[].class));
  }


  @Test
  public void addBlockListener3() {
    TestConsumer consumer = new TestConsumer();
    assertFalse(provider.isHasUnsetListeners());
    provider.addBlockListener(60, 50, consumer);
    assertFalse(provider.isHasUnsetListeners());
    assertTrue(provider.getBlockListeners().isEmpty());
  }


  @Test
  public void addEventListener1() {
    // Check add and remove functionality
    ArrayList<BlockEvent> list = new ArrayList<>();
    Consumer<BlockEvent> listener = list::add;
    assertFalse(provider.getEventListeners().contains(listener));

    provider.addEventListener(listener);
    assertTrue(provider.getEventListeners().contains(listener));

    provider.removeEventListener(listener);
    assertFalse(provider.getEventListeners().contains(listener));
  }



  @Test
  public void addEventListener2() {
    // Check listener receives empty proposal notification
    ArrayList<BlockEvent> list = new ArrayList<>();
    Consumer<BlockEvent> listener = list::add;
    assertFalse(provider.getEventListeners().contains(listener));

    provider.addEventListener(listener);
    EmptyProposal msg = new EmptyProposal(100,hash,20,1_000_000L,"node","");
    provider.eventReceived(peerAddress,msg);

    assertEquals(1,list.size());
    assertTrue(list.get(0).isEmpty());
    assertEquals(20, list.get(0).getHeight());
    assertEquals(100, list.get(0).getChainId());
  }



  @Test
  public void addEventListener3() {
    // Check listener receives block committed notification
    ArrayList<BlockEvent> list = new ArrayList<>();
    Consumer<BlockEvent> listener = list::add;
    assertFalse(provider.getEventListeners().contains(listener));

    provider.addEventListener(listener);
    BlockCommitted msg = new BlockCommitted(100,25,hash);
    provider.eventReceived(peerAddress,msg);

    assertEquals(1,list.size());
    assertFalse(list.get(0).isEmpty());
    assertEquals(26, list.get(0).getHeight());
    assertEquals(100, list.get(0).getChainId());
  }


  @Test
  public void addStateListener1() {
    // Check add and remove functionality
    MutableObject<Pair<StateResponse,PeerAddress>> output = new MutableObject<>(null);
    BiConsumer<StateResponse,PeerAddress> consumer = (s,p) -> output.set(new Pair<>(s,p));

    provider.addStateListener(consumer);
    assertEquals(1,provider.getStateListeners().size());
    assertTrue(provider.getStateListeners().contains(consumer));
    provider.removeStateListener(consumer);
    assertFalse(provider.getStateListeners().contains(consumer));
  }

  @Test
  public void afterProperties() {
    provider.afterPropertiesSet();
    verify(peerManager).addListener(eq(provider));
    // StateRequest method does not have an equals(), so we'll just check something is sent.
    verify(peerManager).broadcast(any());
  }

  @Test
  public void addStateListener2() {
    // Check messages received
    MutableObject<Pair<StateResponse,PeerAddress>> output = new MutableObject<>(null);
    BiConsumer<StateResponse,PeerAddress> consumer = (s,p) -> output.set(new Pair<>(s,p));

    provider.addStateListener(consumer);
    StateResponse stateResponse = new StateResponse(100, 20, new Encodable[0], hash, hash, 100L);
    provider.eventReceived(peerAddress, stateResponse);

    assertSame(stateResponse,output.get().left());
    assertSame(peerAddress,output.get().right());
  }

  @Test
  public void startStop() {
    provider.start();
    provider.destroy();

    verify(peerManager).init(eq(100),any(),eq(false));
    verify(peerManager).start();
    verify(peerManager).stop();
  }


  @Test
  public void testTrigger() {
    provider.requestBlock(20,null);
    provider.requestBlock(21,null);
    provider.requestBlock(22,null);
    provider.requestBlock(23,null);
    // 24 is skipped
    provider.requestBlock(25,null);
    provider.requestBlock(26,null);
    provider.requestBlock(27,null);
    provider.requestBlock(25,null);
    provider.requestBlock(27,null);
    provider.requestBlock(25,null);
    // 28 is skipped
    provider.requestBlock(29,null);
    provider.requestBlock(30,null);
    provider.requestBlock(33,null);
    provider.requestBlock(32,null);
    provider.requestBlock(31,null);

    StateResponse stateResponse = new StateResponse(100, 40, new Encodable[0], hash, hash, 100L);
    provider.eventReceived(peerAddress, stateResponse);

    // 32 and 33 do not appear as they are outside the max block count
    assertEquals(Set.of(20, 21, 22, 23, 25, 26, 27, 29, 30, 31), provider.getPendingBlocks().keySet());

    // 3 contiguous blocks to request
    verify(peerManager, Mockito.times(3)).send(any(),any(Object[].class));
  }
}