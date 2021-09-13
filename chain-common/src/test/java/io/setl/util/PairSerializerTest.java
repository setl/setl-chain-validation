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

import static org.junit.Assert.assertEquals;

import io.setl.common.Pair;
import java.util.Arrays;
import java.util.TreeMap;
import org.junit.Test;


/**
 * @author Simon Greatrix on 23/08/2018.
 */
public class PairSerializerTest {

  private static final TreeMap<Integer, String> ROMAN = new TreeMap<>();


  /**
   * Create a roman number.
   *
   * @param number the number
   *
   * @return the roman equivalent
   */
  public static String roman(int number) {
    int l = ROMAN.floorKey(number);
    if (number == l) {
      return ROMAN.get(number);
    }
    return ROMAN.get(l) + roman(number - l);
  }


  static {
    int[] ri = new int[]{
        1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4,
        1
    };
    String[] rs = new String[]{
        "M", "CM", "D", "CD", "C", "XC", "L", "XL",
        "X", "IX", "V", "IV", "I"
    };
    for (int i = 0; i < ri.length; i++) {
      ROMAN.put(ri[i], rs[i]);
    }
  }

  @Test
  public void test() {
    Pair<?, ?>[] pairs = new Pair<?, ?>[5];
    for (int i = 0; i < pairs.length; i++) {
      pairs[i] = new Pair<>(roman(128), roman(256));
    }

    Object[][] objects = PairSerializer.serialize(pairs);
    assertEquals("[[CXXVIII, CCLVI], [CXXVIII, CCLVI], [CXXVIII, CCLVI], [CXXVIII, CCLVI], [CXXVIII, CCLVI]]", Arrays.deepToString(objects));
  }

}
