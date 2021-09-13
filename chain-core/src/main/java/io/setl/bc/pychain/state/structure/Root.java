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
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.JacksonContentFactory;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.HashWithType;
import java.util.Objects;

/**
 * The encoding of the state structure that contains all parts of a state. This is equivalent to a StateStructure instance, where the sub-structures are
 * referenced by their hashes.
 *
 * @author Simon Greatrix on 2019-05-21.
 */
// Encode as a map with the properties in alphabetic order
@JsonPropertyOrder(alphabetic = true)
public class Root extends Entry {

  /** Factory for serializing and deserializing Root objects to content addressable storage. */
  public static final ContentFactory<Root> FACTORY = new JacksonContentFactory<>(Root.class);

  /** The digest type used in hashing Merkle keys. */
  @JsonProperty
  public String keyDigestType;

  /** The Hash of the management data associated with this state. */
  @JsonProperty
  public HashWithType management;

  /** The Hash of the Merkle structures contained in this state. */
  @JsonProperty
  public HashWithType merkles;

  /** The Hash of the meta-data associated with this state. */
  @JsonProperty
  public HashWithType metadata;

  /** The Hash of the root of the named data partition. */
  @JsonProperty
  public HashWithType namedData;

  /** The encoding version used with this state. */
  @JsonProperty
  public int version;


  /**
   * New instance for a specific state.
   *
   * @param state         the state
   * @param keyDigestType the key digest type
   */
  Root(State state, String keyDigestType) {
    this.version = state.getVersion();
    this.keyDigestType = keyDigestType;
  }


  /**
   * New instance for deserialization. The properties will be set by Jackson.
   */
  public Root() {
    // do nothing
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Root)) {
      return false;
    }
    Root root = (Root) o;
    return version == root.version
        && Objects.equals(keyDigestType, root.keyDigestType)
        && Objects.equals(management, root.management)
        && Objects.equals(merkles, root.merkles)
        && Objects.equals(metadata, root.metadata)
        && Objects.equals(namedData, root.namedData);
  }


  @Override
  public int hashCode() {
    return Objects.hash(keyDigestType, management, merkles, metadata, namedData, version);
  }
}
