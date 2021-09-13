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

import static io.setl.util.PairSerializerTest.roman;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.common.TypeSafeMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import org.json.simple.JSONObject;
import org.junit.Test;

/**
 * @author Simon Greatrix on 23/08/2018.
 */
public class CollectionUtilsTest {

  Random rand = new Random(0x7e57ab1e);


  private Map<String, Object> buildObject() {
    Map<String, Object> map = new HashMap<>();
    for (int i = rand.nextInt(4) + 2; i >= 0; i--) {
      Object val;
      if (rand.nextBoolean()) {
        val = String.format("%04x", rand.nextInt(65536));
      } else {
        val = rand.nextInt(256);
      }
      map.put(roman(rand.nextInt(2048)), val);
    }
    return map;
  }


  @Test
  public void joiner() {
    HashSet<String> set = CollectionUtils.joiner(HashSet::new, asList("A", "B", "C"), asList("a", "b", "c"), asList("A", "Z", "a"));
    assertEquals(7, set.size());
    for (char ch : "ABCZabc".toCharArray()) {
      assertTrue(set.contains(Character.toString(ch)));
    }
  }


  @Test
  public void map() {
    TypeSafeMap m = CollectionUtils.map("a", 1, "b", "2", 3, 4);
    assertEquals(3, m.size());
    assertEquals(1, m.get("a"));
    assertEquals("2", m.get("b"));
    assertEquals(4, m.get("3"));

    try {
      CollectionUtils.map(1, 2, 3, 4, 5, 6, 7);
      fail();
    } catch (IllegalArgumentException e) {
      // correct - must have even number of args
    }
  }


  @Test
  public void mapKV() {
    Map<String, Integer> m = CollectionUtils.mapKV(String.class, Integer.class, "A", 1, "B", 2, "C", 3);
    assertEquals(3, m.size());
    assertEquals(1, (Object) m.get("A"));
    assertEquals(2, (Object) m.get("B"));
    assertEquals(3, (Object) m.get("C"));

    try {
      CollectionUtils.mapKV(String.class, Integer.class, "A", 1, "B", 2, "C");
      fail();
    } catch (IllegalArgumentException e) {
      // correct - must have even number of args
    }

    try {
      CollectionUtils.mapKV(String.class, Integer.class, "A", 1, "B", 2, "C", "D");
      fail();
    } catch (ClassCastException e) {
      // correct - "D" is not an Integer
    }
  }


  @Test
  public void orderObject() {
    Map<String, Object> root = buildObject();
    List<Object> list = asList(buildObject(), buildObject(), buildObject(), asList(1, 2, 3));
    root.put(roman(rand.nextInt(2048)), buildObject());
    root.put("list", list);
    JSONObject json = CollectionUtils.order(root);
    assertEquals(
        "{\"DCCCLIII\":{\"MDCCX\":\"29c7\",\"XIV\":28,\"XLII\":\"5de7\"},\"MLVI\":\"2579\",\"MLXXIII\":\"8c44\",\"XXIV\":\"e656\","
            + "\"list\":[{\"MCCXLVII\":145,\"MCDXXXVIII\":\"4363\",\"MCMI\":\"8721\",\"MMXII\":\"d3c2\"},{\"CMXX\":\"5f26\",\"DCXLVI\":\"b35a\","
            + "\"MCCCXXVII\":105,\"MCMXXXIII\":124},{\"CCCLXXX\":\"52f9\",\"DCCXCIX\":187,\"MCXLVII\":140,\"MDCLXXI\":119},[1,2,3]]}",
        json.toJSONString());
  }


  @Test
  public void set() {
    Set<String> s = CollectionUtils.set("A", "B", "C", "C", "C");
    assertEquals(3, s.size());
    assertTrue(s.contains("A"));
    assertTrue(s.contains("B"));
    assertTrue(s.contains("C"));
  }


  @Test
  public void sorted() {
    SortedSet<Integer> s = CollectionUtils.sorted(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7);
    assertEquals(9, s.size());
    assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9]", s.toString());
  }


  @Test
  public void toIntArray() {
    int[] ia = CollectionUtils.toIntArray(new Object[]{3.5, 8.9, 'a', 'b', 1, 2, 'a', 2, 3, 3.7}, false, false);
    assertEquals("[3, 8, 1, 2, 2, 3, 3]", Arrays.toString(ia));

    ia = CollectionUtils.toIntArray(new Object[]{3.5, 8.9, 'a', 'b', 1, 2, 'a', 2, 3, 3.7}, false, true);
    assertEquals("[3, 8, 1, 2]", Arrays.toString(ia));

    ia = CollectionUtils.toIntArray(new Object[]{3.5, 8.9, 'a', 'b', 1, 2, 'a', 2, 3, 3.7}, true, false);
    assertEquals("[1, 2, 2, 3, 3, 3, 8]", Arrays.toString(ia));

    ia = CollectionUtils.toIntArray(new Object[]{3.5, 8.9, 'a', 'b', 1, 2, 'a', 2, 3, 3.7}, true, true);
    assertEquals("[1, 2, 3, 8]", Arrays.toString(ia));
  }
}