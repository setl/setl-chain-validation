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
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryListTest.TestEntry;
import io.setl.bc.pychain.state.monolithic.NamespaceList;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 20/03/2018.
 */
public class MutableMerkleWrapperTest extends MutableMerkleImplementationTest {

  MutableMerkleImplementation<TestEntry> wrapped;


  @Test
  public void randomTests() throws IOException {
    Random rand = new Random(0x7e57ab1e);

    HashMap<String, TestEntry> map = new HashMap<>();
    list.forEach(e -> map.put(e.getKey(), e));
    HashMap<String, TestEntry> start = new HashMap<>(map);


    for (int i = 0; i < 1000; i++) {
      ((MutableMerkleWrapper<?>) instance).reset();

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
      // instance and wrapped should both look like map now
      for (char j = 'A'; j <= 'Z'; j++) {
        String key = Character.toString(j);
        TestEntry e3 = instance.find(key);
        TestEntry e4 = map.get(key);
        TestEntry e5 = wrapped.find(key);
        assertEquals(e4, e3);
        assertEquals(e4, e5);
      }

      wrapped.commit(3,0, NoOpChangeListener.INSTANCE);
      actual.clear();
      list.forEach(e -> actual.put(e.getKey(), e));
      assertEquals(map, actual);
      start = actual;
    }
  }



  @Test
  public void deleteTest() throws Exception {

    SnapshotMerkle<NamespaceEntry> list = new NamespaceList(new MPWrappedArrayImpl(new Object[]{new Object[0]}), 0);

    MutableMerkleWrapper<NamespaceEntry> instance = new MutableMerkleWrapper<>(new MutableMerkleImplementation(list));

    list.update("A", new NamespaceEntry("A", "A", "A"));
    list.update("B", new NamespaceEntry("B", "B", "B"));
    list.update("C", new NamespaceEntry("C", "C", "C"));
    list.update("D", new NamespaceEntry("D", "D", "D"));

    instance.delete("A");
    instance.delete("D");

    instance.commit(3,0, NoOpChangeListener.INSTANCE);

    assertTrue(instance.find("C", false) != null);


  }


  @Before
  public void setUp() {
    super.setUp();
    wrapped = (MutableMerkleImplementation<TestEntry>) instance;
    instance = new MutableMerkleWrapper<>(wrapped);
  }
}