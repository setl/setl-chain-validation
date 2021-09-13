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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.crypto.provider.SetlProvider;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Decoder;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cipher compatible with python wallet node.
 */
// The AES/CBC/PKCS5Padding cipher is required, despite its problems.
// There is no static IV in this class.
// MD5 is required - and actually not a bad choice in this instance.
@SuppressWarnings("squid:S4432") // Cipher mode, see comment below.
@SuppressFBWarnings({"CIPHER_INTEGRITY", "PADDING_ORACLE", "STATIC_IV", "WEAK_MESSAGE_DIGEST_MD5"})
public class AesMD5Base64Cipher implements WebCipher {

  /**
   * Secure source of salt bytes.
   */
  private static final SecureRandom RANDOM;

  /**
   * The encrypted message is prefixes with the US-ASCII bytes for "Salted__", and then an 8 byte random salt. The salt is combined with the shared secret to
   * produce a message specific cipher key.
   */
  private static final byte[] magic = "Salted__".getBytes(StandardCharsets.US_ASCII);


  /**
   * Prepare a cipher instance to encrypt a message.
   *
   * @param sharedSecret shared secret used to derive the cipher key
   * @param message contains a salt which is used to derive the cipher key
   * @param mode either Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
   *
   * @return an initialised cipher
   */
  private static Cipher prepareCypher(byte[] sharedSecret, byte[] message, int mode) {

    final MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError("Required digest algorithm 'MD5' is not available.", e);
    }

    // This duplicates OpenSSL's bespoke key generation function. To generate the key, the secret and salt are hashed to produce the initial bytes. Subsequent
    // bytes are produced by hashing the previous hash with the secret and the salt. We require an 256-bit AES key and a 128-bit salt, for a total of 384 bits
    // (48 bytes).
    //
    // MD5 produces a 128 bit (16 byte) hash, so we need to invoke the hash function 3 times to get the required number of bytes.

    // Execute the key generation function.
    byte[] keyAndIv = new byte[48];
    for (int i = 0; i < 3; i++) {
      if (i > 0) {
        // include previous hash except on first block
        md.update(keyAndIv, (i - 1) * 16, 16);
      }
      md.update(sharedSecret);
      md.update(message, 8, 8);
      try {
        md.digest(keyAndIv, i * 16, 16);
      } catch (DigestException e) {
        throw new AssertionError("Unexpected failure of message digest.", e);
      }
    }

    // now create symmetric key and IV from the digest output.
    final SecretKeySpec key = new SecretKeySpec(keyAndIv, 0, 32, "AES");
    final IvParameterSpec iv = new IvParameterSpec(keyAndIv, 32, 16);

    // instantiate and activate the cipher
    final Cipher cipher;
    try {
      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(mode, key, iv);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new AssertionError("A required cryptographic primitive was unavailable.", e);
    } catch (InvalidAlgorithmParameterException e) {
      throw new AssertionError("AES/CBC refused to accept an 8 byte IV. Java is broken.", e);
    } catch (InvalidKeyException e) {
      throw new AssertionError(
          "AES/CBC refused to accept a 256-bit key. Check unlimited cryptographic strength has been enabled for Java.",
          e);
    }
    return cipher;

  }


  static {
    RANDOM = SetlProvider.getSecureRandom();
  }


  /**
   * Decrypt a Base 64 encoded messages using the shared secret key.
   *
   * @param message the message
   * @param sharedKey the shared key
   *
   * @return the decrypted text
   */
  public String decrypt(String message, byte[] sharedKey) {

    final Decoder decoder = Base64.getMimeDecoder();
    final byte[] inBytes = decoder.decode(message.replace("\n", ""));

    // Verify message contains at least the magic number, the salt, and one padded block.
    if (inBytes.length < 32) {
      throw new IllegalArgumentException("Message too short");
    }
    // verify the input starts with the required magic prefix
    for (int i = 0; i < 8; i++) {
      if (inBytes[i] != magic[i]) {
        throw new IllegalArgumentException("Bad magic number");
      }
    }

    final Cipher cipher;
    try {
      cipher = prepareCypher(sharedKey, inBytes, Cipher.DECRYPT_MODE);
      final byte[] clear = cipher.doFinal(inBytes, 16, inBytes.length - 16);
      return new String(clear, StandardCharsets.UTF_8);
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      // Input was not encrypted properly.
      throw new IllegalArgumentException("Input data was not properly encrypted.", e);
    }
  }


  /**
   * Encrypt a message using the shared secret key.
   *
   * @param message the message
   * @param sharedKey the secret key
   *
   * @return the Base-64 encoded encrypted message.
   */
  public String encrypt(String message, byte[] sharedKey) {
    // Create an 8 byte salt that will be combined with the shared secret to generate a message specific cipher key.
    byte[] salt = new byte[8];
    RANDOM.nextBytes(salt);

    // create output byte array
    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

    // The output message consists of the 8 byte magic number, the 8 byte salt, and then the PCKS5 padded AES.
    byte[] output = new byte[16 + ((messageBytes.length + 16) & ~15)];
    System.arraycopy(magic, 0, output, 0, 8);
    System.arraycopy(salt, 0, output, 8, 8);

    try {
      final Cipher cipher = prepareCypher(sharedKey, output, Cipher.ENCRYPT_MODE);
      cipher.doFinal(messageBytes, 0, messageBytes.length, output, 16);
      return Base64.getEncoder().encodeToString(output);
    } catch (ShortBufferException e) {
      throw new AssertionError("Calculated buffer size was incorrect.");
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      throw new AssertionError("Decrypt only exception occurred during encryption.");
    }
  }
}
