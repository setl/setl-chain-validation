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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.p2p.message.SignatureMessage.SignatureDetail;
import io.setl.bc.pychain.p2p.message.SignatureMessage.XCSignatureDetail;
import io.setl.websocket.messages.serializers.SignatureSerializer;
import io.setl.websocket.messages.serializers.TimestampSerializer;
import io.setl.websocket.messages.types.NetworkState;
import java.util.List;
import java.util.Map;

public class BlockAPIMessage implements SubscriptionMessage {

  private double averageBlockTime;

  private Hash baseHash;

  private List<Object> contractEvents;

  private List<Object> effectiveTxList;

  private String error;

  private Hash hash;

  private int height;

  private String hostname;

  private double lastBlockTime;

  private NetworkState networkState;

  private Map<String, Integer> protocolCount;

  private List<SignatureDetail> sigList;

  private List<XCSignatureDetail> sigListXC;

  private double signPercentage;

  private List<Object> timeEvents;

  private double timestamp;

  private List<Object> transactions;

  private int tx24Hours;

  private int txCount;

  private int txIn;

  private int txOut;

  private int txPoolSize;

  private double votePercentage;


  /**
   * BlockAPIMessage Constructor.
   *
   * @param protocolCount    :
   * @param effectiveTxList  :
   * @param hostname         :
   * @param networkState     :
   * @param baseHash         :
   * @param hash             :
   * @param transactions     :
   * @param timestamp        :
   * @param sigListXC        :
   * @param tx24Hours        :
   * @param txPoolSize       :
   * @param lastBlockTime    :
   * @param averageBlockTime :
   * @param txIn             :
   * @param contractEvents   :
   * @param error            :
   * @param timeEvents       :
   * @param votePercentage   :
   * @param txOut            :
   * @param height           :
   * @param signPercentage   :
   * @param txCount          :
   * @param sigList          :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public BlockAPIMessage(Map<String, Integer> protocolCount, List<Object> effectiveTxList, String hostname,
      NetworkState networkState, Hash baseHash, Hash hash, List<Object> transactions, double timestamp, List<XCSignatureDetail> sigListXC,
      int tx24Hours, int txPoolSize, double lastBlockTime, double averageBlockTime, int txIn, List<Object> contractEvents, String error,
      List<Object> timeEvents, double votePercentage, int txOut, int height, double signPercentage, int txCount, List<SignatureDetail> sigList
  ) {

    this.protocolCount = protocolCount;
    this.effectiveTxList = effectiveTxList;
    this.hostname = hostname;
    this.networkState = networkState;
    this.baseHash = baseHash;
    this.hash = hash;
    this.transactions = transactions;
    this.timestamp = timestamp;
    this.sigListXC = sigListXC;
    this.tx24Hours = tx24Hours;
    this.txPoolSize = txPoolSize;
    this.lastBlockTime = lastBlockTime;
    this.averageBlockTime = averageBlockTime;
    this.txIn = txIn;
    this.contractEvents = contractEvents;
    this.error = error;
    this.timeEvents = timeEvents;
    this.votePercentage = votePercentage;
    this.txOut = txOut;
    this.height = height;
    this.signPercentage = signPercentage;
    this.txCount = txCount;
    this.sigList = sigList;
  }


  public void addSig(SignatureDetail sigObj) {

    sigList.add(sigObj);
  }


  public void addSigXC(XCSignatureDetail sigXCObj) {

    sigListXC.add(sigXCObj);
  }


  public void addTransaction(Object... txObj) {

    transactions.add(txObj);
  }


  @JsonProperty("AverageBlockTime")
  public double getAverageBlockTime() {

    return averageBlockTime;
  }


  @JsonProperty("BaseHash")
  public String getBaseHash() {

    return baseHash.toHexString();
  }


  @JsonProperty("ContractEvents")
  public List<Object> getContractEvents() {

    return contractEvents;
  }


  @JsonProperty("EffectiveTxList")
  public List<Object> getEffectiveTxList() {

    return effectiveTxList;
  }


  @JsonProperty("Error")
  public String getError() {

    return error;
  }


  @JsonProperty("Hash")
  public String getHash() {

    return hash.toHexString();
  }


  @JsonProperty("Height")
  public int getHeight() {

    return height;
  }


  @JsonProperty("Hostname")
  public String getHostname() {

    return hostname;
  }


  @JsonProperty("LastBlockTime")
  public double getLastBlockTime() {

    return lastBlockTime;
  }


  @JsonProperty("Networkstate")
  public NetworkState getNetworkState() {

    return networkState;
  }


  @JsonProperty("ProtocolCount")
  public Map<String, Integer> getProtocolCount() {

    return protocolCount;
  }


  @JsonSerialize(using = SignatureSerializer.class)
  @JsonProperty("Siglist")
  public List<SignatureDetail> getSigList() {

    return sigList;
  }


  @JsonSerialize(using = SignatureSerializer.class)
  @JsonProperty("SiglistXC")
  public List<XCSignatureDetail> getSigListXC() {

    return sigListXC;
  }


  @JsonProperty("SignPercentage")
  public double getSignPercentage() {

    return signPercentage;
  }


  @JsonProperty("TimeEvents")
  public List<Object> getTimeEvents() {

    return timeEvents;
  }


  @JsonSerialize(using = TimestampSerializer.class)
  @JsonProperty("Timestamp")
  public double getTimestamp() {

    return timestamp;
  }


  @JsonProperty("Transactions")
  public List<Object> getTransactions() {

    return transactions;
  }


  @JsonProperty("TX24Hours")
  public int getTx24Hours() {

    return tx24Hours;
  }


  @JsonProperty("TXCount")
  public int getTxCount() {

    return txCount;
  }


  @JsonProperty("TXIn")
  public int getTxIn() {

    return txIn;
  }


  @JsonProperty("TXOut")
  public int getTxOut() {

    return txOut;
  }


  @JsonProperty("TXPoolSize")
  public int getTxPoolSize() {

    return txPoolSize;
  }


  @JsonProperty("VotePercentage")
  public double getVotePercentage() {

    return votePercentage;
  }


  public void setAverageBlockTime(double averageBlockTime) {

    this.averageBlockTime = averageBlockTime;
  }


  public void setBaseHash(String baseHash) {

    this.baseHash = Hash.fromHex(baseHash);
  }


  public void setContractEvents(List<Object> contractEvents) {

    this.contractEvents = contractEvents;
  }


  public void setEffectiveTxList(List<Object> effectiveTxList) {

    this.effectiveTxList = effectiveTxList;
  }


  public void setError(String error) {

    this.error = error;
  }


  public void setHash(String hash) {

    this.hash = Hash.fromHex(hash);
  }


  public void setHeight(int height) {

    this.height = height;
  }


  public void setHostname(String hostname) {

    this.hostname = hostname;
  }


  public void setLastBlockTime(double lastBlockTime) {

    this.lastBlockTime = lastBlockTime;
  }


  public void setNetworkState(NetworkState networkState) {

    this.networkState = networkState;
  }


  public void setProtocolCount(Map<String, Integer> protocolCount) {

    this.protocolCount = protocolCount;
  }


  public void setSigList(List<SignatureDetail> sigList) {

    this.sigList = sigList;
  }


  public void setSigListXC(List<XCSignatureDetail> sigListXC) {

    this.sigListXC = sigListXC;
  }


  public void setSignPercentage(double signPercentage) {

    this.signPercentage = signPercentage;
  }


  public void setTimeEvents(List<Object> timeEvents) {

    this.timeEvents = timeEvents;
  }


  public void setTimestamp(double timestamp) {

    this.timestamp = timestamp;
  }


  public void setTransactions(List<Object> transactions) {

    this.transactions = transactions;
  }


  public void setTx24Hours(int tx24Hours) {

    this.tx24Hours = tx24Hours;
  }


  public void setTxCount(int txCount) {

    this.txCount = txCount;
  }


  public void setTxIn(int txIn) {

    this.txIn = txIn;
  }


  public void setTxOut(int txOut) {

    this.txOut = txOut;
  }


  public void setTxPoolSize(int txPoolSize) {

    this.txPoolSize = txPoolSize;
  }


  public void setVotePercentage(double votePercentage) {

    this.votePercentage = votePercentage;
  }
}
