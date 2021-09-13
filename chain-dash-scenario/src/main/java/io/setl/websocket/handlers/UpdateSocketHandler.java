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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.setl.bc.pychain.event.ProposalUpdateEvent;
import io.setl.bc.pychain.event.ProposalUpdateEvent.UpdateType;
import io.setl.bc.pychain.event.StateRequestEvent;
import io.setl.bc.pychain.node.StateInitializedEvent;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.Balance;
import io.setl.scenario.ScenarioChangeEvent;
import io.setl.scenario.ScenarioStates;
import io.setl.util.Convert;
import io.setl.util.LoggedThread;
import io.setl.websocket.client.ClientMessageType;
import io.setl.websocket.messages.APITextMessage;
import io.setl.websocket.messages.APITextMessageFactory;
import io.setl.websocket.messages.types.Asset;
import io.setl.websocket.messages.types.AssetBalance;
import io.setl.websocket.util.Last24HourTxCounter;
import io.setl.websocket.util.ValidationNodeStatistics;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.msgpack.core.MessageBufferPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handler for demo2 Created by aanten on 19/07/2017.
 */
@Component
public class UpdateSocketHandler extends TextWebSocketHandler implements ApplicationListener<StateInitializedEvent> {

  private static final int MAX_TX_PER_SECOND = 50;

  private static final String MESSAGE_BODY = "MessageBody";

  private static final String MESSAGE_ID = "RequestID";

  private static final String MESSAGE_TYPE = "MessageType";

  private static final String MT_COMMAND = "Command";

  private static final String MT_COMMAND_COMMAND = "Command";

  private static final String MT_COMMAND_OVERDRIVE = "overdrive";

  private static final String MT_COMMAND_SCENARIO = "scenario";

  private static final String MT_REQUEST = "Request";

  private static final String MT_SUBSCRIBE = "Subscribe";

  private static final String OVERDRIVE_FACTOR = "Overdrive";

  private static final String OVERDRIVE_PARAM_STATE = "State";

  private static final String SCENARIO_PARAM_ID = "Scenario";

  private static final String SCENARIO_PARAM_STATE = "State";

  private static final String TOPIC = "Topic";

  private static final int TX_UPDATE_DELAY = 1000;

  private static final Logger logger = LoggerFactory.getLogger(UpdateSocketHandler.class);

  private final EventBus eventBus;

  private final ScenarioStates scenarioStates;

  private final StateManager stateManager;

  private final String userID;

  private final String uuid;

  private Map<String, WebSocketSession> allConnectedSessions;

  private AssetBalance assetBalance = new AssetBalance();

  private Deque<Byte[]> clientMsgOut;

  private Last24HourTxCounter last24HourTxCounter;

  private ValidationNodeStatistics nodeStats;

  private PeerManager peerManager;

  private TextMessageQueueHandler textMessageQueueHandler;

  private TransactionPool txPool;

  private List<Txi> txQueue;

  private Map<String, Map<String, Boolean>> webSocketSubscriptions;


  @SuppressWarnings("squid:S00107") // Params > 7
  @Autowired
  public UpdateSocketHandler(EventBus eventBus, final ScenarioStates scenarioStates, TransactionPool txPool, ValidationNodeStatistics nodeStats,
      final StateManager stateManager, PeerManager peerManager) {
    this(eventBus, scenarioStates, txPool, nodeStats, stateManager, new TextMessageQueueHandler(), new HashMap<>(), new ConcurrentLinkedDeque<>(),
        new ConcurrentHashMap<>(), peerManager);
  }


  UpdateSocketHandler(EventBus eventBus, final ScenarioStates scenarioStates, TransactionPool txPool, ValidationNodeStatistics nodeStats,
      StateManager stateManager,
      TextMessageQueueHandler textMessageQueueHandler, Map<String, Map<String, Boolean>> webSocketSubscriptions, Deque<Byte[]> clientMsgOut,
      Map<String, WebSocketSession> allConnectedSessions, PeerManager peerManager
  ) {
    eventBus.register(this);

    this.eventBus = eventBus;
    this.scenarioStates = scenarioStates;
    this.webSocketSubscriptions = webSocketSubscriptions;
    this.webSocketSubscriptions.put("block", new HashMap<>());
    this.webSocketSubscriptions.put("balanceview", new HashMap<>());
    this.webSocketSubscriptions.put("stateview", new HashMap<>());
    this.webSocketSubscriptions.put("proposal", new HashMap<>());
    this.webSocketSubscriptions.put("transaction", new HashMap<>());
    this.webSocketSubscriptions.put("serverstatus", new HashMap<>());
    this.webSocketSubscriptions.put("status", new HashMap<>());
    this.webSocketSubscriptions.put("terminal", new HashMap<>());

    this.uuid = UUID.randomUUID().toString();
    this.userID = "0";
    this.textMessageQueueHandler = textMessageQueueHandler;
    this.clientMsgOut = clientMsgOut;
    this.allConnectedSessions = allConnectedSessions;
    this.txPool = txPool;
    this.nodeStats = nodeStats;
    this.last24HourTxCounter = new Last24HourTxCounter();
    this.txQueue = new ArrayList<>();
    this.stateManager = stateManager;
    this.peerManager = peerManager;
  }


  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    logger.info("afterConnectionClosed:{}", session);
    for (Map.Entry<String, Map<String, Boolean>> topic : webSocketSubscriptions.entrySet()) {
      if (topic.getValue().containsKey(session.getId())) {
        topic.getValue().remove(session.getId());
      }
    }
    allConnectedSessions.remove(session.getId());
  }


  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    logger.info("afterConnectionEstablished:{}", session);
    allConnectedSessions.put(session.getId(), session);
    initSession(session);
  }


  private void calculateBalance(Map<String, Number> assetBalancesMap, String assetId, Balance balance) {
    if (balance.greaterThanEqualZero()) {
      if (assetBalancesMap.containsKey(assetId)) {
        Number currentBalance = assetBalancesMap.get(assetId);
        assetBalancesMap.replace(assetId, currentBalance, balance.add(currentBalance).getValue());
      } else {
        assetBalancesMap.put(assetId, balance.getValue());
      }
    }
  }


  private Map<String, Map<String, Number>> getAssetBalances(StateManager stateManager) {
    Map<String, Map<String, Number>> assetBalancesMap = new HashMap<>();
    stateManager.getState().getAssetBalances().forEach(addressEntry -> {
      Map<String, Number> currentAssetBalance = new HashMap<>();

      if (addressEntry.getClassBalance() != null) {
        addressEntry.getClassBalance().forEach((assetId, balance) -> calculateBalance(currentAssetBalance, assetId, balance));
        assetBalancesMap.put(addressEntry.getKey(), currentAssetBalance);
      }
    });

    return assetBalancesMap;
  }


  private Map<String, List<Asset>> getAssetList(StateManager stateManager) {
    Map<String, List<Asset>> assetList = new HashMap<>();
    stateManager.getState().getAssetBalances().forEach(addressEntry -> {
      if (addressEntry.getClassBalance() != null) {
        addressEntry.getClassBalance().forEach((assetId, balance) -> {
          if (balance.greaterThanEqualZero()) {
            String[] namespaceAndAsset = assetId.split("\\|");
            Asset asset = new Asset(namespaceAndAsset[1], namespaceAndAsset[0], balance.getValue(), addressEntry.getAddress());
            List<Asset> assets = assetList.get(namespaceAndAsset[1]);
            if (assets == null) {
              assets = new ArrayList<>();
            }
            assets.add(asset);
            assetList.put(namespaceAndAsset[1], assets);
          }
        });
      }
    });

    return assetList;
  }


  private Map<String, Integer> getSubscriptions() {
    Map<String, Integer> subscriptions = new HashMap<>();
    for (Map.Entry<String, Map<String, Boolean>> topic : webSocketSubscriptions.entrySet()) {
      int length = topic.getValue().size();
      subscriptions.put(topic.getKey(), length);
    }

    return subscriptions;
  }


  /**
   * Handle 'command' message from client.
   *
   * @param body      : Message body received from the client.
   * @param messageID : Websocket request message ID received from the client.
   * @param session   : Websocket session.
   *
   * @throws IOException :
   */
  private void handleCommand(JSONObject body, String messageID, WebSocketSession session) throws IOException {
    logger.info("Handle Command");
    String command = (String) body.get(MT_COMMAND_COMMAND);

    switch (command.toLowerCase()) {
      case MT_COMMAND_SCENARIO:
        handleScenarioCommand(Convert.objectToInt(body.get(SCENARIO_PARAM_ID)), Convert.objectToInt(body.get(SCENARIO_PARAM_STATE)));
        break;

      case MT_COMMAND_OVERDRIVE:
        handleOverdriveCommand(Convert.objectToDouble(body.get(OVERDRIVE_FACTOR)), Convert.objectToInt(body.get(OVERDRIVE_PARAM_STATE)));
        break;

      default:
        break;
    }
    clientMsgOut.add(packageMessage(body, MT_COMMAND, messageID));
  }


  private void handleMessage(WebSocketSession session, String messageType, String messageID, JSONObject messageBody) {
    try {
      switch (messageType) {
        case MT_REQUEST:
          logger.info("REQUEST");
          handleRequest(messageBody, messageID, session);
          break;
        case MT_SUBSCRIBE:
          logger.info("SUBSCRIBE");
          handleSubscribe(messageBody, messageID, session.getId());
          break;
        case MT_COMMAND:
          logger.info("COMMAND");
          handleCommand(messageBody, messageID, session);
          break;
        default:
          break;
      }
    } catch (IOException e) {
      logger.error("Massage Handling error", e);
    }
  }


  private void handleOverdriveCommand(double overdrive, int newState) {
    eventBus.post(new ScenarioChangeEvent(null));
    scenarioStates.setOverdrive(overdrive, newState);
  }


  private void handleRequest(JSONObject body, String messageID, WebSocketSession session) {
    String requestName = (String) body.get("RequestName");
    logger.info("Handle Request {}", requestName);

    switch (requestName.toLowerCase()) {
      case "state":
        Map<String, Integer> subscriptions = getSubscriptions();

        APITextMessage requestTextMessage =
            APITextMessageFactory.getStateRequestTextMessage(
                stateManager.getCurrentStateDetail(),
                scenarioStates,
                body,
                messageID,
                nodeStats,
                subscriptions,
                txPool.getAvailableTransactionCount(),
                peerManager.getActiveConnectionSnapshot());

        textMessageQueueHandler.sendMessage(requestTextMessage.toJSON().getBytes(StandardCharsets.UTF_8), session);
        break;

      default:
        logger.warn("Unhandled request:{}", requestName);
        break;
    }

  }


  private void handleScenarioCommand(int id, int newState) {
    Scenario scenario = Scenario.forId(id);
    logger.info("Change scenario for id {} to {}", scenario, newState);
    eventBus.post(new ScenarioChangeEvent(scenario));
    scenarioStates.setRuningState(scenario, newState == 1);

  }


  private void handleSubscribe(JSONObject body, String messageID, String sessionId) throws IOException {
    logger.info("Handle Subscribe");
    if (body.containsKey(TOPIC)) {
      Object topics = body.get(TOPIC);

      if (topics instanceof JSONArray) {
        JSONArray data = (JSONArray) topics;
        for (Object topic : data) {
          String name = topic.toString();
          if (webSocketSubscriptions.containsKey(name)) {
            webSocketSubscriptions.get(name).put(sessionId, Boolean.TRUE);
          }
        }
      } else {
        logger.error("Topic not an instance of JSONArray");
        throw new IOException();
      }
    }

    Map<String, Integer> subscriptions = getSubscriptions();
    clientMsgOut.add(packageMessage(subscriptions, MT_SUBSCRIBE, messageID));
  }


  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    logger.info("handleTextMessage:{}", message.getPayloadLength());

    try {
      JSONParser parser = new JSONParser();
      final JSONObject jso;

      Object json = parser.parse(message.getPayload());
      if (json instanceof JSONObject) {
        jso = (JSONObject) json;
      } else {
        logger.error("Parsed message not an instance of JSONObject.");
        return;
      }

      String messageType = (String) jso.get(MESSAGE_TYPE);
      String messageID = jso.get(MESSAGE_ID).toString();
      JSONObject messageBody = (JSONObject) jso.get(MESSAGE_BODY);
      handleMessage(session, messageType, messageID, messageBody);
    } catch (ParseException pe) {
      logger.error("ParserException: {}", pe.getMessage());
    }
  }


  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    logger.error("handleTransportError", exception);
  }



  private void initSession(WebSocketSession session) {
    APITextMessage balanceViewTextMessage = APITextMessageFactory.getBalanceViewTextMessage(stateManager.getState(), txPool.getAvailableTransactionCount(),
        getAssetBalances(stateManager), stateManager.getState().getNamespaces(), getAssetList(stateManager));
    if (!textMessageQueueHandler.sendMessage(balanceViewTextMessage.toJSON().getBytes(StandardCharsets.UTF_8), session)) {
      throw new RuntimeException("Balance View Text Message could not be sent.");
    }
  }


  @Override
  public void onApplicationEvent(StateInitializedEvent event) {
    event.getState().getAssetBalances().forEach(addressEntry -> {
      String address = addressEntry.getAddress();
      if (addressEntry.getClassBalance() != null) {
        addressEntry.getClassBalance().forEach((assetId, balance) -> {
          String[] namespaceAndInstrument = assetId.split("\\|");
          assetBalance.addBalance(address, namespaceAndInstrument[0], namespaceAndInstrument[1], balance);
        });
      }
    });
  }


  private Byte[] packageMessage(Object message, String messageType, String messageID) throws IOException {
    try (MessageBufferPacker packer = MsgPackUtil.newBufferPacker()) {
      MsgPackUtil.packAnything(
          packer,
          new Object[]{
              ClientMessageType.WEBSOCKET_REQUEST.getValue(),
              uuid,
              userID,
              messageID,
              messageType,
              message
          }
      );

      byte[] pByteMessage = packer.toByteArray();
      Byte[] oByteMessage = new Byte[pByteMessage.length];
      Arrays.setAll(oByteMessage, n -> pByteMessage[n]);

      return oByteMessage;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }


  /**
   * Accept notification of a block proposal event.
   *
   * @param proposalUpdateEvent the event
   */
  @Subscribe
  public void proposalEventHandler(ProposalUpdateEvent proposalUpdateEvent) {
    logger.info("EVENT:vote {} sigs:{}", proposalUpdateEvent.getVotePercentage(), proposalUpdateEvent.getSignatures().length);

    if (proposalUpdateEvent.getUpdateType() == UpdateType.NEW_PROPOSAL) {
      // TODO - reinstate
      // nodeStats.getProposalStatistics().setLastProposalTime(proposalUpdateEvent.getProposal().getTimestamp());
      nodeStats.getBlockStatistics().setLastBlockTime(nodeStats.getProposalStatistics().getLastProposalTime());
      nodeStats.getBlockStatistics().setAverageBlockTime(nodeStats.getProposalStatistics().getProposalTime(),
          nodeStats.getProposalStatistics().getProposalCount());
      nodeStats.getTxStatistics().setTx24Hours(last24HourTxCounter.getTotal());
    }

    APITextMessage proposalTextMessage = APITextMessageFactory.getProposalTextMessage(proposalUpdateEvent,txPool);
    final APITextMessage blockTextMessage;
    final APITextMessage balanceViewTextMessage;
    if (proposalUpdateEvent.getUpdateType() == UpdateType.COMMITTED) {
      // TODO reinstate
      // blockTextMessage = APITextMessageFactory.getBlockTextMessage(proposalUpdateEvent.getProposal().getBlock(),
      //    (ProposalAPIMessage) proposalTextMessage.getData(), txPool.getAvailableTransactionCount(), nodeStats);
      blockTextMessage = null;
      balanceViewTextMessage = APITextMessageFactory.getBalanceViewTextMessage(stateManager.getState(), txPool.getAvailableTransactionCount(),
          getAssetBalances(stateManager), stateManager.getState().getNamespaces(), getAssetList(stateManager));
    } else {
      blockTextMessage = null;
      balanceViewTextMessage = null;
    }

    logger.info("API UpdateSocketHandler");
    allConnectedSessions.forEach((k, v) -> {
      logger.info("API Sending message to session: {}", k);

      WebSocketSession session = allConnectedSessions.get(k);
      if (!textMessageQueueHandler.sendMessage(proposalTextMessage.toJSON().getBytes(StandardCharsets.UTF_8), session)) {
        throw new RuntimeException("Proposal Text Message could not be sent.");
      }
      if (blockTextMessage != null && !textMessageQueueHandler.sendMessage(blockTextMessage.toJSON().getBytes(StandardCharsets.UTF_8), session)) {
        throw new RuntimeException("Block Text Message could not be sent.");
      }
      if (balanceViewTextMessage != null && !textMessageQueueHandler.sendMessage(balanceViewTextMessage.toJSON().getBytes(StandardCharsets.UTF_8), session)) {
        throw new RuntimeException("Balance View Text Message could not be sent.");
      }
    });
  }


  @Scheduled(fixedDelay = 5000)
  private synchronized void scheduledTick() {
    // I am not adding logging-on-abend for this method. It doesn't need it when all it does is log stuff.
    logger.info("UpdateSocketHandler:Scheduled");
    logger.info("Last 24 Hours transaction count: {}", last24HourTxCounter.getTotal());
    logger.info("Current transaction queue length: {}", txQueue.size());
  }


  /**
   * Accept notification of the block chain's status. This information is used to update the operational statistics.
   *
   * @param stateRequest the block chain's status
   */
  @Subscribe
  public void stateRequestHandler(StateRequestEvent stateRequest) {
    logger.info("UpdateSocketHandler: received ValidationNode stats");

    nodeStats.getNetworkStateStatistics().setLastRequest(stateRequest.getLastRequest());
    nodeStats.getNetworkStateStatistics().setHeight(stateRequest.getHeight());
    nodeStats.getNetworkStateStatistics().setLastBlockHash(stateRequest.getLastBlockHash());
    nodeStats.getNetworkStateStatistics().setChainID(stateRequest.getChainID());
    nodeStats.getPeerStatistics().setPeerStats(stateRequest.getIpAddress(), stateRequest.getHeight(), stateRequest.getTimestamp());
  }


  /**
   * Enqueue a transaction for processing. Silently discards transactions if the queue is becoming too long.
   *
   * @param transaction the transaction to enqueue.
   */
  @Subscribe
  public void transactionMessageHandler(Txi transaction) {
    logger.debug("Queueing transaction {}", transaction.getHash());
    synchronized (txQueue) {
      if (txQueue.size() < MAX_TX_PER_SECOND) {
        txQueue.add(transaction);
      }
    }
  }


  @Scheduled(fixedDelay = TX_UPDATE_DELAY)
  private void updateTxList() {
    LoggedThread.logged(() -> updateTxListImpl()).run();
  }


  private void updateTxListImpl() {
    List<Txi> transactions;
    synchronized (txQueue) {
      if (txQueue.isEmpty()) {
        return;
      }

      transactions = new ArrayList<>((txQueue.size() > MAX_TX_PER_SECOND) ? txQueue.subList(0, MAX_TX_PER_SECOND) : txQueue);
      txQueue.clear();
    }

    logger.info("Sending {} transactions from queue", transactions.size());

    APITextMessage transactionTextMessage = APITextMessageFactory.getTransactionTextMessage(transactions);

    last24HourTxCounter.update(System.currentTimeMillis() / 1000, transactions.size(), TX_UPDATE_DELAY / 1000);

    logger.info("API UpdateSocketHandler");
    allConnectedSessions.forEach((k, session) -> {
      logger.info("API Sending message to session: {}", k);

      if (!textMessageQueueHandler.sendMessage(transactionTextMessage.toJSON().getBytes(StandardCharsets.UTF_8), session)) {
        throw new RuntimeException("Transaction Text Message could not be sent.");
      }
    });
  }

}

