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
package io.setl.bc.json.data;

import java.security.SecureRandom;

import io.setl.crypto.provider.SetlProvider;

/**
 * Create salt strings. A JSON salt uses characters that only require one byte to record in a UTF-8 encoded JSON string. This is everything from SPACE (0x20)
 * to TILDE (0x7e) except double quote, reverse solidus, and, for ease of use, DELETE (0x7f).
 *
 * @author Simon Greatrix on 29/06/2020.
 */
public class JsonSalt {

  private static final char[] ALPHABET = new char[93];


  /**
   * Get a salt that can be stored using single bytes in an unescaped JSON string and contains at least 256 bits of entropy.
   *
   * @return a salt
   */
  public static String create() {
    // To get 256 bits of entropy, we need 40 characters as each character gives us 6.539 bits
    SecureRandom random = SetlProvider.getSecureRandom();
    char[] output = new char[40];
    long v = 0;
    for (int i = 0; i < 40; i++) {
      if (i % 8 == 0) {
        // We grab 63 bits and use them in batches of 8*6.539 = 52.313.
        v = random.nextLong() & 0x7fff_ffff_ffff_ffffL;
      } else {
        v /= 93;
      }

      output[i] = ALPHABET[(int) (v % 93)];
    }

    return new String(output);
  }


  static {
    int i = 0;
    for (char ch = ' '; ch <= '~'; ch++) {
      if (ch != '\\' && ch != '"') {
        ALPHABET[i] = ch;
        i++;
      }
    }
  }

}
