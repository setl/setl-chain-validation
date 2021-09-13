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

import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.Arrays;
import java.util.Random;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Command line to run 1000s of operations on the sparse array. The hope is to catch some erroneous edge condition.
 *
 * @author Simon Greatrix on 2019-06-07.
 */
public class SimulationTest {

  public static void insertAndDeleteTest() throws Exception {
    // size is a power of 2
    int SIZE = 512;

    NullTx[] txis = new NullTx[SIZE];
    for (int i = 0; i < SIZE; i++) {
      txis[i] = new NullTx(1, 1, "hash", i, false, "", "", "", 0, "", 0);
    }

    Long testSeed = 567L;
    boolean isVerbose = testSeed != null;

    Random seeds = new Random();
    int tests = 0;
    boolean[] done = new boolean[SIZE];
    boolean[] check = new boolean[SIZE];
    for (int ii = 0; ii < 250_000; ii++) {
      tests++;
      long seed = testSeed == null ? seeds.nextLong() : testSeed.longValue();
      System.out.println("Test = " + tests + "   Seed = " + seed);
      Random random = new Random(seed);
      SparseArray sparseArray = new SparseArray();
      Arrays.fill(done, false);
      int i = random.nextInt(SIZE) + 1;
      while (i > 0) {
        int j = random.nextInt(SIZE);
        while (done[j]) {
          j = (j + 1) & 511;
        }
        int k = 1 + random.nextInt(8);
        i -= k;
        while (k > 0) {
          k--;
          int l = (j + k) & (SIZE - 1);
          if (!done[l]) {
            NullTx nullTx = txis[l];
            if (isVerbose) {
              System.out.println("Inserting " + l);
            }
            sparseArray.addTx(l, nullTx);
            if (isVerbose) {
              System.out.println(sparseArray);
            }
            done[l] = true;
          }
        }

        j = -1;
        Arrays.fill(check, false);
        AllTxIterator iterator = new AllTxIterator(sparseArray, Long.MIN_VALUE, Long.MAX_VALUE);
        while (iterator.hasNext()) {
          Txi txi = iterator.next();
          int n = (int) txi.getNonce();
          if (n <= j) {
            throw new IllegalStateException("Non-increasing nonces: " + n);
          }
          j = n;
          check[n] = true;
        }
        if (!Arrays.equals(done, check)) {
          for (j = 0; j < SIZE; j++) {
            if (done[j] && !check[j]) {
              System.out.println(j + " written but not read");
            }
            if (check[j] && !done[j]) {
              System.out.println(j + " read but not written");
            }
          }
          throw new IllegalStateException("Mismatched");
        }
      }

      i = 0;
      while (i < SIZE) {
        i += random.nextInt(SIZE);
        if (isVerbose) {
          System.out.println("Removing up to " + i);
        }
        sparseArray.bulkRemove(i);
        if (isVerbose) {
          System.out.println(sparseArray);
        }
      }

      if (testSeed != null) {
        break;
      }
    }
  }


  public static void insertionTest() throws Exception {
    // size is a power of 2
    int SIZE = 512;

    NullTx[] txis = new NullTx[SIZE];
    for (int i = 0; i < SIZE; i++) {
      txis[i] = new NullTx(1, 1, "hash", i, false, "", "", "", 0, "", 0);
    }

    Long testSeed = null;
    boolean isVerbose = testSeed != null;

    Random seeds = new Random();
    int tests = 0;
    boolean[] done = new boolean[SIZE];
    for (int ii = 0; ii < 250_000; ii++) {
      tests++;
      long seed = (testSeed != null) ? testSeed.longValue() : seeds.nextLong();
      System.out.println("Test = " + tests + "   Seed = " + seed);
      Random random = new Random(seed);
      SparseArray sparseArray = new SparseArray();
      Arrays.fill(done, false);
      int i = SIZE;
      while (i > 0) {
        int j = random.nextInt(SIZE);
        while (done[j]) {
          j = (j + 1) & 511;
        }
        int k = 1 + random.nextInt(8);
        i -= k;
        while (k > 0) {
          k--;
          int l = (j + k) & (SIZE - 1);
          if (!done[l]) {
            NullTx nullTx = txis[l];
            if (isVerbose) {
              System.out.println("Inserting " + l);
            }
            sparseArray.addTx(l, nullTx);
            if (isVerbose) {
              System.out.println(sparseArray);
            }
            done[l] = true;
          }
        }
      }
      if (testSeed != null) {
        break;
      }
    }
  }


  public static void main(String[] args) throws Exception {
    insertionTest();
    insertAndDeleteTest();
  }


  /**
   * Dummy unit test to keep static analysis tools happy.
   */
  @Test
  @Ignore
  public void dummy() {
    assertTrue(System.currentTimeMillis() > 1);
  }

}
