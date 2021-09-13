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

/**
 * A root of the sparse array. The root can be one of:
 *
 * <ul>
 * <li>An offset to a 8-item range.
 * <li>An offset to a larger range.
 * <li>An offset to a split.
 * <li>A split.
 * </ul>
 *
 * <p>A root may require replacing after an insert or delete.</p>
 *
 * @author Simon Greatrix on 2019-04-15.
 */
class Root extends Node {

  /** Half way along the non-negative long range. */
  static final long HALF_WAY = 0x4000_0000_0000_0000L;


  long lastNonce;

  Node top;


  Root(long lastNonce) {
    super(0, Long.MAX_VALUE);
    this.lastNonce = lastNonce;
    top = null;
  }


  @Override
  Node add(Split mergePoint, long nonce, Txi txi, MutableBoolean didAdd) {
    throw new UnsupportedOperationException("Cannot add directly to root");
  }


  void add(long nonce, Txi txi, MutableBoolean didAdd) {
    if (nonce > lastNonce) {
      lastNonce = nonce;
    }

    if (top == null) {
      long start = nonce & ~0x7L;
      top = new Range(start, start + 7);
      top.add(null, nonce, txi, didAdd);
      return;
    }

    if (top.contains(nonce)) {
      top = top.add(null, nonce, txi, didAdd);
      return;
    }

    // Need to create either a new split, or extend an 8-item range into a 16-item range.
    if (top instanceof Range && top.coverage() == 8) {
      long common = top.start & ~0xfL;
      if (common == (nonce & ~0xfL)) {
        Range newRange = new Range(common, common + 15);
        top.doMerge(newRange);
        newRange.add(null, nonce, txi, didAdd);
        top = newRange;
        return;
      }
    }

    // Need new split
    long mask = SparseArray.commonMask(top.start, nonce);
    long newStart = top.start & mask;
    long newEnd = newStart + ~mask;
    top = new Split(newStart, newEnd, top, nonce, txi, didAdd);
  }


  @Override
  Root bulkRemove(long end) {
    if (end >= lastNonce) {
      top = null;
    } else {
      top = top.bulkRemove(end);
    }
    return this;
  }


  @Override
  int coverage() {
    return (top != null) ? top.coverage() : 0;
  }


  @Override
  void doMerge(Range target) {
    throw new UnsupportedOperationException("Cannot merge root");
  }


  @Override
  public void getRanges(Deque<Range> list) {
    if (top != null) {
      top.getRanges(list);
    }
  }


  @Override
  public Txi getTx(long nonce, Hash hash) {
    return (top != null && top.contains(nonce)) ? top.getTx(nonce, hash) : null;
  }


  public boolean isEmpty() {
    return top == null;
  }


  @Override
  String toString(int depth) {
    if (top == null) {
      return "Root{ *EMPTY* }";
    }
    return String.format("Root{Up to %d:\n%s%s\n}", lastNonce, toStringPad(1), top.toString(1));
  }
}
