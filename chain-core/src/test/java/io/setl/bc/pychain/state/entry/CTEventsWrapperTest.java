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
import static org.junit.Assert.assertTrue;

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

/**
 * @author Simon Greatrix on 18/12/2019.
 */
public class CTEventsWrapperTest {

  CTEventsWrapper emptyOnEmpty;

  CTEventsWrapper emptyOnParent;

  CTEventsImpl emptyParent;

  CTEventsWrapper instOnEmpty;

  CTEventsWrapper instance;

  CTEventsImpl parent;


  @Test
  public void add() {
    assertEquals(500L, instance.firstTime());

    instance.add("Maggie", 4000);
    instance.add("Homer", 1000);
    instance.add("MrBurns", 500);
    assertTrue(instance.eventExists("Maggie", 4000));
  }


  @Before
  public void before() {
    parent = new CTEventsImpl();
    parent.add("Homer", 1000);
    parent.add("Marge", 1000);
    parent.add("Bart", 2000);
    parent.add("Lisa", 2000);
    parent.add("Maggie", 4000);
    instance = new CTEventsWrapper(parent);
    set(instance);

    emptyParent = new CTEventsImpl();
    instOnEmpty = new CTEventsWrapper(emptyParent);
    set(instOnEmpty);

    emptyOnParent = new CTEventsWrapper(parent);

    emptyOnEmpty = new CTEventsWrapper(emptyParent);
  }


  @Test
  public void commit() {
    instance.commit();
    assertEquals(500L, parent.firstTime());
  }


  @Test
  public void eventExists() {
    assertTrue(instance.eventExists("Moe", 1000));
    assertTrue(instance.eventExists("Homer", 1000));
    assertFalse(instance.eventExists("Maggie", 4000));
  }


  @Test
  public void firstTime() {
    assertEquals(500L, instance.firstTime());
    instance.remove("MrBurns", 500);
    instance.remove("Flanders", 900);
    instance.remove("Marge", 1000);
    instance.remove("Homer", 1000);
    instance.remove("Moe", 1000);
    assertEquals(1200L, instance.firstTime());

    assertEquals(1000, emptyOnParent.firstTime());
    assertEquals(500L, instOnEmpty.firstTime());

    instOnEmpty.remove("MrBurns", 500);
    instOnEmpty.remove("Flanders", 900);
    instOnEmpty.remove("Marge", 1000);
    assertEquals(1000L, instOnEmpty.firstTime());

    emptyParent.add("Homer", 1000);
    emptyOnEmpty.remove("Homer", 1000);
    assertEquals(0L, emptyOnEmpty.firstTime());
    emptyParent.add("Marge", 1000);
    assertEquals(1000L, emptyOnEmpty.firstTime());
  }


  @Test
  public void firstTimeIgnoring() {
    assertEquals(500L, instance.firstTimeIgnoring(null));
    Map<Long, Set<String>> ignored = new HashMap<>();
    ignored.put(500L, Set.of("MrBurns"));
    ignored.put(900L, Set.of("Flanders"));
    ignored.put(1000L, Set.of("Marge", "Homer", "Moe"));
    assertEquals(1200L, instance.firstTimeIgnoring(ignored));
    ignored.put(1200L, Set.of("Krusty", "Skinner"));
    ignored.put(2000L, Set.of("Bart"));
    assertEquals(2000L, instance.firstTimeIgnoring(ignored));
    ignored.put(2000L, Set.of("Bart", "Lisa"));
    assertEquals(4000L, instance.firstTimeIgnoring(ignored));
    ignored.put(4000L, Set.of("Maggie"));
    assertEquals(0L, instance.firstTimeIgnoring(ignored));

    CTEventsWrapper other = new CTEventsWrapper(parent);
    ignored.remove(2000L);
    assertEquals(2000L, other.firstTimeIgnoring(ignored));
  }


  @Test
  public void getEventDetailsBefore() {
    SortedMap<Long, SortedSet<String>> map = instance.getEventDetailsBefore(2500);
    assertTrue(map.containsKey(1000L));
    assertTrue(map.containsKey(2000L));
    assertEquals(3, map.get(1000L).size());
    assertEquals(2, map.get(1200L).size());
    assertEquals(2, map.get(2000L).size());

    instance.remove("Homer",1000);
    map = instance.getEventDetailsBefore(2500, Set.of("Marge", "Lisa"));
    assertTrue(map.containsKey(1000L));
    assertTrue(map.containsKey(2000L));
    assertEquals(1, map.get(1000L).size());
    assertEquals(1, map.get(2000L).size());


  }


  @Test
  public void getEventsBefore() {
    SortedSet<String> set = instance.getEventsBefore(2500, 3);
    assertEquals(3, set.size());
    assertTrue(set.contains("MrBurns"));
    assertTrue(set.contains("Flanders"));
    assertTrue(set.contains("Homer"));
  }


  @Test(expected = UnsupportedOperationException.class)
  public void pack() throws IOException {
    try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      instance.pack(packer);
    }
  }


  @Test
  public void removePendingEventTimeAddresses() {
    instance.removePendingEventTimeAddresses(1250L,Set.of("MrBurns","Homer","Krusty"));
    Set<String> set = instance.getEventsBefore(1250L,Integer.MAX_VALUE);
    System.out.println(set);
  }


  @Test
  public void reset() {
    instance.reset();
  }


  private void set(CTEventsWrapper wrapper) {
    wrapper.add("MrBurns", 500);
    wrapper.add("Krusty", 1200);
    wrapper.add("Skinner", 1200);
    wrapper.add("Moe", 1000);
    wrapper.add("Flanders", 900);
    wrapper.remove("Maggie", 4000);
  }


  @Test
  public void update() {
    instance.update(Map.of(500L, Set.of("MrBurns")), Map.of(1500L, Set.of("Krusty"), 4000L, Set.of("Maggie")));
  }
}