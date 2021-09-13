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
package io.setl.common;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.util.MsgPackUtil;

/**
 * Read a mixed type array.
 *
 * @author Simon Greatrix on 21/01/2020.
 */
public class ObjectArrayReader {

  public static <K, T extends Collection<K>, I extends T> T convertToCollection(
      Supplier<I> creator,
      Function<I, T> finisher,
      Object o,
      Function<Object, K> loader
  ) {
    I collection = creator.get();
    if (o instanceof Object[]) {
      for (Object v : (Object[]) o) {
        collection.add(loader.apply(v));
      }
    } else {
      collection.add(loader.apply(o));
    }
    return finisher.apply(collection);
  }


  /**
   * Converts an object to an unmodifiable sorted set.
   *
   * @param o      the object
   * @param loader the method of loading the entries
   *
   * @return the sorted set
   */
  public static <K> SortedSet<K> convertToSortedSet(Object o, Function<ObjectArrayReader, K> loader) {
    return ObjectArrayReader.<K, SortedSet<K>, TreeSet<K>>convertToCollection(
        TreeSet::new,
        Collections::unmodifiableSortedSet,
        o,
        v -> loader.apply(new ObjectArrayReader((Object[]) v))
    );
  }


  /**
   * Converts an object to an unmodifiable sorted set.
   *
   * @param o the object
   *
   * @return the sorted set
   */
  public static SortedSet<String> convertToSortedStrings(Object o) {
    return ObjectArrayReader.<String, SortedSet<String>, TreeSet<String>>convertToCollection(
        TreeSet::new,
        Collections::unmodifiableSortedSet,
        o,
        String::valueOf
    );
  }


  public static <K, V> Object[] encode(Map<K, V> map, Function<K, Object> keyEncoder, Function<V, Object> valueEncoder) {
    int s = map.size();
    Object[] output = new Object[s * 2];
    int i = 0;
    for (Entry<K, V> entry : map.entrySet()) {
      output[i++] = keyEncoder.apply(entry.getKey());
      output[i++] = valueEncoder.apply(entry.getValue());
    }
    return output;
  }


  private final Object[] queue;

  private int position = 0;


  /**
   * New instance.
   *
   * @param queue the queue of objects to read through
   */
  public ObjectArrayReader(Object[] queue) {
    this.queue = queue.clone();
  }


  public ObjectArrayReader(MessageUnpacker unpacker) throws IOException {
    MPWrappedArray array = MsgPackUtil.unpackWrapped(unpacker);
    queue = array.unwrap();
  }


  private int checkNext() {
    if (remaining() == 0) {
      throw new NoSuchElementException();
    }
    int p = position;
    position++;
    return p;
  }


  /**
   * Get the next element as an object array.
   *
   * @return the object array
   */
  public Object[] getArray() {
    Object o = getObject();
    if (o instanceof Object[]) {
      return (Object[]) o;
    }
    return new Object[]{o};
  }


  /**
   * Get the next element as a byte array.
   *
   * @return the byte array
   */
  public byte[] getBinary() {
    return (byte[]) getObject();
  }


  /**
   * Get the next element as a Boolean.
   *
   * @return the next boolean
   */
  public Boolean getBoolean() {
    return TypeSafeMap.asBoolean(getObject());
  }


  /**
   * Get the next element as a Byte.
   *
   * @return the next byte
   */
  public Byte getByte() {
    Integer i = getInt();
    if (i == null) {
      return null;
    }
    return i.byteValue();
  }


  /**
   * Get the next element as a byte.
   *
   * @param dflt the default to return, if the value is null or not a number.
   *
   * @return the byte
   */
  public byte getByte(byte dflt) {
    Byte b = getByte();
    return b != null ? b : dflt;
  }


  /**
   * Get the next element as a Double.
   *
   * @return the Double
   */
  public Double getDouble() {
    return TypeSafeMap.asDouble(getObject());
  }


  /**
   * Get the next element as a double
   *
   * @param dflt the default to return, if the value is null or not a number.
   *
   * @return the double
   */
  public double getDouble(double dflt) {
    Double d = TypeSafeMap.asDouble(getObject());
    return d != null ? d : dflt;
  }


  /**
   * The next value as an Integer.
   *
   * @return the integer
   */
  public Integer getInt() {
    return TypeSafeMap.asInt(getObject());
  }


  /**
   * The next value as an int.
   *
   * @param dflt the default to return if the value is null or not a number
   *
   * @return the int
   */
  public int getInt(int dflt) {
    Integer i = TypeSafeMap.asInt(getObject());
    return i != null ? i : dflt;
  }


  /**
   * The next value as a Long.
   *
   * @return the Long
   */
  public Long getLong() {
    return TypeSafeMap.asLong(getObject());
  }


  /**
   * The next value as a long.
   *
   * @param dflt the default to return if the value is null or not a number
   *
   * @return the long
   */
  public long getLong(long dflt) {
    Long l = getLong();
    return l != null ? l : dflt;
  }


  public <K, V, T extends Map<K, V>> T getMap(Supplier<T> mapCreator, Function<Object, K> keyLoader, Function<Object, V> valueLoader) {
    T map = mapCreator.get();
    Object[] array = getArray();
    for (int i = 0; i < array.length; i += 2) {
      map.put(keyLoader.apply(array[i]), valueLoader.apply(array[i + 1]));
    }
    return map;
  }


  /**
   * The next value as a map.
   *
   * @return the map.
   */
  public TypeSafeMap getMap() {
    return TypeSafeMap.asMap(getObject());
  }


  /**
   * The next value.
   *
   * @return the next value
   */
  public Object getObject() {
    return queue[checkNext()];
  }


  /**
   * Get the next value as an object array with a reader over it.
   *
   * @return the next value as a reader
   */
  public ObjectArrayReader getReader() {
    return new ObjectArrayReader(getArray());
  }


  /**
   * The next value a String.
   *
   * @return the next value
   */
  public String getString() {
    return TypeSafeMap.asString(getObject());
  }


  /**
   * The next value as a String.
   *
   * @param dflt the default to return if the value is null
   *
   * @return the String
   */
  public String getString(String dflt) {
    String s = getString();
    return s != null ? s : dflt;
  }


  /**
   * The next value as an array of Strings.
   *
   * @return the array
   */
  public String[] getStringArray() {
    Object[] array = getArray();
    if (array instanceof String[]) {
      return (String[]) array;
    }
    int l = array.length;
    String[] output = new String[l];
    for (int i = 0; i < l; i++) {
      output[i] = TypeSafeMap.asString(array[i]);
    }
    return output;
  }


  /**
   * Get the next value with the specified conversion applied.
   *
   * @param caster the conversion function
   * @param <T>    the desired return type
   *
   * @return the value
   */
  public <T> T getValue(Function<Object, T> caster) {
    return caster.apply(getObject());
  }


  /**
   * Get a value from the reader's backing array at the specified offset from the current position.
   *
   * @param caster the conversion function
   * @param offset the offset
   * @param <T>    the desired return type
   *
   * @return the value
   */
  public <T> T getValue(Function<Object, T> caster, int offset) {
    return caster.apply(queue[position + offset]);
  }


  /**
   * Does this reader have a next value?.
   *
   * @return true if there is another value
   */
  public boolean hasNext() {
    return remaining() > 0;
  }


  /**
   * How many items are left in this reader?.
   *
   * @return number of items remaining
   */
  public int remaining() {
    return queue.length - position;
  }


  /**
   * Skip the next value.
   */
  public void skip() {
    checkNext();
  }


  /**
   * Skip the next N values.
   *
   * @param toSkip the number of values to skip
   *
   * @return the number actually skipped
   */
  public int skip(int toSkip) {
    int p = position;
    position = Math.min(p + toSkip, queue.length);
    return position - p;
  }


  /**
   * Unread the last value.
   */
  public void unread() {
    if (position > 0) {
      position--;
    }
  }

}
