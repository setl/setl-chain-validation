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
package io.setl.bc.pychain.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.entry.AddressEntry;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class NamedObjectTest {

  NamedObject<AddressEntry> instance = new NamedObject(10, true, "foo", new AddressEntry());


  @Test
  public void compareTo() {
    NamedObject<AddressEntry> instance2 = new NamedObject(11, true, "foo", new AddressEntry());
    assertEquals(-1, instance.compareTo(instance2));
    assertEquals(1, instance2.compareTo(instance));
    assertEquals(0, instance2.compareTo(instance2));
  }


  @Test
  public void copy() {
    NamedObject<AddressEntry> instance2 = instance.copy(10,false);
    assertEquals(instance,instance2);
  }


  @Test
  public void equals1() {
    NamedObject<AddressEntry> instance2 = new NamedObject(10, true, "foo", new AddressEntry());
    assertTrue(instance.equals(instance2));

    instance2 = new NamedObject(10, true, "bar", new AddressEntry());
    assertFalse(instance.equals(instance2));
  }


  @Test
  public void hashCode1() {
    NamedObject<AddressEntry> instance2 = new NamedObject(10, true, "foo", new AddressEntry());
    assertEquals(instance.hashCode(), instance2.hashCode());

    instance2 = new NamedObject(10, true, "bar", new AddressEntry());
    assertNotEquals(instance.hashCode(), instance2.hashCode());
  }


  @Test
  public void toString1() {
    // shameless coverage
    assertTrue(! instance.toString().isEmpty());
  }
}