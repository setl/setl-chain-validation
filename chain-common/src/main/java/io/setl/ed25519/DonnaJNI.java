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
package io.setl.ed25519;

import java.security.InvalidKeyException;

public class DonnaJNI {


  static {
    System.loadLibrary("ed25519-donna-2.0");
  }


  /**
   * Create a public key for a given private key.
   *
   * @param privateKey the seed bytes of the private key
   */
  public native byte[] createPublicKey(byte[] privateKey);


  /**
   * Sign a message.
   *
   * @param message   the message
   * @param publicKey the private key
   *
   * @return the 64-byte signature.
   */
  public native byte[] createSignature(byte[] message, byte[] privateKey, byte[] publicKey);


  /**
   * Prepare a public key for fast signing.
   *
   * @param publicKey the public key
   *
   * @return the prepared key
   */
  public native byte[] prepareKey(byte[] publicKey) throws InvalidKeyException;


  public native void testCryptoLibrary();


  /**
   * Verify a signature.
   *
   * @param message   the message
   * @param publicKey the public key
   * @param signature the 64-byte signature
   */
  public native boolean verifySignature(byte[] message, byte[] publicKey, byte[] signature);


  /**
   * Verify a signature with a prepared key.
   *
   * @param message           the message
   * @param preparedPublicKey the prepared public key
   * @param signature         the signature to validate
   */
  public native boolean verifyWithPreparedKey(byte[] message, byte[] preparedPublicKey, byte[] signature);
}

