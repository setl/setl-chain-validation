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


import io.setl.common.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PairSerializer {

  public static Object[][] serialize(Pair<?, ?>[] pairs) {
    return Arrays.stream(pairs).map(p -> new Object[]{p.left(), p.right()}).collect(Collectors.toList()).toArray(new Object[pairs.length][]);
  }


  /**
   * Convert a List of Pairs as to an array of two element arrays for serialization.
   *
   * @param pairs the pairs to serialize
   * @param <L>   type of the pair's left element
   * @param <R>   type of the pair's right element
   *
   * @return the serialized pairs
   */
  public static <L, R> Object[][] serialize(List<Pair<L, R>> pairs) {
    Object[][] output = new Object[pairs.size()][];
    int i = 0;
    for (Pair<?, ?> pair : pairs) {
      output[i] = new Object[]{pair.left(), pair.right()};
      i++;
    }
    return output;
  }
}
