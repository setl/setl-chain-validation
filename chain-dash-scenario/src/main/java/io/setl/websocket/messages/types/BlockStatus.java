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

public class BlockStatus {

  private String hash;

  private int height;

  private double timestamp;

  private int txCount;


  /**
   * New instance.
   *
   * @param timestamp the timestamp as a double (?)
   * @param hash      this block's hash
   * @param txCount   the number of transactions in this block
   * @param height    the chain height
   */
  public BlockStatus(double timestamp, String hash, int txCount, int height) {
    this.timestamp = timestamp;
    this.hash = hash;
    this.txCount = txCount;
    this.height = height;
  }


  @JsonProperty("Hash")
  public String getHash() {
    return hash;
  }


  @JsonProperty("Height")
  public int getHeight() {
    return height;
  }


  @JsonProperty("Timestamp")
  public double getTimestamp() {
    return timestamp;
  }


  @JsonProperty("TXCount")
  public int getTxCount() {
    return txCount;
  }


  public void setHash(String hash) {
    this.hash = hash;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setTimestamp(double timestamp) {
    this.timestamp = timestamp;
  }


  public void setTxCount(int txCount) {
    this.txCount = txCount;
  }
}
