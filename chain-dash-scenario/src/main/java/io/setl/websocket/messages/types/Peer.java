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

public class Peer {

  private int height;

  private String ip;

  private double timestamp;


  /**
   * New instance.
   *
   * @param ip        this peer's ID address
   * @param height    the chain height known to this peer
   * @param timestamp the timestamp as a double(?)
   */
  public Peer(String ip, int height, double timestamp) {
    this.ip = ip;
    this.height = height;
    this.timestamp = timestamp;
  }


  public int getHeight() {
    return height;
  }


  public String getIp() {
    return ip;
  }


  public double getTimestamp() {
    return timestamp;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setIp(String ip) {
    this.ip = ip;
  }


  public void setTimestamp(double timestamp) {
    this.timestamp = timestamp;
  }
}
