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
package io.setl.bc.pychain.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
public class NodeStatus {

  private static final int MAX_HEIGHT_DIFF = 2;

  private static final long MAX_INACTIVE_PERIOD = 120L;

  private static final long VOTING_POWER_PERIOD = 10L;

  public static class PeerDetails {

    public static PeerDetails init(String address, int height, long timestamp) {
      return new PeerDetails(address, height, timestamp);
    }

    private String address;

    private int height;

    private long timestamp;


    private PeerDetails(String address, int height, long timestamp) {
      this.address = address;
      this.height = height;
      this.timestamp = timestamp;
    }


    public String getAddress() {
      return address;
    }


    public int getHeight() {
      return height;
    }


    public long getTimestamp() {
      return timestamp;
    }


    @Override
    public String toString() {
      return new StringJoiner(", ", PeerDetails.class.getSimpleName() + "[", "]")
          .add("address='" + address + "'")
          .add("height=" + height)
          .add("timestamp=" + timestamp)
          .toString();
    }
  }

  private final List<PeerDetails> peers = new ArrayList<>();

  private final Object peersLock = new Object();

  private int height;

  private String uniqueNodeIdentifier;

  private final Map<String, NodeVotingPower> votingPowerRecords = new HashMap<>();
  
  private Long lastStatusTime;

  private boolean lastVerificationResult;
  
  private Long lastVerificationTime;
  
  private Long totalRequiredVotingPower;
  
  private Long ownVotingPower;
  
  
  /**
   * Add, or update, the details of a peer node.
   *
   * @param address   the peer's address
   * @param height    the peer's height
   * @param timestamp the peer's last known timestamp
   */
  public void addPeerDetails(String address, int height, long timestamp) {
    synchronized (peersLock) {
      for (PeerDetails peer : peers) {
        if (peer.address.equals(address)) {
          peer.height = height;
          peer.timestamp = timestamp;
          return;
        }
      }

      peers.add(PeerDetails.init(address, height, timestamp));
    }
  }


  /**
   * dropStalePeers.
   *
   * @param currentTimestamp :
   */
  public void dropStalePeers(long currentTimestamp) {
    synchronized (peersLock) {
      peers.removeIf(peer -> TimeUnit.SECONDS.convert(currentTimestamp - peer.timestamp, TimeUnit.MILLISECONDS) > MAX_INACTIVE_PERIOD);
    }
  }


  public int getHeight() {
    return height;
  }


  public List<PeerDetails> getPeers() {
    return Collections.unmodifiableList(peers);
  }


  public String getUniqueNodeIdentifier() {
    return uniqueNodeIdentifier;
  }
  
  
  public Long getLastStatusTime() {
    return lastStatusTime;
  }

  
  public boolean getLastVerificationResult() {
    return lastVerificationResult;
  }
  
  
  public Long getLastVerificationTime() {
    return lastVerificationTime;
  }
  

  /**
   * Test if this node meets the UP criteria and is keeping up with the rest of the peer network.
   *
   * @return true if the last verification run was successful, height tracked by this node is in keeping with the rest of the network
   *         and is in contact with >50% of the voting share of the other nodes within the last 10 seconds i.e. two proposal intervals
   */
  public boolean isNodeUp(boolean isVotingPowerIgnored) {
    if (lastVerificationResult && (isVotingPowerIgnored || hasSufficientVotingPower())) {
      synchronized (peersLock) {
        if (peers.stream().allMatch(peer -> (peer.height - height) <= MAX_HEIGHT_DIFF)) {
          return true;
        }
      }
    }

    return false;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setUniqueNodeIdentifier(String uniqueNodeIdentifier) {
    this.uniqueNodeIdentifier = uniqueNodeIdentifier;
  }

  
  public void setLastStatusTime(Long statusTime) {
    this.lastStatusTime = statusTime;
  }

  
  public void setLastVerificationResult(boolean verificationResult) {
    this.lastVerificationResult = verificationResult;
  }

  
  public void setLastVerificationTime(Long verificationTime) {
    this.lastVerificationTime = verificationTime;
  }
  
  
  public void setTotalRequiredVotingPower(Long totalRequiredVotingPower) {
    this.totalRequiredVotingPower = totalRequiredVotingPower;
  }
  
  
  public void setOwnVotingPower(Long ownVotingPower) {
    this.ownVotingPower = ownVotingPower;
  }
  
  private static class NodeVotingPower {
    
    private final Long time;
    
    private final Long votingPower;
    

    private NodeVotingPower(Long time, Long votingPower) {
      this.time = time;
      this.votingPower = votingPower;
    }
    
    private Long getTime() {
      return time;
    }
    
    private Long getVotingPower() {
      return votingPower;
    }

  }
  
  
  public void recordVotingPower(String peerAddress, Long recordedTime, Long peerVotingPower) {
    int index = peerAddress.indexOf('=');
    if (index > 0) {
      votingPowerRecords.put(peerAddress.substring(0, index), new NodeVotingPower(recordedTime, peerVotingPower));
    }
  }
  
  
  private boolean hasSufficientVotingPower() {
    long totalPeerVotingPower = votingPowerRecords.values().stream()
        .filter(vp -> TimeUnit.SECONDS.convert(System.currentTimeMillis() - vp.getTime(), TimeUnit.MILLISECONDS) <= VOTING_POWER_PERIOD)
        .mapToLong(NodeVotingPower::getVotingPower)
        .sum();
    
    return (totalPeerVotingPower + ownVotingPower) >= totalRequiredVotingPower;
  }
  
}
