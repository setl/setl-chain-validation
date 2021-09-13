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
package io.setl.websocket.handlers;

import static org.mockito.ArgumentMatchers.any;

import com.google.common.eventbus.EventBus;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.event.ProposalUpdateEvent;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.p2p.message.ProposalMessage;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.scenario.ScenarioStates;
import io.setl.websocket.util.ValidationNodeStatistics;
import io.setl.websocket.util.ValidationNodeStatistics.BlockStatistics;
import io.setl.websocket.util.ValidationNodeStatistics.NetworkStateStatistics;
import io.setl.websocket.util.ValidationNodeStatistics.PeerStatistics;
import io.setl.websocket.util.ValidationNodeStatistics.TxStatistics;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class UpdateSocketHandlerTest {

  private static final String requestMessage = "{\"MessageType\":\"Request\",\"MessageHeader\":\"\",\"MessageBody\":{\"RequestName\":\"state\"},"
      + "\"RequestID\":1004,\"Compress\":\"lz\"}";
  private static final String subscribeMessage = "{\"MessageType\":\"Subscribe\",\"MessageHeader\":\"\","
      + "\"MessageBody\":{\"RequestName\":\"\",\"Topic\":[\"block\",\"balanceview\",\"proposal\",\"transaction\",\"serverstatus\"]},"
      + "\"RequestID\":1003,\"Compress\":\"lz\"}";
  private static final String commandMessage = "{\"MessageType\":\"Command\",\"MessageHeader\":\"\","
      + "\"MessageBody\":{\"Command\":\"txdiagnosis\",\"Action\":2},\"RequestID\":1000,\"Compress\":\"lz\"}";

  private UpdateSocketHandler updateSocketHandler;

  @Mock
  private EventBus eventBus;

  @Mock
  private TransactionPool txPool;

  @Mock
  private TextMessageQueueHandler textMessageQueueHandler;

  @Mock
  private Map<String, Map<String, Boolean>> webSocketSubscriptions;

  @Mock
  private Deque<Byte[]> clientMsgOut;

  @Mock
  private ProposalUpdateEvent proposalUpdateEvent;

  @Mock
  private Block block;

  @Mock
  private Txi transaction;

  @Mock
  private WebSocketSession webSocketSession;

  @Mock
  private ValidationNodeStatistics nodeStatistics;

  @Mock
  private StateManager stateManager;

  @Mock
  private PeerManager peerManager;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {

    webSocketSession = Mockito.mock(WebSocketSession.class);

    eventBus = Mockito.mock(EventBus.class);
    txPool = Mockito.mock(TransactionPool.class);
    textMessageQueueHandler = Mockito.mock(TextMessageQueueHandler.class);
    webSocketSubscriptions = Mockito.mock(Map.class);
    clientMsgOut = Mockito.mock(Deque.class);
    nodeStatistics = Mockito.mock(ValidationNodeStatistics.class);
    stateManager = Mockito.mock(StateManager.class);
    peerManager = Mockito.mock(PeerManager.class);

    State state = Mockito.mock(State.class);
    Mockito.when(stateManager.getState()).thenReturn(state);

    Merkle<AddressEntry> assetBalanceState = Mockito.mock(Merkle.class);
    Mockito.when(state.getAssetBalances()).thenReturn(assetBalanceState);

    Map<String, WebSocketSession> allConnectedSessions = new HashMap<>();
    allConnectedSessions.put("abcd1234", webSocketSession);

    Map<String, Object> locationMap = Mockito.mock(Map.class);
    final MPWrappedMap<String, Object> locationInfo = new MPWrappedMap<>(locationMap);

    proposalUpdateEvent = Mockito.mock(ProposalUpdateEvent.class);
    ProposalMessage proposalMessage = Mockito.mock(ProposalMessage.class);
    block = Mockito.mock(Block.class);
    transaction = Mockito.mock(Txi.class);
    SignatureMessage signature = Mockito.mock(SignatureMessage.class);

 //   Mockito.when(proposalUpdateEvent.getProposal()).thenReturn(proposalMessage);
    Mockito.when(proposalUpdateEvent.getVotePercentage()).thenReturn(0.5);
    Mockito.when(proposalUpdateEvent.getSignatures()).thenReturn(new SignatureMessage[] {signature});
    Mockito.when(proposalUpdateEvent.getUuid()).thenReturn(new UUID(1,1));
    Mockito.when(proposalMessage.getBlock()).thenReturn(block);
    Mockito.when(proposalMessage.getStateHash()).thenReturn(Hash.fromHex("99887654"));
    Mockito.when(block.getTransactionCount()).thenReturn(1);
    Mockito.when(block.getTransactions()).thenReturn(new Txi[] {transaction});

    PeerStatistics peerStats = Mockito.mock(PeerStatistics.class);
    Map<String, Object> peerStatsData = Mockito.mock(Map.class);
    Mockito.when(nodeStatistics.getPeerStatistics()).thenReturn(peerStats);
    Mockito.when(peerStats.get()).thenReturn(peerStatsData);

    NetworkStateStatistics networkStats = Mockito.mock(NetworkStateStatistics.class);
    Mockito.when(nodeStatistics.getNetworkStateStatistics()).thenReturn(networkStats);
    Mockito.when(networkStats.getLastBlockRequest()).thenReturn(123456789.0);
    Mockito.when(networkStats.getLastRequest()).thenReturn(123456789.0);

    TxStatistics txStats = Mockito.mock(TxStatistics.class);
    Mockito.when(nodeStatistics.getTxStatistics()).thenReturn(txStats);
    Mockito.when(txStats.getTx24Hours()).thenReturn(5);

    BlockStatistics blockStats = Mockito.mock(BlockStatistics.class);
    Mockito.when(nodeStatistics.getBlockStatistics()).thenReturn(blockStats);
    Mockito.when(blockStats.getLastBlockTime()).thenReturn(0.02345);
    Mockito.when(blockStats.getAverageBlockTime()).thenReturn(0.01234);

    ScenarioStates scenarioStates = new ScenarioStates();

    updateSocketHandler = new UpdateSocketHandler(eventBus, scenarioStates, txPool, nodeStatistics, stateManager,
        textMessageQueueHandler, webSocketSubscriptions, clientMsgOut, allConnectedSessions, peerManager);
  }

  @Test
  public void validRequestIsHandledCorrectly() throws Exception {

    TextMessage textMessage = new TextMessage(requestMessage.getBytes());
    updateSocketHandler.handleTextMessage(webSocketSession, textMessage);
    Mockito.verify(textMessageQueueHandler, Mockito.only()).sendMessage(any(byte[].class), any(WebSocketSession.class));
  }

  @Test
  public void validSubscribeIsHandledCorrectly() throws Exception {

    TextMessage textMessage = new TextMessage(subscribeMessage.getBytes());
    updateSocketHandler.handleTextMessage(webSocketSession, textMessage);
    Mockito.verify(clientMsgOut, Mockito.only()).add(any(Byte[].class));
  }

  @Test
  public void validCommandIsHandledCorrectly() throws Exception {

    TextMessage textMessage = new TextMessage(commandMessage.getBytes());
    updateSocketHandler.handleTextMessage(webSocketSession, textMessage);
    Mockito.verify(clientMsgOut, Mockito.only()).add(any(Byte[].class));
  }

  @Test
  public void invalidTextMessageThrowsParseException() throws Exception {

    TextMessage textMessage = new TextMessage("".getBytes());
    updateSocketHandler.handleTextMessage(webSocketSession, textMessage);
    Mockito.verify(textMessageQueueHandler, Mockito.never()).sendMessage(any(byte[].class), any(WebSocketSession.class));
  }

  @Test
  public void validProposalIsHandledCorrectly() throws Exception {

    Mockito.when(textMessageQueueHandler.sendMessage(any(byte[].class), any(WebSocketSession.class))).thenReturn(true);
    updateSocketHandler.proposalEventHandler(proposalUpdateEvent);
    Mockito.verify(textMessageQueueHandler, Mockito.times(1)).sendMessage(any(byte[].class), any(WebSocketSession.class));
  }

  @Test(expected = RuntimeException.class)
  public void invalidProposalIsNotSent() throws Exception {

    Mockito.when(textMessageQueueHandler.sendMessage(any(byte[].class), any(WebSocketSession.class))).thenReturn(false);
    updateSocketHandler.proposalEventHandler(proposalUpdateEvent);
  }
}
