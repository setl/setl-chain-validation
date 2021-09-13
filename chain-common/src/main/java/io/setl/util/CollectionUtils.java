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

import io.setl.common.TypeSafeMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CollectionUtils {

  /**
   * Combine all the lists into a single collection.
   *
   * @param supplier supplier of the collection
   * @param lists    the lists to combine
   * @param <T>      List entry type
   * @param <X>      Collection type
   *
   * @return the collection containing the combined elements
   */
  @SafeVarargs
  public static <T, X extends Collection<T>> X joiner(Supplier<X> supplier, List<T>... lists) {

    X collection = supplier.get();

    for (List<T> list : lists) {
      collection.addAll(list);
    }

    return collection;
  }


  /**
   * Convert an array of keys and values to a map.
   *
   * @param keysAndValues the keys and values
   *
   * @return the map
   */
  public static TypeSafeMap map(Object... keysAndValues) {
    int length = keysAndValues.length;
    if ((length & 1) == 1) {
      throw new IllegalArgumentException("Map requires an equal number of pairs and values");
    }
    TypeSafeMap map = new TypeSafeMap();
    for (int i = 0; i < length; i += 2) {
      map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
    }
    return map;
  }


  /**
   * Convert an array of keys and values to a map.
   *
   * @param keysAndValues the keys and values
   *
   * @return the map
   */
  public static <K, V> Map<K, V> mapKV(Class<K> keyClass, Class<V> valueClass, Object... keysAndValues) {
    int length = keysAndValues.length;
    if ((length & 1) == 1) {
      throw new IllegalArgumentException("Map requires an equal number of pairs and values");
    }
    HashMap<K, V> map = new HashMap<>();
    for (int i = 0; i < length; i += 2) {
      map.put(keyClass.cast(keysAndValues[i]), valueClass.cast(keysAndValues[i + 1]));
    }
    return map;
  }


  /**
   * Sort the maps in a collection, replacing any contained maps with sorted maps.
   *
   * @param input the input collection
   *
   * @return the collection with sorted maps.
   */
  public static JSONArray order(Collection<?> input) {
    JSONArray output = new JSONArray();
    for (Object v : input) {
      if (v instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) v;
        output.add(order(m));
      } else if (v instanceof Collection) {
        output.add(order((Collection<?>) v));
      } else {
        output.add(v);
      }
    }
    return output;
  }


  /**
   * Put an map into alphabetical order of keys.
   *
   * @param input the input map
   *
   * @return a JSONObject with sorted keys
   */
  public static JSONObject order(Map<String, Object> input) {
    JSONObject output = new JSONObject(true);
    for (Map.Entry<String, Object> entry : input.entrySet()) {
      Object v = entry.getValue();
      if (v instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) v;
        output.put(entry.getKey(), order(m));
      } else if (v instanceof Collection) {
        output.put(entry.getKey(), order((Collection<?>) v));
      } else {
        output.put(entry.getKey(), v);
      }
    }
    return output;
  }


  /**
   * Convert an array of values into a set.
   *
   * @param values the values
   * @param <T>    the values' type
   *
   * @return the set
   */
  @SafeVarargs
  public static <T> Set<T> set(T... values) {
    HashSet<T> hashSet = new HashSet<>();
    Collections.addAll(hashSet, values);
    return hashSet;
  }


  /**
   * Convert an array of values into a sorted set.
   *
   * @param values the values
   * @param <T>    the values' type
   *
   * @return the set
   */
  @SafeVarargs
  public static <T> NavigableSet<T> sorted(T... values) {
    TreeSet<T> treeSet = new TreeSet<>();
    Collections.addAll(treeSet, values);
    return treeSet;
  }


  /**
   * Convert an array of objects, such as may be returned from Message Pack, into an array of integers.
   *
   * @param objects  the objects. Anything that is not a number is ignored.
   * @param sort     true if the output should be sorted
   * @param distinct true if the output should be distinct (implies sorted)
   *
   * @return the integers
   */
  public static int[] toIntArray(Object[] objects, boolean sort, boolean distinct) {
    if (objects == null) {
      return null;
    }

    IntStream intStream = Stream.of(objects).filter(o -> o instanceof Number).mapToInt(o -> ((Number) o).intValue());
    if (distinct) {
      intStream = intStream.distinct();
    }
    if (sort) {
      intStream = intStream.sorted();
    }
    return intStream.toArray();
  }
}
