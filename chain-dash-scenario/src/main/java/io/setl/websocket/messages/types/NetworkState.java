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
package io.setl.websocket.messages.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.setl.websocket.messages.serializers.PeersHeightSerializer;
import io.setl.websocket.messages.serializers.TimestampSerializer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"lastRequest", "lastBlockRequest", "peersHeight"})
public class NetworkState {

  private double lastBlockRequest;

  private double lastRequest;

  private Map<String, List<Object>> peersHeight = new HashMap<>();


  /**
   * New instance.
   *
   * @param lastRequest      the last request as a double(?)
   * @param lastBlockRequest as the last block request as a double(?)
   * @param peers            the list of network peers in this network
   */
  public NetworkState(double lastRequest, double lastBlockRequest, List<Peer> peers) {
    this.lastRequest = lastRequest;
    this.lastBlockRequest = lastBlockRequest;
    setPeersHeight(peers);
  }


  public void addPeer(String key, List<Object> value) {
    peersHeight.put(key, value);
  }


  @JsonProperty("lastblockrequest")
  public double getLastBlockRequest() {
    return lastBlockRequest;
  }


  @JsonSerialize(using = TimestampSerializer.class)
  @JsonProperty("last_request")
  public double getLastRequest() {
    return lastRequest;
  }


  @JsonSerialize(using = PeersHeightSerializer.class)
  @JsonProperty("peersheight")
  public Map<String, List<Object>> getPeersHeight() {
    return peersHeight;
  }


  public void setLastBlockRequest(double lastBlockRequest) {
    this.lastBlockRequest = lastBlockRequest;
  }


  public void setLastRequest(double lastRequest) {
    this.lastRequest = lastRequest;
  }


  /**
   * Set the chain heights across this network from the list of network peers.
   *
   * @param peers the peers
   */
  public void setPeersHeight(List<Peer> peers) {
    for (Peer peer : peers) {
      peersHeight.put(peer.getIp(), Arrays.asList(peer.getHeight(), peer.getTimestamp()));
    }
  }


  public void setPeersHeight(Map<String, List<Object>> peersHeight) {
    this.peersHeight = peersHeight;
  }
}
