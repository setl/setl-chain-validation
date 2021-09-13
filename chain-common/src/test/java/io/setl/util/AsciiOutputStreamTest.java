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
package io.setl.util;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * @author Simon Greatrix on 22/08/2018.
 */
public class AsciiOutputStreamTest {

  AsciiOutputStream ascii = new AsciiOutputStream(64);


  @Test
  public void close() {
    // close does nothing
    ascii.close();
    ascii.write(64);
    assertEquals("@", ascii.toString());
  }


  @Test
  public void flush() {
    // flush does nothing
    ascii.write(64);
    ascii.flush();
    assertEquals("@", ascii.toString());
  }


  @Test
  public void write() {
    ascii.write(64);
    ascii.write(64 + 128);
    assertEquals("@@", ascii.toString());
  }


  @Test
  public void write1() {
    // The '£' symbol will be converted to "B#" by UTF-8 and 7-bit ASCII
    ascii.write("It's over £10000!".getBytes(StandardCharsets.UTF_8));
    assertEquals("It's over B#10000!", ascii.toString());
  }
}