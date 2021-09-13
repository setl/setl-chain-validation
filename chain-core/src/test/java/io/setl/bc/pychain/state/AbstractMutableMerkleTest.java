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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryListTest.TestEntry;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * @author Simon Greatrix on 06/12/2019.
 */
public class AbstractMutableMerkleTest {

  TestEntry entry0 = new TestEntry("zero", "0");

  TestEntry entry1 = new TestEntry("one", "1");

  TestEntry entry2 = new TestEntry("two", "2");

  TestEntry entry3 = new TestEntry("three", "3");

  NamedObject named0 = new NamedObject(0, false, "zero", entry0);

  AbstractMutableMerkle<TestEntry> instance = new AbstractMutableMerkle<TestEntry>() {
    @Override
    public void commit(int version, long blockHeight, StateChangeListener changeListener) {

    }


    @Override
    protected NamedObject<TestEntry> findEntryInState(String key) {
      return key.equals("zero") ? named0 : null;
    }


    @Override
    public Class<TestEntry> getLeafType() {
      return TestEntry.class;
    }


    @Override
    protected boolean itemExistsInState(String key) {
      return key.equals("zero");
    }


    @Override
    int nextSequence() {
      return 0;
    }


    @Override
    public Stream<TestEntry> stream() {
      return null;
    }
  };


  @Test
  public void testAdd() {
    instance.add(entry1);
    assertEquals(1, instance.getChangedEntriesCount());

    instance.add(entry2);
    assertEquals(2, instance.getChangedEntriesCount());

    instance.add(entry2);
    assertEquals(2, instance.getChangedEntriesCount());

    instance.delete("two");
    assertEquals(2, instance.getChangedEntriesCount());

    instance.add(entry2);
    assertEquals(2, instance.getChangedEntriesCount());

    assertTrue(instance.unchanged.isEmpty());
    assertEquals(2, instance.changes.size());
  }


  @Test
  public void testBulkChange() {
    HashMap<String, NamedObject<TestEntry>> map = new HashMap<>();
    map.put("one", new NamedObject<>(0, false, "one", entry1));
    map.put("two", new NamedObject<>(0, false, "two", entry2));

    instance.unchanged.put("one", new NamedObject<>(0, false, "one", entry1));

    instance.bulkChange(map);

    assertEquals(2, instance.changes.size());
    assertTrue(instance.unchanged.isEmpty());
  }


  @Test
  public void testDelete() {
    instance.delete("one");
    assertTrue(instance.changes.isEmpty());

    instance.delete("zero");
    assertFalse(instance.changes.isEmpty());

    instance.add(entry1);
    instance.delete("one");
    assertEquals(2, instance.changes.size());
  }


  @Test
  public void testFind() {
    assertEquals(entry0, instance.find("zero"));
    assertEquals(entry0, instance.find("zero"));
    assertEquals(1, instance.unchanged.size());
    assertEquals(0, instance.changes.size());
    assertNull(instance.find("one"));

    instance.add(entry1);
    assertEquals(entry1, instance.find("one"));
  }


  @Test
  public void testFindAndMarkUpdated() {
    assertEquals(entry0, instance.find("zero"));
    assertEquals(entry0, instance.findAndMarkUpdated("zero"));
    assertEquals(1, instance.changes.size());
    assertEquals(0, instance.unchanged.size());
    assertNull(instance.findAndMarkUpdated("one"));

    instance.add(entry1);
    assertEquals(entry1, instance.findAndMarkUpdated("one"));
  }

  @Test
  public void testGetUpdatedKeys() {
    assertEquals(0,instance.getUpdatedKeys().size());
    instance.find("zero");
    assertEquals(0,instance.getUpdatedKeys().size());
    instance.findAndMarkUpdated("zero");
    assertEquals(1,instance.getUpdatedKeys().size());
  }

  @Test
  public void testItemExists() {
    assertTrue(instance.itemExists("zero"));
    instance.find("zero");
    assertTrue(instance.itemExists("zero"));
    instance.findAndMarkUpdated("zero");
    assertTrue(instance.itemExists("zero"));

    instance.delete("zero");
    assertFalse(instance.itemExists("zero"));

    assertFalse(instance.itemExists("wombat"));
  }

  @Test
  public void testReset() {
    instance.find("zero");
    instance.add(entry1);
    assertEquals(1,instance.getChangedEntriesCount());

    instance.reset();
    assertEquals(0,instance.getChangedEntriesCount());
  }
}