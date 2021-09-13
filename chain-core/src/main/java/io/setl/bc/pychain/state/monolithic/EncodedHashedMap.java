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
package io.setl.bc.pychain.state.monolithic;

import io.setl.bc.pychain.HashedMap;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.msgpack.core.MessageBufferPacker;

/**
 * A hashed map where the values are encoded as a specific type.
 *
 * @author Simon Greatrix on 2019-06-25.
 */
public class EncodedHashedMap<K extends Comparable<? super K>, V, Y> extends HashedMap<K, V> {


  protected final Function<V, Y> valueEncoder;

  protected MPWrappedMap<K, Y> encoded = null;


  public EncodedHashedMap(Function<V, Y> valueEncoder) {
    this.valueEncoder = valueEncoder;
  }


  /**
   * New instance.
   *
   * @param valueEncoder  function to encode values into their persisted format.
   * @param valueDecoder  function to decode values from their persisted format.
   * @param initialValues the initial values in this map
   */
  public EncodedHashedMap(
      Function<V, Y> valueEncoder,
      Function<Y, V> valueDecoder,
      Map<K, Y> initialValues
  ) {
    for (Entry<K, Y> e : initialValues.entrySet()) {
      K k = e.getKey();
      V v = valueDecoder.apply(e.getValue());
      map.put(k, v);
    }
    this.valueEncoder = valueEncoder;
  }


  @Override
  protected Map<K, V> createNewMap(int suggestedSize) {
    encoded = null;
    return super.createNewMap(suggestedSize);
  }


  /**
   * Get the encoded form of this instance.
   *
   * @return the encoded data
   */
  public MPWrappedMap<K, Y> getEncoded() {
    if (encoded == null) {
      // Linked hash map maintains order.
      LinkedHashMap<K, Y> halfWay = new LinkedHashMap<>();
      for (Entry<K, V> e : entrySet()) {
        halfWay.put(e.getKey(), valueEncoder.apply(e.getValue()));
      }
      encoded = new MPWrappedMap<>(halfWay);
    }
    return encoded;
  }


  @Override
  protected byte[] toBytes() {
    try (MessageBufferPacker packer = MsgPackUtil.newBufferPacker()) {
      MsgPackUtil.packAnything(packer, getEncoded());
      return packer.toByteArray();
    } catch (IOException ioe) {
      throw new IllegalArgumentException("Unable to serialize", ioe);
    }
  }
}
