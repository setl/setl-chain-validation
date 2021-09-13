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

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.util.MsgPackUtil;
import org.msgpack.core.MessagePackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard factory for Merkle leaf values that implement the MEntry interface. The MEntry interface provides an encoding as an array of values which a
 * decoder can use to reconstruct the original entry.
 *
 * @param <V> the value class
 */
public class ObjectArrayFactory<V extends MEntry> implements ContentFactory<V> {

  private static final Logger logger = LoggerFactory.getLogger(ObjectArrayFactory.class);

  private final EntryDecoder<V> decoder;

  private final Class<V> type;


  public ObjectArrayFactory(Class<V> type, EntryDecoder<V> decoder) {
    this.type = type;
    this.decoder = decoder;
  }


  @Override
  public Content asContent(int digestType, V value) {
    Object[] array = value.encode(0);
    byte[] binary = HashSerialisation.getInstance().serialise(array);
    return new Content(Digest.create(digestType), binary);
  }


  @Override
  public V asValue(byte[] data) {
    if (data == null) {
      return null;
    }

    MPWrappedArray array = MsgPackUtil.unpackWrapped(data, true);
    return decoder.decode(array);
  }


  @Override
  public Class<V> getType() {
    return type;
  }
}
