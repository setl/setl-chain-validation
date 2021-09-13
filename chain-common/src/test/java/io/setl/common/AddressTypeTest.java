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
package io.setl.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AddressTypeTest {


  @Test
  public void get() {

    assertTrue(AddressType.get(0).equals(AddressType.NORMAL));
    assertTrue(AddressType.get(5).equals(AddressType.CONTRACT));
  }


  @Test
  public void getId() {

    assertTrue(AddressType.NORMAL.getId() == 0);
    assertTrue(AddressType.CONTRACT.getId() == 5);

  }
}