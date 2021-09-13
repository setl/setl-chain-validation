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
package io.setl.bc.store;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.ipfs.MerkleStore;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.Hex;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around a raw store to provide serialization/deserialization Created by aanten on 01/06/2017.
 */
public class MerkleStoreWrapper implements MerkleStore<Object> {

  private static final Logger logger = LoggerFactory.getLogger(MerkleStoreWrapper.class);

  private final RawStore store;

  private int readCount = 0;

  private int writeCount = 0;


  public MerkleStoreWrapper(RawStore store) {
    this.store = store;
  }


  @Override
  public void flush() {
    store.flush();
  }


  @Override
  public Object get(@Nonnull Hash hash) {
    byte[] bytes;
    try {
      bytes = store.get(hash.get());
      readCount++;
    } catch (Exception e1) {
      logger.error("get", e1);
      throw new RuntimeException(e1);
    }
    if (bytes == null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Not found:{}", Hex.encode(hash.get()));
      }
      return null;
    }

    try {
      MessageUnpacker unpacker = MsgPackUtil.newUnpacker(bytes);
      Object obj = MsgPackUtil.unpackObject(unpacker, true);

      // Message pack does not support byte[][] - so kludge
      if (obj instanceof Object[]) {
        Object[] arr = (Object[]) obj;
        if (arr.length >= 1 && (arr[0] instanceof byte[])) {
          byte[][] oo;
          oo = new byte[arr.length][];
          for (int ii = 0; ii < oo.length; ii++) {
            oo[ii] = (byte[]) arr[ii];
          }
          return oo;
        }
      }

      return obj;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void put(Hash hash, Object data) {
    try {
      byte[] bytes = MsgPackUtil.pack(data);
      store.put(hash.get(), bytes);
      writeCount++;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
}
