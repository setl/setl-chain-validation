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
package io.setl.bc.pychain.serialise.hash;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashableObjectArray;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Sha256Hash;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import org.msgpack.core.MessageBufferPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashSerialisation implements SerialiseToByte {

  private static HashSerialisation instance;

  private static Logger logger = LoggerFactory.getLogger(HashSerialisation.class);


  /**
   * Get the shared instance of the hash serializer.
   *
   * @return the shared instance
   */
  public static HashSerialisation getInstance() {
    if (instance == null) {
      instance = new HashSerialisation();
    }
    return instance;
  }


  /**
   * Calculate the hash of a message packable object.
   *
   * @param packable the object
   *
   * @return the hash of the object
   */
  public Hash hash(MsgPackable packable) {
    MessageBufferPacker packer = MsgPackUtil.newBufferPacker();
    try {
      packable.pack(packer);
      byte[] bytes = Sha256Hash.newDigest().digest(packer.toByteArray());
      packer.close();
      return new Hash(bytes);
    } catch (IOException ioe) {
      // Should never happen as we are doing memory buffering
      throw new AssertionError("Unexpected I/O exception occurred", ioe);
    } catch (Exception e) {
      // TODO: remove this catch when pack() stops throwing Exception
      // Should never happen and can be removed at some point
      throw new AssertionError("Unexpected exception occurred", e);
    }
  }


  /**
   * Hash an object which provides a hashable object array definition.
   *
   * @param array the object which provides the array
   *
   * @return the hash of the array
   */
  public Hash hash(HashableObjectArray array) {
    Object[] objects = array.getHashableObject();
    byte[] bytes = serialise(objects);

    MessageDigest digest = Sha256Hash.newDigest();
    byte[] hash = digest.digest(bytes);
    return new Hash(hash);
  }


  @Override
  public byte[] serialise(Object o) {
    byte[] output = MsgPackUtil.pack(o);
    if (logger.isTraceEnabled()) {
      if (o instanceof Object[]) {
        logger.trace("Input: {}", Arrays.deepToString((Object[]) o));
      } else {
        logger.trace("Input: {}", o);
      }
      logger.trace("Hash serialization: {}", Base64.getMimeEncoder().encodeToString(output));
    }
    return output;
  }
}
