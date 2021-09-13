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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.State;
import io.setl.common.MutableInt;

/**
 * Track when contracts have time-based events for state.
 *
 * @author Simon Greatrix on 16/12/2019.
 */
public class CTEventsImpl implements ContractTimeEvents, Serializable {

  private static final long serialVersionUID = 1L;


  /**
   * Check if a tree->set structure contains an entry.
   *
   * @param map   the map
   * @param key   the key
   * @param value the value
   *
   * @return if the set existed and contained the value
   */
  static boolean contains(TreeMap<Long, TreeSet<String>> map, Long key, String value) {
    key = State.getEventTimeKey(key);
    TreeSet<String> set = map.get(key);
    return (set != null) && set.contains(value);
  }


  /**
   * Add an entry to a tree->set structure, creating a new set entry if necessary.
   *
   * @param map   the map
   * @param key   the key
   * @param value the value
   *
   * @return true if the set did not exist, or did not already contain the value
   */
  static boolean doAdd(TreeMap<Long, TreeSet<String>> map, Long key, String value) {
    key = State.getEventTimeKey(key);
    TreeSet<String> set = map.computeIfAbsent(key, k -> new TreeSet<>());
    return set.add(value);
  }


  /**
   * Remove an entry from a tree->set structure, removing the set if it becomes empty.
   *
   * @param map   the map
   * @param key   the key
   * @param value the value
   *
   * @return if the set existed and contained the value
   */
  static boolean doRemove(TreeMap<Long, TreeSet<String>> map, Long key, String value) {
    key = State.getEventTimeKey(key);
    TreeSet<String> set = map.get(key);
    if (set == null) {
      return false;
    }
    boolean changed = set.remove(value);
    if (changed && set.isEmpty()) {
      // set is empty now, so remove from map
      map.remove(key);
    }
    return changed;
  }


  /**
   * Extract all the events prior to, or at, the specified time, whilst honouring the specified limit.
   *
   * @param map       the map of times to event addresses
   * @param eventTime the event time
   * @param limit     the limit to the number of addresses to return
   *
   * @return the addresses
   */
  static SortedSet<String> eventsBefore(SortedMap<Long, ? extends SortedSet<String>> map, long eventTime, int limit) {
    final SortedSet<String> addresses = new TreeSet<>();

    map.headMap(eventTime + 1L).values().forEach(v -> {
      if (v.size() + addresses.size() <= limit) {
        // no danger of overflow
        addresses.addAll(v);
      } else {
        // add one at a time up to the limit
        Iterator<String> iter = v.iterator();
        while (iter.hasNext() && addresses.size() < limit) {
          addresses.add(iter.next());
        }
      }
    });

    return addresses;
  }


  static long firstNotRemoved(NavigableMap<Long, ? extends Set<String>> addedEvents, Map<Long, ? extends Set<String>> removedTimeEvents, long dflt) {
    Long time = addedEvents.firstKey();
    if (dflt != 0 && dflt <= time) {
      // not going to find anything better than the default
      return dflt;
    }
    while (true) {
      Set<String> set = removedTimeEvents.get(time);
      if (set == null || !set.containsAll(addedEvents.get(time))) {
        // something was not removed
        return time;
      }

      time = addedEvents.ceilingKey(time + 1);
      if (time == null || (dflt != 0 && dflt <= time)) {
        // everything was removed prior to the default, so use default
        return dflt;
      }
    }
  }


  /** Map of event time to affected addresses. */
  private TreeMap<Long, TreeSet<String>> events;

  /** Has this been changed?. */
  private boolean isChanged = false;

  /** Has a copy being performed to make this value mutable?. */
  private boolean isCopied = true;

  /** Once a new state is finalized, this cannot be modified. */
  private boolean isLocked = false;


  public CTEventsImpl() {
    events = new TreeMap<>();
  }


  /**
   * New instance from encoded form.
   *
   * @param encoded the encoded form
   */
  public CTEventsImpl(MPWrappedArray encoded) {
    events = new TreeMap<>();
    for (int i = 0; i < encoded.size(); i++) {
      MPWrappedArray entry = encoded.asWrapped(i);
      Long key = entry.asLong(0);
      TreeSet<String> set = new TreeSet<>();
      events.put(key, set);
      MPWrappedArray values = entry.asWrapped(1);
      for (int j = 0; j < values.size(); j++) {
        set.add(values.asString(j));
      }
    }
  }


  /**
   * Create a contract-events from the raw data.
   *
   * @param map the contract events
   */
  CTEventsImpl(Map<Long, Set<String>> map) {
    events = new TreeMap<>();
    map.forEach((k, v) -> {
      events.put(k, new TreeSet<>(v));
    });
  }


  /**
   * New instance from packed form.
   *
   * @param unpacker the packed form unpacker
   */
  public CTEventsImpl(MessageUnpacker unpacker) throws IOException {
    events = new TreeMap<>();
    int entryCount = unpacker.unpackArrayHeader();
    for (int i = 0; i < entryCount; i++) {
      unpacker.unpackArrayHeader(); // should be 2
      Long key = unpacker.unpackLong();
      TreeSet<String> set = new TreeSet<>();
      events.put(key, set);
      int valueCount = unpacker.unpackArrayHeader();
      for (int j = 0; j < valueCount; j++) {
        set.add(unpacker.unpackString());
      }
    }
  }


  /**
   * Copy constructor.
   *
   * @param contractTimeEvents the events to copy
   */
  public CTEventsImpl(CTEventsImpl contractTimeEvents) {
    isCopied = false;
    isLocked = false;
    events = contractTimeEvents.events;
  }


  @Override
  public boolean add(String address, long time) {
    checkLocked();
    return doAdd(events, time, address);
  }


  private void checkLocked() {
    if (isLocked) {
      throw new IllegalStateException("Contract Time events are locked against modification");
    }
    if (!isCopied) {
      // copy the parent to make this mutable
      TreeMap<Long, TreeSet<String>> myMap = new TreeMap<>();
      events.forEach((time, set) -> myMap.put(time, new TreeSet<>(set)));
      events = myMap;
      isCopied = true;
    }
  }


  /**
   * Encode this event list as an object array.
   *
   * @return the encoded data
   */
  public Object[] encode() {
    Object[] out = new Object[events.size()];
    MutableInt index = new MutableInt(-1);
    events.forEach((time, set) -> out[index.increment()] = new Object[]{time, set.toArray()});
    return out;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CTEventsImpl)) {
      return false;
    }
    CTEventsImpl ctEvents = (CTEventsImpl) o;
    return events.equals(ctEvents.events);
  }


  @Override
  public boolean eventExists(String address, long time) {
    return contains(events, time, address);
  }


  @Override
  public long firstTime() {
    return events.isEmpty() ? 0L : events.firstKey();
  }


  @Override
  public long firstTimeIgnoring(Map<Long, ? extends Set<String>> removedTimeEvents) {
    if (removedTimeEvents == null || removedTimeEvents.isEmpty()) {
      return firstTime();
    }
    if (events.isEmpty()) {
      return 0L;
    }

    return firstNotRemoved(events, removedTimeEvents, 0);
  }


  @Override
  public SortedMap<Long, SortedSet<String>> getEventDetailsBefore(long eventTime, Set<String> addresses) {
    // We need "+1" on the "headMap" as the match is strictly less than the limit.
    SortedMap<Long, ? extends SortedSet<String>> m = events.headMap(eventTime + 1L);

    // Convert the head-map into an unmodifiable map with unmodifiable values.
    TreeMap<Long, SortedSet<String>> result = new TreeMap<>();
    if (addresses == null) {
      m.forEach((k, v) -> result.put(k, Collections.unmodifiableSortedSet(v)));
    } else {
      m.forEach((k, v) -> {
        TreeSet<String> set = new TreeSet<>();
        set.addAll(v);
        set.retainAll(addresses);
        if (!set.isEmpty()) {
          result.put(k, Collections.unmodifiableSortedSet(set));
        }
      });
    }
    return Collections.unmodifiableSortedMap(result);
  }


  @Override
  public SortedSet<String> getEventsBefore(long eventTime, int limit) {
    return Collections.unmodifiableSortedSet(eventsBefore(events, eventTime, limit));
  }


  @Override
  public Map<Long, Set<String>> getRawData() {
    HashMap<Long, Set<String>> data = new HashMap<>();
    events.forEach((k, v) -> data.put(k, new HashSet<>(v)));
    return data;
  }


  @Override
  public int hashCode() {
    return Objects.hash(events);
  }


  @Override
  public boolean isChanged() {
    return isChanged;
  }


  @Override
  public void lock() {
    isLocked = true;
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    p.packArrayHeader(events.size());
    for (Entry<Long, ? extends Set<String>> e : events.entrySet()) {
      p.packArrayHeader(2);
      p.packLong(e.getKey());
      p.packArrayHeader(e.getValue().size());
      for (String v : e.getValue()) {
        p.packString(v);
      }
    }
  }


  @Override
  public boolean remove(String address, long time) {
    checkLocked();
    return doRemove(events, time, address);
  }


  @Override
  public ContractTimeEvents snapshot() {
    return new CTEventsWrapper(this);
  }


  @Override
  public String toString() {
    return String.format(
        "CTEventsImpl(events=%s, isCopied=%s, isLocked=%s)",
        this.events, this.isCopied, this.isLocked
    );
  }


  @Override
  public ContractTimeEvents unlock() {
    return new CTEventsImpl(this);
  }


  @Override
  public ContractTimeEvents unwrap() {
    return this;
  }


  /**
   * Update this.
   *
   * @param newTimeEvents     events to add to this
   * @param removedTimeEvents events to remove from this
   */
  @Override
  public void update(Map<Long, ? extends Set<String>> newTimeEvents, Map<Long, ? extends Set<String>> removedTimeEvents) {
    checkLocked();

    if (newTimeEvents != null) {
      newTimeEvents.forEach(this::updateAdd);
    }

    if (removedTimeEvents != null) {
      removedTimeEvents.forEach(this::updateRemove);
    }
  }


  private void updateAdd(Long k, Set<String> v) {
    if (v.isEmpty()) {
      return;
    }
    TreeSet<String> set = events.computeIfAbsent(k, x -> new TreeSet<>());
    if (set.addAll(v)) {
      isChanged = true;
    }
  }


  private void updateRemove(Long k, Set<String> v) {
    if (v.isEmpty()) {
      return;
    }
    TreeSet<String> set = events.get(k);
    if (set != null) {
      if (set.removeAll(v)) {
        isChanged = true;
      }
      if (set.isEmpty()) {
        events.remove(k);
      }
    }
  }

}
