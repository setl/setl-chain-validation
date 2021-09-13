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
package io.setl.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nonnull;

public class Sha256Hash {

  /**
   * Get an instance of the SHA-256 message digest.
   * @return a digest implementation
   */
  @Nonnull
  public static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e); // Can't happen.
    }
  }

  /**
   * Calculates the SHA-256 hash of the given bytes, and then hashes the resulting hash again.
   *
   * @param input the bytes to hash
   * @return the double-hash (in big-endian order)
   */
  public static byte[] hashTwice(byte[] input) {
    return hashTwice(input, 0, input.length);
  }

  /**
   * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash again.
   *
   * @param input the array containing the bytes to hash
   * @param offset the offset within the array of the bytes to hash
   * @param length the number of bytes to hash
   * @return the double-hash (in big-endian order)
   */
  public static byte[] hashTwice(byte[] input, int offset, int length) {
    MessageDigest digest = newDigest();
    digest.update(input, offset, length);
    return digest.digest(digest.digest());
  }

  /**
   * Calculates the hash of hash on the given byte ranges. This is equivalent to concatenating the
   * two ranges and then passing the result to {@link #hashTwice(byte[])}.
   */
  public static byte[] hashTwice(byte[] input1, int offset1, int length1, byte[] input2,
      int offset2, int length2) {
    MessageDigest digest = newDigest();
    digest.update(input1, offset1, length1);
    digest.update(input2, offset2, length2);
    return digest.digest(digest.digest());
  }

}
