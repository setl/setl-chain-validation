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
package io.setl.websocket.util;

import io.setl.utils.TimeUtil;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ValidationNodeStatistics {

  public static class BlockStatistics {

    private double averageBlockTime = 0.0;

    private double lastBlockTime = 0.0;


    public void clear() {
      lastBlockTime = 0.0;
      averageBlockTime = 0.0;
    }


    /**
     * <p>ValidationNodeStatistics
     * Return Map of Node statistics.</p>
     *
     * @return :
     */
    public Map<String, Object> get() {
      Map<String, Object> blockStats = new HashMap<>();
      blockStats.put("lastBlockTime", lastBlockTime);
      blockStats.put("averageBlockTime", averageBlockTime);

      return blockStats;
    }


    public double getAverageBlockTime() {
      return averageBlockTime;
    }


    public double getLastBlockTime() {
      return lastBlockTime;
    }


    public void setAverageBlockTime(double proposalTime, int proposalCount) {
      this.averageBlockTime = proposalTime / Math.max(1, proposalCount);
    }


    public void setLastBlockTime(double lastProposalTime) {
      lastBlockTime = lastProposalTime;
    }
  }



  public static class NetworkStateStatistics {

    private int chainID;

    private int height = 0;

    private String lastBlockHash = "";

    private double lastBlockRequest = 0.0;

    private double lastRequest = 0.0;


    public void clear() {
      lastRequest = 0.0;
      lastBlockRequest = 0.0;
    }


    /**
     * <p>ValidationNodeStatistics
     * Return Map of network statistics.</p>
     *
     * @return :
     */
    public Map<String, Object> get() {
      Map<String, Object> networkStats = new HashMap<>();
      networkStats.put("lastRequest", lastRequest);
      networkStats.put("lastBlockRequest", lastBlockRequest);
      networkStats.put("height", height);
      networkStats.put("lastBlockHash", lastBlockHash);
      networkStats.put("chainID", chainID);

      return networkStats;
    }


    public int getChainID() {
      return chainID;
    }


    public int getHeight() {
      return height;
    }


    public String getLastBlockHash() {
      return lastBlockHash;
    }


    public double getLastBlockRequest() {
      return lastBlockRequest;
    }


    public double getLastRequest() {
      return lastRequest;
    }


    public void setChainID(int chainID) {
      this.chainID = chainID;
    }


    public void setHeight(int height) {
      this.height = height;
    }


    public void setLastBlockHash(String lastBlockHash) {
      this.lastBlockHash = lastBlockHash;
    }


    /**
     * Set the time of the last request. The new value is only accepted if the existing value is more than 10 seconds in the past.
     *
     * @param lastRequest the last request
     */
    public void setLastRequest(double lastRequest) {
      if (this.lastRequest < TimeUtil.unixTimeDouble() - 10.0) {
        this.lastRequest = lastRequest;
      }
    }
  }



  public static class PeerStatistics {

    private Map<String, Object> peerStats = new HashMap<>();


    public void clear() {
      peerStats.clear();
    }


    public Map<String, Object> get() {
      return peerStats;
    }


    public void setPeerStats(String ipAddress, int height, double timestamp) {
      peerStats.put(ipAddress, new Object[]{height, timestamp});
    }
  }



  public static class ProposalStatistics {

    private double lastProposalTime = 0.0;

    private int proposalCount = 0;

    private double proposalTime = 0.0;


    /**
     * <p>ProposalStatistics.clear().
     * Clear proposal statistics.</p>
     */
    public void clear() {
      proposalCount = 0;
      lastProposalTime = 0.0;
      proposalTime = 0.0;
    }


    /**
     * Get a map of the statistics.
     *
     * @return the statistics as key-value pairs
     */
    public Map<String, Object> get() {
      Map<String, Object> proposalStats = new HashMap<>();
      proposalStats.put("proposalCount", proposalCount);
      proposalStats.put("proposalTime", proposalTime);

      return proposalStats;
    }


    public double getLastProposalTime() {
      return lastProposalTime;
    }


    public int getProposalCount() {
      return proposalCount;
    }


    public double getProposalTime() {
      return proposalTime;
    }


    /**
     * Set the time of the last proposal. This updates the expected time for the next proposal and the count of proposals so far.
     *
     * @param proposalTimeStamp the time of the proposal
     */
    public void setLastProposalTime(long proposalTimeStamp) {
      lastProposalTime = Math.max(0, TimeUtil.unixTimeDouble() - proposalTimeStamp);
      updateProposalTime();
      updateProposalCount();
    }


    private void updateProposalCount() {
      proposalCount += 1;
    }


    private void updateProposalTime() {
      proposalTime += lastProposalTime;
    }
  }



  public static class TxStatistics {

    private int tx24Hours = 0;

    private int txIn = 0;

    private int txOut = 0;


    /**
     * <p>TxStatistics.clear().
     * Clear statistics.</p>
     */
    public void clear() {
      txIn = 0;
      txOut = 0;
      tx24Hours = 0;
    }


    /**
     * <p>TxStatistics
     * Return Map of Transaction statistics.</p>
     *
     * @return :
     */
    public Map<String, Object> get() {
      Map<String, Object> txStats = new HashMap<>();
      txStats.put("txIn", txIn);
      txStats.put("txOut", txOut);
      txStats.put("tx24Hours", tx24Hours);

      return txStats;
    }


    public int getTx24Hours() {
      return tx24Hours;
    }


    public int getTxIn() {
      return txIn;
    }


    public int getTxOut() {
      return txOut;
    }


    public void setTx24Hours(int tx24Hours) {
      this.tx24Hours = tx24Hours;
    }
  }



  private BlockStatistics blockStatistics = new BlockStatistics();

  private NetworkStateStatistics networkStateStatistics = new NetworkStateStatistics();

  private PeerStatistics peerStatistics = new PeerStatistics();

  private ProposalStatistics proposalStatistics = new ProposalStatistics();

  private TxStatistics txStatistics = new TxStatistics();


  /**
   * <p>TxStatistics.clear().
   * Clear TX statistics.</p>
   */
  public void clear() {
    txStatistics.clear();
    proposalStatistics.clear();
    blockStatistics.clear();
    networkStateStatistics.clear();
    peerStatistics.clear();
  }


  /**
   * <p>ValidationNodeStatistics
   * Return Map of proposal statistics.</p>
   *
   * @return :
   */
  public Map<String, Object> get() {
    Map<String, Object> fullStat = new HashMap<>();
    fullStat.put("transaction", txStatistics.get());
    fullStat.put("proposal", proposalStatistics.get());
    fullStat.put("block", blockStatistics.get());
    fullStat.put("networkState", networkStateStatistics.get());
    fullStat.put("peer", peerStatistics.get());

    return fullStat;
  }


  public BlockStatistics getBlockStatistics() {
    return blockStatistics;
  }


  public NetworkStateStatistics getNetworkStateStatistics() {
    return networkStateStatistics;
  }


  public PeerStatistics getPeerStatistics() {
    return peerStatistics;
  }


  public ProposalStatistics getProposalStatistics() {
    return proposalStatistics;
  }


  public TxStatistics getTxStatistics() {
    return txStatistics;
  }
}
