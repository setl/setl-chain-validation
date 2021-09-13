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
package io.setl.bc.json.tx.external;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Simon Greatrix on 31/07/2020.
 */
public class SpaceIdTest {

  @Test
  public void test() {
    SpaceId in = new SpaceId("one", "two");
    SpaceId out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("one", "two" + SpaceId.NAMESPACE_ESCAPE + "four");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("one", "two" + SpaceId.NAMESPACE_SEPARATOR + "four");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("one", "two" + SpaceId.NAMESPACE_ESCAPE + SpaceId.NAMESPACE_SEPARATOR + "four");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("one", "two" + SpaceId.NAMESPACE_SEPARATOR + SpaceId.NAMESPACE_ESCAPE + "four");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("two" + SpaceId.NAMESPACE_ESCAPE + "four", "six");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("two" + SpaceId.NAMESPACE_SEPARATOR + "four", "six");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("two" + SpaceId.NAMESPACE_ESCAPE + SpaceId.NAMESPACE_SEPARATOR + "four", "six");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

    in = new SpaceId("two" + SpaceId.NAMESPACE_SEPARATOR + SpaceId.NAMESPACE_ESCAPE + "four", "six");
    out = new SpaceId(in.getFullId());
    assertEquals(in, out);

  }


  @Test(expected = IllegalArgumentException.class)
  public void testBadFullId1() {
    // no separator
    new SpaceId("one");
  }


  @Test(expected = IllegalArgumentException.class)
  public void testBadFullId2() {
    // no separator
    new SpaceId("one" + SpaceId.NAMESPACE_ESCAPE + "two");
  }


  @Test(expected = IllegalArgumentException.class)
  public void testBadFullId3() {
    // separator is escaped
    new SpaceId("one" + SpaceId.NAMESPACE_ESCAPE + SpaceId.NAMESPACE_SEPARATOR + "two");
  }


  @Test(expected = IllegalArgumentException.class)
  public void testBadFullId4() {
    // invalid escape
    new SpaceId("one" + SpaceId.NAMESPACE_ESCAPE + "bad" + SpaceId.NAMESPACE_SEPARATOR + "two");
  }

}