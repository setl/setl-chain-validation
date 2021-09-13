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
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceListSerializer extends JsonSerializer<Merkle<NamespaceEntry>> {
  private static final Logger logger = LoggerFactory.getLogger(NamespaceListSerializer.class);
  private int counter = 0;

  @Override
  public void serialize(Merkle<NamespaceEntry> value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
    generator.writeStartArray();
    value.forEach(addressEntry -> {
      try {
        generator.writeStartArray();
        generator.writeNumber(counter);
        updateCounter();
        generator.writeString(addressEntry.getKey());
        generator.writeStartArray();
        generator.writeStartArray();
        generator.writeString(addressEntry.getKey());
        generator.writeString(addressEntry.getAddress());
        generator.writeStartObject();
        addressEntry.getClasses().forEach((name, data) -> writeClass(name, data, generator));
        generator.writeEndObject();
        String metadata = (addressEntry.getMetadata().isEmpty())
            ? "oA=="
            : Base64.getEncoder().encodeToString(addressEntry.getMetadata().getBytes(StandardCharsets.UTF_8));
        generator.writeString(metadata);
        generator.writeEndArray();
        generator.writeNull();
        generator.writeEndArray();
        generator.writeEndArray();
      } catch (IOException ioe) {
        logger.error("IOException in NamespaceListSerializer", ioe);
      }
    });
    generator.writeEndArray();
  }

  private void updateCounter() {
    counter++;
  }

  private void writeClass(String name, Asset data, JsonGenerator generator) {
    try {
      generator.writeFieldName(name);
      generator.writeStartArray();
      generator.writeString(data.getAssetId());
      generator.writeString(data.getMetadata());
      generator.writeEndArray();
    } catch (IOException ioe) {
      logger.error("IOException in NamespaceListSerializer", ioe);
    }
  }
}
