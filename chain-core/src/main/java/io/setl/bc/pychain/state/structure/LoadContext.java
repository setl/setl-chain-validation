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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.serialise.factories.FactoryProvider;
import io.setl.bc.pychain.state.HashStore;
import io.setl.common.CommonPy.VersionConstants;
import java.util.Map;

/**
 * The context for loading state from some external representation.
 *
 * @author Simon Greatrix on 30/12/2019.
 */
public class LoadContext {

  private int contentDigestType = Digest.getRecommended();

  private FactoryProvider factoryProvider = FactoryProvider.get();

  private HashStore hashStore;

  private Map<String, Object> input;

  private int keyDigestType = Digest.getRecommended();

  private ObjectMapper objectMapper;

  private JsonNode output;

  private int version = VersionConstants.VERSION_DEFAULT;


  public int getContentDigestType() {
    return contentDigestType;
  }


  public FactoryProvider getFactoryProvider() {
    return factoryProvider;
  }


  public HashStore getHashStore() {
    return hashStore;
  }


  public Map<String, Object> getInput() {
    return input;
  }


  public int getKeyDigestType() {
    return keyDigestType;
  }


  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }


  public JsonNode getOutput() {
    return output;
  }


  public int getVersion() {
    return version;
  }


  public void setContentDigestType(int contentDigestType) {
    this.contentDigestType = contentDigestType;
  }


  public void setFactoryProvider(FactoryProvider factoryProvider) {
    this.factoryProvider = factoryProvider;
  }


  public void setHashStore(HashStore hashStore) {
    this.hashStore = hashStore;
  }


  public void setInput(Map<String, Object> input) {
    this.input = input;
  }


  public void setKeyDigestType(int keyDigestType) {
    this.keyDigestType = keyDigestType;
  }


  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }


  public void setOutput(JsonNode output) {
    this.output = output;
  }


  public void setVersion(int version) {
    this.version = version;
  }
}
