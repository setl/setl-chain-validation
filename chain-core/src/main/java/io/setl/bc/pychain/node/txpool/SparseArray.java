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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.common.MutableBoolean;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Storage for transactions in nonce order. The storage assumes that most transactions will be well-behaved and have a unique nonce with no gaps.
 *
 * <p>A TX could try to re-use an already consumed nonce. Such a TX will be discarded as soon as it identified.</p>
 *
 * <p>A TX could use a future nonce with a gap. Such a TX must be retained until the gap is filled and it can be applied to state.</p>
 *
 * <p>A TX could re-use a nonce that is not consumed. Such a TX is always an error but one of the TXs with that nonce must be applied to state.</p>
 *
 * <h2>Implementation</h2>
 *
 * <p>The non-negative long range is conceptually accessed by a binary tree, with each branch dividing the range in half. For efficiency a run of branches
 * which all have one child is replaced by a single offset node. As the array fills up, offset nodes may be replaced by an actual binary split.</p>
 *
 * <p>The nonce (which must be non-negative), is used to determine an initial eight nonce range where a suitable array is created if necessary. Two adjacent
 * arrays will be merged if they complete a natural range of twice the size. For example, ranged 0-7 and 8-15 can be merged into 0-15, but 8-15 and 16-23 fall
 * in different 16-nonce ranges and cannot be merged.</p>
 *
 * <p>Range merging may roll-up multiple levels at once. For example, if we had 0-31, 32-47, and 48-55, then adding 56 will create a merged range of 0-63.</p>
 *
 * <p>To avoid having to locate large areas of contiguous memory for arrays, ranges of 8192 members will not be merged.</p>
 *
 * @author Simon Greatrix on 2019-04-14.
 */
public class SparseArray {


  /**
   * Identify the largest mask that when applied to the two operands yields the same result.
   */
  static long commonMask(long l1, long l2) {
    int h = 64;
    int l = 3;

    long b;
    while (true) {
      int m = (h + l) / 2;
      b = -(1L << m);
      if ((l1 & b) == (l2 & b)) {
        h = m;
        if (h == l) {
          return b;
        }
      } else {
        l = m + 1;
        if (h == l) {
          return (b << 1);
        }
      }
    }
  }


  static long commonRoot(long l1, long l2) {
    return l1 & commonMask(l1, l2);
  }


  Root root;


  public SparseArray() {
    root = new Root(-1);
  }


  /**
   * Add a transaction to his array.
   *
   * @param nonce the transaction's nonce
   * @param txi   the transaction
   *
   * @return true if this is added, false if there was already a value for this nonce
   */
  public boolean addTx(long nonce, Txi txi) {
    MutableBoolean didAdd = new MutableBoolean(false);
    root.add(nonce, txi, didAdd);
    return didAdd.get();
  }


  public void bulkRemove(long max) {
    root = root.bulkRemove(max);
  }


  Deque<Range> getRanges() {
    LinkedList<Range> list = new LinkedList<>();
    root.getRanges(list);
    return list;
  }


  public Txi getTx(long nonce, Hash hash) {
    return root.getTx(nonce, hash);
  }


  public boolean isEmpty() {
    return root.isEmpty();
  }


  public String toString() {
    return root.toString();
  }
}
