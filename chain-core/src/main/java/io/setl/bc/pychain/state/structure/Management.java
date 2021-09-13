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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;

import io.setl.bc.pychain.ConfigMap;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.JacksonContentFactory;
import io.setl.bc.pychain.serialise.factories.FactoryProvider;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.HashStore;
import io.setl.bc.pychain.state.entry.HashWithType;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;

/**
 * Management data associated with state.
 *
 * @author Simon Greatrix on 2019-05-21.
 */
// Serialize as a map with the properties in alphabetical order.
@JsonPropertyOrder(alphabetic = true)
public class Management extends Entry {

  /** Factory for serializing and deserializing management data. */
  public static final ContentFactory<Management> FACTORY = new JacksonContentFactory<>(Management.class);


  /**
   * Save the management information of a state to a JSON structure.
   *
   * @param loadContext the load context for the save
   * @param state       the state to save
   *
   * @return the JSON structure containing the management data
   */
  public static JsonNode save(LoadContext loadContext, AbstractState state) {
    ObjectMapper mapper = loadContext.getObjectMapper();
    ObjectNode data = mapper.createObjectNode();

    data.set("configuration", mapper.valueToTree(state.getConfig()));
    data.set("privilegedKeys", mapper.valueToTree(state.getPrivilegedKeys()));
    data.set("crossChainDetails", mapper.valueToTree(state.getXChainSignNodes()));

    Digest digest = Digest.create(loadContext.getKeyDigestType());

    return data;
  }


  /** Hash of the map containing the configuration settings. */
  @JsonProperty
  public HashWithType configuration;

  /** Hash of the definition of cross chain nodes. */
  @JsonProperty
  public HashWithType crossChainNodes;

  /** Hash of the list of privileged keys. */
  @JsonProperty
  public HashWithType privilegedKeys;

  /** Hash of the specification of the validation nodes. */
  @JsonProperty
  public HashWithType validationNodes;


  /** New instance for deserialization. Properties will be set by Jackson. */
  public Management() {
    // do nothing
  }


  /**
   * Create management data from a JSON serialized for.
   *
   * @param loadContext the context of the deserialization.
   * @param data        the data to load
   */
  Management(LoadContext loadContext, Map<?, ?> data) {
    final ObjectMapper mapper = loadContext.getObjectMapper();
    final HashStore hashStore = loadContext.getHashStore();

    ConfigMap configMap = mapper.convertValue(data.get("configuration"), ConfigMap.class);


    PrivilegedKey.Store keyStore = mapper.convertValue(data.get("privilegedKeys"), PrivilegedKey.Store.class);

    XChainDetails.Store xChainStore = mapper.convertValue(data.get("crossChainDetails"), XChainDetails.Store.class);

    CollectionType signNodeType = mapper.getTypeFactory().constructCollectionType(List.class, SignNodeEntry.class);
    List<SignNodeEntry> signNodes = mapper.convertValue(data.get("validationNodes"), signNodeType);
  }


  /**
   * New instance for a specified state.
   *
   * @param state the state
   * @param type  the digest type used to generate the hashes
   */
  public Management(AbstractState state, int type) {
    configuration = new HashWithType(state.getConfigHash(type), type);
    crossChainNodes = new HashWithType(state.getXChainSignNodes().getHash(type), type);
    privilegedKeys = new HashWithType(state.getPrivilegedKeys().getHash(type), type);
    validationNodes = new HashWithType(state.getSignNodes().getHash(), type);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Management)) {
      return false;
    }
    Management that = (Management) o;
    return Objects.equals(configuration, that.configuration) && Objects.equals(crossChainNodes, that.crossChainNodes)
        && Objects.equals(privilegedKeys, that.privilegedKeys) && Objects.equals(validationNodes, that.validationNodes);
  }


  @Override
  public int hashCode() {
    return Objects.hash(configuration, crossChainNodes, privilegedKeys, validationNodes);
  }

}
