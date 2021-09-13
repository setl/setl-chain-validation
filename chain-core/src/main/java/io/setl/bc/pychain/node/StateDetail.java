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

import io.setl.bc.pychain.Hash;

/**
 * @author Simon Greatrix on 02/01/2019.
 */
public class StateDetail {

  private int chainId;

  private Hash blockHash;

  private Hash stateHash;

  private int height;

  private int signNodeCount;

  private long timestamp;


  /**
   * StateDetail : Set State detail.
   *
   * @param stateHash     :
   * @param blockHash     :
   * @param height        :
   * @param timestamp     :
   * @param signNodeCount :
   */
  public StateDetail(Hash stateHash, Hash blockHash, int chainId, int height, long timestamp, int signNodeCount) {
    this.stateHash = stateHash;
    this.blockHash = blockHash;
    this.chainId = chainId;
    this.height = height;
    this.timestamp = timestamp;
    this.signNodeCount = signNodeCount;
  }


  public int getBlockHeight() {
    return height - 1;
  }


  public int getChainId() {
    return chainId;
  }


  public Hash getBlockHash() {
    return blockHash;
  }


  public Hash getStateHash() {
    return stateHash;
  }


  public int getHeight() {
    return height;
  }


  public int getSignSize() {
    return signNodeCount;
  }


  public long getTimestamp() {
    return timestamp;
  }
}
