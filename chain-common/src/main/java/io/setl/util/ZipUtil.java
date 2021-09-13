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
package io.setl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Utility class for compressing and decompressing strings.
 */
public class ZipUtil {

  /**
   * Uncompress a base64 encoded string.
   *
   * @param base64Message String
   *
   * @return byte[]
   */
  public static byte[] unzipB64(String base64Message) {
    byte[] packedMessage = Base64.getDecoder().decode(base64Message);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(packedMessage.length * 2 + 16);
    InflaterOutputStream inflater = new InflaterOutputStream(baos);
    try {
      inflater.write(packedMessage);
      inflater.close();
    } catch (IOException e) {
      // As ByteArrayOutputStream never throws an IOException, the above will never throw one either.
      throw new InternalError("Impossible IOException", e);
    }
    return baos.toByteArray();
  }


  /**
   * Compress and base64 encode a string.
   *
   * @param packedMessage byte[]
   *
   * @return String
   */
  public static String zipB64(byte[] packedMessage) {
    AsciiOutputStream ascii = new AsciiOutputStream(packedMessage.length + 16);
    OutputStream encOut = Base64.getEncoder().wrap(ascii);

    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    DeflaterOutputStream deflateStream = new DeflaterOutputStream(encOut, deflater, 1);
    try {
      deflateStream.write(packedMessage);
      deflateStream.close();
    } catch (IOException e) {
      // As AsciiOutputStream never throws an IOException, the above will never throw one either.
      throw new InternalError("Impossible IOException", e);
    }
    deflater.end();

    return ascii.toString();
  }


  /**
   * Private constructor.
   */
  private ZipUtil() {
  }
}
