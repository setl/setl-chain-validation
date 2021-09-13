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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class ContentTest {

  @Test
  public void test() {
    Content content = new Content(Digest.create(Digest.TYPE_SHA_256), new byte[0]);
    assertEquals(Digest.TYPE_SHA_256, content.getDigestType());
    assertEquals(Hash.fromHex("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), content.getKey());
    assertArrayEquals(new byte[0], content.getData());
  }

}