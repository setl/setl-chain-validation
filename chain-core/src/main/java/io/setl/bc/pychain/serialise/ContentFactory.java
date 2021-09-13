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

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.entry.HashWithType;
import io.setl.common.Hex;
import java.security.DigestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory which can convert values to addressable Content.
 *
 * @author Simon Greatrix on 2019-02-19.
 */
public interface ContentFactory<T> {

  static Logger logger = LoggerFactory.getLogger(ContentFactory.class);

  /**
   * Convert a value to addressable content using the specified digest type.
   *
   * @param digestType the digest type
   * @param value      the value to convert
   *
   * @return the Content
   */
  Content asContent(int digestType, T value);

  /**
   * Convert a serialized value, from a Content instance, back to the value.
   *
   * @param data the serialized value
   *
   * @return the deserialized value
   */
  T asValue(byte[] data);

  /**
   * Verify that the content matches the claimed digest. Note, the default implementation assumes the digest is calculated directly from the stored data.
   *
   * @param hash the digest
   * @param data the content
   *
   * @return the value
   */
  default T asVerifiedValue(HashWithType hash, byte[] data) throws DigestException {
    Hash actualHash = Digest.create(hash.getType()).digest(data);
    if (actualHash.equals(hash.getHash())) {
      return asValue(data);
    }
    if (logger.isErrorEnabled()) {
      logger.error("Hash mismatch detected\nExpected = {}\nActual   = {}", hash, actualHash);
      try {
        T value = asValue(data);
        logger.error("Invalid value\n:{}", value);
        byte[] asData = asContent(hash.getType(), value).getData();
        logger.error("\nInput : {}\nOutput: {}", Hex.encode(data), Hex.encode(asData));
      } catch (RuntimeException e) {
        logger.error("Invalid value was unreadable", e);
      }
    }
    throw new DigestException("Hash " + hash.getHash().toHexString() + " referred to object with digest " + actualHash.toHexString());
  }

  /**
   * Get the name against which this type is stored in state.
   *
   * @return the type's name
   */
  default String getName() {
    return getType().getName();
  }

  /**
   * Get the type which this factory converts.
   *
   * @return the type
   */
  Class<T> getType();
}
