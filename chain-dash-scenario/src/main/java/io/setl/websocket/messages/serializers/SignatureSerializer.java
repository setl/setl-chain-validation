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
import io.setl.bc.pychain.p2p.message.SignatureMessage.SignatureDetail;
import io.setl.bc.pychain.p2p.message.SignatureMessage.XCSignatureDetail;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignatureSerializer extends JsonSerializer<List<Object>> {
  private static final Logger logger = LoggerFactory.getLogger(SignatureSerializer.class);

  @Override
  public void serialize(List<Object> value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
    generator.writeStartArray();

    for (Object obj : value) {
      generator.writeStartArray();

      if (obj instanceof XCSignatureDetail) {
        XCSignatureDetail xcSig = (XCSignatureDetail) obj;
        generator.writeString(xcSig.getSignature());
        generator.writeString(xcSig.getPublicKey());
        generator.writeString(xcSig.getXChainId());
      } else if (obj instanceof SignatureDetail) {
        SignatureDetail sig = (SignatureDetail) obj;
        generator.writeString(sig.getSignature());
        generator.writeString(sig.getPublicKey());
        generator.writeNumber(sig.getChainId());
      } else {
        logger.warn("Wrong instance of Signature Details.");
      }

      generator.writeEndArray();
    }

    generator.writeEndArray();
  }
}
