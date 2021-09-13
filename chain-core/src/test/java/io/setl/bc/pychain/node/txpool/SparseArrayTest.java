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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.create.NullTX;
import io.setl.common.MutableBoolean;
import java.util.Random;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-04-15.
 */
public class SparseArrayTest {

  static long timestamp = System.currentTimeMillis();



  static class ArrayBuilder {

    SparseArray sparseArray = new SparseArray();


    Txi add(long n) {
      Txi txi = makeTx(n);
      sparseArray.addTx(n, txi);
      return txi;
    }


    ArrayBuilder check(String structure, long start, long end) {
//      System.out.println("\n\n" + sparseArray.toString());
      assertEquals("Array structure not as expected", structure, sparseArray.toString());
      TxIterator iterator = new TxIterator(sparseArray, start, Integer.MAX_VALUE);
      long expected = start;
      while (iterator.hasNext()) {
        Txi tx = iterator.next();
        assertEquals("TX nonce not as expected", expected, tx.getNonce());
        if (expected > end) {
          fail("TX nonce greater than expected " + expected);
        }
        expected++;
      }
      assertEquals("Array ended to soon", end, expected - 1);
      return this;
    }


    ArrayBuilder check(String structure) {
//      System.out.println("\n\n" + sparseArray.toString());
      assertEquals("Array structure not as expected", structure, sparseArray.toString());
      return this;
    }


    ArrayBuilder range(long start, long end) {
      int step = (start < end) ? 1 : -1;
      for (long l = start; l != end; l += step) {
        Txi txi = makeTx(l);
//        System.out.println(l+" : "+sparseArray.root);
        sparseArray.addTx(l, txi);
      }

      Txi txi = makeTx(end);
      sparseArray.addTx(end, txi);

      return this;
    }


    ArrayBuilder remove(long upTo) {
      sparseArray.bulkRemove(upTo);
      return this;
    }


    ArrayBuilder reset() {
      sparseArray = new SparseArray();
      return this;
    }
  }


  static void down(long start, long end, SparseArray sparseArray) {
    for (long l = end; l >= start; l--) {
      Txi txi = makeTx(l);
      sparseArray.addTx(l, txi);
    }
  }


  static Txi makeTx(long nonce) {
    NullTX creator = new NullTX();
    creator.setAddress("myAddress");
    creator.setNonce(nonce);
    creator.setTimestamp(timestamp++);
    return creator.create();
  }


  @Test
  public void basicOpsTest() {
    ArrayBuilder builder = new ArrayBuilder();

    // build up to a 16 item range filling 256+128+4 (=388) to (256+128+11) = 395.
    builder.range(388, 395).check("Root{Up to 395:\n"
        + "|  Range{180-18f -> 16:388-395}\n"
        + "}", 388, 395);

    // advance into the next 8 block (256+128+32+5 = 421
    builder.range(396, 401).check("Root{Up to 401:\n"
        + "|  Split{180-18f/190-19f -> 20=10/10:\n"
        + "|  |  Range{180-18f -> 16:388-399},\n"
        + "|  |  Range{190-197 -> 8:400-401}\n"
        + "|  }\n"
        + "}", 390, 401);

    // move into the 4th 8 block to force a roll-up
    builder.range(402, 410).range(412, 415).check("Root{Up to 415:\n"
        + "|  Range{180-19f -> 32:388-410,412-415}\n"
        + "}", 388, 410)
        .range(411, 411).check("Root{Up to 415:\n"
        + "|  Range{180-19f -> 32:388-415}\n"
        + "}", 389, 415);

    // add new low block to create a split
    builder.range(195, 210).check("Root{Up to 415:\n"
        + "|  Split{0-ff/100-1ff -> 200=100/100:\n"
        + "|  |  Split{c0-cf/d0-df -> 20=10/10:\n"
        + "|  |  |  Range{c0-cf -> 16:195-207},\n"
        + "|  |  |  Range{d0-d7 -> 8:208-210}\n"
        + "|  |  },\n"
        + "|  |  Range{180-19f -> 32:388-415}\n"
        + "|  }\n"
        + "}", 195, 210);

    // Split again...
    builder.range(70, 70).check("Root{Up to 415:\n"
        + "|  Split{0-ff/100-1ff -> 200=100/100:\n"
        + "|  |  Split{0-7f/80-ff -> 100=80/80:\n"
        + "|  |  |  Range{40-47 -> 8:70},\n"
        + "|  |  |  Split{c0-cf/d0-df -> 20=10/10:\n"
        + "|  |  |  |  Range{c0-cf -> 16:195-207},\n"
        + "|  |  |  |  Range{d0-d7 -> 8:208-210}\n"
        + "|  |  |  }\n"
        + "|  |  },\n"
        + "|  |  Range{180-19f -> 32:388-415}\n"
        + "|  }\n"
        + "}", 70, 70);

    // try a remove that doesn't change anything
    builder.remove(69).check("Root{Up to 415:\n"
        + "|  Split{0-ff/100-1ff -> 200=100/100:\n"
        + "|  |  Split{0-7f/80-ff -> 100=80/80:\n"
        + "|  |  |  Range{40-47 -> 8:70},\n"
        + "|  |  |  Split{c0-cf/d0-df -> 20=10/10:\n"
        + "|  |  |  |  Range{c0-cf -> 16:195-207},\n"
        + "|  |  |  |  Range{d0-d7 -> 8:208-210}\n"
        + "|  |  |  }\n"
        + "|  |  },\n"
        + "|  |  Range{180-19f -> 32:388-415}\n"
        + "|  }\n"
        + "}", 70, 70);

    // try a remove that removes the first branch entirely
    builder.remove(100).check("Root{Up to 415:\n"
        + "|  Split{0-ff/100-1ff -> 200=100/100:\n"
        + "|  |  Split{c0-cf/d0-df -> 20=10/10:\n"
        + "|  |  |  Range{c0-cf -> 16:195-207},\n"
        + "|  |  |  Range{d0-d7 -> 8:208-210}\n"
        + "|  |  },\n"
        + "|  |  Range{180-19f -> 32:388-415}\n"
        + "|  }\n"
        + "}", 100, 99);

    // try a remove that removes most of the first range
    builder.remove(206).check("Root{Up to 415:\n"
        + "|  Split{0-ff/100-1ff -> 200=100/100:\n"
        + "|  |  Split{c0-cf/d0-df -> 20=10/10:\n"
        + "|  |  |  Range{c0-cf -> 16:207},\n"
        + "|  |  |  Range{d0-d7 -> 8:208-210}\n"
        + "|  |  },\n"
        + "|  |  Range{180-19f -> 32:388-415}\n"
        + "|  }\n"
        + "}", 207, 210);

    // try a remove that removes all but the last range
    builder.remove(300).check("Root{Up to 415:\n"
        + "|  Range{180-19f -> 32:388-415}\n"
        + "}", 388, 415);

    // trim enough to make the Range32 go to Range16
    builder.remove(401).check("Root{Up to 415:\n"
        + "|  Range{190-19f -> 16:402-415}\n"
        + "}", 402, 415);

    // trim some more, but Range16 does not go to Range8
    builder.remove(410).check("Root{Up to 415:\n"
        + "|  Range{190-19f -> 16:411-415}\n"
        + "}", 411, 415);

    builder = new ArrayBuilder();
    builder.range(24, 124)
        .range(1024 + 32, 1024 + 127)
        .check("Root{Up to 1151:\n"
            + "|  Split{0-3ff/400-7ff -> 800=400/400:\n"
            + "|  |  Split{0-3f/40-7f -> 80=40/40:\n"
            + "|  |  |  Split{0-1f/20-3f -> 40=20/20:\n"
            + "|  |  |  |  Range{18-1f -> 8:24-31},\n"
            + "|  |  |  |  Range{20-3f -> 32:32-63}\n"
            + "|  |  |  },\n"
            + "|  |  |  Range{40-7f -> 64:64-124}\n"
            + "|  |  },\n"
            + "|  |  Split{400-43f/440-47f -> 80=40/40:\n"
            + "|  |  |  Range{420-43f -> 32:1056-1087},\n"
            + "|  |  |  Range{440-47f -> 64:1088-1151}\n"
            + "|  |  }\n"
            + "|  }\n"
            + "}");
  }


  @Test
  public void commonMask() {
    Random rand = new Random(1234);
    for (int i = 0; i < 126; i++) {
      long b = rand.nextLong() & 0x7fff_ffff_ffff_ffffL;
      long l1 = rand.nextLong() & 0x7fff_ffff_ffff_ffffL;
      long l2 = rand.nextLong() & 0x7fff_ffff_ffff_ffffL;
      long m = 1L << (2 + i % 61);
      long m1 = m - 1;
      b = b & ~m1;
      l1 = (l1 & m1) | b;
      l2 = (l2 & m1) | b;
      l1 = l1 & ~m;
      l2 = l2 | m;
      long r = SparseArray.commonMask(l1, l2);

      assertEquals(l1 & r, l2 & r);
      assertNotEquals(l1 & (r >> 1), l2 & (r >> 1));
    }
  }


  @Test
  public void testAllKindsOfGets() {
    Hash mismatch = Hash.fromHex("0123456789abcdef");
    // Test from Root8
    ArrayBuilder b = new ArrayBuilder();
    Txi t = b.range(3, 5).add(6);
    assertSame(t, b.sparseArray.getTx(6, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(6, mismatch));
    assertNull(b.sparseArray.getTx(10000, Hash.fromHex(t.getHash())));

    // Test RootEmpty
    b.reset();
    assertNull(b.sparseArray.getTx(6, Hash.fromHex(t.getHash())));

    // Test RootRange
    t = b.reset().range(3, 19).range(21, 60).add(50);
    assertSame(t, b.sparseArray.getTx(50, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(50, mismatch));
    assertNull(b.sparseArray.getTx(10000, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(20, mismatch));

    // Test Root pure split
    t = b.reset().range(3, 60).range(Root.HALF_WAY + 3, Root.HALF_WAY + 60).add(50);
    assertSame(t, b.sparseArray.getTx(50, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(50, mismatch));
    assertNull(b.sparseArray.getTx(Root.HALF_WAY + 50, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(10000, Hash.fromHex(t.getHash())));

    // Test root split
    t = b.reset().range(3, 60).range(1003, 1060).add(50);
    assertSame(t, b.sparseArray.getTx(50, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(50, mismatch));
    assertNull(b.sparseArray.getTx(1050, mismatch));
    assertNull(b.sparseArray.getTx(2050, mismatch));

    // Test with offset16
    t = b.reset().range(49, 54).range(1040, 1050).add(50);
    assertSame(t, b.sparseArray.getTx(50, Hash.fromHex(t.getHash())));
    assertNull(b.sparseArray.getTx(50, mismatch));
    assertNull(b.sparseArray.getTx(58, mismatch));
  }


  @Test
  public void testEmptyRoot() {
    Root root = new Root(-1);
    assertTrue(root.isEmpty());
    MutableBoolean check = new MutableBoolean(false);
    root.add(100, makeTx(100), check);
    assertTrue(check.get());
    check.set(false);
    root.add(100, makeTx(100), check);
    assertFalse(check.get());
    assertEquals(8, root.coverage());
    root.bulkRemove(200);
    assertTrue(root.isEmpty());
    assertEquals(0, root.coverage());
  }


  @Test
  public void testOnEmpty() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.check("Root{ *EMPTY* }", 100, 99).remove(1000).check("Root{ *EMPTY* }", 100, 99);
  }


  @Test
  public void testRangeBecomesEmpty() {
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(30, 40).range(10, 12).check("Root{Up to 40:\n"
        + "|  Split{0-1f/20-3f -> 40=20/20:\n"
        + "|  |  Split{0-f/10-1f -> 20=10/10:\n"
        + "|  |  |  Range{8-f -> 8:10-12},\n"
        + "|  |  |  Range{18-1f -> 8:30-31}\n"
        + "|  |  },\n"
        + "|  |  Range{20-2f -> 16:32-40}\n"
        + "|  }\n"
        + "}")
        .remove(13).check("Root{Up to 40:\n"
        + "|  Split{0-1f/20-3f -> 40=20/20:\n"
        + "|  |  Range{18-1f -> 8:30-31},\n"
        + "|  |  Range{20-2f -> 16:32-40}\n"
        + "|  }\n"
        + "}");
  }


  @Test
  public void testVeryBig() {
    // very big ranges do not merge.
    ArrayBuilder builder = new ArrayBuilder();
    builder.range(16400, 32000).check("Root{Up to 32000:\n"
        + "|  Split{4000-5fff/6000-7fff -> 4000=2000/2000:\n"
        + "|  |  Range{4000-5fff -> 8192:16400-24575},\n"
        + "|  |  Range{6000-7fff -> 8192:24576-32000}\n"
        + "|  }\n"
        + "}");
    builder.range(16385, 16399).check("Root{Up to 32000:\n"
        + "|  Split{4000-5fff/6000-7fff -> 4000=2000/2000:\n"
        + "|  |  Range{4000-5fff -> 8192:16385-24575},\n"
        + "|  |  Range{6000-7fff -> 8192:24576-32000}\n"
        + "|  }\n"
        + "}", 16400, 32000);
  }
}