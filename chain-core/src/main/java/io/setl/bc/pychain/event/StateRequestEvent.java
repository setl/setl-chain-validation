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
package io.setl.bc.pychain.event;

public class StateRequestEvent {

  private final int chainID;

  private final int height;

  private final String ipAddress;

  private final String lastBlockHash;

  private final double lastRequest;

  private final double timestamp;


  /**
   * StateRequestEvent Constructor.
   *
   * @param lastRequest   :
   * @param ipAddress     :
   * @param height        :
   * @param timestamp     :
   * @param chainID       :
   * @param lastBlockHash :
   */
  public StateRequestEvent(double lastRequest, String ipAddress, int height, double timestamp, int chainID, String lastBlockHash) {
    this.lastRequest = lastRequest;
    this.ipAddress = ipAddress;
    this.height = height;
    this.timestamp = timestamp;
    this.chainID = chainID;
    this.lastBlockHash = lastBlockHash;
  }


  public int getChainID() {
    return chainID;
  }


  public int getHeight() {
    return height;
  }


  public String getIpAddress() {
    return ipAddress;
  }


  public String getLastBlockHash() {
    return lastBlockHash;
  }


  public double getLastRequest() {
    return lastRequest;
  }


  public double getTimestamp() {
    return timestamp;
  }
}
