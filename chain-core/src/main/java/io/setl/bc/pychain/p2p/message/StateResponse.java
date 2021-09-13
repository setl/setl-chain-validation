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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.P2PType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Simon Greatrix on 2019-02-11.
 */
public class StateResponse implements Message {

  private final MPWrappedArray array;


  public StateResponse(MPWrappedArray array) {
    this.array = array;
  }


  /**
   * New instance.
   *
   * @param chainId       chain ID
   * @param height        height
   * @param xChainDetails cross chain details
   * @param lastStateHash hash of last state
   * @param lastBlockHash hash of last block
   */
  public StateResponse(int chainId, int height, Encodable[] xChainDetails, Hash lastStateHash, Hash lastBlockHash, Long votingPower) {
    if (xChainDetails == null) {
      xChainDetails = new Encodable[0];
    }
    Object[] encodedXChain = Arrays.stream(xChainDetails).map(Encodable::encode).toArray();
    array = new MPWrappedArrayImpl(new Object[]{
        /* 0 */ chainId,
        /* 1 */ P2PType.STATE_RESPONSE.getId(),
        /* 2 */ height,
        /* 3 */ encodedXChain,
        /* 4 */ (lastStateHash != null) ? lastStateHash.get() : null,
        /* 5 */ (lastBlockHash != null) ? lastBlockHash.get() : null,
        /* 6 */ votingPower
    });
  }


  /**
   * Returns message as Object array.
   *
   * @return this message in encoded form
   */
  @Override
  public Object[] encode() {
    return array.unwrap();
  }


  /**
   * Returns chain id.
   *
   * @return chain id
   */
  public int getChainId() {
    return array.asInt(0);
  }


  /**
   * Returns state height.
   *
   * @return height
   */
  public int getHeight() {
    return array.asInt(2);
  }


  /**
   * Returns last block hash.
   *
   * @return block hash
   */
  public Hash getLastBlockHash() {
    return new Hash(array.asByte(5));
  }


  /**
   * Returns state hash.
   *
   * @return state hash
   */
  public Hash getLastStateHash() {
    return new Hash(array.asByte(4));
  }


  /**
   * Returns message type.
   *
   * @return message type
   */
  @Override
  public P2PType getType() {
    return P2PType.STATE_RESPONSE;
  }


  /**
   * Extract x-chain details as list.
   *
   * @param reader function which extracts x-chain state data and converts it to type T
   *
   * @return list of structures T that represents x-chain state data
   */
  public <T> List<T> getXChainDetails(Function<Object[], T> reader) {
    return Arrays.stream(array.asObjectArray(3)).map(o -> reader.apply((Object[]) o)).collect(Collectors.toList());
  }


  /**
   * Returns voting power.
   * 
   * @return voting power
   */
  public Long getVotingPower() {
    return array.asLong(6);
  }

  @Override
  public String toString() {

    return String.format("Chain : %d, Height : %d, Last S-Hash : %s, Last B-Hash : %s, Voting Power : %d ",
        getChainId(),
        getHeight(),
        getLastStateHash().toB64(),
        getLastBlockHash().toB64(),
        getVotingPower()
    );
  }

}
