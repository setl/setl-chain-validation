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
package io.setl.bc.pychain.state.entry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectWriter;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.serialise.MsgPack;
import java.io.IOException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-10.
 */
public class HashWithTypeTest {

  @Test
  public void testMessagePack() throws IOException {
    Digest digest = Digest.create(Digest.TYPE_SHA_512_256);
    Hash hash = digest.digest("Hello World".getBytes(UTF_8));
    HashWithType value = new HashWithType(hash, Digest.TYPE_SHA_512_256);

    // pack with Jackson
    ObjectWriter writer = MsgPack.writer();
    byte[] bytes = writer.writeValueAsBytes(value);

    // unpack with Jackson
    HashWithType hash3 = MsgPack.reader(HashWithType.class).readValue(bytes);
    assertTrue(hash3 instanceof HashWithType);
    assertEquals(value, hash3);
  }
}