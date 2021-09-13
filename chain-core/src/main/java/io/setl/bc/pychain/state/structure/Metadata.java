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
package io.setl.bc.pychain.state.structure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.JacksonContentFactory;
import io.setl.bc.pychain.state.AbstractState;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Holder for metadata associated with a state.
 *
 * @author Simon Greatrix on 2019-05-21.
 */
// Encode as a map with the properties in alphabetic order.
@JsonPropertyOrder(alphabetic = true)
public class Metadata extends Entry {

  public static final ContentFactory<Metadata> FACTORY = new JacksonContentFactory<>(Metadata.class);


  public static JsonNode save(LoadContext loadContext, AbstractState state) {
    Metadata metadata = new Metadata(state);
    return loadContext.getObjectMapper().valueToTree(metadata);
  }


  /** The chain associated with this state. */
  @JsonProperty
  public int chainId;

  /** The block height at which this state corresponds to the chain. */
  @JsonProperty
  public int height;

  /** The timestamp at which this state was created. */
  @JsonProperty
  public long timestamp;

  /** The hash of the block which produced this state. */
  @JsonProperty
  private Hash blockHash;


  /**
   * New instance for deserialization. The properties will be set by Jackson.
   */
  public Metadata() {
    blockHash = Hash.NULL_HASH;
  }


  /**
   * Gather the meta-data for a specified state.
   *
   * @param state the state
   */
  public Metadata(AbstractState state) {
    blockHash = state.getBlockHash();
    chainId = state.getChainId();
    height = state.getHeight();
    timestamp = state.getTimestamp();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Metadata)) {
      return false;
    }
    Metadata metadata = (Metadata) o;
    return chainId == metadata.chainId && height == metadata.height && timestamp == metadata.timestamp && Objects.equals(blockHash, metadata.blockHash);
  }


  public Hash getBlockHash() {
    return blockHash;
  }


  @Override
  public int hashCode() {
    return Objects.hash(blockHash, chainId, height, timestamp);
  }


  @JsonProperty
  public void setBlockHash(Hash blockHash) {
    this.blockHash = blockHash != null ? blockHash : Hash.NULL_HASH;
  }
}
