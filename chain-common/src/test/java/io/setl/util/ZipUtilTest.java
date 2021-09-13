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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtilTest {

  private static final Logger logger = LoggerFactory.getLogger(ZipUtilTest.class);

  private static final String refMessage = "{\"Request\": {}, \"Data\": \"Chain: 20\\tHeight: 917\\tNetwork : None\\tTime: 2017-08-11 13:32:38"
      + "\\nPeerMsgsIn: 0  PeerMsgsOut: 1  proc_q: 0  TXPool: 0 \\nContracts : 0  TimeEvent : None"
      + "\\n==============\\n\", \"RequestID\": \"Update\", \"MessageType\": \"terminal\"}";

  private static final String refZippedMessage = "eNpVj08LwjAMxb9K6FlhdQe14EkFd/APMsFDQcoM23Cms42KiN/dVt3BnF7ywi8vT7HFyxU9CwXPVw/"
      + "EzLAJWkwrU5OCQaJ5gXVZsYKxHGpeId+tO4GClSXUnNdnjGty2E9GfSlBpiodqHSkaYPolr70WeAkAF27vgaWBGidLQ6Xj5XvN9Y2UWqaWmJnCvbwtQJ/"
      + "fkPi7iJN/kqTCKl/T2SzGH3XHg1jHC/Re1Ni/mgxGozuXJNpxOsNea5L0A==";


  @Test
  public void compressMessage() throws Exception {
    String zippedMessage = ZipUtil.zipB64(refMessage.getBytes());
    Assert.assertEquals(refZippedMessage, zippedMessage);
  }


  @Test
  public void decompressMessage() throws Exception {
    byte[] unzippedBytes = ZipUtil.unzipB64(refZippedMessage);
    String message = new String(unzippedBytes);
    Assert.assertEquals(refMessage, message);
  }
}
