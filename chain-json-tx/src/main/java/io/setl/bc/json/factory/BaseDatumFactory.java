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
package io.setl.bc.json.factory;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.json.data.BaseDatum;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.serialise.Content;
import io.setl.bc.pychain.serialise.ContentException;
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.MerkleLeafFactory;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public abstract class BaseDatumFactory<T extends BaseDatum> implements MerkleLeafFactory<T>, ContentFactory<T> {

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // False positive from try-with-resources
  @Override
  public Content asContent(int digestType, T value) {
    Object[] encoded = value.encode();
    // We create a default buffer packer, implicitly allowing support for STR8.
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      MsgPackUtil.packAnything(packer, encoded);
      return new Content(Digest.create(digestType), packer.toByteArray());
    } catch (IOException e) {
      // Should never happen, as buffer packers do not do I/O.
      throw new ContentException("IOException without I/O", e);
    }
  }


  @Override
  public T asValue(byte[] data) {
    if (data == null) {
      return null;
    }

    Object[] encoded;
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
      encoded = MsgPackUtil.unpackWrapped(unpacker).unwrap();
    } catch (IOException e) {
      throw new ContentException("IOException without I/O", e);
    }

    return construct(new ObjectArrayReader(encoded));
  }


  protected abstract T construct(ObjectArrayReader reader);


  @Override
  public ContentFactory<T> getFactory() {
    return this;
  }

}
