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
package io.setl.bc.pychain.block;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;

/**
 * @author Simon Greatrix on 2019-06-10.
 */
public class MissingTxIdsTest {

  @Test
  public void add() {
    UUID uuid = UUID.randomUUID();
    Hash hash = new Hash(new byte[2]);
    MissingTxIds ids = new MissingTxIds(uuid);
    assertEquals(uuid, ids.getUuid());
    assertTrue(ids.isEmpty());
    assertEquals(0, ids.size());
    ids.add("a1", 100, hash);
    assertFalse(ids.isEmpty());
    assertEquals(1, ids.size());
    assertArrayEquals(new String[]{"a1"}, ids.getAddresses());
    assertTrue(ids.getMissing("a2").isEmpty());

    NonceAndHash nah1 = new NonceAndHash(100, hash);
    NonceAndHash nah2 = new NonceAndHash(999, hash);
    assertTrue(ids.getMissing("a1").contains(nah1));
    assertFalse(ids.getMissing("a1").contains(nah2));
  }


  @Test
  public void addAll() throws IOException {
    UUID uuid = UUID.randomUUID();
    Hash hash = new Hash(new byte[2]);
    MissingTxIds ids = new MissingTxIds(uuid);

    ArrayList<NonceAndHash> list = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      list.add(new NonceAndHash(i, hash));
    }

    ids.addAll("a3", list);

    assertEquals(5, ids.size());

    MessageBufferPacker packer = MsgPackUtil.newBufferPacker();
    ids.pack(packer);
    byte[] bytes = packer.toByteArray();

    MPWrappedArray encoded = MsgPackUtil.unpackWrapped(bytes, true);
    MissingTxIds ids2 = new MissingTxIds(encoded);
    assertEquals(5, ids2.size());
  }
}