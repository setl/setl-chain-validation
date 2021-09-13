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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.OutputStream;

/**
 * An output stream where the data is known to be 7-bit ASCII and which is to be accumulated into a String.
 *
 * @author Simon Greatrix on 30/10/2017.
 */
@SuppressFBWarnings("COM_COPIED_OVERRIDDEN_METHOD")
public class AsciiOutputStream extends OutputStream {

  StringBuilder buffer;


  public AsciiOutputStream(int initialSize) {
    buffer = new StringBuilder(initialSize);
  }


  @Override
  public void close() {
    // do nothing
  }


  @Override
  public void flush() {
    // do nothing
  }


  @Override
  public String toString() {
    return buffer.toString();
  }


  @Override
  public void write(byte[] b, int off, int len) {
    int end = off + len;
    for (int i = off; i < end; i++) {
      buffer.append((char) (b[i] & 0x7f));
    }
  }


  @Override
  public void write(int b) {
    buffer.append((char) (b & 0x7f));
  }


  @Override
  public void write(byte[] b) {
    write(b, 0, b.length);
  }
}
