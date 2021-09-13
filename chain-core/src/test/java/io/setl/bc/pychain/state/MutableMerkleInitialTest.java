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

import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryListTest.TestEntry;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-12-04.
 */
public class MutableMerkleInitialTest {

  MutableMerkleInitial<TestEntry> instance;


  @Test
  public void randomTests() throws IOException {
    Random rand = new Random(0x7e57ab1e);

    HashMap<String, TestEntry> map = new HashMap<>();
    HashMap<String, TestEntry> start = new HashMap<>(map);

    for (int i = 0; i < 1000; i++) {
      start.clear();

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

      // Instance should look like map
      for (char j = 'A'; j <= 'Z'; j++) {
        String key = Character.toString(j);
        TestEntry e3 = instance.find(key);
        TestEntry e4 = map.get(key);
        assertEquals(e4, e3);
      }
    }
  }


  @Before
  public void setUp() {
    instance = new MutableMerkleInitial<>(TestEntry.class);
  }


  @Test
  public void testAdd() throws IOException {
    assertFalse(instance.hasInsert());
    instance.add(new TestEntry("D"));
    assertTrue(instance.hasInsert());
    instance.add(new TestEntry("E"));
    assertEquals(new TestEntry("E"), instance.find("E"));

    instance.delete("E");
    assertTrue(instance.hasInsert());
    instance.add(new TestEntry("E"));
    assertTrue(instance.hasInsert());
    assertEquals(new TestEntry("E"), instance.find("E"));

    instance.delete("D");
    instance.delete("E");
    assertFalse(instance.hasInsert());
  }


  @Test
  public void testChangedEntriesCount() throws IOException {
    instance.add(new TestEntry("A"));
    assertEquals(1, instance.getChangedEntriesCount());
    instance.findAndMarkUpdated("A");
    assertEquals(1, instance.getChangedEntriesCount());
    instance.findAndMarkUpdated("B");
    assertEquals(1, instance.getChangedEntriesCount());
  }


  @Test
  public void testFind() throws IOException {
    assertNull(instance.find("Z"));
    assertTrue(instance.changes.isEmpty());
  }


  @Test
  public void testFindForUpdate() throws IOException {
    assertNull(instance.findAndMarkUpdated("Z"));
    assertTrue(instance.changes.isEmpty());

    instance.add(new TestEntry("B"));
    instance.add(new TestEntry("C"));

    assertEquals(new TestEntry("C"), instance.findAndMarkUpdated("C"));
    assertFalse(instance.changes.isEmpty());

    TestEntry oldB = instance.find("B");
    TestEntry newB = instance.findAndMarkUpdated("B");
    assertEquals(oldB, newB);
  }


  @Test
  public void testGetLeafType() {
    assertEquals(TestEntry.class, instance.getLeafType());
  }


  @Test
  public void testGetUpdatedKeys() throws IOException {
    instance.add(new TestEntry("A", "foo"));
    instance.add(new TestEntry("C", "bar"));
    instance.findAndMarkUpdated("Z");

    assertEquals(new HashSet<>(Arrays.asList("A", "C")), instance.getUpdatedKeys());
  }


  @Test
  public void testItemExists() throws IOException {
    assertFalse(instance.itemExists("A"));
    instance.add(new TestEntry("A", "cat"));
    assertTrue(instance.itemExists("A"));
    instance.delete("A");
    assertFalse(instance.itemExists("A"));
  }


  @Test
  public void testStream() {
    instance.add(new TestEntry("A", "foo"));
    instance.add(new TestEntry("C", "bar"));
    instance.add(new TestEntry("D", "bat"));
    instance.delete("C");

    Set<TestEntry> actual = instance.stream().collect(Collectors.toSet());
    assertEquals(2, actual.size());
    assertTrue(actual.contains(new TestEntry("A", "foo")));
    assertTrue(actual.contains(new TestEntry("D", "bat")));
  }


  @Test
  public void testWriteTo() {
    TestEntry ta = new TestEntry("A", "foo");
    TestEntry tc = new TestEntry("C", "bar");
    TestEntry td = new TestEntry("D", "bat");

    SnapshotMerkle<TestEntry> snapshotMerkle = mock(SnapshotMerkle.class);
    when(snapshotMerkle.getLeafType()).thenReturn(TestEntry.class);

    instance.add(ta);
    instance.add(tc);
    instance.add(td);
    instance.delete("C");

    instance.writeTo(VERSION_USE_UPDATE_HEIGHT, 5, snapshotMerkle, NoOpChangeListener.INSTANCE);
    verify(snapshotMerkle).update(eq("A"), any());
    verify(snapshotMerkle).update(eq("D"), any());
    verify(snapshotMerkle, times(2)).update(any(), any());
  }
}