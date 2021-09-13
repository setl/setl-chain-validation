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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;

/**
 * @author Simon Greatrix on 2019-08-01.
 */
public class EdDsaKeyPairGenerator extends java.security.KeyPairGeneratorSpi {

  private SecureRandom random = null;


  @Override
  public KeyPair generateKeyPair() {
    SetlProvider.checkNativeMode();

    SecureRandom secureRandom = random;
    if (secureRandom == null) {
      secureRandom = SetlProvider.getSecureRandom();
    }
    byte[] keyBytes = new byte[32];
    secureRandom.nextBytes(keyBytes);

    try {
      EdDsaPrivateKey privateKey = new EdDsaPrivateKey(keyBytes);
      EdDsaPublicKey publicKey = new EdDsaPublicKey(privateKey);
      return new KeyPair(publicKey, privateKey);
    } catch (InvalidKeySpecException e) {
      throw new AssertionError("Key generator suffered internal error", e);
    }
  }


  @Override
  public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
    SetlProvider.checkNativeMode();
    if (params == null) {
      // EdDSA could be using the Ed448 key and this only supports Ed25519
      throw new InvalidAlgorithmParameterException("No parameter specified");
    }
    if (!(params instanceof EdDSAParameterSpec)) {
      throw new InvalidAlgorithmParameterException("Cannot handle parameter of type " + params.getClass());
    }
    EdDSAParameterSpec spec = (EdDSAParameterSpec) params;
    String curve = spec.getCurveName();
    if (!curve.equalsIgnoreCase("Ed25519")) {
      throw new InvalidAlgorithmParameterException("Only curve 25519 is supported, not " + curve);
    }
  }


  @Override
  public void initialize(int keysize, SecureRandom random) {
    SetlProvider.checkNativeMode();

    if (keysize != 255 && keysize != 256) {
      throw new InvalidParameterException("Key size for Ed25519 must be 255 or 256");
    }
    this.random = random;
  }
}
