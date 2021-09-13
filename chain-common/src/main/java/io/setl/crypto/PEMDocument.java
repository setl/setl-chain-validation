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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;

import io.setl.util.RuntimeIOException;

/**
 * Support for PEM files.
 *
 * <p>Can read and write PEM files for public keys, certificates, private keys and encrypted private keys.</p>
 *
 * <p>The PEM format is defined in RFC7468, but many generators pre-date this standard. We accept the "standard" format, and output the "strict" format. We do
 * not accept the "lax" format, nor deprecated formats that incorporate encapsulated headers.</p>
 *
 * @author Simon Greatrix on 10/11/2019.
 */
public class PEMDocument extends PEM {

  private final String[] lines;

  private int lineNumber = 0;


  public PEMDocument(String source) {
    lines = source.split("\r|\n|\r\n");
  }


  @Override
  public X509Certificate getCertificate() throws GeneralSecurityException, InvalidBase64Exception {
    try {
      return super.getCertificate();
    } catch (IOException e) {
      // Should never happen
      throw new RuntimeIOException(e);
    }
  }


  @Override
  public EncryptedPrivateKeyInfo getEncryptedPrivateKey() throws GeneralSecurityException, InvalidBase64Exception {
    try {
      return super.getEncryptedPrivateKey();
    } catch (IOException e) {
      // Should never happen
      throw new RuntimeIOException(e);
    }
  }


  @Override
  public PrivateKey getPrivateKey() throws GeneralSecurityException, InvalidBase64Exception {
    try {
      return super.getPrivateKey();
    } catch (IOException e) {
      // Should never happen
      throw new RuntimeIOException(e);
    }
  }


  @Override
  public PublicKey getPublicKey() throws GeneralSecurityException, InvalidBase64Exception {
    try {
      return super.getPublicKey();
    } catch (IOException e) {
      // Should never happen
      throw new RuntimeIOException(e);
    }

  }


  @Override
  protected String readLine() {
    if (lineNumber < lines.length) {
      String t = lines[lineNumber];
      lineNumber++;
      return t;
    }
    return null;
  }

}
