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
package io.setl.crypt.webcipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

public class AesMD5CipherTest {

  private static final String ENCRYPTED = "U2FsdGVkX1+Ur/2cu3hyKZ8j4o7e7XOhxBA3TpOyJA0=";

  private static final String MESSAGE = "A test message";

  private static final String SHARED = "aa859bc16333e030c307490cd3f4471662c5dc7775877e875e2c0809a927e730";

  WebCipher c = new AesMD5Base64Cipher();


  @Test
  public void badMessages() throws Exception {
    // Verify rejection of messages that are too short.
    try {
      c.decrypt("AAAA", SHARED.getBytes(StandardCharsets.US_ASCII));
      fail();
    } catch (IllegalArgumentException e) {
      // success
      assertEquals("Message too short", e.getMessage());
    }

    // Verify rejection of messages that do not start with the magic number.
    try {
      c.decrypt("AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHIIIIJJJJKKKKLLLLMMMMNNNN", SHARED.getBytes(StandardCharsets.US_ASCII));
      fail();
    } catch (IllegalArgumentException e) {
      // success
      assertEquals("Bad magic number", e.getMessage());
    }

    // Verify rejection of messages that do not have correct padding.
    // Create incorrectly padded message.
    byte[] bytes = Base64.getDecoder().decode(ENCRYPTED.getBytes(StandardCharsets.US_ASCII));
    bytes[bytes.length - 1]++;
    String broken = Base64.getEncoder().encodeToString(bytes);
    try {
      c.decrypt(broken, SHARED.getBytes(StandardCharsets.US_ASCII));
      fail();
    } catch (IllegalArgumentException e) {
      // success
      assertEquals("Input data was not properly encrypted.", e.getMessage());
    }
  }


  @Test
  public void decrypt() throws Exception {

    String decrypted = c.decrypt(ENCRYPTED, SHARED.getBytes(StandardCharsets.US_ASCII));
    assertTrue("Decrypt error", MESSAGE.equals(decrypted));
  }


  @Test
  public void encrypt() throws Exception {

    String encrypted = c.encrypt(MESSAGE, SHARED.getBytes(StandardCharsets.US_ASCII));
    String decrypted = c.decrypt(encrypted, SHARED.getBytes(StandardCharsets.US_ASCII));
    assertTrue("Encrypt/Decrypt error", MESSAGE.equals(decrypted));
  }
}