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

import static io.setl.common.StringUtils.logSafe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Function;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator.Feature;
import com.fasterxml.jackson.dataformat.smile.SmileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.crypto.provider.SetlProvider;
import io.setl.json.jackson.JacksonGenerator;
import io.setl.json.jackson.JacksonReader;
import io.setl.util.RuntimeIOException;

/**
 * @author Simon Greatrix on 18/02/2020.
 */
public class SmileHelper {

  private static final SmileFactory FACTORY;

  private static final Logger logger = LoggerFactory.getLogger(SmileHelper.class);


  /**
   * Encode a JSON value using the Smile data interchange format.
   *
   * @param value the value to encode
   *
   * @return the encoded value
   */
  public static byte[] createSmile(JsonValue value) {
    return createSmile(value, false);
  }


  /**
   * Encode a JSON value using the Smile data interchange format.
   *
   * @param value    the value to encode
   * @param withSalt if true, include a random salt
   *
   * @return the encoded value
   */
  public static byte[] createSmile(JsonValue value, boolean withSalt) {
    if (value == null) {
      value = JsonValue.NULL;
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      // For privacy and GDPR we can include a salt with the JSON so that the hash of said JSON cannot be mapped to a well-known value
      if (withSalt) {
        // Salt header
        outputStream.write('#');
        outputStream.write('#');
        outputStream.write('\n');

        // for now, the salt is always 32 bytes (256 bits)
        outputStream.write(32);
        byte[] salt = new byte[32];
        SetlProvider.getSecureRandom().nextBytes(salt);
        outputStream.write(salt);
      }

      SmileGenerator smileGenerator = FACTORY.createGenerator(outputStream);
      JacksonGenerator jacksonGenerator = new JacksonGenerator(smileGenerator);
      jacksonGenerator.generate(value);
      jacksonGenerator.close();
    } catch (IOException e) {
      // This should never happen. There is no I/O and Smile should handle any JSON value.
      logger.error("Unexpected error encoding JSON as SMILE:\n{}", logSafe(value.toString()));
      throw new RuntimeIOException(e);
    }
    return outputStream.toByteArray();
  }


  private static <T extends JsonValue> T doRead(byte[] data, Function<JacksonReader, T> reader) {
    if (data.length < 3) {
      throw new RuntimeIOException(new IOException("Invalid SMILE data - bad header"));
    }

    // Smile data normally starts with a ":)\n"
    int offset = 0;
    if (data[0] == '#' && data[1] == '#' && data[2] == '\n') {
      // Not a smile header but a hash, so skip the length of the hash
      offset = data[3] + 4;
    }

    try (SmileParser smileParser = FACTORY.createParser(data, offset, data.length - offset)) {
      JacksonReader jacksonParser = new JacksonReader(smileParser);
      T output = reader.apply(jacksonParser);
      jacksonParser.close();
      return output;
    } catch (IOException e) {
      // This should only happen if the data is corrupt.
      logger.error("Invalid SMILE data: {}", Base64.getUrlEncoder().encodeToString(data));
      throw new RuntimeIOException(e);
    }
  }


  public static JsonStructure readSmile(byte[] data) {
    return doRead(data, JacksonReader::read);
  }


  public static JsonArray readSmileArray(byte[] data) {
    return doRead(data, JacksonReader::readArray);
  }


  public static JsonObject readSmileObject(byte[] data) {
    return doRead(data, JacksonReader::readObject);
  }


  public static JsonValue readSmileValue(byte[] data) {
    return doRead(data, JacksonReader::readValue);
  }


  static {
    SmileFactory smileFactory = new SmileFactory();
    smileFactory.enable(Feature.CHECK_SHARED_STRING_VALUES);
    FACTORY = smileFactory;
  }

}
