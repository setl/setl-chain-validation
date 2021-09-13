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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetBalanceSerializer extends JsonSerializer<Map<String, Map<String, Number>>> {
  private static final Logger logger = LoggerFactory.getLogger(AssetBalanceSerializer.class);

  @Override
  public void serialize(Map<String, Map<String, Number>> value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
    generator.writeStartObject();

    value.forEach((key, values) -> {
      if (logger.isDebugEnabled()) {
        logger.debug(key);
      }
      
      try {
        generator.writeFieldName(key);
        generator.writeStartObject();

        serializeValues(values, generator);

        generator.writeEndObject();
      } catch (IOException e) {
        logger.error("IOException in AssetBalanceSerializer", e);
      }
    });

    generator.writeEndObject();
  }

  private void serializeValues(Map<String, Number> values, JsonGenerator generator) {
    values.forEach((m, n) -> {
      try {
        generator.writeFieldName(m);
        
        if (n instanceof Long) {
          generator.writeNumber((Long)n);
        } else if (n instanceof BigInteger) {
          generator.writeNumber((BigInteger) n);
        } else if (n instanceof BigDecimal) {
          generator.writeNumber((BigDecimal)n);
        } else if (n instanceof Double) {
          generator.writeNumber((Double)n);
        } else if (n instanceof Float) {
          generator.writeNumber((Float)n);
        } else {
          generator.writeNumber(n.longValue());
        }
        
      } catch (IOException e) {
        logger.error("IOException in AssetBalanceSerializer", e);
      }
    });
  }
}
