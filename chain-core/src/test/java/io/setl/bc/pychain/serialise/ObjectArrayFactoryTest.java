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
package io.setl.bc.pychain.serialise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.bc.pychain.state.entry.MEntry;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.junit.Test;

/**
 * @author Simon Greatrix on 03/12/2019.
 */
public class ObjectArrayFactoryTest {

  static class TestEntry implements MEntry {

    private long height = 0;

    private String key;

    private int value;


    public TestEntry(String key, int value) {
      this.key = key;
    }


    public TestEntry(MPWrappedArray array) {
      key = array.asString(0);
      value = array.asInt(1);
      height = array.asInt(2);
    }


    @Override
    public MEntry copy() {
      return new TestEntry(key, value);
    }


    @Override
    public Object[] encode(long index) {
      return new Object[]{key, value, height};
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
      return height == testEntry.height &&
          value == testEntry.value &&
          key.equals(testEntry.key);
    }


    @Override
    public long getBlockUpdateHeight() {
      return height;
    }


    @Nonnull
    @Override
    public String getKey() {
      return key;
    }


    @Override
    public int hashCode() {
      return Objects.hash(height, key, value);
    }


    @Override
    public void setBlockUpdateHeight(long blockHeight) {
      height = blockHeight;
    }
  }



  static class TestFactory implements EntryDecoder<TestEntry> {

    @Override
    public TestEntry decode(MPWrappedArray va) {
      return new TestEntry(va);
    }
  }



  ObjectArrayFactory<TestEntry> factory = new ObjectArrayFactory<>(TestEntry.class, new TestFactory());


  @Test
  public void asContent() {
    Content c1 = factory.asContent(Digest.TYPE_SHA_256, new TestEntry("tom", 1));
    Content c2 = factory.asContent(Digest.TYPE_SHA_256, new TestEntry("tom", 1));
    Content c3 = factory.asContent(Digest.TYPE_SHA_256, new TestEntry("harry", 1));

    assertEquals(c1.getKey(), c2.getKey());
    assertNotEquals(c1.getKey(), c3.getKey());
  }


  @Test
  public void asValue() {
    TestEntry e1 = new TestEntry("tom", 1);
    Content c1 = factory.asContent(Digest.TYPE_SHA_256, e1);
    TestEntry e2 = factory.asValue(c1.getData());

    assertEquals(e1, e2);
  }


  @Test
  public void getType() {
    assertEquals(TestEntry.class, factory.getType());
  }
}