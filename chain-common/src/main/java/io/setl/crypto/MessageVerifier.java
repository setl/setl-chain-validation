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

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Verify a byte array and signature against a given public key.
 *
 * @author aanten
 */
public interface MessageVerifier {

  /**
   * Verify a signature.
   *
   * @param message      the message
   * @param hexPublicKey the binary representation of the public key in hex
   * @param signature    the signature
   *
   * @return true if verified
   */
  boolean verifySignature(byte[] message, String hexPublicKey, byte[] signature);


  /**
   * Verify a message's signature.
   *
   * @param message   the message which will be converted to UTF-8 and verified
   * @param publicKey the signer's public key
   * @param signature the alleged signature
   *
   * @return true if the signature is valid
   */
  default boolean verifySignature(String message, PublicKey publicKey, String signature) {
    return verifySignature(
        message.getBytes(StandardCharsets.UTF_8),
        publicKey,
        Base64.getDecoder().decode(signature));
  }


  boolean verifySignature(byte[] message, PublicKey publicKey, byte[] signature);


  /**
   * Verify a signature.
   *
   * @param message   the message
   * @param publicKey Hex encoded representation of the public key as appropriate to the underlying algorithm
   * @param signature the signature
   *
   * @return true if verified
   */
  default boolean verifySignature(String message, String publicKey, String signature) {
    return verifySignature(
        message.getBytes(StandardCharsets.UTF_8), publicKey, Base64.getDecoder().decode(signature)
    );
  }

}
