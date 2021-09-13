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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.setl.bc.pychain.Digest;
import io.setl.bc.serialise.SafeMsgPackable;
import java.io.IOException;
import java.util.Objects;
import org.junit.Test;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class MsgPackableFactoryTest {

  static class PackedString implements SafeMsgPackable {

    private final String value;


    public PackedString(String value) {
      this.value = value;
    }


    public PackedString(MessageUnpacker unpacker) throws IOException {
      value = unpacker.unpackString();
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PackedString)) {
        return false;
      }
      PackedString that = (PackedString) o;
      return Objects.equals(value, that.value);
    }


    @Override
    public int hashCode() {
      return Objects.hash(value);
    }


    @Override
    public void pack(MessagePacker p) throws IOException {
      p.packString(value);
    }
  }


  @Test
  public void test() {
    final PackedString input = new PackedString("Hello, World!");
    MsgPackableFactory<PackedString> factory = new MsgPackableFactory<>(PackedString.class);
    Content content = factory.asContent(Digest.TYPE_SHA_256, input);
    PackedString text = factory.asValue(content.getData());
    assertEquals(input, text);
    assertNull(factory.asValue(null));
  }

}