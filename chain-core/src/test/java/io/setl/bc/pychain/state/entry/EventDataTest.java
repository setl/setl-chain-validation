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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventDataTest {

  @Test
  public void testEventData() throws Exception {

    //
    String eventAddress = "eventAddress";
    String eventFunction = "eventFunction";
    String eventName = "eventName";
    Number eventDataNumber = 42L;
    final String eventDataString = "edString";

    EventData evData1 = new EventData(eventAddress, eventFunction, eventName, eventDataNumber);

    assertTrue(eventAddress.equals(evData1.getEventAddress()));
    assertTrue(eventFunction.equals(evData1.getEventFunction()));
    assertTrue(eventName.equals(evData1.getEventName()));
    assertTrue(eventDataNumber.equals(evData1.getLongValue()));
    assertTrue("42".equals(evData1.getStringValue()));

    Object[] encoded1 = evData1.encode();

    assertTrue(eventAddress.equals(encoded1[0]));
    assertTrue(eventName.equals(encoded1[1]));
    assertTrue(eventFunction.equals(encoded1[2]));
    assertTrue(eventDataNumber.equals(encoded1[3]));

    evData1.setEventData(eventDataString);

    assertTrue(eventDataString.equals(evData1.getStringValue()));

    encoded1 = evData1.encode();
    assertTrue(!eventDataNumber.equals(encoded1[3]));
    assertTrue(eventDataString.equals(encoded1[3]));

    evData1 = new EventData(eventAddress, eventFunction, eventName, eventDataString);

    assertTrue(eventAddress.equals(evData1.getEventAddress()));
    assertTrue(eventFunction.equals(evData1.getEventFunction()));
    assertTrue(eventName.equals(evData1.getEventName()));
    assertTrue(!eventDataNumber.equals(evData1.getLongValue()));
    assertTrue(eventDataString.equals(evData1.getStringValue()));

  }


}