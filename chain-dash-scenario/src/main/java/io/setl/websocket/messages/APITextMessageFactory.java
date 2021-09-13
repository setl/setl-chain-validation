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
package io.setl.websocket.messages;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.event.ProposalUpdateEvent;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.node.StateDetail;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.bc.pychain.p2p.message.SignatureMessage.SignatureDetail;
import io.setl.bc.pychain.p2p.message.SignatureMessage.XCSignatureDetail;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.AssetTransferTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.scenario.ScenarioState;
import io.setl.scenario.ScenarioStates;
import io.setl.utils.TimeUtil;
import io.setl.websocket.messages.types.Asset;
import io.setl.websocket.messages.types.BlockStatus;
import io.setl.websocket.messages.types.Location;
import io.setl.websocket.messages.types.NetworkState;
import io.setl.websocket.messages.types.Peer;
import io.setl.websocket.messages.types.ProtocolCounter;
import io.setl.websocket.util.ValidationNodeStatistics;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APITextMessageFactory {

  public static final Logger logger = LoggerFactory.getLogger(APITextMessageFactory.class);

  private static final int TRANSACTION_BLOCK_LIMIT = 100;


  /**
   * Create a message detailing the assets owned by addresses.
   *
   * @param state            state containing the assets
   * @param txCount          number of transactions
   * @param assetBalancesMap the addresses and what they own
   * @param namespaceList    the namespaces
   * @param assetList        the assets
   *
   * @return the message
   */
  public static APITextMessage getBalanceViewTextMessage(State state, int txCount, Map<String, Map<String, Number>> assetBalancesMap,
      Merkle<NamespaceEntry> namespaceList, Map<String, List<Asset>> assetList) {

    BalanceViewAPIMessage balanceViewAPIMessage = new BalanceViewAPIMessage(
        "OK",
        state.getChainId(),
        state.getBlockHash(),
        txCount,
        new ArrayList<>(), // XC Details, at the moment it's always empty.
        state.getHeight(),
        namespaceList,
        assetBalancesMap,
        assetList,
        TimeUtil.unixTimeDouble()
    );

    return new APITextMessage(balanceViewAPIMessage, "Update", "balanceview");
  }


  /**
   * Create a message describing a block.
   *
   * @param block               the block
   * @param proposalAPIMessage  the associated proposal
   * @param txPoolCount         the number of transactions
   * @param validationNodeStats the state of the validation nodes
   *
   * @return the message
   */
  public static APITextMessage getBlockTextMessage(Block block, ProposalAPIMessage proposalAPIMessage, int txPoolCount,
      ValidationNodeStatistics validationNodeStats) {

    Txi[] blockTransactions = block.getTransactions();

    List<Object> transactions = new ArrayList<>();
    for (int i = 0; i < TRANSACTION_BLOCK_LIMIT && i < blockTransactions.length; i++) {
      Txi transaction = blockTransactions[i];
      if (transaction != null) {
        Object[] encoded = transaction.encodeTx();
        transactions.add(encoded);
      }
    }

    Map<String, Object> peerStats = validationNodeStats.getPeerStatistics().get();
    List<Peer> peers = new ArrayList<>();
    peerStats.forEach((ipAddress, stats) -> {
      Object[] statsArr = (Object[]) stats;
      Peer peer = new Peer(ipAddress, (int) statsArr[0], (double) statsArr[1]);
      peers.add(peer);
    });

    BlockAPIMessage blockAPIMessage = new BlockAPIMessage(
        ProtocolCounter.getCounter(block.getTransactions()),
        new ArrayList<>(), // Effective Tx List - Not used in Demo2 (Contracts)
        block.getNodeName(),
        new NetworkState(
            validationNodeStats.getNetworkStateStatistics().getLastRequest(),
            validationNodeStats.getNetworkStateStatistics().getLastBlockRequest(),
            peers
        ),
        block.getBaseStateHash(),
        block.getPreviousBlockHash(),
        transactions,
        block.getTimeStamp(),
        proposalAPIMessage.getSignaturesXC(),
        validationNodeStats.getTxStatistics().getTx24Hours(),
        txPoolCount,
        validationNodeStats.getBlockStatistics().getLastBlockTime(),
        validationNodeStats.getBlockStatistics().getAverageBlockTime(),
        0, // Tx in - TODO ask Wojciech
        Arrays.asList(block.getContractEvents().unwrap()), // Contract events - Not used in Demo2 (Contracts)
        "",
        Arrays.asList(block.getTimeEvents().unwrap()), // Time events - Not used in Demo2 (Contracts)
        proposalAPIMessage.getVotePercentage(),
        0, // Tx out - TODO ask Wojciech
        block.getHeight(),
        proposalAPIMessage.getSignPercentage(),
        block.getTransactionCount(),
        proposalAPIMessage.getSignatures()
    );

    return new APITextMessage(blockAPIMessage, "Update", "block");
  }


  /**
   * Create a message detailing a proposal event.
   *
   * @param proposalUpdateEvent the event
   *
   * @return the message
   */
  public static APITextMessage getProposalTextMessage(ProposalUpdateEvent proposalUpdateEvent, TransactionPool txPool) {

    SignatureMessage[] signatureMessages = proposalUpdateEvent.getSignatures();
    List<XCSignatureDetail> signaturesXC = new ArrayList<>();
    List<SignatureDetail> signatures = new ArrayList<>();

    for (SignatureMessage signatureMessage : signatureMessages) {
      signaturesXC.add(signatureMessage.getXChainSignature());
      signatures.add(signatureMessage.getSignature());
    }

    // TODO re-instate missing fields
    ProposalAPIMessage proposalAPIMessage = new ProposalAPIMessage(
        proposalUpdateEvent.getVotePercentage() / 100.0,
        proposalUpdateEvent.getUuid().toString(),
        signaturesXC,
        0, // Proposal timestamp
        "", // Proposer's hostname
        0, // Current chain height
        signatures,
        proposalUpdateEvent.getUuid().toString(),
        new Location(new MPWrappedMap<String, Object>(new HashMap<>())),
        0, // transaction count
        "",
        proposalUpdateEvent.getSignaturePercentage() / 100.0
    );

    return new APITextMessage(proposalAPIMessage, "Update", "proposal");
  }


  /**
   * getStateRequestTextMessage.
   *
   * @param stateDetail              :
   * @param scenarioSettings         :
   * @param request                  :
   * @param messageID                :
   * @param nodeStats                :
   * @param subscriptions            :
   * @param txPoolCount              :
   * @param activeConnectionSnapshot :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static APITextMessage getStateRequestTextMessage(
      StateDetail stateDetail,
      ScenarioStates scenarioSettings,
      JSONObject request,
      String messageID,
      ValidationNodeStatistics nodeStats,
      Map<String, Integer> subscriptions,
      int txPoolCount,
      List<Object> activeConnectionSnapshot
  ) {

    //TODO: This message is only partially populated.

    List<Object> scenarios = new ArrayList<>();
    for (ScenarioState scenarioState : scenarioSettings.getStates()) {
      scenarios.add(new Object[]{scenarioState.getID(), scenarioState.isRunning(), scenarioState.getName()});
    }

    Map<String, Object> peerStats = nodeStats.getPeerStatistics().get();
    List<Peer> networkPeers = new ArrayList<>();

    peerStats.forEach((ipAddress, stats) -> {
      Object[] statsArr = (Object[]) stats;
      Peer peer = new Peer(ipAddress, (int) statsArr[0], (double) statsArr[1]);
      networkPeers.add(peer);
    });

    List<Object> peerList = new ArrayList<>();
    peerList.add(new Object[]{});

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (Exception ex) {
      hostname = "unknown";
    }

    RequestAPIMessage requestAPIMessage = new RequestAPIMessage(
        nodeStats.getNetworkStateStatistics().getHeight(), //    NetworkHeight
        hostname, // "127.0.0.1", //                             Hostname
        scenarios, //                                            Scenarios
        new BlockStatus(
            nodeStats.getBlockStatistics().getLastBlockTime(),
            (stateDetail != null ? stateDetail.getBlockHash().toHexString() : "BLOCK-HASH"),
            0,
            (stateDetail != null ? stateDetail.getBlockHeight() : 0)), // LastBlock (Timestamp, Hash, TxCount, Height)
        new NetworkState(
            nodeStats.getNetworkStateStatistics().getLastRequest(),
            nodeStats.getNetworkStateStatistics().getLastBlockRequest(),
            networkPeers
        ), //                                                    Network State ()
        "OK", //                                                 Status
        nodeStats.getNetworkStateStatistics().getChainID(), //   ChainID
        subscriptions, //                                        Subscriptions
        0, //                                                    LogLevel
        TimeUtil.unixTimeDouble(), //                            Proposaltimestamp
        nodeStats.getTxStatistics().getTx24Hours(), //           Tx 24 Hours
        txPoolCount, //                                          Tx Pool Size
        nodeStats.getBlockStatistics().getLastBlockTime(), //    Last Block Time
        nodeStats.getBlockStatistics().getAverageBlockTime(), // Average Block Time
        0, //                                                    LogAreas
        activeConnectionSnapshot, //                             Peers
        false, //                                                txLogBlock
        (scenarioSettings.getOverdriveState() != 0), //          Overdrive
        peerList, //                                             Peer List
        false, //                                                txLogScenario
        true, //                                                 signing
        (stateDetail != null ? stateDetail.getHeight() : 0) //   Height
    );

    return new APITextMessage("state", hostname, requestAPIMessage, messageID, "request");
  }


  /**
   * Create a message detailing the specified transactions.
   *
   * @param transactions the transactions to include in the message
   *
   * @return the message
   */
  public static APITextMessage getTransactionTextMessage(List<Txi> transactions) {

    List<Object> outboundTxs = new ArrayList<>();

    transactions.forEach(tx -> {
      Object[] txData = {
          tx.getTxType(),
          tx.getFromAddress(),
          (tx instanceof AssetTransferTx) ? ((AssetTransferTx) tx).getToAddress() : "",
          (tx instanceof AssetTransferTx) ? ((AssetTransferTx) tx).getNameSpace() : "",
          (tx instanceof AssetTransferTx) ? ((AssetTransferTx) tx).getClassId() : "",
          (tx instanceof AssetTransferTx) ? ((AssetTransferTx) tx).getAmount() : 0,
          0,
          tx.getHash(),
          (tx instanceof AssetTransferTx) ? tx.getHeight() : 0,
          (tx instanceof AssetTransferTx) ? ((AssetTransferTx) tx).getProtocol() : "",
          tx.getNonce(),
          (tx instanceof AssetTransferTx) ? tx.getMetadata() : ""
      };
      outboundTxs.add(txData);
    });

    TransactionAPIMessage transactionAPIMessage = new TransactionAPIMessage(TimeUtil.unixTimeDouble(), outboundTxs, "");
    return new APITextMessage(transactionAPIMessage, "Update", "transaction");
  }


  private APITextMessageFactory() {

  }

}
