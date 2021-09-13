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
package io.setl.bc.pychain.state.monolithic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.ipfs.MerkleStoreWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 19/03/2018.
 */
public class KeyedIndexedEntryListTest {

  public static EntryDecoder<TestEntry> decoder = va -> new TestEntry(va.asString(0), va.asString(1));



  /**
   * Decoder for test entries.
   */
  public static class TestDecoder implements EntryDecoder<TestEntry> {

    @Override
    public TestEntry decode(MPWrappedArray va) {
      return new TestEntry(va.asString(0));
    }
  }



  /**
   * A entry in the test list.
   */
  public static class TestEntry implements MEntry {

    public String key;

    public String value;

    private long updateHeight = -1;


    public TestEntry(String key) {
      this(key, "");
    }


    public TestEntry(String key, String value) {
      this.key = key;
      this.value = value;
    }


    @Override
    public TestEntry copy() {
      return new TestEntry(key, value);
    }


    @Override
    public Object[] encode(long index) {
      return new Object[]{key, value};
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TestEntry)) {
        return false;
      }
      TestEntry testEntry = (TestEntry) o;
      return Objects.equals(key, testEntry.key) && Objects.equals(value, testEntry.value);
    }


    public long getBlockUpdateHeight() {
      return updateHeight;
    }


    @Override
    public String getKey() {
      return key;
    }


    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }


    public void setBlockUpdateHeight(long updateHeight) {
      this.updateHeight = Math.max(updateHeight, this.updateHeight);
    }


    @Override
    public String toString() {
      return "TestEntry{key='" + key + "','" + value + "'}";
    }

  }



  /**
   * A test list.
   */
  public static class TestList extends KeyedIndexedEntryList<TestEntry> {

    public TestList() {
      super(new MPWrappedArrayImpl(new Object[]{0, 0, 0, 0, new Object[0]}), 3, decoder, TestEntry.class);
    }


    public TestList(Object[] data) {
      super(new MPWrappedArrayImpl(data), 3, decoder, TestEntry.class);
    }
  }



  private KeyedIndexedEntryList<TestEntry> instance = new TestList();


  @Before
  public void setUp() {
    instance.update(-1, new TestEntry("A"));
    instance.update(-1, new TestEntry("B"));
    instance.update(-1, new TestEntry("C"));
    instance.update(-1, new TestEntry("D"));
  }


  @Test
  public void testComputeRootHash() {
    assertEquals("3443a735eb580ad50fb2d13235df04a96d3d7017083b45c4c4ec73bd39fa6afe", instance.computeRootHash());
    assertEquals("", new TestList().computeRootHash());
  }


  @Test
  public void testEncodeDecode() {
    Object[] encoded = instance.encode();
    TestList decoded = new TestList(encoded);
    assertEquals(instance.getEntryCount(), decoded.getEntryCount());
    instance.forEach(e -> {
      assertEquals(e, decoded.find(e.getKey()));
    });
  }


  @Test
  public void testIterate() throws NoSuchAlgorithmException {
    @SuppressWarnings("unchecked") MerkleStoreWriter<Object[]> msw = mock(MerkleStoreWriter.class);
    instance.setMerkleStoreWriter(msw);

    try {
      instance.iterate(msw);
      fail();
    } catch (RuntimeException e) {
      // correct
    }

    instance.computeRootHash();
    instance.iterate(msw);

    verify(msw).put(any(), eq(new Object[]{"A", ""}));
    verify(msw).put(any(), eq(new Object[]{"B", ""}));
    verify(msw).put(any(), eq(new Object[]{"C", ""}));
    verify(msw).put(any(), eq(new Object[]{"D", ""}));
  }


  @Test
  public void testRemove() throws IOException {
    // Initial order is A,B,C,D
    long idB = instance.findIndex("B");

    instance.remove(idB, "B");

    // Order should be A,D,C
    assertEquals(-1, instance.findIndex("B"));
    assertEquals("D", instance.get(idB).key);

    long idA = instance.findIndex("A");
    instance.remove(-1, "A");
    // Order should be C,D
    assertEquals(-1, instance.findIndex("A"));
    assertEquals("C", instance.get(idA).key);
    assertEquals("D", instance.get(idB).key);
  }


  @Test
  public void testUpdate() throws IOException {
    long id = instance.findIndex("A");
    assertEquals("", instance.get(id).value);
    instance.update(id, new TestEntry("A", "cat"));
    assertEquals(id, instance.findIndex("A"));
    assertEquals("cat", instance.get(id).value);
  }
}
