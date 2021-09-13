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
package io.setl.websocket.messages.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeersHeightSerializer extends JsonSerializer<Map<String, List<Object>>> {
  private static final Logger logger = LoggerFactory.getLogger(PeersHeightSerializer.class);
  private DecimalFormat decForm = new DecimalFormat("#.######");

  @Override
  public void serialize(Map<String, List<Object>> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartObject();

    value.forEach((key, values) -> {
      try {
        gen.writeFieldName(key);
        gen.writeStartArray();

        for (Object val : values) {
          if (val instanceof Double) {
            gen.writeNumber(decForm.format(val));
          } else {
            gen.writeNumber(val.toString());
          }
        }

        gen.writeEndArray();
      } catch (IOException e) {
        logger.error("IOException in PeersHeightSerializer", e);
      }
    });

    gen.writeEndObject();
  }
}
