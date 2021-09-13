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

import io.setl.crypto.KeyGen.Type;
import io.setl.crypto.PBES.EncryptionType;
import io.setl.crypto.PEM.InvalidBase64Exception;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-11-12.
 */
public class PBESTest {

  private EncryptedPrivateKeyInfo getInfo(String input) throws IOException, GeneralSecurityException, InvalidBase64Exception {
    PEMReader pem = new PEMReader(new StringReader(input));
    return pem.getEncryptedPrivateKey();
  }


  @Test
  public void testDecryptV1_PBE_M5_DES() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    // generated with: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v1 PBE-MD5-DES
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIGoMBsGCSqGSIb3DQEFAzAOBAimQxk+F4sQxQICCAAEgYhedqzICkJKCyUhIEkI\n"
        + "FO2B8FXi7zBbUK13DLk9zw220d2H7qdTk98jycX0icO0sz5AQn4DvKoqt4GH9x4G\n"
        + "Rbnf/3oziIMOruTupxyIa2KLYdpWNQrOmFGZ4umYa4ILtfJCcyLdYWhn9l1h96zm\n"
        + "x4y389reoja4BPLmh0GAijt9XkDT6UJWTAfF\n"
        + "-----END ENCRYPTED PRIVATE KEY-----";

    EncryptedPrivateKeyInfo info = getInfo(input);
    PrivateKey privateKey = PBES.decrypt("hello".toCharArray(), info);
    assertNotNull(privateKey);
    assertEquals("EC", privateKey.getAlgorithm());
  }


  @Test
  public void testDecryptV1_PBE_SHA1_3DES() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    // generated with: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v1 PBE-SHA1-3DES
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIGpMBwGCiqGSIb3DQEMAQMwDgQIpWcUv0ON720CAggABIGIg2ak+oIIXP7RgYWW\n"
        + "9vI4Wq2wj81eHOTeGLlfTe0j0gefRtfQqNpkQmpy3K7EuIE9bTpyFNO+aKJS1J92\n"
        + "4H08NEkI7PJ8GuX9nappTexEhl37H74f/u01+GpaxNZUUINv+dSWbL0lSpCW5bg+\n"
        + "KjwnTTiYY5yynasFl1wlyXr18mc/9Rv7eGy6Tw==\n"
        + "-----END ENCRYPTED PRIVATE KEY-----";

    EncryptedPrivateKeyInfo info = getInfo(input);
    PrivateKey privateKey = PBES.decrypt("hello".toCharArray(), info);
    assertNotNull(privateKey);
    assertEquals("EC", privateKey.getAlgorithm());
  }


  @Test
  public void testDecryptV1_PBE_SHA1_RC2_128() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    // generated with: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v1 PBE-SHA1-RC2-128
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIGpMBwGCiqGSIb3DQEMAQUwDgQIciHxdiwrEPsCAggABIGIwjs11pG4gmRRs7xp\n"
        + "GpTPLnfi2LAg7GDkt2+XtNgsTATsCnITBCdt+1JxnbMecTVmoj2yxD6dtHamMUsd\n"
        + "dlBRmUl2+P/aLgkVvYukUISzUH5EUWS7YlscAohdDRwaQHkVwB2JqMyucB3vbVJN\n"
        + "1GwrqwNeNZvrvanEpUEYbs4rFPph/mnjlracbQ==\n"
        + "-----END ENCRYPTED PRIVATE KEY-----";

    EncryptedPrivateKeyInfo info = getInfo(input);
    PrivateKey privateKey = PBES.decrypt("hello".toCharArray(), info);
    assertNotNull(privateKey);
    assertEquals("EC", privateKey.getAlgorithm());
  }


  @Test
  public void testDecryptV2_AES128_SHA256() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    // generated with: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v2 AES-128 -v2prf hmacWithSHA256
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIHsMFcGCSqGSIb3DQEFDTBKMCkGCSqGSIb3DQEFDDAcBAgkPap5yt2dmQICCAAw\n"
        + "DAYIKoZIhvcNAgkFADAdBglghkgBZQMEAQIEEFH/5/sLZIGZNoynaTD9dEYEgZCI\n"
        + "Et1y/vha6hfTQx9XXFnWesvuTATUOvPwnOuMoc16d0aO3riEMkooJCYWYjKA4tiu\n"
        + "R5RgGT8HmqmuxtApbVVNYY7AnJCsys8mKquChyB0mPL5N60DHXAK4uzxxW2iP6/3\n"
        + "6/IkBZ7MwJbIKyMDFGRYt5qjA7R2OnOvlG7jDXhcZsfY+VJ9TfAYddssHhW1Dsk=\n"
        + "-----END ENCRYPTED PRIVATE KEY-----";

    EncryptedPrivateKeyInfo info = getInfo(input);
    PrivateKey privateKey = PBES.decrypt("hello".toCharArray(), info);
    assertNotNull(privateKey);
    assertEquals("EC", privateKey.getAlgorithm());
  }


  @Test
  public void testDecryptV2_AES256_SHA512() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    // generated with: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v2 AES-256 -v2prf hmacWithSHA512
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIHsMFcGCSqGSIb3DQEFDTBKMCkGCSqGSIb3DQEFDDAcBAjwbXM3ZQW78gICCAAw\n"
        + "DAYIKoZIhvcNAgsFADAdBglghkgBZQMEASoEELn8ysasuqVEOtgygCgCjdUEgZDV\n"
        + "AJpi5PPNNgy9F3N15TCq3ntDJaWSD2N+n4Xq4AtQ+zYrSG66pH6T/38Nt60dt860\n"
        + "U/uyxzPR7uJiSHdUm/MyyVbF6swLkCdUOd8ghhbmFW64+ztuewZzAhKLXUehlp9B\n"
        + "oq5uxpig+rm1mvqej+n/mUGtRGH6HWKtNKZAxHzEdcfc0s8Fyr7AXEXvzEOyj44=\n"
        + "-----END ENCRYPTED PRIVATE KEY-----";

    EncryptedPrivateKeyInfo info = getInfo(input);
    PrivateKey privateKey = PBES.decrypt("hello".toCharArray(), info);
    assertNotNull(privateKey);
    assertEquals("EC", privateKey.getAlgorithm());
  }


  @Test
  public void testDecryptV2_DES3_SHA1() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    // generated with: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v2 DES3 -v2prf hmacWithSHA1
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIHNMEAGCSqGSIb3DQEFDTAzMBsGCSqGSIb3DQEFDDAOBAhno3bDCuxDAgICCAAw\n"
        + "FAYIKoZIhvcNAwcECBnSItA3H1VUBIGIIYNJag/BJ5N8E7bobqt4FJFZ6oPgSu0L\n"
        + "A5JmIGpExdKbTUeJG7ihxB6+tBDpbCfslwm1eYxI+ZJDJBiyhmDE/BqcOHqJcTPu\n"
        + "stPFs8LdsEw10ln13EsRwTh+4ZAvK36eujzFRlnwutY9inerFiuGwMmXBoIWKQ0P\n"
        + "X7Njh7xFBPI23aCvuS6nLg==\n"
        + "-----END ENCRYPTED PRIVATE KEY-----";

    EncryptedPrivateKeyInfo info = getInfo(input);
    PrivateKey privateKey = PBES.decrypt("hello".toCharArray(), info);
    assertNotNull(privateKey);
    assertEquals("EC", privateKey.getAlgorithm());
  }


  @Test
  public void testEncrypt3DES() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    KeyPair keyPair = Type.EC_NIST_P256.generate();
    EncryptedPrivateKeyInfo info = PBES.encrypt(EncryptionType.TRIPLE_DES_CBC, "hello".toCharArray(), keyPair.getPrivate());
    String pemText = PEMReader.write(info);
    System.out.println(pemText);
    EncryptedPrivateKeyInfo infoNew = getInfo(pemText);
    PrivateKey newPrivateKey = PBES.decrypt("hello".toCharArray(), infoNew);
    assertTrue(newPrivateKey instanceof ECPrivateKey);

    BigInteger oldS = ((ECPrivateKey) keyPair.getPrivate()).getS();
    BigInteger newS = ((ECPrivateKey) newPrivateKey).getS();
    assertTrue(oldS.compareTo(newS) == 0);
  }


  @Test
  public void testEncryptAES256Wrap() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    KeyPair keyPair = Type.EC_NIST_P256.generate();
    EncryptedPrivateKeyInfo info = PBES.encrypt(EncryptionType.AES_WRAP, "hello".toCharArray(), keyPair.getPrivate());
    String pemText = PEMReader.write(info);
    EncryptedPrivateKeyInfo infoNew = getInfo(pemText);
    PrivateKey newPrivateKey = PBES.decrypt("hello".toCharArray(), infoNew);
    assertTrue(newPrivateKey instanceof ECPrivateKey);

    BigInteger oldS = ((ECPrivateKey) keyPair.getPrivate()).getS();
    BigInteger newS = ((ECPrivateKey) newPrivateKey).getS();
    assertTrue(oldS.compareTo(newS) == 0);
  }


  @Test
  public void testEncryptAES_CBC() throws IOException, GeneralSecurityException, InvalidBase64Exception {
    KeyPair keyPair = Type.EC_NIST_P256.generate();
    EncryptedPrivateKeyInfo info = PBES.encrypt(EncryptionType.AES_CBC, "hello".toCharArray(), keyPair.getPrivate());
    String pemText = PEMReader.write(info);
    System.out.println(pemText);
    EncryptedPrivateKeyInfo infoNew = getInfo(pemText);
    PrivateKey newPrivateKey = PBES.decrypt("hello".toCharArray(), infoNew);
    assertTrue(newPrivateKey instanceof ECPrivateKey);

    BigInteger oldS = ((ECPrivateKey) keyPair.getPrivate()).getS();
    BigInteger newS = ((ECPrivateKey) newPrivateKey).getS();
    assertTrue(oldS.compareTo(newS) == 0);
  }
}