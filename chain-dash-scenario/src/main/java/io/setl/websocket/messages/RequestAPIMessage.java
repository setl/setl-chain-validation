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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.setl.websocket.messages.types.BlockStatus;
import io.setl.websocket.messages.types.NetworkState;
import java.util.List;
import java.util.Map;

public class RequestAPIMessage implements SubscriptionMessage {

  private double averageBlockTime;

  private int chainId;

  private int height;

  private String hostname;

  private BlockStatus lastBlock;

  private double lastBlockTime;

  private int logAreas;

  private int logLevel;

  private int networkHeight;

  private NetworkState networkState;

  private boolean overDrive;

  private List<Object> peerList;

  private List<Object> peers;

  private double proposalTimestamp;

  private List<Object> scenarios;

  private boolean signing;

  private String status;

  private Map<String, Integer> subscriptions;

  private int tx24Hours;

  private boolean txLogBlock;

  private boolean txLogScenario;

  private int txPoolSize;


  /**
   * New instance.
   *
   * @param networkHeight     message parameter
   * @param hostname          message parameter
   * @param scenarios         message parameter
   * @param lastBlock         message parameter
   * @param networkState      message parameter
   * @param status            message parameter
   * @param chainId           message parameter
   * @param subscriptions     message parameter
   * @param logLevel          message parameter
   * @param proposalTimestamp message parameter
   * @param tx24Hours         message parameter
   * @param txPoolSize        message parameter
   * @param lastBlockTime     message parameter
   * @param averageBlockTime  message parameter
   * @param logAreas          message parameter
   * @param peers             message parameter
   * @param txLogBlock        message parameter
   * @param overDrive         message parameter
   * @param peerList          message parameter
   * @param txLogScenario     message parameter
   * @param signing           message parameter
   * @param height            message parameter
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public RequestAPIMessage(int networkHeight, String hostname, List<java.lang.Object> scenarios, BlockStatus lastBlock, NetworkState networkState,
      String status, int chainId, Map<String, Integer> subscriptions, int logLevel, double proposalTimestamp, int tx24Hours, int txPoolSize,
      double lastBlockTime, double averageBlockTime, int logAreas, List<Object> peers, boolean txLogBlock, boolean overDrive, List<Object> peerList,
      boolean txLogScenario, boolean signing, int height
  ) {
    this.networkHeight = networkHeight;
    this.hostname = hostname;
    this.scenarios = scenarios;
    this.lastBlock = lastBlock;
    this.networkState = networkState;
    this.status = status;
    this.chainId = chainId;
    this.subscriptions = subscriptions;
    this.logLevel = logLevel;
    this.proposalTimestamp = proposalTimestamp;
    this.tx24Hours = tx24Hours;
    this.txPoolSize = txPoolSize;
    this.lastBlockTime = lastBlockTime;
    this.averageBlockTime = averageBlockTime;
    this.logAreas = logAreas;
    this.peers = peers;
    this.txLogBlock = txLogBlock;
    this.overDrive = overDrive;
    this.peerList = peerList;
    this.txLogScenario = txLogScenario;
    this.signing = signing;
    this.height = height;
  }


  @JsonProperty("AverageBlockTime")
  public double getAverageBlockTime() {
    return averageBlockTime;
  }


  @JsonProperty("ChainID")
  public int getChainId() {
    return chainId;
  }


  @JsonProperty("Height")
  public int getHeight() {
    return height;
  }


  @JsonProperty("Hostname")
  public String getHostname() {
    return hostname;
  }


  @JsonProperty("LastBlock")
  public BlockStatus getLastBlock() {
    return lastBlock;
  }


  @JsonProperty("LastBlockTime")
  public double getLastBlockTime() {
    return lastBlockTime;
  }


  @JsonProperty("LogAreas")
  public int getLogAreas() {
    return logAreas;
  }


  @JsonProperty("LogLevel")
  public int getLogLevel() {
    return logLevel;
  }


  @JsonProperty("NetworkHeight")
  public int getNetworkHeight() {
    return networkHeight;
  }


  @JsonProperty("Networkstate")
  public NetworkState getNetworkState() {
    return networkState;
  }


  @JsonProperty("PeerList")
  public List<Object> getPeerList() {
    return peerList;
  }


  @JsonProperty("Peers")
  public List<Object> getPeers() {
    return peers;
  }


  @JsonProperty("ProposalTimestamp")
  public double getProposalTimestamp() {
    return proposalTimestamp;
  }


  @JsonProperty("Scenarios")
  public List<java.lang.Object> getScenarios() {
    return scenarios;
  }


  @JsonProperty("Signing")
  public String getSigning() {
    return signing ? "True" : "False";
  }


  @JsonProperty("Status")
  public String getStatus() {
    return status;
  }


  @JsonProperty("Subscriptions")
  public Map<String, Integer> getSubscriptions() {
    return subscriptions;
  }


  @JsonProperty("TX24Hours")
  public int getTx24Hours() {
    return tx24Hours;
  }


  @JsonProperty("TXPoolSize")
  public int getTxPoolSize() {
    return txPoolSize;
  }


  @JsonProperty("Overdrive")
  public boolean isOverDrive() {
    return overDrive;
  }


  @JsonProperty("txLogBlock")
  public boolean isTxLogBlock() {
    return txLogBlock;
  }


  @JsonProperty("txLogScenario")
  public boolean isTxLogScenario() {
    return txLogScenario;
  }


  public void setAverageBlockTime(double averageBlockTime) {
    this.averageBlockTime = averageBlockTime;
  }


  public void setChainId(int chainId) {
    this.chainId = chainId;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setHostname(String hostname) {
    this.hostname = hostname;
  }


  public void setLastBlock(BlockStatus lastBlock) {
    this.lastBlock = lastBlock;
  }


  public void setLastBlockTime(double lastBlockTime) {
    this.lastBlockTime = lastBlockTime;
  }


  public void setLogAreas(int logAreas) {
    this.logAreas = logAreas;
  }


  public void setLogLevel(int logLevel) {
    this.logLevel = logLevel;
  }


  public void setNetworkHeight(int networkHeight) {
    this.networkHeight = networkHeight;
  }


  public void setNetworkState(NetworkState networkState) {
    this.networkState = networkState;
  }


  public void setOverDrive(boolean overDrive) {
    this.overDrive = overDrive;
  }


  public void setPeerList(List<Object> peerList) {
    this.peerList = peerList;
  }


  public void setPeers(List<Object> peers) {
    this.peers = peers;
  }


  public void setProposalTimestamp(double proposalTimestamp) {
    this.proposalTimestamp = proposalTimestamp;
  }


  public void setScenarios(List<java.lang.Object> scenarios) {
    this.scenarios = scenarios;
  }


  public void setSigning(boolean signing) {
    this.signing = signing;
  }


  public void setStatus(String status) {
    this.status = status;
  }


  public void setSubscriptions(Map<String, Integer> subscriptions) {
    this.subscriptions = subscriptions;
  }


  public void setTx24Hours(int tx24Hours) {
    this.tx24Hours = tx24Hours;
  }


  public void setTxLogBlock(boolean txLogBlock) {
    this.txLogBlock = txLogBlock;
  }


  public void setTxLogScenario(boolean txLogScenario) {
    this.txLogScenario = txLogScenario;
  }


  public void setTxPoolSize(int txPoolSize) {
    this.txPoolSize = txPoolSize;
  }
}
