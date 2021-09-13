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
package io.setl.bc.serialise;

import java.io.IOException;
import java.util.UUID;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

/**
 * Encode a UUID in 16 bytes. Bytes are stored in big-ending order.
 *
 * @author Simon Greatrix on 2019-05-03.
 */
public class UUIDEncoder {

  /**
   * Decode an UUID from its encoded form.
   *
   * @param data the encoded form
   *
   * @return the UUID
   */
  public static UUID decode(byte[] data) {
    long mostSignificant = toLong(data, 0);
    long lesastSignificant = toLong(data, 8);
    return new UUID(mostSignificant, lesastSignificant);
  }


  /**
   * Encode a UUID.
   *
   * @param uuid the UUID to encode
   *
   * @return the encoded datum
   */
  public static byte[] encode(UUID uuid) {
    byte[] data = new byte[16];
    toBytes(uuid.getMostSignificantBits(), data, 0);
    toBytes(uuid.getLeastSignificantBits(), data, 8);
    return data;
  }


  /**
   * Pack the UUID.
   *
   * @param packer the packer to pack with
   * @param uuid   the UUID to pack
   */
  public static void pack(MessagePacker packer, UUID uuid) throws IOException {
    byte[] data = encode(uuid);
    packer.packBinaryHeader(16);
    packer.addPayload(data);
  }


  private static void toBytes(long value, byte[] data, int offset) {
    int index = offset + 7;
    do {
      data[index] = (byte) (0xff & value);
      value >>>= 8;
      index--;
    }
    while (index >= offset);
  }


  private static long toLong(byte[] data, int offset) {
    long value = 0;
    int shift = 56;
    int index = offset;
    do {
      value |= (0xffL & data[index]) << shift;
      index++;
      shift -= 8;
    }
    while (shift >= 0);
    return value;
  }


  /**
   * Unpack a UUID.
   *
   * @param unpacker the UUID to read from
   *
   * @return the UUID
   */
  public static UUID unpack(MessageUnpacker unpacker) throws IOException {
    int len = unpacker.unpackBinaryHeader();
    if (len != 16) {
      throw new IOException("Invalid UUID binary format");
    }
    byte[] data = new byte[16];
    unpacker.readPayload(data);
    return decode(data);
  }
}
