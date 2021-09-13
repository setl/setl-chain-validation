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

import io.setl.utils.Base64;
import io.setl.utils.ByteUtil;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * Sign a byte array using private key.
 *
 * @author aanten
 */
public interface MessageSigner {

  default byte[] createSignature(byte[] message, PrivateKey privateKey) {
    // The Donna libraries require the binary representation of the private key. When we move to NIST curves, this should be changed.
    return createSignature(message, privateKey.getEncoded());
  }

  byte[] createSignature(byte[] b, byte[] privateKey);

  default String createSignatureB64(String messageBytes, PrivateKey privateKey) {
    return Base64.encode(createSignature(messageBytes.getBytes(StandardCharsets.UTF_8), privateKey));
  }

  default String createSignatureB64(String messageBytes, String hexPrivateKey) {
    return Base64.encode(createSignature(messageBytes.getBytes(StandardCharsets.UTF_8), ByteUtil.hexToBytes((hexPrivateKey))));
  }
}
