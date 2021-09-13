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
package io.setl.bc.json.data;

import java.util.List;
import java.util.stream.Collectors;
import javax.json.JsonStructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.setl.json.jackson.Convert;

/**
 * @author Simon Greatrix on 02/07/2020.
 */
public class JsonConvert {

  /** Jackson Object Mapper for conversions. */
  public static final ObjectMapper OBJECT_MAPPER;


  /**
   * Convert a JsonStructure to a POJO.
   *
   * @param jsonStructure the JSON
   * @param type          the required POJO type
   * @param <T>           the required POJO type
   *
   * @return the POJO
   *
   * @throws JsonProcessingException if JSON cannot be converted
   */
  public static <T> T toInstance(JsonStructure jsonStructure, Class<T> type) throws JsonProcessingException {
    return OBJECT_MAPPER.treeToValue(Convert.toJackson(jsonStructure), type);
  }


  /**
   * Convert a POJO to a JsonStructure.
   *
   * @param object the POJO to convert
   *
   * @return the JSON
   */
  public static JsonStructure toJson(Object object) {
    return (JsonStructure) Convert.toJson(OBJECT_MAPPER.<JsonNode>valueToTree(object));
  }


  static {
    OBJECT_MAPPER = new ObjectMapper();
    //Beware the DefaultScalaModule, it's evilness causes changes in mapper deserialisation, and consequential test failure
    List<String> allowedModules = List.of("JsonModule", "Jdk8Module", "JavaTimeModule", "ParameterNamesModule", "AfterburnerModule");
    OBJECT_MAPPER.registerModules(ObjectMapper.findModules().stream().filter(m -> allowedModules.contains(m.getClass().getSimpleName())).collect(Collectors.toList()));
  }

}
