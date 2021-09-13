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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryListTest.TestEntry;
import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryListTest.TestList;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 19/03/2018.
 */
public class MutableMerkleImplementationTest {

  TestList list = new TestList();

  AbstractMutableMerkle<TestEntry> instance;


  @Test
  public void randomTests() throws IOException {
    Random rand = new Random(0x7e57ab1e);

    HashMap<String, TestEntry> map = new HashMap<>();
    list.forEach(e -> map.put(e.getKey(), e));
    HashMap<String, TestEntry> start = new HashMap<>(map);

    for (int i = 0; i < 1000; i++) {

      for (int j = 0; j < 20; j++) {
        String key = Character.toString((char) ('A' + rand.nextInt(26)));
        TestEntry entry = new TestEntry(key, "<" + i + "," + j + ">");
        int op = rand.nextInt(4);
        switch (op) {
          case 0: // add
            map.putIfAbsent(key, entry);
            instance.add(entry);
            break;

          case 1: // find
            instance.find(key);
            break;

          case 2: // find and update
            TestEntry e2 = instance.findAndMarkUpdated(key);
            if (e2 != null) {
              e2.value = entry.value;
            }

            e2 = map.get(key);
            if (e2 != null) {
              e2.value = entry.value;
            }
            break;

          default: // remove
            instance.delete(key);
            map.remove(key);
            break;
        }
      }

      // Nothing committed yet, so list should be unchanged
      HashMap<String, TestEntry> actual = new HashMap<>();
      list.forEach(e -> actual.put(e.getKey(), e));
      assertEquals(start, actual);

      // Instance should look like map
      for (char j = 'A'; j <= 'Z'; j++) {
        String key = Character.toString(j);
        TestEntry e3 = instance.find(key);
        TestEntry e4 = map.get(key);
        assertEquals(e4, e3);
      }

      instance.commit(3,0, NoOpChangeListener.INSTANCE);
      actual.clear();
      list.forEach(e -> actual.put(e.getKey(), e));
      assertEquals(map, actual);
      start = actual;
    }
  }


  @Before
  public void setUp() {
    list.update(-1, new TestEntry("A"));
    list.update(-1, new TestEntry("B"));
    list.update(-1, new TestEntry("C"));
    list.update(-1, new TestEntry("D"));
    list.computeRootHash();
    instance = new MutableMerkleImplementation<>(list);
    instance.commit(3,0, NoOpChangeListener.INSTANCE);
  }


  @Test
  public void testAdd() throws IOException {
    // re-add does nothing
    instance.add(new TestEntry("D"));
    instance.add(new TestEntry("E"));
    assertEquals(new TestEntry("E"), instance.find("E"));

    instance.delete("E");
    instance.add(new TestEntry("E"));
    assertEquals(new TestEntry("E"), instance.find("E"));

  }


  @Test
  public void testChangedEntriesCount() throws IOException {
    instance.findAndMarkUpdated("A");
    assertEquals(1, instance.getChangedEntriesCount());
    instance.findAndMarkUpdated("A");
    assertEquals(1, instance.getChangedEntriesCount());
    instance.findAndMarkUpdated("B");
    assertEquals(2, instance.getChangedEntriesCount());
  }


  @Test
  public void testFind() throws IOException {
    assertNull(instance.find("Z"));
    assertEquals(new TestEntry("C"), instance.find("C"));
    assertTrue(instance.changes.isEmpty());
  }


  @Test
  public void testFindForUpdate() throws IOException {
    assertNull(instance.findAndMarkUpdated("Z"));
    assertTrue(instance.changes.isEmpty());
    assertEquals(new TestEntry("C"), instance.findAndMarkUpdated("C"));
    assertFalse(instance.changes.isEmpty());

    TestEntry oldB = instance.find("B");
    TestEntry newB = instance.findAndMarkUpdated("B");
    assertNotSame(oldB, newB);
    assertEquals(oldB, newB);
  }



  @Test
  public void testGetUpdatedKeys() throws IOException {
    instance.findAndMarkUpdated("A");
    instance.findAndMarkUpdated("C");
    instance.findAndMarkUpdated("Z");

    assertEquals(new HashSet<>(Arrays.asList("A", "C")), instance.getUpdatedKeys());
  }


  @Test
  public void testItemExists() throws IOException {
    assertTrue(instance.itemExists("A"));
    assertFalse(instance.itemExists("Z"));

    instance.delete("A");
    assertFalse(instance.itemExists("A"));

    instance.add(new TestEntry("A", "cat"));
    assertTrue(instance.itemExists("A"));

    instance.find("B");
    assertTrue(instance.itemExists("B"));
  }


  @Test
  public void testMultipleRemoves1() throws IOException {
    instance.delete("A");
    instance.delete("D");

    instance.commit(3,0, NoOpChangeListener.INSTANCE);

    assertNull(instance.find("A", false));
    assertNotNull(instance.find("C", false));
  }


  @Test
  public void testMultipleRemoves2() throws IOException {
    instance.delete("D");
    instance.delete("A");

    instance.commit(3,0, NoOpChangeListener.INSTANCE);

    assertNull(instance.find("A", false));
    assertNotNull(instance.find("C", false));
  }
}