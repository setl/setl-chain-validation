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
package io.setl.bc.pychain.node.txpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.bc.pychain.node.txpool.SparseArrayTest.ArrayBuilder;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-04-29.
 */
public class TxIteratorTest {

  @Test
  public void basicTest() {
    // Create an array and iterate across it.
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(10, 15).range(12, 12);
    TxIterator iterator = new TxIterator(builder.sparseArray, 11, 100);
    assertEquals(100, iterator.getRemaining());
    for (int i = 0; i < 5; i++) {
      assertTrue(iterator.hasNext());
      Txi txi = iterator.next();
      assertNotNull(txi);
      assertEquals(i + 11, txi.getNonce());
    }
    assertFalse(iterator.hasNext());
    assertEquals(95, iterator.getRemaining());
  }


  @Test
  public void emptyArray() {
    ArrayBuilder builder = new ArrayBuilder();
    TxIterator iterator = new TxIterator(builder.sparseArray, 5, 100);
    assertFalse(iterator.hasNext());

    builder.range(10, 15);
    iterator = new TxIterator(builder.sparseArray, 10, 0);
    assertFalse(iterator.hasNext());
  }


  @Test
  public void multiRange() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(30, 37);
    TxIterator iterator = new TxIterator(builder.sparseArray, 30, 100);
    for (int i = 0; i < 8; i++) {
      assertTrue(iterator.hasNext());
      Txi txi = iterator.next();
      assertNotNull(txi);
      assertEquals(30 + i, txi.getNonce());
    }
    assertFalse(iterator.hasNext());

    builder.range(10, 15);
  }


  @Test
  public void onlyFutures() {
    // Create an array without the first required nonce
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(10, 15);
    TxIterator iterator = new TxIterator(builder.sparseArray, 5, 100);
    assertEquals(100, iterator.getRemaining());
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail();
    } catch (NoSuchElementException e) {
      // correct
    }
  }


  @Test
  public void onlyReplays() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(10, 15);
    TxIterator iterator = new TxIterator(builder.sparseArray, 25, 100);
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail();
    } catch (NoSuchElementException e) {
      // correct
    }
  }


  @Test
  public void testGap() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(4, 7).range(16, 18);
    TxIterator iterator = new TxIterator(builder.sparseArray, 4, 100);
    for (int i = 0; i < 4; i++) {
      assertTrue(iterator.hasNext());
      Txi txi = iterator.next();
      assertNotNull(txi);
    }
    assertFalse(iterator.hasNext());
  }


  @Test
  public void testLimit() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(10, 20);
    TxIterator iterator = new TxIterator(builder.sparseArray, 10, 5);
    for (int i = 0; i < 5; i++) {
      assertTrue(iterator.hasNext());
      Txi txi = iterator.next();
      assertNotNull(txi);
    }
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail();
    } catch (NoSuchElementException e) {
      // correct
    }
  }
}