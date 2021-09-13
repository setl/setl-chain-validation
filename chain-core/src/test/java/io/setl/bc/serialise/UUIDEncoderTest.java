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

import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.IOException;
import java.util.UUID;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageTypeException;
import org.msgpack.core.MessageUnpacker;

/**
 * @author Simon Greatrix on 2019-05-03.
 */
public class UUIDEncoderTest {

  @Test
  public void pack() throws IOException {
    UUID uuid = UUID.randomUUID();
    MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
    UUIDEncoder.pack(packer, uuid);
    byte[] packed = packer.toByteArray();
    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packed);
    UUID output = UUIDEncoder.unpack(unpacker);
    assertEquals(uuid, output);
  }


  @Test
  public void test() {
    UUID uuid = UUID.randomUUID();
    byte[] data = UUIDEncoder.encode(uuid);
    UUID output = UUIDEncoder.decode(data);
    assertEquals(uuid, output);
  }


  @Test(expected = IOException.class)
  public void unpackBad() throws IOException {
    // data is too short
    byte[] packed = MsgPackUtil.pack(new byte[10]);
    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packed);
    UUIDEncoder.unpack(unpacker);
  }


  @Test(expected = MessageTypeException.class)
  public void unpackBad2() throws IOException {
    // data is not an array
    byte[] packed = MsgPackUtil.pack(System.currentTimeMillis());
    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packed);
    UUIDEncoder.unpack(unpacker);
  }

}