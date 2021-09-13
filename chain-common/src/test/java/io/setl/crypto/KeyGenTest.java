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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.crypto.KeyGen.Type;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import org.junit.Test;

/**
 * @author Simon Greatrix on 01/12/2017.
 */
public class KeyGenTest {

  @Test
  public void generateKeyPair() throws Exception {
    KeyPair kp = KeyGen.generateKeyPair();
    assertNotNull(kp);
    assertEquals("EC", kp.getPublic().getAlgorithm());

    PublicKey pk = kp.getPublic();
    assertTrue(pk instanceof ECPublicKey);
    ECPublicKey epk = (ECPublicKey) pk;

    assertEquals(256, epk.getParams().getCurve().getField().getFieldSize());
  }


  @Test
  public void generateKeyPair1() throws Exception {
    KeyPair kp = KeyGen.generateKeyPair("EdDSA");
    assertNotNull(kp);
    assertEquals("Ed25519", kp.getPublic().getAlgorithm());
  }


  @Test
  public void generateKeyPair2() throws Exception {
    KeyPair kp = KeyGen.generateKeyPair("EC/X9.62 c2tnb191v1");
    assertNotNull(kp);
    assertEquals("EC", kp.getPublic().getAlgorithm());

    PublicKey pk = kp.getPublic();
    assertTrue(pk instanceof ECPublicKey);
    ECPublicKey epk = (ECPublicKey) pk;

    assertEquals(191, epk.getParams().getCurve().getField().getFieldSize());
  }


  @Test
  public void generateKeyPair3() throws Exception {
    try {
      KeyPair kp = KeyGen.generateKeyPair("EC/wibble");
      fail();
    } catch (IllegalArgumentException e) {
      // success
    }
  }


  @Test
  public void getPrivateKey1() throws Exception {
    KeyPair kp = Type.EC_NIST_P256.generate();
    PrivateKey pk1 = kp.getPrivate();
    PrivateKey pk2 = KeyGen.getPrivateKey(pk1.getEncoded());

    assertTrue(Arrays.equals(pk1.getEncoded(), pk2.getEncoded()));
  }


  @Test
  public void getPrivateKey3() throws Exception {
    KeyPair kp = Type.ED25519.generate();
    PrivateKey pk1 = kp.getPrivate();
    PrivateKey pk2 = KeyGen.getPrivateKey(pk1.getEncoded());
    assertEquals(pk1, pk2);
  }


  @Test
  public void getPrivateKey4() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(1024);
    KeyPair kp = generator.generateKeyPair();
    PrivateKey pk1 = kp.getPrivate();
    PrivateKey pk2 = KeyGen.getPrivateKey(pk1.getEncoded());
    assertEquals(pk1, pk2);
  }


  @Test
  public void getPublicKey1() throws Exception {
    KeyPair kp = Type.EC_NIST_P256.generate();
    PublicKey pk1 = kp.getPublic();
    PublicKey pk2 = KeyGen.getPublicKey(pk1.getEncoded());
    assertEquals(pk1, pk2);
  }


  @Test
  public void getPublicKey3() throws Exception {
    KeyPair kp = Type.ED25519.generate();
    PublicKey pk1 = kp.getPublic();
    PublicKey pk2 = KeyGen.getPublicKey(pk1.getEncoded());
    assertEquals(pk1, pk2);
  }


  @Test
  public void getPublicKey4() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(1024);
    KeyPair kp = generator.generateKeyPair();
    PublicKey pk1 = kp.getPublic();
    PublicKey pk2 = KeyGen.getPublicKey(pk1.getEncoded());
    assertEquals(pk1, pk2);
  }


  @Test
  public void tryTypes() throws Exception {
    for (Type t : Type.values()) {
      KeyPair kp = t.generate();
      assertNotNull(kp);
    }
  }
}