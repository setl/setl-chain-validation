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

import io.setl.ed25519.DonnaJNI;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureSpi;

/**
 * @author Simon Greatrix on 2019-08-01.
 */
public class EdDsaSignature extends SignatureSpi {

  final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  private final DonnaJNI jni = new DonnaJNI();

  private EdDsaPrivateKey edPrivateKey;

  private EdDsaPublicKey edPublicKey;


  @Override
  protected Object engineGetParameter(String param) {
    // There are no parameters
    return null;
  }


  @Override
  protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
    buffer.reset();
    if (privateKey instanceof EdDsaPrivateKey) {
      edPrivateKey = (EdDsaPrivateKey) privateKey;
      edPublicKey = edPrivateKey.getPublicKey();
    } else {
      throw new InvalidKeyException("Native signatures require compatible private key, not " + privateKey.getClass());
    }
  }


  @Override
  protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
    buffer.reset();
    edPrivateKey = null;
    if (publicKey instanceof EdDsaPublicKey) {
      edPublicKey = (EdDsaPublicKey) publicKey;
    } else {
      throw new InvalidKeyException("Native signatures require compatible public key, not " + publicKey.getClass());
    }
  }


  @Override
  protected void engineSetParameter(String param, Object value) {
    // No parameters to set
  }


  @Override
  protected byte[] engineSign() {
    return jni.createSignature(buffer.toByteArray(), edPrivateKey.getKeyBytes(), edPublicKey.getKeyBytes());
  }


  @Override
  protected void engineUpdate(byte[] b, int off, int len) {
    buffer.write(b, off, len);
  }


  @Override
  protected void engineUpdate(byte b) {
    buffer.write(b);
  }


  @Override
  protected boolean engineVerify(byte[] sigBytes) {
    byte[] message = buffer.toByteArray();
    byte[] preparedKey = edPublicKey.getPreparedKey(jni);
    return jni.verifyWithPreparedKey(message,preparedKey,sigBytes);
  }
}
