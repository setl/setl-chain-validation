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
package io.setl.common;

import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.msgpack.core.MessagePacker;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class JsonTypeSafeMap extends TypeSafeMap implements MsgPackable, JSONStreamAware {

  private static final Predicate<Entry<String, Object>> ISNOT_INNER_PROPERTY_PREDICATE = entry -> !entry.getKey()
      .startsWith(".");

  private static final Predicate<String> IS_INNER_KEY_PREDICATE = key -> key.startsWith(".");


  public JsonTypeSafeMap() {
    super();
  }


  public JsonTypeSafeMap(Map<String, Object> map) {
    super(map);
  }


  private JSONObject exernalOnlyJSONObject() {
    Supplier<JSONObject> suppier = () -> new JSONObject(false);
    BiConsumer<JSONObject, ? super Entry<String, Object>> acceptor = (a, b) -> a.put(b.getKey(), b.getValue());
    BiConsumer<JSONObject, JSONObject> combine = (a, b) -> a.putAll(b);
    return map.entrySet().stream().filter(ISNOT_INNER_PROPERTY_PREDICATE).collect(suppier, acceptor, combine);
  }


  private JSONObject fullJSONObject() {
    if (map instanceof JSONObject) {
      return (JSONObject) map;
    }

    JSONObject json = new JSONObject(false);
    json.putAll(map);
    return json;
  }


  private boolean hasInnerValues() {
    return map.keySet().stream().anyMatch(IS_INNER_KEY_PREDICATE);
  }


  @Override
  public void pack(MessagePacker messagePacker) throws Exception {
    messagePacker.packMapHeader(map.size());
    for (Entry<String, Object> e : map.entrySet()) {
      messagePacker.packString(e.getKey());
      MsgPackUtil.packAnything(messagePacker, e.getValue());
    }
  }


  /**
   * Convert this map to a JSON object.
   *
   * @return JSON equivalent of this
   */
  public JSONObject toJSONObject() {
    if (hasInnerValues()) {
      return exernalOnlyJSONObject();
    }
    return fullJSONObject();

  }


  @Override
  public void writeJSONString(Writer writer) throws IOException {
    JSONObject.writeJSONString(map, writer);
  }
}
