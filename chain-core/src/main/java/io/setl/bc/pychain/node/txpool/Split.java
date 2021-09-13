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
 * @author Simon Greatrix on 2019-04-14.
 */
class Split extends Node {

  final long midpoint;

  private int highCoverage;

  private Node highNode;

  private int lowCoverage;

  private Node lowNode;

  private Node me = this;


  Split(long start, long end, Node node, long nonce, Txi txi, MutableBoolean didAdd) {
    super(start, end);

    this.midpoint = (start >> 1) + (end >> 1) + 1;
    long rangeStart = nonce & ~7;
    long rangeEnd = rangeStart + 7;
    Range range = new Range(rangeStart, rangeEnd);
    range.add(null, nonce, txi, didAdd);

    if (node.start < midpoint) {
      lowNode = node;
      highNode = range;
    } else {
      lowNode = range;
      highNode = node;
    }
    highCoverage = highNode.coverage();
    lowCoverage = lowNode.coverage();
  }


  @Override
  Node add(Split mergePoint, long nonce, Txi txi, MutableBoolean didAdd) {
    // if more than 3/4 full, we will consider merging
    if (mergePoint == null) {
      long size = end - start;
      boolean canMerge = (size < 8192) && (4L * (8 + coverage()) > 3L * (size + 1));
      if (canMerge) {
        mergePoint = this;
      }
    }

    if (nonce < midpoint) {
      lowNode = addToChild(mergePoint, lowNode, nonce, txi, didAdd);
      lowCoverage = lowNode.coverage();
      return me;
    }

    highNode = addToChild(mergePoint, highNode, nonce, txi, didAdd);
    highCoverage = highNode.coverage();
    return me;
  }


  Node addToChild(Split mergePoint, Node child, long nonce, Txi txi, MutableBoolean didAdd) {
    if (child.contains(nonce)) {
      return child.add(mergePoint, nonce, txi, didAdd);
    }

    // Do merge?
    if (mergePoint != null) {
      mergePoint.merge(nonce, txi, didAdd);
      return Range.DUMMY;
    }

    // Need to create either a new split, or extend an 8-item range into a 16-item range.
    if (child instanceof Range && child.coverage() == 8) {
      long common = child.start & ~0xfL;
      if (common == (nonce & ~0xfL)) {
        Range newRange = new Range(common, common + 15);
        child.doMerge(newRange);
        newRange.add(null, nonce, txi, didAdd);
        return newRange;
      }
    }

    // Need new split
    long mask = SparseArray.commonMask(child.start, nonce);
    long newStart = child.start & mask;
    long newEnd = newStart + ~mask;
    return new Split(newStart, newEnd, child, nonce, txi, didAdd);
  }


  @Override
  Node bulkRemove(long start) {
    if (start >= midpoint) {
      // discard lower half and replace this with whatever the upper half becomes
      return highNode.bulkRemove(start);
    }

    lowNode = lowNode.bulkRemove(start);
    if (lowNode != null) {
      lowCoverage = lowNode.coverage();
      return this;
    }

    // low is now empty
    return highNode;
  }


  int coverage() {
    return lowCoverage + highCoverage;
  }


  @Override
  void doMerge(Range target) {
    lowNode.doMerge(target);
    highNode.doMerge(target);
  }


  @Override
  void getRanges(Deque<Range> list) {
    lowNode.getRanges(list);
    highNode.getRanges(list);
  }


  @Override
  Txi getTx(long nonce, Hash hash) {
    Node child = nonce < midpoint ? lowNode : highNode;
    return child.contains(nonce) ? child.getTx(nonce, hash) : null;
  }


  void merge(long nonce, Txi txi, MutableBoolean didAdd) {
    Range range = new Range(start, end);
    doMerge(range);
    range.add(null, nonce, txi, didAdd);
    me = range;
  }


  @Override
  String toString(int depth) {
    return String.format("Split{%x-%x/%x-%x -> %x=%x/%x:\n%s%s,\n%s%s\n%s}",
        start, midpoint - 1, midpoint, end, end - start + 1, midpoint - start, end - midpoint + 1,
        toStringPad(depth + 1), lowNode.toString(depth + 1),
        toStringPad(depth + 1), highNode.toString(depth + 1),
        toStringPad(depth));
  }
}
