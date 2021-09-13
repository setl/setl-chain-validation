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

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * Support for translating key which can be used with the native library to and from their specifications.
 *
 * @author Simon Greatrix on 2019-08-01.
 */
public class EdDsaKeyFactory extends KeyFactorySpi {

  @Override
  protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException {
    SetlProvider.checkNativeMode();

    if (!(keySpec instanceof PKCS8EncodedKeySpec)) {
      throw new InvalidKeySpecException("Only PKCS#8 private key specifications are supported");
    }

    PrivateKeyInfo keyInfo = PrivateKeyInfo.getInstance(((PKCS8EncodedKeySpec) keySpec).getEncoded());
    if (!EdECObjectIdentifiers.id_Ed25519.equals(keyInfo.getPrivateKeyAlgorithm().getAlgorithm())) {
      throw new InvalidKeySpecException("Not an Ed25519 key specification");
    }

    return new EdDsaPrivateKey(keyInfo);
  }


  @Override
  protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException {
    SetlProvider.checkNativeMode();

    if (!(keySpec instanceof X509EncodedKeySpec)) {
      throw new InvalidKeySpecException("Only X.509 public key specifications are supported");
    }

    SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec) keySpec).getEncoded());
    if (!EdECObjectIdentifiers.id_Ed25519.equals(keyInfo.getAlgorithm().getAlgorithm())) {
      throw new InvalidKeySpecException("Not an Ed25519 key specification");
    }

    return new EdDsaPublicKey(keyInfo);
  }


  @Override
  protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> spec) throws InvalidKeySpecException {
    SetlProvider.checkNativeMode();

    if (spec.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8")) {
      return spec.cast(new PKCS8EncodedKeySpec(key.getEncoded()));
    } else if (spec.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509")) {
      return spec.cast(new X509EncodedKeySpec(key.getEncoded()));
    }

    throw new InvalidKeySpecException("Conversion of " + key.getFormat() + " to " + spec.getName() + " is not supported");
  }


  @Override
  protected Key engineTranslateKey(Key key) throws InvalidKeyException {
    SetlProvider.checkNativeMode();
    if ((key instanceof EdDsaPrivateKey) || (key instanceof EdDsaPublicKey)) {
      return key;
    }
    try {
      if (key instanceof PrivateKey && key.getFormat().equals("PKCS#8")) {
        return engineGeneratePrivate(new PKCS8EncodedKeySpec(key.getEncoded()));
      }
      if (key instanceof PublicKey && key.getFormat().equals("X.509")) {
        return engineGeneratePublic(new X509EncodedKeySpec(key.getEncoded()));
      }
    } catch (InvalidKeySpecException e) {
      throw new InvalidKeyException("Unable to convert key", e);
    }

    throw new InvalidKeyException("Conversion of " + key.getFormat() + " is not supported");
  }
}
