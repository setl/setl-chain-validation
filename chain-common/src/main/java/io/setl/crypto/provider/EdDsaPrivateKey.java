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
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;

/**
 * @author Simon Greatrix on 2019-08-01.
 */
public class EdDsaPrivateKey implements PrivateKey {

  private static final long serialVersionUID = 1L;

  protected final byte[] keyBytes;

  protected final transient PrivateKeyInfo privateKeyInfo;

  protected transient EdDsaPublicKey publicKey;


  /**
   * New instance.
   *
   * @param privateKeyInfo the ASN.1 private key info
   *
   * @throws InvalidKeySpecException if key is invalid
   */
  public EdDsaPrivateKey(PrivateKeyInfo privateKeyInfo) throws InvalidKeySpecException {
    this.privateKeyInfo = privateKeyInfo;
    ASN1Encodable keyOctets = null;
    try {
      keyOctets = privateKeyInfo.parsePrivateKey();
    } catch (IOException e) {
      throw new InvalidKeySpecException("Cannot parse private key", e);
    }
    keyBytes = ASN1OctetString.getInstance(keyOctets).getOctets();
  }


  /**
   * New instance.
   *
   * @param keyBytes the 32 bytes of an Ed 25519 key.
   *
   * @throws InvalidKeySpecException if the key is invalid
   */
  public EdDsaPrivateKey(byte[] keyBytes) throws InvalidKeySpecException {
    if (keyBytes.length != 32) {
      throw new InvalidKeySpecException("Ed25519 keys must have 32 bytes, not " + keyBytes.length);
    }
    this.keyBytes = keyBytes.clone();

    Ed25519PrivateKeyParameters parameters = new Ed25519PrivateKeyParameters(keyBytes, 0);
    try {
      privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(parameters);
    } catch (IOException e) {
      throw new InvalidKeySpecException("Failed to create private key info", e);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EdDsaPrivateKey)) {
      return false;
    }
    EdDsaPrivateKey that = (EdDsaPrivateKey) o;
    return Arrays.equals(keyBytes, that.keyBytes) && privateKeyInfo.equals(that.privateKeyInfo);
  }


  @Override
  public String getAlgorithm() {
    return "Ed25519";
  }


  @Nullable
  @Override
  public byte[] getEncoded() {
    try {
      return privateKeyInfo.getEncoded();
    } catch (IOException e) {
      // Seems a bit odd to return null, but this is what the BouncyCastle implementation does.
      return null;
    }
  }


  @Override
  public String getFormat() {
    return "PKCS#8";
  }


  public byte[] getKeyBytes() {
    return keyBytes.clone();
  }


  /**
   * Get the public key associated with this private key.
   *
   * @return the public key
   */
  public EdDsaPublicKey getPublicKey() {
    if (publicKey == null) {
      publicKey = new EdDsaPublicKey(this);
    }
    return publicKey;
  }


  @Override
  public int hashCode() {
    int result = Objects.hash(privateKeyInfo);
    result = 31 * result + Arrays.hashCode(keyBytes);
    return result;
  }


  protected Object readResolve() throws ObjectStreamException {
    try {
      return new EdDsaPrivateKey(keyBytes);
    } catch (InvalidKeySpecException e) {
      InvalidObjectException ioe = new InvalidObjectException("Invalid key in stream");
      ioe.initCause(e);
      throw ioe;
    }
  }


  @Override
  public String toString() {
    return String.format(
        "EdDsaPrivateKey(keyBytes=%s, privateKeyInfo=%s, publicKey=%s)",
        Hex.encode(keyBytes), this.privateKeyInfo, this.publicKey
    );
  }
}
