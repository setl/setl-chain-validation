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
package io.setl.crypto.provider;

import io.setl.common.Hex;
import io.setl.ed25519.DonnaJNI;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

/**
 * @author Simon Greatrix on 2019-08-01.
 */
public class EdDsaPublicKey implements PublicKey {

  private static final AlgorithmIdentifier ED25519_ALGORITHM = new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519);

  private static final long serialVersionUID = 1L;


  /**
   * Create a Edwards Digital Signing key from a public key object.
   *
   * @param publicKey the public key object
   *
   * @return the EdDSA key
   *
   * @throws InvalidKeySpecException if the key cannot be converted
   */
  public static EdDsaPublicKey from(PublicKey publicKey) throws InvalidKeySpecException {
    if (publicKey instanceof EdDsaPublicKey) {
      return (EdDsaPublicKey) publicKey;
    }
    if (publicKey.getFormat().equals("X.509")) {
      X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
      SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(spec.getEncoded());
      return new EdDsaPublicKey(info);
    }
    throw new InvalidKeySpecException("Cannot convert " + publicKey + " to SETL format");
  }


  protected final transient SubjectPublicKeyInfo info;

  protected final byte[] keyBytes;

  private byte[] preparedKey;


  /**
   * Create a public key from the ANS.1 subject public key info
   *
   * @param info the key info
   */
  public EdDsaPublicKey(SubjectPublicKeyInfo info) throws InvalidKeySpecException {
    if (!ED25519_ALGORITHM.equals(info.getAlgorithm())) {
      throw new InvalidKeySpecException("Algorithm is not Ed25519");
    }
    this.info = info;
    keyBytes = info.getPublicKeyData().getOctets();
  }


  /**
   * Create a public key from the 32 bytes of the curve point.
   *
   * @param keyBytes the 32 bytes of the key
   */
  public EdDsaPublicKey(byte[] keyBytes) throws InvalidKeySpecException {
    if (keyBytes.length != 32) {
      throw new InvalidKeySpecException("Ed25519 keys must have 32 bytes, not " + keyBytes.length);
    }
    this.keyBytes = keyBytes.clone();
    info = new SubjectPublicKeyInfo(ED25519_ALGORITHM, keyBytes);
  }


  /**
   * Create a public key that corresponds to an EdDSA private key.
   *
   * @param privateKey the private key
   */
  public EdDsaPublicKey(EdDsaPrivateKey privateKey) {
    Ed25519PrivateKeyParameters privateParameters = new Ed25519PrivateKeyParameters(privateKey.keyBytes, 0);
    Ed25519PublicKeyParameters publicParameters = privateParameters.generatePublicKey();
    this.keyBytes = publicParameters.getEncoded();
    info = new SubjectPublicKeyInfo(ED25519_ALGORITHM, keyBytes);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EdDsaPublicKey)) {
      return false;
    }
    EdDsaPublicKey that = (EdDsaPublicKey) o;
    return info.equals(that.info) && Arrays.equals(keyBytes, that.keyBytes);
  }


  @Override
  public String getAlgorithm() {
    return "Ed25519";
  }


  @Override
  @Nullable
  public byte[] getEncoded() {
    try {
      return info.getEncoded();
    } catch (IOException e) {
      // Returning null because that is what BouncyCastle does in this situation
      return null;
    }
  }


  @Override
  public String getFormat() {
    return "X.509";
  }


  /**
   * Get the 32 bytes of the key.
   *
   * @return the key bytes
   */
  public byte[] getKeyBytes() {
    return keyBytes.clone();
  }


  /**
   * Get the prepared bytes for the native code.
   *
   * @param jni the native implementation
   *
   * @return the bytes
   */
  @Nullable
  public byte[] getPreparedKey(DonnaJNI jni) {
    if (preparedKey == null) {
      try {
        preparedKey = jni.prepareKey(getKeyBytes());
      } catch (InvalidKeyException e) {
        return null;
      }
    }
    return preparedKey.clone();
  }


  @Override
  public int hashCode() {
    int result = Objects.hash(info);
    result = 31 * result + Arrays.hashCode(keyBytes);
    return result;
  }


  protected Object readResolve() throws ObjectStreamException {
    try {
      return new EdDsaPublicKey(keyBytes);
    } catch (InvalidKeySpecException e) {
      InvalidObjectException ioe = new InvalidObjectException("Invalid key in stream");
      ioe.initCause(e);
      throw ioe;
    }
  }


  @Override
  public String toString() {
    return String.format(
        "EdDsaPublicKey(info=%s, keyBytes=%s, preparedKey=%s)",
        this.info, Hex.encode(this.keyBytes), Hex.encode(this.preparedKey)
    );
  }
}
