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
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.websocket.messages.serializers.AssetBalanceSerializer;
import io.setl.websocket.messages.serializers.NamespaceListSerializer;
import io.setl.websocket.messages.serializers.TimestampSerializer;
import io.setl.websocket.messages.types.Asset;
import java.util.List;
import java.util.Map;

public class BalanceViewAPIMessage implements SubscriptionMessage {

  private Map<String, Map<String, Number>> assetBalances;

  private Map<String, List<Asset>> assetList;

  private int chainID;

  private Hash hash;

  private int height;

  private Merkle<NamespaceEntry> namespaces;

  private String status;

  private double timestamp;

  private int txCount;

  private List<Object> xcDetails;


  /**
   * BalanceViewAPIMessage Constructor.
   *
   * @param status        :
   * @param chainID       :
   * @param hash          :
   * @param txCount       :
   * @param xcDetails     :
   * @param height        :
   * @param namespaces    :
   * @param assetBalances :
   * @param assetList     :
   * @param timestamp     :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public BalanceViewAPIMessage(String status, int chainID, Hash hash, int txCount, List<Object> xcDetails, int height,
      Merkle<NamespaceEntry> namespaces, Map<String, Map<String, Number>> assetBalances, Map<String, List<Asset>> assetList, double timestamp
  ) {

    this.status = status;
    this.chainID = chainID;
    this.hash = hash;
    this.txCount = txCount;
    this.xcDetails = xcDetails;
    this.height = height;
    this.namespaces = namespaces;
    this.assetBalances = assetBalances;
    this.assetList = assetList;
    this.timestamp = timestamp;
  }


  public void addAssetBalance(String key, Map<String, Number> value) {

    assetBalances.put(key, value);
  }


  @JsonSerialize(using = AssetBalanceSerializer.class)
  @JsonProperty("Assetbalances")
  public Map<String, Map<String, Number>> getAssetBalances() {

    return assetBalances;
  }


  @JsonProperty("assetList")
  public Map<String, List<Asset>> getAssetList() {

    return assetList;
  }


  @JsonProperty("ChainID")
  public int getChainID() {

    return chainID;
  }


  @JsonProperty("Hash")
  public String getHash() {

    return hash.toHexString();
  }


  @JsonProperty("Height")
  public int getHeight() {

    return height;
  }


  @JsonSerialize(using = NamespaceListSerializer.class)
  @JsonProperty("Namespaces")
  public Merkle<NamespaceEntry> getNamespaces() {

    return namespaces;
  }


  @JsonProperty("Status")
  public String getStatus() {

    return status;
  }


  @JsonSerialize(using = TimestampSerializer.class)
  @JsonProperty("Timestamp")
  public double getTimestamp() {

    return timestamp;
  }


  @JsonProperty("TXCount")
  public int getTxCount() {

    return txCount;
  }


  @JsonProperty("xcdetails")
  public List<Object> getXcDetails() {

    return xcDetails;
  }


  public void setAssetBalances(Map<String, Map<String, Number>> assetBalances) {

    this.assetBalances = assetBalances;
  }


  public void setAssetList(Map<String, List<Asset>> assetList) {

    this.assetList = assetList;
  }


  public void setChainID(int chainID) {

    this.chainID = chainID;
  }


  public void setHash(String hash) {

    this.hash = Hash.fromHex(hash);
  }


  public void setHeight(int height) {

    this.height = height;
  }


  public void setNamespaces(Merkle<NamespaceEntry> namespaces) {

    this.namespaces = namespaces;
  }


  public void setStatus(String status) {

    this.status = status;
  }


  public void setTimestamp(double timestamp) {

    this.timestamp = timestamp;
  }


  public void setTxCount(int txCount) {

    this.txCount = txCount;
  }


  public void setXcDetails(List<Object> xcDetails) {

    this.xcDetails = xcDetails;
  }
}
