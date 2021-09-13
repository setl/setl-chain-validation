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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.setl.bc.pychain.msgpack.MsgPackable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.core.MessagePacker;


/**
 * @author Simon Greatrix on 18/07/2018.
 */
public class ShortTermStorageTest {

  static class V implements MsgPackable {

    String q;


    V(String q) {
      this.q = q;
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      V v = (V) o;
      return Objects.equals(q, v.q);
    }


    @Override
    public int hashCode() {

      return Objects.hash(q);
    }


    @Override
    public void pack(MessagePacker p) throws Exception {
      p.packString(q);
    }
  }



  FileSystem fileSystem;

  Path folder;

  private Random random = new Random(0x7e57ab1e);


  private int countFiles() throws IOException {
    DirectoryStream<Path> stream = Files.newDirectoryStream(folder);
    Iterator<Path> iterator = stream.iterator();
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return count;
  }


  @Before
  public void setUp() throws IOException {
    FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    folder = fs.getPath("/test");
    Files.createDirectory(folder);
  }


  @Test
  public void test1() throws Exception {
    // write some values out, close the store, re-open the store and check the values are still there.
    HashMap<String, V> map = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      map.put(Integer.toString(i), new V(Long.toString(random.nextLong())));
    }

    ShortTermStorage<V> sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    for (Entry<String, V> e : map.entrySet()) {
      sts.put(e.getKey(), e.getValue());
    }
    sts.close();

    sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    for (Entry<String, V> e : map.entrySet()) {
      assertEquals(e.getValue(), sts.get(e.getKey()));
    }

    assertEquals(33, countFiles());
  }


  @Test
  public void test2() throws Exception {
    // write some values out, close the store, re-open the store and check the values are still there.
    HashMap<String, V> map = new HashMap<>();
    ShortTermStorage<V> sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    for (int i = 0; i < 1000; i++) {
      String k = Integer.toString(i);
      V v = new V(Long.toString(random.nextLong()));
      map.put(k, v);
      sts.put(k, v);
      if (random.nextInt(3) == 1) {
        int r = random.nextInt(i + 1);
        k = Integer.toString(r);
        map.remove(k);
        sts.remove(k);
      }
    }

    sts.close();

    sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    for (Entry<String, V> e : map.entrySet()) {
      assertEquals(e.getValue(), sts.get(e.getKey()));
    }

    assertEquals(36, countFiles());
  }


  @Test
  public void test3() throws Exception {
    // write some values out, close the store, re-open the store and check the values are still there.
    HashMap<String, V> map = new HashMap<>();
    ShortTermStorage<V> sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    for (int i = 0; i < 1000; i++) {
      String k = Integer.toString(i);
      V v = new V(Long.toString(random.nextLong()));
      map.put(k, v);
      sts.put(k, v);
      if (random.nextInt(3) == 1) {
        int r = random.nextInt(i + 1);
        k = Integer.toString(r);
        map.remove(k);
        sts.remove(k);
      }
    }

    sts.close();

    sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    for (int i = 0; i < 800; i++) {
      String k = Integer.toString(i);
      map.remove(k);
      sts.remove(k);
    }

    for (Entry<String, V> e : map.entrySet()) {
      assertEquals(e.getValue(), sts.get(e.getKey()));
    }

    assertEquals(18, countFiles());

    for (int i = 800; i < 1000; i++) {
      String k = Integer.toString(i);
      map.remove(k);
      sts.remove(k);
    }

    assertEquals(1, countFiles());
  }


  @Test
  public void testLoad() throws Exception {
    ShortTermStorage<V> sts = new ShortTermStorage<>(folder, 1024, "test", u -> new V(u.unpackString()));
    LinkedList<String> keys = new LinkedList<>();
    for (int i = 0; i < 1000; i++) {
      if (random.nextBoolean()) {
        // insert
        String k = Integer.toString(i);
        V v = new V(Long.toString(random.nextLong()));
        keys.addLast(k);
        sts.put(k, v);
      } else if (!keys.isEmpty()) {
        String k = keys.removeFirst();
        sts.remove(k);
      }
    }

    assertEquals(keys.size(),sts.size());
  }
}