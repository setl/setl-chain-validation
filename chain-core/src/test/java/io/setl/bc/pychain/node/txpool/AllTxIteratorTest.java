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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.bc.pychain.node.txpool.SparseArrayTest.ArrayBuilder;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-05-13.
 */
public class AllTxIteratorTest {

  @Test
  public void testBasicOps() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(10, 20);

    SparseArray array = builder.sparseArray;

    AllTxIterator value = new AllTxIterator(array, 14, 17);

    int e = 14;
    for (int i = 0; i < 3; i++) {
      assertTrue(value.hasNext());
      Txi txi = value.next();
      assertEquals(e, txi.getNonce());
      e++;
    }
    assertFalse(value.hasNext());
    try {
      value.next();
      fail("Expected no such element exception");
    } catch (NoSuchElementException x) {
      // correct
    }

    value = new AllTxIterator(array, 9, 100);

    e = 10;
    for (int i = 0; i < 11; i++) {
      assertTrue(value.hasNext());
      Txi txi = value.next();
      assertEquals(e, txi.getNonce());
      e++;
    }
    assertFalse(value.hasNext());
    try {
      value.next();
      fail("Expected no such element exception");
    } catch (NoSuchElementException x) {
      // correct
    }

    value = new AllTxIterator(array, 90, 100);
    assertFalse(value.hasNext());

    value = new AllTxIterator(array, 15, 15);
    assertFalse(value.hasNext());
  }


  @Test
  public void testWithGap() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(5, 9).range(130, 134);

    SparseArray array = builder.sparseArray;

    AllTxIterator value = new AllTxIterator(array, 1, 20);

    int e = 5;
    for (int i = 0; i < 5; i++) {
      assertTrue("Entry " + i + " is missing", value.hasNext());
      Txi txi = value.next();
      assertEquals(e, txi.getNonce());
      e++;
    }
    assertFalse(value.hasNext());

    value = new AllTxIterator(array, 1, 132);

    e = 5;
    for (int i = 0; i < 7; i++) {
      assertTrue("Entry " + i + "=" + e + " is missing", value.hasNext());
      Txi txi = value.next();
      assertEquals(e, txi.getNonce());
      e++;
      if (e == 10) {
        e = 130;
      }
    }
    assertFalse(value.hasNext());
  }
}