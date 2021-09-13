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
package io.setl.bc.pychain.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryListTest.TestEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import org.junit.Test;

/**
 * @author Simon Greatrix on 20/03/2018.
 */
public class IndexedObjectTest {

  @Test
  public void compareTo() {
    TestEntry entry = new TestEntry("A");
    ArrayList<IndexedObject<TestEntry>> list = new ArrayList<>();
    list.add(new IndexedObject<>(1, 5, entry));
    list.add(new IndexedObject<>(5, 13, entry));
    list.add(new IndexedObject<>(4, 8, entry));
    list.add(new IndexedObject<>(3, 2, entry));
    list.add(new IndexedObject<>(2, entry));
    list.add(new IndexedObject<>(0, entry));
    list.add(new IndexedObject<>(6, entry));

    list.sort(Comparator.naturalOrder());
    System.out.println(list);
    int[] o1 = new int[]{-1, 5, -1, 2, 8, 13, -1};
    for (int i = 0; i < 7; i++) {
      IndexedObject<TestEntry> io = list.get(i);
      assertEquals("Entry " + i, o1[i], io.index);
      assertEquals(i, io.sequence);
    }
  }


  @Test
  public void copy() {
    TestEntry entry = new TestEntry("A");
    IndexedObject<TestEntry> instance = new IndexedObject<>(5, 5, entry);
    IndexedObject<TestEntry> copy = instance.copy(6);

    assertEquals(5, copy.index);
    assertFalse(instance.isDeleted);
    assertEquals(6, copy.sequence);
    assertEquals(instance.object, copy.object);
    assertNotSame(instance.object, copy.object);
  }


  @Test
  public void test() {
    TestEntry entry = new TestEntry("A");
    IndexedObject<TestEntry> instance = new IndexedObject<>(5, entry);
    assertEquals(-1, instance.index);
    assertFalse(instance.isDeleted);
    assertNotNull(instance.toString());

    HashSet<IndexedObject<TestEntry>> set = new HashSet<>();
    set.add(instance);
    set.add(new IndexedObject<>(13, entry));
    set.add(new IndexedObject<>(8, entry));
    set.add(new IndexedObject<>(4, 26, entry));
    set.add(new IndexedObject<>(5, 36, entry));

    assertTrue(set.contains(instance));
    assertTrue(set.contains(new IndexedObject<>(5, 36, entry)));
  }
}