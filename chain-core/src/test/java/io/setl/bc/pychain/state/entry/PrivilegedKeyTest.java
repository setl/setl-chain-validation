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
package io.setl.bc.pychain.state.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.bc.pychain.state.entry.PrivilegedKey.Store;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.crypto.KeyGen;

/**
 * @author Simon Greatrix on 2019-04-10.
 */
public class PrivilegedKeyTest {

  /** A NIST P-521 public key. */
  static final String hexPublicKey =
      "30819b301006072a8648ce3d020106052b81040023038186000401cb8b2c01f9533ba2dcf81eef26e72ecd5b73fee7a483bd98766b5993115705a1e96e6789d5a1d3d8c5e59ac1547"
          + "c6f6aa522ad2b9e184e9c514402ab01c5cb730d00cd6b420b08114a0dd0e52a27889d1172abdb460f5a3f19accf0f02383e1d01d7bd945a6242edf2a862a2c038bae00ac01b"
          + "9082ce6f5bf110d7e6bc57964f853731";

  PrivilegedKey key;


  @Test
  public void addPermission() {
    assertFalse(key.getPermissions().contains("rabbit"));
    PrivilegedKey key2 = key.addPermission("rabbit", 51);
    assertTrue(key2.getPermissions().contains("rabbit"));
    assertFalse(key.getPermissions().contains("rabbit"));

    assertEquals(50, key.getBlockUpdateHeight());
    assertEquals(51, key2.getBlockUpdateHeight());

    PrivilegedKey key3 = key2.addPermission("rabbit", 55);
    assertSame(key2, key3);
  }


  @Before
  public void before() {
    key = new PrivilegedKey("keyName", Instant.now().getEpochSecond() + 10_000, KeyGen.getPublicKey(hexPublicKey), 50)
        .addPermission("magic", 50)
        .addPermission("top-hat", 50);
  }


  @Test
  public void deletePermission() {
    assertTrue(key.getPermissions().contains("top-hat"));
    assertTrue(key.getPermissions().contains("magic"));
    PrivilegedKey key2 = key.deletePermission("magic", 51);
    assertEquals(51, key2.getBlockUpdateHeight());
    assertTrue(key.getPermissions().contains("magic"));
    assertTrue(key.getPermissions().contains("top-hat"));
    assertFalse(key2.getPermissions().contains("magic"));
    assertTrue(key2.getPermissions().contains("top-hat"));

    PrivilegedKey key3 = key2.deletePermission("magic", 55);
    assertSame(key2, key3);
  }


  @Test
  public void getAddress() {
    assertEquals("BILPHdK08PTpgw268IXZLzaBChiy93dV2Q", key.getAddress());
  }


  @Test
  public void getHexPublicKey() {
    assertEquals(hexPublicKey, key.getHexPublicKey());
  }


  @Test
  public void getKey() {
    assertEquals("BILPHdK08PTpgw268IXZLzaBChiy93dV2Q", key.getKey());
  }


  @Test
  public void getName() {
    assertEquals("keyName", key.getName());
  }


  @Test
  public void hasPermission() {
    assertTrue(key.hasPermission("magic"));
    assertFalse(key.hasPermission("mundane"));
  }


  @Test
  public void serialise() throws IOException {
    Store store = new Store(new HashMap<>());
    store.put(key.getKey(), key);

    ObjectWriter writer = MsgPack.writer();
    byte[] data = writer.writeValueAsBytes(store);

    Store store2 = MsgPack.reader(Store.class).readValue(data);
    assertEquals(store, store2);

    ObjectMapper mapper = new ObjectMapper();
    JSONObject jsonObject = mapper.convertValue(store, JSONObject.class);
    MPWrappedMap<String, Object> mpWrappedMap = new MPWrappedMap<>(jsonObject);
    data = MsgPackUtil.pack(mpWrappedMap);
    mpWrappedMap = (MPWrappedMap<String, Object>) MsgPackUtil.unpackObject(MsgPackUtil.newUnpacker(data));
    store2 = mapper.convertValue(mpWrappedMap, Store.class);
    assertEquals(store, store2);
  }


  @Test
  public void setKey() {
    String hex1 = "3059301306072a8648ce3d020106082a8648ce3d0301070342000473cde7710bdbbfc457a59a1292ea2ca30b8dff1227b0fbd96b8833de9e40b905010dac5bc2375f"
        + "c91a29ff3e968b473b7900d47dc5fcc13645b48b68fc6dc893";
    String hex2 = "3059301306072a8648ce3d020106082a8648ce3d0301070342000405443c5b3d6b0cc609abc1e234034b174376394d8b6f5cd74e35b8377b060c0bec69fa0ca86332"
        + "05701da69506bf89a1b7a9a911e02a2ea6beaa14367b1c2e7a";

    PrivilegedKey key1 = key.setKey(Instant.now().getEpochSecond() - 1000, KeyGen.getPublicKey(hex1), 60);
    assertEquals(hex1, key1.getHexPublicKey());
    assertEquals("keyName", key1.getName());
    assertEquals("BDSGaqin5MHOcGB4DOsXAhq9Vp-L1SWmdw", key1.getAddress());
    assertTrue(key1.getPermissions().isEmpty());

    key1 = key.setKey(Instant.now().getEpochSecond() + 1000, KeyGen.getPublicKey(hex2), 70);
    assertEquals(hex2, key1.getHexPublicKey());
    assertEquals("keyName", key1.getName());
    assertEquals("BKt6JiL3E3g9rZj2G1faNyn5_IK7o8Af0A", key1.getAddress());
    assertFalse(key1.getPermissions().isEmpty());
  }


  @Test
  public void testHashCode() {
    int hc1 = key.hashCode();
    PrivilegedKey key1 = key.addPermission("card trick", 70);
    assertNotEquals(hc1, key1.hashCode());
  }

}