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
package io.setl.bc.blockprovider;

/**
 * A "heart beat" event on the block chain, indicating that either a block was committed to the chain, or an empty proposal was made.
 *
 * @author Simon Greatrix on 01/09/2018.
 */
public class BlockEvent {

  private final int chainId;

  private final int height;

  private final boolean isEmpty;

  /**
   * what if I change in openchain as well as chain in same commit?
   */
  private boolean whatIf;


  /**
   * New instance.
   *
   * @param chainId the chain's ID
   * @param height  the chain height
   * @param isEmpty true if the corresponding proposal was empty
   */
  public BlockEvent(int chainId, int height, boolean isEmpty) {
    this.chainId = chainId;
    this.height = height;
    this.isEmpty = isEmpty;
  }

  /**
   * Returns chain id.
   *
   * @return chain id
   */
  public int getChainId() {
    return chainId;
  }

  /**
   * Returns the state height, which is one higher than the last block height.
   *
   * @return height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Returns true if the corresponding proposal was empty.
   *
   * @return boolean
   */
  public boolean isEmpty() {
    return isEmpty;
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("BlockEvent{");
    sb.append("chainId=").append(chainId);
    sb.append(", height=").append(height);
    sb.append(", isEmpty=").append(isEmpty);
    sb.append('}');
    return sb.toString();
  }
}
