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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.setl.common.Pair;
import java.math.BigDecimal;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class EventData implements Comparable<EventData> {

  private String eventAddress;

  private Object eventDatum;

  private String eventFunction;

  private String eventName;


  /**
   * New instance.
   *
   * @param encoded the output of encode()
   */
  public EventData(Object[] encoded) {
    if (encoded == null || encoded.length != 4) {
      throw new IllegalArgumentException("Encoded form must be a 4 value array");
    }
    eventAddress = String.valueOf(encoded[0]);
    eventName = String.valueOf(encoded[1]);
    eventFunction = String.valueOf(encoded[2]);
    eventDatum = encoded[3];
  }


  /**
   * EventData() Constructor.
   *
   * @param eventAddress  :
   * @param eventFunction :
   * @param eventName     :
   * @param eventData     :
   */
  public EventData(String eventAddress, String eventFunction, String eventName, Number eventData) {

    this.eventAddress = (eventAddress == null ? "" : eventAddress);
    this.eventFunction = (eventFunction == null ? "" : eventFunction);
    this.eventName = (eventName == null ? "" : eventName);
    this.eventDatum = (eventData == null ? 0L : eventData);

  }


  /**
   * EventData() Constructor.
   *
   * @param eventAddress  :
   * @param eventFunction :
   * @param eventName     :
   * @param eventData     :
   */
  public EventData(String eventAddress, String eventFunction, String eventName, String eventData) {
    this.eventAddress = (eventAddress == null ? "" : eventAddress);
    this.eventFunction = (eventFunction == null ? "" : eventFunction);
    this.eventName = (eventName == null ? "" : eventName);
    this.eventDatum = (eventData == null ? "" : eventData);
  }


  /**
   * EventData() Constructor.
   *
   * @param eventAddress  :
   * @param eventFunction :
   * @param eventName     :
   * @param eventData     :
   */
  @JsonCreator
  public EventData(
      @JsonProperty("address") String eventAddress,
      @JsonProperty("function") String eventFunction,
      @JsonProperty("name") String eventName,
      @JsonProperty("isNumeric") boolean isNumeric,
      @JsonProperty("value") String eventData
  ) {
    this.eventAddress = (eventAddress == null ? "" : eventAddress);
    this.eventFunction = (eventFunction == null ? "" : eventFunction);
    this.eventName = (eventName == null ? "" : eventName);
    if (eventData == null) {
      this.eventDatum = "";
    } else {
      if (isNumeric) {
        // use BigDecimal for maximum generality.
        eventDatum = new BigDecimal(eventData);
      } else {
        eventDatum = eventData;
      }
    }
  }


  /**
   * Compare this to another EventData for block ordering. The ordering is based upon the event's ID, which is composed of the address and the event name. A
   * single block will only contain one event per address and name.
   */
  @Override
  public int compareTo(@NotNull EventData o) {
    int c = eventAddress.compareTo(o.eventAddress);
    if (c != 0) {
      return c;
    }
    return eventName.compareTo(o.eventName);
  }


  /**
   * Encode Event Data.
   *
   * @return :
   */
  public Object[] encode() {
    Object[] rVal = new Object[4];

    rVal[0] = eventAddress;
    rVal[1] = eventName;
    rVal[2] = eventFunction;
    rVal[3] = eventDatum;

    return rVal;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EventData)) {
      return false;
    }
    // Because the block only allows one event data per address-name combo, we judge event data equal on just these two properties.
    EventData eventData = (EventData) o;
    return eventAddress.equals(eventData.eventAddress) && eventName.equals(eventData.eventName);
  }


  @JsonProperty("address")
  public String getEventAddress() {
    return eventAddress;
  }


  @JsonProperty("function")
  public String getEventFunction() {
    return eventFunction;
  }


  /**
   * getEventName().
   *
   * @return :
   */
  @JsonProperty("name")
  public String getEventName() {
    return this.eventName;
  }


  /**
   * Get the address and event name that uniquely identifies this event.
   *
   * @return the identifier
   */
  @JsonIgnore
  public Pair<String, String> getId() {
    return new Pair<>(eventAddress, eventName);
  }


  /**
   * getLongValue().
   *
   * @return :
   */
  @JsonIgnore
  public long getLongValue() {

    if (this.eventDatum instanceof Number) {
      return ((Number) this.eventDatum).longValue();
    }

    return 0L;
  }


  /**
   * getLongValue().
   *
   * @return :
   */
  @JsonIgnore
  public Number getNumericValue() {
    if (this.eventDatum instanceof Number) {
      return ((Number) this.eventDatum);
    }

    return 0L;
  }


  /**
   * getStringValue().
   *
   * @return :
   */
  @JsonProperty("value")
  public String getStringValue() {
    if (this.eventDatum instanceof String) {
      return ((String) this.eventDatum);
    }

    return this.eventDatum.toString();
  }


  @Override
  public int hashCode() {
    return Objects.hash(eventAddress, eventName);
  }


  @JsonProperty("isNumeric")
  public boolean isNumeric() {
    return eventDatum instanceof Number;
  }


  public void setEventData(Object eventData) {
    this.eventDatum = eventData;
  }
}

