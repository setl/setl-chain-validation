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
package io.setl.crypto;

import java.io.IOException;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for PEM files.
 *
 * <p>Can read and write PEM files for public keys, certificates, private keys and encrypted private keys.</p>
 *
 * <p>The PEM format is defined in RFC7468, but many generators pre-date this standard. We accept the "standard" format, and output the "strict" format. We do
 * not accept the "lax" format, nor deprecated formats that incorporate encapsulated headers.</p>
 *
 * @author Simon Greatrix on 10/11/2019.
 */
public class PEMReader extends PEM {

  private static final Logger logger = LoggerFactory.getLogger(PEMReader.class);

  private final Reader reader;

  private boolean lastWasCR = false;


  public PEMReader(Reader reader) {
    this.reader = reader;
  }


  /**
   * Read a character, standardising line endings to '\n'.
   *
   * @return the character, or -1.
   */
  private int readCharacter() throws IOException {
    int r = reader.read();

    if (r == '\r') {
      lastWasCR = true;
      // Standardise to \n
      return '\n';
    }

    if (lastWasCR) {
      lastWasCR = false;
      if (r == '\n') {
        // Actually \r\n so skip the \n
        return readCharacter();
      }
    }

    return r;
  }


  @Override
  protected String readLine() throws IOException {
    StringBuilder builder = new StringBuilder(200);
    while (true) {
      int r = readCharacter();
      if (r == -1) {
        if (builder.length() == 0) {
          logger.trace("Read reached EOF after {} lines", lineNumber);
          return null;
        } else {
          lineNumber++;
          logger.trace("Read final line {}", lineNumber);
          return builder.toString();
        }
      }
      if (r == '\n') {
        lineNumber++;
        logger.trace("Read line {}", lineNumber);
        return builder.toString();
      }
      builder.append((char) r);
    }
  }

}
