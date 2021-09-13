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

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.setl.bc.pychain.serialise.JacksonContentFactory;
import io.setl.bc.serialise.SafeMsgPackable;

/**
 * Track of when contracts (as identified by their address) have time-related events.
 *
 * @author Simon Greatrix on 17/12/2019.
 */
public interface ContractTimeEvents extends SafeMsgPackable, NamedDatum<ContractTimeEvents> {

  /** Name under which this is stored in state. */
  DataName<ContractTimeEvents> NAME = new DataName<>(
      "contractTimeEvents",
      ContractTimeEvents.class,
      store -> new JacksonContentFactory<>(ContractTimeEvents.class),
      store -> new CTEventsImpl(),
      true
  );

  @JsonCreator
  static ContractTimeEvents create(Map<Long, Set<String>> data) {
    return new CTEventsImpl(data);
  }

  /**
   * Add an event time against an address. If a contract adds multiple events, its event handler will a single notification whenever at least one is due. It is
   * the event handler's responsibility to identify and handle all due events and to remove them from the current snapshot as they are completed.
   *
   * @param address the address
   * @param time    the time
   *
   * @return true if the event was added.
   */
  boolean add(String address, long time);

  /**
   * Does the specified event exist?.
   *
   * @param address the address
   * @param time    the time
   *
   * @return true if it exists
   */
  boolean eventExists(String address, long time);

  /**
   * The time of the earliest outstanding event. If there are no events, zero is returned.
   *
   * @return time of the earliest outstanding event.
   */
  long firstTime();

  /**
   * The time of the earliest outstanding event ignoring the provided events. If there are no events, zero is returned.
   *
   * @return time of the earliest outstanding event.
   */
  long firstTimeIgnoring(Map<Long, ? extends Set<String>> removedTimeEvents);

  /**
   * Get all the events prior to or at the specified time. The returned map will not be modifiable.
   *
   * @param eventTime the event time
   *
   * @return unmodifiable map of event times to affected addresses.
   */
  default SortedMap<Long, SortedSet<String>> getEventDetailsBefore(long eventTime) {
    return getEventDetailsBefore(eventTime, null);
  }

  /**
   * Get all the events prior to or at the specified time for the given addresses. The returned map will not be modifiable.
   *
   * @param eventTime the event time
   * @param addresses the addresses to get events for (if null, all addresses are matched)
   *
   * @return unmodifiable map of event times to affected addresses.
   */
  SortedMap<Long, SortedSet<String>> getEventDetailsBefore(long eventTime, Set<String> addresses);

  /**
   * Get all the addresses with an event prior to, or at, the specified time. If there are more such addresses than the limit, then preference is given to
   * addresses with the earlier events. If a contract supports multiple time events, then its event handling will only receive a single notification regardless
   * of how many events are due. Every individual handled event should be explicitly removed from state.
   *
   * @param eventTime the event time
   * @param limit     the maximum number of addresses to return
   *
   * @return the event addresses.
   */
  SortedSet<String> getEventsBefore(long eventTime, int limit);

  /**
   * Get a copy of the raw data. The data is immutable.
   *
   * @return the raw data
   */
  @JsonValue
  Map<Long, Set<String>> getRawData();

  /**
   * Remove an event from this.
   *
   * @param address the event's address
   * @param time    the event's time
   *
   * @return true if the event existed
   */
  boolean remove(String address, long time);


  /**
   * Update this.
   *
   * @param newTimeEvents     events to add to this
   * @param removedTimeEvents events to remove from this
   */
  void update(Map<Long, ? extends Set<String>> newTimeEvents, Map<Long, ? extends Set<String>> removedTimeEvents);

}
