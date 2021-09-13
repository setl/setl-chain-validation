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
package io.setl.bc.pychain.serialise;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.msgpack.core.ExtensionTypeHeader;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.ArrayBufferOutput;
import org.msgpack.jackson.dataformat.ExtensionTypeCustomDeserializers;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;
import org.msgpack.jackson.dataformat.MessagePackExtensionType.Serializer;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.value.ValueType;

import io.setl.bc.pychain.msgpack.SetlExtensions;

/**
 * Helper class for working with Message Pack encoding via Jackson.
 *
 * @author Simon Greatrix on 2019-05-29.
 */
public class MsgPack {

  private static final JsonSerializer<MessagePackExtensionType> EXTENSION_TYPE_JSON_SERIALIZER = new Serializer();

  private static ObjectMapper OBJECT_MAPPER;



  static class BigDecIn extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonToken token = p.currentToken();
      // Standard numeric value can be converted directly to a BigDecimal
      if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
        return p.getDecimalValue().stripTrailingZeros();
      }

      // An embedded object could be a BigInteger or a BigDecimal
      if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
        Object o = p.getEmbeddedObject();
        if (o instanceof BigDecimal) {
          return (BigDecimal) o;
        }
        if (o instanceof BigInteger) {
          return new BigDecimal((BigInteger) o).stripTrailingZeros();
        }

        // Unknown embedded object
        throw new JsonParseException(p, "Expected BigDecimal, but found " + o.getClass() + ": " + o);
      }

      // unknown type
      throw new JsonParseException(p, "Cannot recover Number");
    }


    @Override
    public Class<BigDecimal> handledType() {
      return BigDecimal.class;
    }


    @Override
    public boolean isCachable() {
      return true;
    }

  }



  /**
   * Serializer for BigDecimals in Message Pack.
   */
  static class BigDecOut extends JsonSerializer<BigDecimal> {

    @Override
    public Class<BigDecimal> handledType() {
      return BigDecimal.class;
    }


    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      // Special handling for zero
      if (value.signum() == 0) {
        gen.writeNumber(0);
        return;
      }

      // Message pack packs float and double as is, even if the value represented is a whole number which could be represented in less bytes. We honour that
      // convention here and do not test if the value is really a whole number, nor if it can be exactly represented as a float or double.
      value = value.stripTrailingZeros();
      int scale = value.scale();
      BigInteger unscaled = value.unscaledValue();

      // Encode as a BigDecimal extension. Create a buffer which is big enough
      ArrayBufferOutput bufferOutput = new ArrayBufferOutput(16 + unscaled.bitLength() / 8);
      try (MessagePacker temp = MessagePack.newDefaultPacker(bufferOutput)) {
        temp.packInt(scale);

        if (unscaled.bitLength() <= 63) {
          temp.packLong(unscaled.longValue());
        } else if (unscaled.bitLength() == 64 && value.signum() == 1) {
          // Can be packed as UINT64
          temp.packBigInteger(unscaled);
        } else {
          byte[] bigBytes = unscaled.toByteArray();
          temp.packExtensionTypeHeader(SetlExtensions.EXT_BIGINTEGER, bigBytes.length);
          temp.addPayload(bigBytes);
        }

        temp.flush();
      }
      bufferOutput.flush();
      byte[] buffer = bufferOutput.toByteArray();
      MessagePackExtensionType extensionType = new MessagePackExtensionType(SetlExtensions.EXT_BIGDECIMAL, buffer);
      EXTENSION_TYPE_JSON_SERIALIZER.serialize(extensionType, gen, serializers);
    }

  }



  static class BigIntIn extends JsonDeserializer<BigInteger> {

    @Override
    public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonToken token = p.currentToken();
      if (token == JsonToken.VALUE_NUMBER_INT) {
        return p.getBigIntegerValue();
      }
      if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
        Object o = p.getEmbeddedObject();
        if (o instanceof BigInteger) {
          return (BigInteger) o;
        }
        throw new JsonParseException(p, "Expected BigInteger, but found " + o.getClass() + ": " + o);
      }
      throw new JsonParseException(p, "Cannot recover Number");
    }


    @Override
    public Class<BigInteger> handledType() {
      return BigInteger.class;
    }


    @Override
    public boolean isCachable() {
      return true;
    }

  }



  /**
   * Serialiazer for BigIntegers in Message Pack.
   */
  static class BigIntOut extends JsonSerializer<BigInteger> {

    @Override
    public Class<BigInteger> handledType() {
      return BigInteger.class;
    }


    @Override
    public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      // Message pack packs whole numbers into their shortest representation - a convention we honour here.
      if (value.bitLength() <= 63) {
        gen.writeNumber(value.longValue());
        return;
      } else if (value.bitLength() == 64 && value.signum() == 1) {
        // Can be packed as UINT64
        gen.writeNumber(value);
        return;
      }

      byte[] bigBytes = value.toByteArray();
      MessagePackExtensionType extensionType = new MessagePackExtensionType(SetlExtensions.EXT_BIGINTEGER, bigBytes);
      EXTENSION_TYPE_JSON_SERIALIZER.serialize(extensionType, gen, serializers);
    }

  }



  static class NumberIn extends JsonDeserializer<Number> {

    @Override
    public Number deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonToken token = p.currentToken();
      if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
        return p.getNumberValue();
      }
      if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
        Object o = p.getEmbeddedObject();
        if (o instanceof Number) {
          return (Number) o;
        }
        throw new JsonParseException(p, "Expected Number, but found " + o.getClass() + ": " + o);
      }
      throw new JsonParseException(p, "Cannot recover Number");
    }

  }


  public static <T> T convert(Object input, Class<T> type) {
    // As far as I know, this is thread safe.
    return OBJECT_MAPPER.convertValue(input, type);
  }


  public static ObjectMapper copy() {
    return OBJECT_MAPPER.copy();
  }


  public static ObjectReader reader(Class<?> type) {
    return OBJECT_MAPPER.readerFor(type);
  }


  public static void setObjectMapper(ObjectMapper newMapper) {
    OBJECT_MAPPER = newMapper;
  }


  public static ObjectWriter writer() {
    return OBJECT_MAPPER.writer();
  }

  static {
    MessagePackFactory factory = new MessagePackFactory();
    ExtensionTypeCustomDeserializers desers = new ExtensionTypeCustomDeserializers();

    desers.addCustomDeser(SetlExtensions.EXT_BIGINTEGER, BigInteger::new);
    desers.addCustomDeser(SetlExtensions.EXT_BIGDECIMAL, byteArray -> {
      MessageUnpacker temp = MessagePack.newDefaultUnpacker(byteArray);
      int scale = temp.unpackInt();

      BigInteger unscaledBI;
      ValueType type = temp.getNextFormat().getValueType();
      if (type == ValueType.INTEGER) {
        unscaledBI = temp.unpackBigInteger();
      } else {
        ExtensionTypeHeader header = temp.unpackExtensionTypeHeader();
        assert header.getType() == SetlExtensions.EXT_BIGINTEGER;
        unscaledBI = new BigInteger(temp.readPayload(header.getLength()));
      }
      return new BigDecimal(unscaledBI, scale);
    });
    factory.setExtTypeCustomDesers(desers);

    SimpleModule module = new SimpleModule();
    module.addSerializer(BigInteger.class, new BigIntOut());
    module.addDeserializer(BigInteger.class, new BigIntIn());

    module.addSerializer(BigDecimal.class, new BigDecOut());
    module.addDeserializer(BigDecimal.class, new BigDecIn());

    module.addDeserializer(Number.class, new NumberIn());

    ObjectMapper mapper = new ObjectMapper(factory);
    mapper.registerModule(module);

    // The "Afterburner" module is an official Jackson extension which uses dynamic bytecode generation to reduce over-head in data binding.
    // Following things are optimized:
    //
    // For serialization (POJOs to JSON):
    // Accessors for "getting" values (field access, calling getter method) are inlined using generated code instead of reflection
    // Serializers for small number of 'primitive' types (int, long, String) are replaced with direct calls, instead of getting delegated to JsonSerializers
    //
    // For deserialization (JSON to POJOs):
    // Calls to default (no-argument) constructors are byte-generated instead of using reflection
    // Mutators for "setting" values (field access, calling setter method) are inlined using generated code instead of reflection
    // Deserializers for small number of 'primitive' types (int, long, String) are replaced with direct calls, instead of getting delegated to JsonDeserializers
    AfterburnerModule afterburnerModule = new AfterburnerModule().setUseValueClassLoader(false);
    mapper.registerModule(afterburnerModule);

    OBJECT_MAPPER = mapper;
  }

}
