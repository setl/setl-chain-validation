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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.junit.Test;

import io.setl.common.Pair;
import io.setl.crypto.PEM.DataType;
import io.setl.crypto.PEM.InvalidBase64Exception;

/**
 * @author Simon Greatrix on 11/11/2019.
 */
public class PEMDocumentTest {

  @Test
  public void testCertificate() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    // generated via: openssl req -x509 -newkey rsa:512 -nodes -days 365 -outform pem -subj /CN=foo
    final String input = "-----\r"
        + "-----BEGIN CERTIFICATE-----\r"
        + "MIIBDTCBuAIJAO1bZJWt7+ppMA0GCSqGSIb3DQEBCwUAMA4xDDAKBgNVBAMMA2Zv\n"
        + "bzAeFw0xOTExMTEyMjMzNTBaFw0yMDExMTAyMjMzNTBaMA4xDDAKBgNVBAMMA2Zv\n"
        + "bzBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQC82buysNkKioaLDlMwggeaesQ2hOfp\n"
        + "uamg+u2bdQYXL+OPvfU9lRpbl0YRiudZNDrNddAZWY+E2aePfapLq83NAgMBAAEw\r\n"
        + "DQYJKoZIhvcNAQELBQADQQBwEdcjU6bvPzDNp7Bq0bQPEvwCLChAHA40ww33actP\r\n"
        + "hUtWL0IvlyXdcPeqWXebfvWj8cquh8izMCknRKOLwZq7\n"
        + "-----END CERTIFICATE-----\n";

    PEMDocument pem = new PEMDocument(input);
    Pair<DataType, Object> pair = pem.extractData();
    assertNotNull(pair);
    assertEquals(DataType.CERTIFICATE, pair.left());
    assertTrue(pair.right() instanceof X509Certificate);

    X509Certificate certificate = (X509Certificate) pair.right();
    assertEquals("CN=foo", certificate.getIssuerDN().getName());
  }


  @Test
  public void testCertificate2() throws GeneralSecurityException, InvalidBase64Exception {
    // generated via: openssl req -x509 -newkey rsa:512 -nodes -days 365 -outform pem -subj /CN=foo
    final String input = "-----\n"
        + "-----BEGIN CERTIFICATE-----\n"
        + "MIIBDTCBuAIJAO1bZJWt7+ppMA0GCSqGSIb3DQEBCwUAMA4xDDAKBgNVBAMMA2Zv\n"
        + "bzAeFw0xOTExMTEyMjMzNTBaFw0yMDExMTAyMjMzNTBaMA4xDDAKBgNVBAMMA2Zv\n"
        + "bzBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQC82buysNkKioaLDlMwggeaesQ2hOfp\n"
        + "uamg+u2bdQYXL+OPvfU9lRpbl0YRiudZNDrNddAZWY+E2aePfapLq83NAgMBAAEw\n"
        + "DQYJKoZIhvcNAQELBQADQQBwEdcjU6bvPzDNp7Bq0bQPEvwCLChAHA40ww33actP\n"
        + "hUtWL0IvlyXdcPeqWXebfvWj8cquh8izMCknRKOLwZq7\n"
        + "-----END CERTIFICATE-----\n";

    PEMDocument pem = new PEMDocument(input);
    X509Certificate certificate = pem.getCertificate();
    assertEquals("CN=foo", certificate.getIssuerDN().getName());
  }


  @Test
  public void testEncryptedPrivateKey1() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    // generated via: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v1 PBE-MD5-DES
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIGoMBsGCSqGSIb3DQEFAzAOBAgq5krCNWIF/QICCAAEgYiFRKPBfEyAvGpdeqZD\n"
        + "wyL355EPr0vzidfEiqr/gq8aR2/AVQq8pEh2xpKT8qRSR1fccAnZCQMyw4/rV2zZ\n"
        + "Ia5efcwpVVjTZ2S5V9hKgiJHDoFFpFdSSjjGLBmTLdhvM0vJqgVdGSsXudoehdDd\n"
        + "OhZ8flQX2GHjP3We/DcsirEHagWXojn59ftm\n"
        + "-----END ENCRYPTED PRIVATE KEY-----\n";

    PEMDocument pem = new PEMDocument(input);
    Pair<DataType, Object> pair = pem.extractData();
    assertNotNull(pair);
    assertEquals(DataType.ENCRYPTED_PRIVATE_KEY, pair.left());
    assertTrue(pair.right() instanceof EncryptedPrivateKeyInfo);
  }


  @Test
  public void testEncryptedPrivateKey2() throws GeneralSecurityException, InvalidBase64Exception {
    // generated via: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -v1 PBE-MD5-DES
    final String input = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
        + "MIGoMBsGCSqGSIb3DQEFAzAOBAgq5krCNWIF/QICCAAEgYiFRKPBfEyAvGpdeqZD\n"
        + "wyL355EPr0vzidfEiqr/gq8aR2/AVQq8pEh2xpKT8qRSR1fccAnZCQMyw4/rV2zZ\n"
        + "Ia5efcwpVVjTZ2S5V9hKgiJHDoFFpFdSSjjGLBmTLdhvM0vJqgVdGSsXudoehdDd\n"
        + "OhZ8flQX2GHjP3We/DcsirEHagWXojn59ftm\n"
        + "-----END ENCRYPTED PRIVATE KEY-----\n";

    PEMDocument pem = new PEMDocument(input);
    EncryptedPrivateKeyInfo info = pem.getEncryptedPrivateKey();
    assertNotNull(info);
  }


  @Test
  public void testPrivateKey() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    // generated via: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -nocrypt
    final String input = "-----BEGIN PRIVATE KEY-----\n"
        + "MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgk4RZFSM2dVfnxk6r/0M3\n"
        + "dzb/imyoXWHZnqPTuoOAWbWhRANCAASdLt4FIYfF15IrUZNgB6FIucNJVHh5J5hX\n"
        + "44JcRrANleWtVjqtzfn2p6O2PMXBu3S7NkHZyyDJJwyrKCTCYaFX\n"
        + "-----END PRIVATE KEY-----";

    PEMDocument pem = new PEMDocument(input);
    Pair<DataType, Object> pair = pem.extractData();
    assertNotNull(pair);
    assertEquals(DataType.PRIVATE_KEY, pair.left());
    assertTrue(pair.right() instanceof PrivateKey);
    assertEquals("EC", ((PrivateKey) pair.right()).getAlgorithm());
    assertNull(pem.extractData());
  }


  @Test
  public void testPrivateKey2() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    // generated via: openssl genrsa 256 | openssl pkcs8 -topk8 -nocrypt
    final String input = "e is 65537 (0x10001)\n"
        + "-----BEGIN PRIVATE KEY-----\n"
        + "MIHCAgEAMA0GCSqGSIb3DQEBAQUABIGtMIGqAgEAAiEAmMZo8qZ1zgHoiIrDHjMb\n"
        + "AXB6YgFKVgWsNo8WHPUru2MCAwEAAQIgBaJjondTJurZGZgMhCOcXk2iFVWazRsT\n"
        + "ue5F7i+TRrkCEQDH/CZZNXso0l4Dp+pAJ+TXAhEAw5EW78XEGoMZeLKtaSFAVQIQ\n"
        + "am7A/hNfg59KF8oC+rgAqwIRAL2t3503J3i/ZtkWE/M/ePECEDaYma+7vJWQRFpr\n"
        + "Iqj34gI=\n"
        + "-----END PRIVATE KEY-----\n"
        + "extra data\n"
        + "\n"
        + "-----BEGIN FOOBAR----\n";

    PEMDocument pem = new PEMDocument(input);
    Pair<DataType, Object> pair = pem.extractData();
    assertNotNull(pair);
    assertEquals(DataType.PRIVATE_KEY, pair.left());
    assertTrue(pair.right() instanceof PrivateKey);
    assertEquals("RSA", ((PrivateKey) pair.right()).getAlgorithm());
    assertNull(pem.extractData());
  }


  @Test
  public void testPrivateKey3() throws GeneralSecurityException, InvalidBase64Exception {
    // generated via: openssl genrsa 256 | openssl pkcs8 -topk8 -nocrypt
    final String input = "e is 65537 (0x10001)\n"
        + "-----BEGIN PRIVATE KEY-----\n"
        + "MIHCAgEAMA0GCSqGSIb3DQEBAQUABIGtMIGqAgEAAiEAmMZo8qZ1zgHoiIrDHjMb\n"
        + "AXB6YgFKVgWsNo8WHPUru2MCAwEAAQIgBaJjondTJurZGZgMhCOcXk2iFVWazRsT\n"
        + "ue5F7i+TRrkCEQDH/CZZNXso0l4Dp+pAJ+TXAhEAw5EW78XEGoMZeLKtaSFAVQIQ\n"
        + "am7A/hNfg59KF8oC+rgAqwIRAL2t3503J3i/ZtkWE/M/ePECEDaYma+7vJWQRFpr\n"
        + "Iqj34gI=\n"
        + "-----END PRIVATE KEY-----\n"
        + "extra data\n"
        + "\n"
        + "-----BEGIN FOOBAR----\n";

    PEMDocument pem = new PEMDocument(input);
    PrivateKey privateKey = pem.getPrivateKey();
    assertNotNull(privateKey);
  }


  @Test
  public void testPublicKey() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    // generated via: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -nocrypt
    final String input = "read EC key\n"
        + "writing EC key\n"
        + "-----BEGIN PUBLIC KEY-----\n"
        + "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEyDHFN0P8PDsBsgWZmfuY2YG2vQ20Ro3S\n"
        + "3dcqcYmIllMun85hembqnb6y0Pl88Cjn2/piIKVZX2M5k12lmsNsHw==\n"
        + "-----END PUBLIC KEY-----";

    PEMDocument pem = new PEMDocument(input);
    Pair<DataType, Object> pair = pem.extractData();
    assertNotNull(pair);
    assertEquals(DataType.PUBLIC_KEY, pair.left());
    assertTrue(pair.right() instanceof PublicKey);
    assertEquals("EC", ((PublicKey) pair.right()).getAlgorithm());
    assertNull(pem.extractData());
  }


  @Test
  public void testPublicKey2() throws GeneralSecurityException, InvalidBase64Exception {
    // generated via: openssl ecparam -name secp256k1 -genkey | openssl pkcs8 -topk8 -nocrypt
    final String input = "read EC key\n"
        + "writing EC key\n"
        + "-----BEGIN PUBLIC KEY-----\n"
        + "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEyDHFN0P8PDsBsgWZmfuY2YG2vQ20Ro3S\n"
        + "3dcqcYmIllMun85hembqnb6y0Pl88Cjn2/piIKVZX2M5k12lmsNsHw==\n"
        + "-----END PUBLIC KEY-----";

    PEMDocument pem = new PEMDocument(input);
    PublicKey publicKey = pem.getPublicKey();
    assertNotNull(publicKey);
  }

}