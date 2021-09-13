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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.common.Pair;
import io.setl.crypto.provider.SetlProvider;

/**
 * Support for PEM files.
 *
 * <p>Can read and write PEM files for public keys, certificates, private keys and encrypted private keys.</p>
 *
 * <p>The PEM format is defined in RFC7468, but many generators pre-date this standard. We accept the "standard" format, and output the "strict" format. We do
 * not accept the "lax" format, nor deprecated formats that incorporate encapsulated headers.</p>
 *
 * @author Simon Greatrix on 13/07/2020.
 */
public abstract class PEM {

  //  PEM is old-school. The data is encoded in Base64, you add a header and footer to say if it is a public key or an encrypted private key, and you are done.
  //
  //  To protect the private keys, PEM relies on “Password Based Encryption Scheme” (PBES).
  //
  //  Unfortunately, PBES is an old standard which predates the invention of AES, so the best cipher available in PBES is three key DES in CBC mode. Our crypto
  //  committee mandated that we should always use AES in key-wrapping mode to protect cipher keys, so PBES is not good enough for us.
  //
  //  Fortunately, in January 2017 the IETF issued RFC8018 describing PBES version 2, which updated the encryption scheme so it can work with any key-derivation
  //  function and cipher, future proofing it. They documented a few examples, announced the exact list of ciphers to support was up to implementors and called
  //  it done. In theory this allows us to use AES in key-wrapping mode.
  //
  //  Unfortunately, ciphers require parameters to nail down precisely how they operate. PBES version 2 specifies that they are passed in an array. Until the
  //  IETF say what order a specific cipher’s parameters go in and how they are interpreted, no-one can use that cipher in a truly standards compliant way.
  //
  //  Fortunately, AES in key-wrapping mode does not have any parameters. In keeping with other parameter-less algorithms in RFC8018, this should means we just
  //  write a null, but a future standard may mandate that it should be an empty array.
  //
  //  Our default output format will have maximum security and compliance with our internal standards. It will use AES key wrapping.
  //
  //  To support interoperability we will also support generating the files with PBES version 1 and PBES version two with AES-CBC.

  private static final Logger logger = LoggerFactory.getLogger(PEM.class);



  /**
   * Data type encapsulated in the PEM message.
   */
  public enum DataType {
    /** An X.509 certificate. */
    CERTIFICATE("CERTIFICATE"),

    /** A public key. */
    PUBLIC_KEY("PUBLIC KEY"),

    /** An unprotected private key. */
    PRIVATE_KEY("PRIVATE KEY"),

    /** An encrypted private key. */
    ENCRYPTED_PRIVATE_KEY("ENCRYPTED PRIVATE KEY");

    private final String label;


    DataType(String label) {
      this.label = label;
    }


    public String getLabel() {
      return label;
    }
  }



  /**
   * Exception thrown on invalid Base-64 data.
   */
  public static class InvalidBase64Exception extends Exception {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;


    public InvalidBase64Exception(String message) {
      super(message);
    }

  }


  static PrivateKey createPrivateKey(byte[] bytes) throws GeneralSecurityException {
    PrivateKeyInfo info = PrivateKeyInfo.getInstance(bytes);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
    KeyFactory kf = KeyFactory.getInstance(info.getPrivateKeyAlgorithm().getAlgorithm().getId());
    return kf.generatePrivate(spec);
  }


  static PublicKey createPublicKey(byte[] bytes) throws GeneralSecurityException {
    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
    SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(bytes);
    KeyFactory kf = KeyFactory.getInstance(info.getAlgorithm().getAlgorithm().getId());
    return kf.generatePublic(spec);
  }


  static boolean isBadBase64(char ch) {
    return ((ch < '+') || ('+' < ch && ch < '/') || ('9' < ch && ch < 'A') || ('Z' < ch && ch < 'a') || ('z' < ch));
  }


  static String write(DataType type, byte[] data) {
    Base64.Encoder encode = Base64.getMimeEncoder(64, new byte[]{(byte) '\n'});
    return "-----BEGIN " + type.getLabel() + "-----\n"
        + encode.encodeToString(data)
        + "\n-----END " + type.getLabel() + "-----\n";
  }


  /**
   * Create the PEM representation of a public key.
   *
   * @param publicKey the public key
   *
   * @return the PEM representation
   */
  public static String write(PublicKey publicKey) throws GeneralSecurityException {
    KeyFactory keyFactory = KeyFactory.getInstance(publicKey.getAlgorithm());
    X509EncodedKeySpec spec = keyFactory.getKeySpec(publicKey, X509EncodedKeySpec.class);
    return write(DataType.PUBLIC_KEY, spec.getEncoded());
  }


  /**
   * Create the PEM representation of a private key.
   *
   * @param privateKey the private key
   *
   * @return the PEM representation
   */
  public static String write(PrivateKey privateKey) throws GeneralSecurityException {
    KeyFactory keyFactory = KeyFactory.getInstance(privateKey.getAlgorithm());
    PKCS8EncodedKeySpec spec = keyFactory.getKeySpec(privateKey, PKCS8EncodedKeySpec.class);
    return write(DataType.PRIVATE_KEY, spec.getEncoded());
  }


  /**
   * Create the PEM representation of an encrypted private key.
   *
   * @param info the private key
   *
   * @return the PEM representation
   */
  public static String write(EncryptedPrivateKeyInfo info) throws GeneralSecurityException {
    try {
      return write(DataType.ENCRYPTED_PRIVATE_KEY, info.getEncoded());
    } catch (IOException e) {
      throw new GeneralSecurityException("Invalid EncryptedPrivateKeyInfo structure", e);
    }
  }


  /**
   * Create the PEM representation of a certificate.
   *
   * @param certificate the certificate
   *
   * @return the PEM representation
   */
  public static String write(Certificate certificate) throws GeneralSecurityException {
    return write(DataType.CERTIFICATE, certificate.getEncoded());
  }


  static {
    SetlProvider.install();
  }

  protected int lineNumber = 0;

  boolean lookingForBegin;

  private final StringBuilder base64 = new StringBuilder();

  private DataType dataType;

  private String endLine;

  private int padCount;


  int buildBase64(StringBuilder base64, int padCount, String line) throws InvalidBase64Exception {
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (Character.isWhitespace(ch)) {
        continue;
      }

      if (ch == '=') {
        padCount++;
        if ((base64.length() & 3) < 2) {
          throw new InvalidBase64Exception(String.format("Badly positioned Base64 pad character at line %d, position %d", lineNumber, i + 1));
        }
      } else if (padCount > 0) {
        throw new InvalidBase64Exception(String.format("Data found after Base64 padding at line %d, position %d", lineNumber, i + 1));
      } else if (isBadBase64(ch)) {
        throw new InvalidBase64Exception(String.format("Invalid Base64 character found at line %d, position %d", lineNumber, i + 1));
      }

      base64.append(ch);
    }
    return padCount;
  }


  Pair<DataType, Object> extractData() throws IOException, InvalidBase64Exception, GeneralSecurityException {
    reset();

    while (scanLine(readLine())) {
      // do nothing
    }

    return parseObject();
  }


  public X509Certificate getCertificate() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    return getFirstMatch(DataType.CERTIFICATE, X509Certificate.class);
  }


  public EncryptedPrivateKeyInfo getEncryptedPrivateKey() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    return getFirstMatch(DataType.ENCRYPTED_PRIVATE_KEY, EncryptedPrivateKeyInfo.class);
  }


  private <T> T getFirstMatch(DataType type, Class<T> target) throws GeneralSecurityException, InvalidBase64Exception, IOException {
    Pair<DataType, Object> pair;
    do {
      pair = extractData();
    }
    while (pair != null && !pair.left().equals(type));

    if (pair == null) {
      return null;
    }
    return target.cast(pair.right());
  }


  public PrivateKey getPrivateKey() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    return getFirstMatch(DataType.PRIVATE_KEY, PrivateKey.class);
  }


  public PublicKey getPublicKey() throws GeneralSecurityException, InvalidBase64Exception, IOException {
    return getFirstMatch(DataType.PUBLIC_KEY, PublicKey.class);
  }


  private DataType matchDataType(String line) {
    String typeName = line.substring(11, line.length() - 5).trim();
    for (DataType type : DataType.values()) {
      if (typeName.equals(type.getLabel())) {
        logger.trace("Found BEGIN line at line {} for type {}", lineNumber, type);
        return type;
      }
    }
    logger.debug("Found BEGIN line at line {} for unrecognised type {}", lineNumber, typeName);
    return null;
  }


  protected Pair<DataType, Object> parseObject() throws GeneralSecurityException {
    if (dataType == null || base64.length() == 0) {
      return null;
    }
    byte[] data = Base64.getMimeDecoder().decode(base64.toString());
    switch (dataType) {
      case CERTIFICATE:
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return new Pair<>(dataType, factory.generateCertificate(new ByteArrayInputStream(data)));

      case PUBLIC_KEY:
        return new Pair<>(dataType, createPublicKey(data));

      case PRIVATE_KEY:
        return new Pair<>(dataType, createPrivateKey(data));

      case ENCRYPTED_PRIVATE_KEY:
        return new Pair<>(dataType, EncryptedPrivateKeyInfo.getInstance(data));

      default:
        throw new InternalError("ENUM not matched");

    }
  }


  protected abstract String readLine() throws IOException;


  protected void reset() {
    base64.setLength(0);
    dataType = null;
    lookingForBegin = true;
    padCount = 0;
    endLine = null;
  }


  protected boolean scanLine(String line) throws InvalidBase64Exception {
    if (line == null) {
      // end of input reached without finding data
      dataType = null;
      base64.setLength(0);
      return false;
    }

    line = line.trim();

    if (lookingForBegin) {
      if (
          line.startsWith("-----BEGIN ")
              && line.endsWith("-----")
              && ((dataType = matchDataType(line)) != null)) {
        lookingForBegin = false;
        endLine = "-----END " + dataType.getLabel() + "-----";
      }

      // send more data
      return true;
    }

    // If we've found the end line, we are done
    if (endLine.equals(line)) {
      logger.trace("Found Base64 data for {}: {}", dataType, base64);
      return false;
    }

    // should be valid base 64.
    padCount = buildBase64(base64, padCount, line);

    // send more data
    return true;
  }

}
