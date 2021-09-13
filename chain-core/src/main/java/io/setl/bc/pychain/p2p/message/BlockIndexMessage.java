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
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.common.CommonPy.IndexStatus;
import io.setl.common.CommonPy.P2PType;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Nicholas Pennington on 2021-04-21
 */

public class BlockIndexMessage implements Message {

  /*
  The Block index message is designed to provide a compact index with which to find finalised blocks in the
  'blocks' topic.
  When published, it is not always possible to get a precise topic/partition.offset breakdown, so we will
  use the timestamp provided on publish and will retrieve on this basis.
   */

  protected final int chainId;
  protected final P2PType type;
  protected final int blockHeight;
  protected final Hash blockHash;
  protected final IndexStatus status;
  protected final long timestamp;
  protected final Map<String, Object> metadata;

  public BlockIndexMessage(MPWrappedArray message) {

    this.chainId = message.asInt(0);
    this.type = P2PType.get(message.asInt(1));
    this.blockHeight = message.asInt(2);
    this.blockHash = new Hash(message.asByte(3));
    this.status = IndexStatus.get(message.asInt(4));
    this.timestamp = message.asLong(5);
    this.metadata = message.asWrappedMap(6).toMap();
  }

  public BlockIndexMessage(int chainId, int blockHeight, Hash blockHash, IndexStatus status, long timestamp, Map<String, Object> metadata) {
    this.chainId = chainId;
    this.type = P2PType.BLOCK_INDEX;
    this.blockHeight = blockHeight;
    this.blockHash = blockHash;
    this.status = status;
    this.timestamp = timestamp;       // UTC Epoch Milliseconds
    this.metadata = (metadata == null) ? new TreeMap<>() : metadata;
  }

  @Override
  public Object[] encode() {
    return new Object[]{
        chainId,
        type.getId(),
        blockHeight,
        blockHash,
        status.getId(),
        timestamp,
        new MPWrappedMap<>(metadata)};
  }

  @Override
  public int getChainId() {
    return chainId;
  }

  @Override
  public P2PType getType() {
    return this.type;
  }

  public int getBlockHeight() {
    return this.blockHeight;
  }

  public Hash getBlockHash() {
    return this.blockHash;
  }

  public IndexStatus getStatus() { return this.status; }

  public long getTimestamp() {
    return this.timestamp;
  }

  public Map<String, Object> getMetaData() {
    return this.metadata;
  }

  @Override
  public String toString() {

    return String.format("Chain : %d, Height : %d, Status : %s, Timestamp : %d, B-Hash : %s ",
        chainId,
        blockHeight,
        status.toString(),
        timestamp,
        blockHash.toB64()
        );
  }
}
