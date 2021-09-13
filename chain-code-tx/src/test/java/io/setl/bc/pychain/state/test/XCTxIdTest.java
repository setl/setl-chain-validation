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
package io.setl.bc.pychain.state.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Hash;
import org.junit.Test;

/**
 * @author Simon Greatrix on 09/01/2020.
 */
public class XCTxIdTest {

  Hash he = Hash.fromHex("eeee");

  Hash hf = Hash.fromHex("ffff");

  XCTxId id1 = new XCTxId(1, hf);

  XCTxId id2 = new XCTxId(1, hf);

  XCTxId id3 = new XCTxId(1, he);

  XCTxId id4 = new XCTxId(2, hf);

  XCTxId id5 = new XCTxId(2, he);


  @Test
  public void compareTo() {
    assertTrue(id1.compareTo(id2) == 0);
    assertTrue(id1.compareTo(id3) > 0);
    assertTrue(id1.compareTo(id4) < 0);
    assertTrue(id1.compareTo(id5) < 0);

    assertTrue(id3.compareTo(id1) < 0);
    assertTrue(id4.compareTo(id1) > 0);
    assertTrue(id5.compareTo(id1) > 0);
  }


  @Test
  public void getChainId() {
    assertEquals(2, id4.getChainId());
  }


  @Test
  public void getHash() {
    assertEquals(hf, id4.getHash());
  }


  @Test
  public void setChainId() {
    id1.setChainId(18);
    assertEquals(18, id1.getChainId());
  }


  @Test
  public void setHash() {
    id1.setHash(he);
    assertEquals(he, id1.getHash());
  }


  @Test
  public void testEquals() {
    assertTrue(id1.equals(id2));
    assertTrue(id1.equals(id1));
    assertFalse(id1.equals(null));
    assertFalse(id1.equals(id3));
    assertFalse(id1.equals(id4));
    assertFalse(id1.equals(id5));
  }


  @Test
  public void testHashCode() {
    assertNotEquals(id1.hashCode(), id4.hashCode());
  }


  @Test
  public void testToString() {
    assertNotNull(id1.toString());
  }
}