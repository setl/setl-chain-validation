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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {
  
  // Private constructor - prevents instantiation (sonarcube)
  private SHA256() {}
  
  /**
   * sha256().
   * Return Hash for given byte array.
   *
   * @param input : Source byte array
   * @return :      byte array containing message digest (hash)
   * @throws NoSuchAlgorithmException :
   */
  public static byte[] sha256(byte[] input) {
    
    // TODO Performanvce check vs cached stack of persistent Instances.
    try {
      MessageDigest thisDigest = MessageDigest.getInstance("SHA-256"); // TX.getSHA256Digest();

      // Ensure no existing state (necessary if calling getInstance() ?
      thisDigest.reset();
      return thisDigest.digest(input);
    } catch ( NoSuchAlgorithmException e ) {
      // SHA-256 is required to be present in all JVMs.
      throw new InternalError("SHA-256 is required");
    }
  }
}
