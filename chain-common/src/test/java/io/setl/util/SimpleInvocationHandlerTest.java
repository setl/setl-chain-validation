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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Simon Greatrix on 22/08/2018.
 */
public class SimpleInvocationHandlerTest {

  @Test
  public void invoke() {
    List<Number> list = Arrays.asList(1, 2, 3, 4, 5);
    List<?> other = SimpleInvocationHandler.newProxy(List.class, list);
    Assert.assertSame(list, other);

    // List is not a Set
    SortedSet<?> set = SimpleInvocationHandler.newProxy(SortedSet.class, list);
    assertEquals(list.toString(), set.toString());
    assertEquals(list.size(), set.size());

    // list does not have a "first" method
    try {
      set.first();
      fail();
    } catch (NoSuchMethodError e) {
      // correct
    }
  }
}