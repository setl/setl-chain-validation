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
package io.setl.bc.pychain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.IOException;
import java.util.Arrays;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;

/**
 * @author Simon Greatrix on 07/12/2018.
 */
@SuppressWarnings("unlikely-arg-type")
public class HashTest {

  @Test
  public void jsonSerialisationTest() throws IOException {
    Hash hash = Hash.fromHex("abad3aba01d1facd8b31337497a5e282add07dca52927b923cf2662e5bdea21e");
    JSONObject payload = new JSONObject();
    payload.put("data", hash);
    String json = payload.toJSONString();

    JSONParser p = new JSONParser();
    try {
      JSONObject content = (JSONObject) p.parse(json);
      String receivedHash = (String) content.get("data");
      assertEquals(hash.toHexString(), receivedHash);
    } catch (ParseException e) {
      Assert.fail(e.getMessage());
    }
  }


  @SuppressWarnings("ConstantConditions")
  @Test
  public void test() throws IOException {
    byte[] data = new byte[32];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }

    Hash hash = new Hash(data);
    assertFalse(hash.isNull());

    MessageBufferPacker packer = MsgPackUtil.newBufferPacker();
    hash.pack(packer);
    byte[] packedBytes = packer.toByteArray();

    // Data should have been cloned
    data[0] = 100;
    byte[] hashBytes = hash.get();
    assertNotNull(hashBytes);
    assertEquals(0, hashBytes[0]);

    // returned data should also be cloned
    hashBytes[0] = 99;
    hashBytes = hash.get();
    assertNotNull(hashBytes);
    assertEquals(0, hashBytes[0]);
    assertFalse(hash.equals(null));
    assertFalse(hash.equals(""));
    assertFalse(hash.equals(Hash.NULL_HASH));

    // Test equality.
    Hash hash2 = new Hash(data);
    assertNotEquals(hash, hash2);
    assertNotEquals(hash.hashCode(), hash2.hashCode());
    data[0] = 0;
    hash2 = new Hash(data);
    assertEquals(hash, hash2);
    assertEquals(hash.hashCode(), hash2.hashCode());

    // Test the magic hash value (CASTOFF)
    byte[] magic = new byte[]{-24, 12, 22, 28, 10, 30};
    assertEquals(0xca570ff, Arrays.hashCode(magic));
    hash2 = new Hash(magic);
    assertNotEquals(0xca570ff, hash2.hashCode());

    assertEquals("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", hash.toHexString());
    assertEquals("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8", hash.toB64());
    assertEquals("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", hash.toString());
  }


  @SuppressWarnings("ConstantConditions")
  @Test
  public void testNulls() throws IOException {
    Hash hash = new Hash((byte[]) null);
    assertTrue(hash.isNull());

    assertNull(hash.get());

    MessageBufferPacker packer = MsgPackUtil.newBufferPacker();
    hash.pack(packer);
    byte[] packedBytes = packer.toByteArray();

    assertFalse(hash.equals(null));
    assertFalse(hash.equals(""));
    assertTrue(hash.equals(Hash.NULL_HASH));

    // Test equality.
    Hash hash2 = new Hash(new byte[0]);
    assertNotEquals(hash, hash2);
    assertNotEquals(hash.hashCode(), hash2.hashCode());

    hash2 = new Hash((byte[]) null);
    assertEquals(hash, hash2);
    assertEquals(hash.hashCode(), hash2.hashCode());

    assertEquals(0, hash2.hashCode());

    assertEquals("", hash.toHexString());
    assertEquals("", hash.toB64());
    assertEquals("HASH=NULL_TX", hash.toString());
  }
}