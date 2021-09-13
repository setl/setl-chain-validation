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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashedMap;
import io.setl.bc.pychain.serialise.Content;
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.JacksonContentFactory;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.entry.DataName;
import io.setl.bc.pychain.state.entry.DataNameManager;
import io.setl.bc.pychain.state.entry.HashWithType;
import io.setl.bc.pychain.state.entry.NamedDatum;

/**
 * Map for the locations of named objects.
 *
 * @author Simon Greatrix on 16/12/2019.
 */
public class NamedData extends HashedMap<String, HashWithType> {

  public static final ContentFactory<NamedData> FACTORY = new JacksonContentFactory<>(NamedData.class);


  /**
   * Load a named datum from external representation into the hash store.
   *
   * @param loadContext the deserialization context
   * @param name        the named datum's name
   * @param input       the representation of the datum
   * @param <T>         the datum's type
   *
   * @return the hash under which the datum was stored in storage.
   */
  private static <T extends NamedDatum<T>> HashWithType loadNamedDatum(LoadContext loadContext, DataName<T> name, Object input) {
    ContentFactory<T> factory = name.getFactory(loadContext.getHashStore());
    Class<T> type = factory.getType();
    T value = loadContext.getObjectMapper().convertValue(input, type);
    Content content = factory.asContent(loadContext.getContentDigestType(), value);
    Hash hash = loadContext.getHashStore().insert(content.getData());
    return new HashWithType(hash, Digest.TYPE_SHA_512_256);
  }


  public static JsonNode save(LoadContext loadContext, AbstractState state) {
    ObjectMapper objectMapper = loadContext.getObjectMapper();
    ObjectNode output = objectMapper.createObjectNode();
    NamedData namedData = state.getNamedDataHashes();
    namedData.forEach((k, v) -> {
      DataName<?> name = DataNameManager.getByName(k);
      if (name != null) {
        NamedDatum<?> datum = state.getDatum(name);
        JsonNode value = objectMapper.valueToTree(datum);
        output.set(k, value);
      }
    });
    return output;
  }


  public NamedData() {
    // do nothing
  }


  public NamedData(HashedMap<String, HashWithType> map) {
    super(map);
  }


  @JsonCreator
  public NamedData(Map<String, HashWithType> map) {
    super(map);
  }


  public void load(LoadContext loadContext, Map<?, ?> input) {
    input.forEach((k, v) -> {
      String key = String.valueOf(k);
      DataName<?> name = DataNameManager.getByName(key);
      if (name != null) {
        put(key, loadNamedDatum(loadContext, name, v));
      }
    });

  }



}
