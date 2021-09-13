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
package io.setl.bc.pychain.serialise;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.SafeMsgPackable;

/**
 * A content factory for values that are message packable. The value must implement the SafeMsgPackable interface.
 *
 * @author Simon Greatrix on 2019-05-31.
 */
public abstract class BaseMsgPackableFactory<Q extends SafeMsgPackable> implements ContentFactory<Q> {

  @Override
  public Content asContent(int digestType, Q value) {
    try (MessageBufferPacker packer = MsgPackUtil.newBufferPacker()) {
      value.pack(packer);
      return new Content(Digest.create(digestType), packer.toByteArray());
    } catch (IOException e) {
      // Should never happen, as buffer packers do not do I/O.
      throw new ContentException("IOException without I/O", e);
    }
  }


  @Override
  public Q asValue(byte[] data) {
    if (data == null) {
      return null;
    }

    try (MessageUnpacker unpacker = MsgPackUtil.newUnpacker(data)) {
      return newInstance(unpacker);
    } catch (IOException e) {
      throw new ContentException("IOException without I/O", e);
    }
  }


  protected abstract Q newInstance(MessageUnpacker unpacker) throws IOException;

}
