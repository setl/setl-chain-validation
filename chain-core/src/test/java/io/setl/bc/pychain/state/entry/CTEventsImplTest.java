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
package io.setl.bc.pychain.state.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.MsgPack;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

/**
 * @author Simon Greatrix on 18/12/2019.
 */
public class CTEventsImplTest {

  CTEventsImpl instance;


  @Test
  public void add() {
    // We did "add" in set up
    assertTrue(instance.eventExists("Lisa", 2000));
    assertFalse(instance.eventExists("Homer", 2000));
    assertFalse(instance.eventExists("Bart", 2345));
  }


  @Before
  public void before() {
    instance = new CTEventsImpl();
    instance.add("Homer", 1000);
    instance.add("Marge", 1000);
    instance.add("Bart", 2000);
    instance.add("Lisa", 2000);
    instance.add("Maggie", 4000);
  }


  @Test
  public void coverage() {
    instance.hashCode();
    instance.toString();
    assertTrue(instance.equals(instance));
    assertFalse(instance.equals(null));
  }


  @Test
  public void encode() {
    Object[] data = instance.encode();
    assertEquals(3, data.length);
    assertEquals(Long.valueOf(2000L), ((Object[]) data[1])[0]);
    assertEquals("Homer", ((Object[]) ((Object[]) data[0])[1])[0]);
    assertEquals("Marge", ((Object[]) ((Object[]) data[0])[1])[1]);

    CTEventsImpl other = new CTEventsImpl(new MPWrappedArrayImpl(data));
    assertEquals(instance, other);
  }


  @Test
  public void firstTime() {
    assertEquals(1000L, instance.firstTime());

    instance.remove("Marge", 1000);
    instance.remove("Homer", 1000);
    assertEquals(2000L, instance.firstTime());

    assertEquals(0L, new CTEventsImpl().firstTime());
  }

  @Test
  public void firstTimeIgnoring() {
    assertEquals(1000L, instance.firstTimeIgnoring(null));
    Map<Long,Set<String>> ignored = new HashMap<>();
    ignored.put(1000L,Set.of("Marge","Homer"));
    assertEquals(2000L, instance.firstTimeIgnoring(ignored));
    ignored.put(2000L,Set.of("Bart"));
    assertEquals(2000L, instance.firstTimeIgnoring(ignored));
    ignored.put(2000L,Set.of("Bart","Lisa"));
    assertEquals(4000L, instance.firstTimeIgnoring(ignored));
    ignored.put(4000L,Set.of("Maggie"));
    assertEquals(0L, instance.firstTimeIgnoring(ignored));
  }

  @Test
  public void getEventDetailsBefore() {
    SortedMap<Long, SortedSet<String>> map = instance.getEventDetailsBefore(2500);
    assertTrue(map.containsKey(1000L));
    assertTrue(map.containsKey(2000L));
    assertEquals(2, map.get(1000L).size());
    assertEquals(2, map.get(2000L).size());

    map = instance.getEventDetailsBefore(2500, Set.of("Marge","Lisa"));
    assertTrue(map.containsKey(1000L));
    assertTrue(map.containsKey(2000L));
    assertEquals(1, map.get(1000L).size());
    assertEquals(1, map.get(2000L).size());
  }


  @Test
  public void getEventsBefore() {
    SortedSet<String> set = instance.getEventsBefore(2500, 3);
    assertEquals(3, set.size());
    assertTrue(set.contains("Homer"));
    assertTrue(set.contains("Marge"));
    assertTrue(set.contains("Bart"));
  }


  @Test(expected = IllegalStateException.class)
  public void lock() {
    instance.lock();
    instance.remove("Homer", 1000);
  }


  @Test
  public void pack() throws IOException {
    byte[] data;
    try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      instance.pack(packer);
      packer.flush();
      data = packer.toByteArray();
    }
    try(MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
      CTEventsImpl other = new CTEventsImpl(unpacker);
      assertEquals(instance,other);
    }

  }


  @Test
  public void remove() {
    CTEventsImpl other = new CTEventsImpl(instance);
    other.remove("Homer",1000);
    other.remove("Marge",1000);
    other.remove("NotPresent",1000);

    assertNotEquals(instance,other);
    assertEquals(2000L,other.firstTime());
  }


  @Test
  public void update() {
    instance.update(Map.of(500L,Set.of("MrBurns")), Map.of(1500L, Set.of("Krusty"), 4000L,Set.of("Maggie")));
  }
}