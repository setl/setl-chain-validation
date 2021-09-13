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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.msgpack.core.MessagePacker;

import io.setl.bc.pychain.state.State;

/**
 * Contract time events for snapshots.
 *
 * @author Simon Greatrix on 17/12/2019.
 */
public class CTEventsWrapper implements ContractTimeEvents {

  /** Add entries to a set from another that are not in an excluded set. */
  private static void filteredAdd(TreeSet<String> s, SortedSet<String> inSet, Set<String> addresses) {
    if (inSet != null) {
      if (addresses == null) {
        s.addAll(inSet);
      } else {
        inSet.stream().filter(addresses::contains).forEach(s::add);
      }
    }
  }


  private final ContractTimeEvents parent;

  private TreeMap<Long, TreeSet<String>> addedEvents = new TreeMap<>();

  private TreeMap<Long, TreeSet<String>> removedEvents = new TreeMap<>();


  public CTEventsWrapper(ContractTimeEvents parent) {
    this.parent = parent;
  }


  @Override
  public boolean add(String address, long time) {
    if (CTEventsImpl.contains(addedEvents, time, address)) {
      // already added
      return false;
    }
    if (CTEventsImpl.doRemove(removedEvents, time, address)) {
      // it was previously removed in this, so we re-add
      return CTEventsImpl.doAdd(addedEvents, time, address);
    }

    if (parent.eventExists(address, time)) {
      // exists in parent and was not removed in this, so exists already
      return false;
    }

    // not in this, not in parent, so actually add.
    return CTEventsImpl.doAdd(addedEvents, time, address);
  }


  public void commit() {
    parent.update(addedEvents, removedEvents);
  }


  @Override
  public boolean eventExists(String address, long time) {
    if (CTEventsImpl.contains(removedEvents, time, address)) {
      // removed by this
      return false;
    }
    if (CTEventsImpl.contains(addedEvents, time, address)) {
      // added by this
      return true;
    }
    // check parent
    return parent.eventExists(address, time);
  }


  @Override
  public long firstTime() {
    long parentFirst = parent.firstTime();
    if (parentFirst == 0) {
      return addedEvents.isEmpty() ? 0L : addedEvents.firstKey();
    }

    long myFirst;
    if (addedEvents.isEmpty()) {
      myFirst = parentFirst;
    } else {
      myFirst = addedEvents.firstKey();
    }

    long bestFirst = Math.min(parentFirst, myFirst);

    if (removedEvents.isEmpty() || removedEvents.firstKey() > bestFirst) {
      // we either haven't removed anything, or we've only removed after the first event.
      return bestFirst;
    }

    // We may have removed the first event.

    parentFirst = parent.firstTimeIgnoring(removedEvents);
    if (parentFirst == 0) {
      // nothing left in parent, so it is whatever we have in this
      return addedEvents.isEmpty() ? 0L : addedEvents.firstKey();
    }

    // if we've not added anything prior to the parent, the first is in the parent.
    if (addedEvents.isEmpty() || addedEvents.firstKey() >= parentFirst) {
      return parentFirst;
    }
    return addedEvents.firstKey();
  }


  @Override
  public long firstTimeIgnoring(Map<Long, ? extends Set<String>> removedTimeEvents) {
    if (removedTimeEvents == null || removedTimeEvents.isEmpty()) {
      return firstTime();
    }

    long parentTime = parent.firstTimeIgnoring(removedTimeEvents);
    if (addedEvents.isEmpty()) {
      return parentTime;
    }

    return CTEventsImpl.firstNotRemoved(addedEvents, removedTimeEvents, parentTime);
  }


  @Override
  public SortedMap<Long, SortedSet<String>> getEventDetailsBefore(long eventTime, Set<String> addresses) {
    // get all the details from the parent, then we will merge in this's changes.
    SortedMap<Long, SortedSet<String>> parentMap = parent.getEventDetailsBefore(eventTime);

    SortedMap<Long, SortedSet<String>> myMap = new TreeMap<>();

    // get all the affected times
    HashSet<Long> allTimes = new HashSet<>();
    allTimes.addAll(parentMap.keySet());
    allTimes.addAll(addedEvents.keySet());

    // for each time slot
    for (Long t : allTimes) {
      TreeSet<String> s = new TreeSet<>();

      // get everything relevant from the parent
      SortedSet<String> inSet = parentMap.get(t);
      filteredAdd(s, inSet, addresses);

      // add everything from this
      inSet = addedEvents.get(t);
      filteredAdd(s, inSet, addresses);

      // remove everything removed by this
      if (removedEvents.containsKey(t)) {
        s.removeAll(removedEvents.get(t));
      }

      // add to output if not empty
      if (!s.isEmpty()) {
        myMap.put(t, Collections.unmodifiableSortedSet(s));
      }
    }
    return Collections.unmodifiableSortedMap(myMap);
  }


  @Override
  public SortedSet<String> getEventsBefore(long eventTime, int limit) {
    return Collections.unmodifiableSortedSet(CTEventsImpl.eventsBefore(getEventDetailsBefore(eventTime), eventTime, limit));
  }


  @Override
  public Map<Long, Set<String>> getRawData() {
    throw new UnsupportedOperationException("Contract time events are deserialized as state-based, so cannot serialize snapshot-based events");
  }


  @Override
  public boolean isChanged() {
    return !(addedEvents.isEmpty() && removedEvents.isEmpty());
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    // we do not expect to save snapshots
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean remove(String address, long time) {
    if (CTEventsImpl.contains(removedEvents, time, address)) {
      // it is already removed by this
      return false;
    }
    if (CTEventsImpl.doRemove(addedEvents, time, address)) {
      // it was added by this and now it is not, so it has been removed.
      return true;
    }
    if (parent.eventExists(address, time)) {
      // exists in parent, so can remove in this
      return CTEventsImpl.doAdd(removedEvents, time, address);
    }

    // does not exist, so cannot be removed
    return false;
  }


  /**
   * Remove all pending events prior to, or at, the specified time for the given addresses.
   *
   * @param eventTime  the time
   * @param addressSet the addresses
   */
  public void removePendingEventTimeAddresses(Long eventTime, Set<String> addressSet) {
    eventTime = State.getEventTimeKey(eventTime);
    SortedMap<Long, SortedSet<String>> events = getEventDetailsBefore(eventTime, addressSet);
    events.forEach((time, set) -> {
      Set<String> myAdded = addedEvents.get(time);
      Set<String> myRemoved = removedEvents.computeIfAbsent(time, t -> new TreeSet<>());
      set.forEach(address -> {
        if (myAdded == null || myAdded.remove(address)) {
          // not added by this so must remove in this
          myRemoved.add(address);
        }
      });
      if (myRemoved.isEmpty()) {
        // everything was added in this, so nothing actually removed
        removedEvents.remove(time);
      }
    });
  }


  @Override
  public void replaceWith(ContractTimeEvents copy) {
    reset();

    // remove everything from the parent
    parent.getRawData().forEach((time, ids) -> ids.forEach(id -> remove(id, time)));

    // add everything in the copy
    copy.getRawData().forEach((time, ids) -> ids.forEach(id -> add(id, time)));


  }


  public void reset() {
    addedEvents.clear();
    removedEvents.clear();
  }


  @Override
  public ContractTimeEvents snapshot() {
    return new CTEventsWrapper(this);
  }


  @Override
  public ContractTimeEvents unwrap() {
    return this;
  }


  @Override
  public void update(Map<Long, ? extends Set<String>> newTimeEvents, Map<Long, ? extends Set<String>> removedTimeEvents) {
    if (newTimeEvents != null) {
      newTimeEvents.forEach((k, v) -> v.forEach(a -> add(a, k)));
    }
    if (removedTimeEvents != null) {
      removedTimeEvents.forEach((k, v) -> v.forEach(a -> remove(a, k)));
    }
  }

}
