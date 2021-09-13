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

import io.setl.util.LRUCache;
import java.util.function.Function;

/**
 * Recycle common values.
 *
 * @author Simon Greatrix on 2019-04-01.
 */
public class NearlyConstant {

  private static int CACHE_SIZE = 20_000;

  /** Constants used in messages. Reduces memory usage. */
  private static LRUCache<Object, Object> CONSTANTS = new LRUCache<>(CACHE_SIZE, Function.identity());


  /**
   * Get the common representation of a String. If <code>a</code> and <code>b</code> are Strings, then it is very likely that <code>fixed(a)==fixed(b)
   * </code>, but not guaranteed.
   *
   * @param v the String to find a common representation for
   *
   * @return the common representation
   */
  public static String fixed(String v) {
    if (v == null) {
      return null;
    }
    return (String) CONSTANTS.get(v);
  }


  /**
   * Get the common representation of a Long. If <code>a</code> and <code>b</code> are Long, then it is very likely that <code>fixed(a)==fixed(b)
   * </code>, but not guaranteed.
   *
   * @param v the Long to find a common representation for
   *
   * @return the common representation
   */
  public static Long fixed(Long v) {
    if (v == null) {
      return null;
    }
    return (Long) CONSTANTS.get(v);
  }


  /**
   * Replace as many of the items in the array with their common representations.
   *
   * @param v the array to process
   */
  public static void fixed(Object[] v) {
    if (v == null) {
      return;
    }
    for (int i = v.length - 1; i >= 0; i--) {
      Object u = v[i];
      if (u instanceof String) {
        v[i] = CONSTANTS.get(u);
      } else if (u instanceof Long) {
        v[i] = CONSTANTS.get(u);
      } else if (u instanceof Object[]) {
        fixed((Object[]) u);
      }
    }
  }


  /**
   * Set the number of common representations retained in memory at any one time.
   *
   * @param newSize the new cache size
   */
  public static void setCacheSize(int newSize) {
    if (newSize != CACHE_SIZE) {
      CONSTANTS = new LRUCache<>(newSize, Function.identity());
      CACHE_SIZE = newSize;
    }
  }
}
