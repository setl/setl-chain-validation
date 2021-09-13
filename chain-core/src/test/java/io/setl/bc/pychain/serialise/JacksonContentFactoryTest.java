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
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class JacksonContentFactoryTest {

  @Test
  public void test() {
    final String input = "Hello, World!";
    JacksonContentFactory<String> factory = new JacksonContentFactory<>(String.class);
    Content content = factory.asContent(Digest.TYPE_SHA_256, input);
    String text = factory.asValue(content.getData());
    assertEquals(input, text);
    assertNull(factory.asValue(null));
  }

}