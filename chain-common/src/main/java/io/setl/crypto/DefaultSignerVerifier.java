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

import io.setl.crypto.provider.SetlProvider;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;

/**
 * Produce and verify digital signatures.
 *
 * <p>For FIPS compliance we must follow the requirements set out in FIPS 186-4. This states:</p>
 * <blockquote>
 * <p>An approved hash function, as specified in FIPS 180, shall be used during the generation of
 * digital signatures. The security strength associated with the DSA digital signature process is no
 * greater than the minimum of the security strength of the [key] pair and the security strength of
 * the hash function that is employed. Both the security strength of the hash function used and the
 * security strength of the [key] pair shall meet or exceed the security strength required for the
 * digital signature process. The security strength for each [key] pair and hash function is provided
 * in SP 800-57.</p>
 * <p>...</p>
 * <p>It is recommended that the security strength of the [key] pair and the security strength of the
 * hash function used for the generation of digital signatures be the same unless an agreement has
 * been made between participating entities to use a stronger hash function. </p>
 * </blockquote>
 *
 * <p>To this end, where a choice of hash functions is available, the hash algorithm will be assumed to be the minimum
 * strength algorithm that meets or exceeds the bit-strength of the key-pair. The following table combines Table 2 and
 * Table 3 from NIST SP 800-57 (fourth revision).</p>
 *
 * <table border="1">
 * <caption></caption>
 * <tr>
 * <th>Security<br>Strength</th>
 * <th>Symmetric<br>key<br>algorithm</th>
 * <th>DSA/RSA</th>
 * <th>ECDSA</th>
 * <th>Digest</th>
 * </tr>
 * <tr>
 * <td>&lt;=80</td>
 * <td>2TDEA</td>
 * <td>1024</td>
 * <td>160-223</td>
 * <td>SHA-1</td>
 * </tr>
 * <tr>
 * <td>112</td>
 * <td>3TDEA</td>
 * <td>2048</td>
 * <td>224-255</td>
 * <td>SHA-224</td>
 * </tr>
 * <tr>
 * <td>128</td>
 * <td>AES-128</td>
 * <td>3072</td>
 * <td>256-383</td>
 * <td>SHA-256</td>
 * </tr>
 * <tr>
 * <td>192</td>
 * <td>AES-192</td>
 * <td>7680</td>
 * <td>384-511</td>
 * <td>SHA-384</td>
 * </tr>
 * <tr>
 * <td>256</td>
 * <td>AES-256</td>
 * <td>15360</td>
 * <td>&gt;=512</td>
 * <td>SHA-512</td>
 * </tr>
 * </table>
 *
 * <p>NIST permit both SHA2 and SHA3, but we will use the faster SHA2 algorithms.</p>
 *
 * @author Simon Greatrix on 01/12/2017.
 */
public class DefaultSignerVerifier implements MessageSignerVerifier {

  private static String algorithmForEC(int size) {
    if (size < 224) {
      return "SHA1withECDSA";
    } else if (size < 256) {
      // NB: Not in the list of JCE standard names
      return "SHA224withECDSA";
    } else if (size < 384) {
      return "SHA256withECDSA";
    } else if (size < 512) {
      return "SHA384withECDSA";
    }
    return "SHA512withECDSA";
  }


  private static String algorithmForRSA(int size) {
    if (size < 2048) {
      return "SHA1withRSA";
    } else if (size < 3072) {
      // NB: Not in the list of JCE standard names
      return "SHA224withRSA";
    } else if (size < 7680) {
      return "SHA256withRSA";
    } else if (size < 15360) {
      return "SHA384withRSA";
    }
    return "SHA512withRSA";
  }


  private static String getAlgorithm(PrivateKey key) {
    if ("Ed25519".equals(key.getAlgorithm())) {
      return "EdDSA";
    }

    if (key instanceof ECPrivateKey) {
      ECPrivateKey myKey = (ECPrivateKey) key;
      int size = myKey.getParams().getCurve().getField().getFieldSize();
      return algorithmForEC(size);
    }

    if (key instanceof RSAKey) {
      int size = ((RSAKey) key).getModulus().bitLength();
      return algorithmForRSA(size);
    }

    throw new IllegalArgumentException("Unknown key type: " + key.getAlgorithm() + " (" + key.getClass() + ")");
  }


  private static String getAlgorithm(PublicKey key) {
    if ("Ed25519".equals(key.getAlgorithm())) {
      return "EdDSA";
    }

    if (key instanceof ECPublicKey) {
      ECPublicKey myKey = (ECPublicKey) key;
      int size = myKey.getParams().getCurve().getField().getFieldSize();
      return algorithmForEC(size);
    }

    if (key instanceof RSAKey) {
      int size = ((RSAKey) key).getModulus().bitLength();
      return algorithmForRSA(size);
    }

    throw new IllegalArgumentException("Unknown key type: " + key.getAlgorithm() + " (" + key.getClass() + ")");
  }


  static {
    // Verify our security providers are properly installed.
    SetlProvider.install();
  }


  @Override
  public byte[] createSignature(byte[] message, PrivateKey privateKey) {
    String alg = getAlgorithm(privateKey);
    try {
      Signature signer = Signature.getInstance(alg);
      signer.initSign(privateKey);
      signer.update(message);
      return signer.sign();
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      throw new AssertionError("Failure of cryptographic primitives", e);
    }
  }


  @Override
  public byte[] createSignature(byte[] message, byte[] privateKey) {
    return createSignature(message, KeyGen.getPrivateKey(privateKey));
  }


  @Override
  public boolean verifySignature(byte[] message, String hexPublicKey, byte[] signature) {

    PublicKey publicKey;

    try {
      publicKey = KeyGen.getPublicKey(hexPublicKey);
    } catch (IllegalArgumentException e) {
      return false;
    }

    return verifySignature(message, publicKey, signature);
  }


  @Override
  public boolean verifySignature(byte[] message, PublicKey publicKey, byte[] signature) {
    if (publicKey == null || signature == null) {
      return false;
    }
    String alg = getAlgorithm(publicKey);
    try {
      Signature signer = Signature.getInstance(alg);
      signer.initVerify(publicKey);
      signer.update(message);
      return signer.verify(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new AssertionError("Failure of cryptographic primitives", e);
    } catch (SignatureException e) {
      // Signature bytes are not appropriate to the key algorithm, so definitely does not verify.
      return false;
    }
  }
}
