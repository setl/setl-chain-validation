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
 * A Node is a branch point in the tree which represents the sparse array.  A node is either a split, or a range. There is no representation for sections of
 * the tree that do not divide.
 *
 * @author Simon Greatrix on 2019-04-15.
 */
abstract class Node {

  static String toStringPad(int depth) {
    StringBuilder builder = new StringBuilder(depth * 2);
    for (int i = 0; i < depth; i++) {
      builder.append("|  ");
    }
    return builder.toString();
  }


  /** Last index included in this node. */
  final long end;

  /** First index included in this node. */
  final long start;


  protected Node(long start, long end) {
    this.start = start;
    this.end = end;
  }


  abstract Node add(Split mergePoint, long nonce, Txi txi, MutableBoolean didAdd);


  abstract Node bulkRemove(long end);


  boolean contains(long nonce) {
    return start <= nonce && nonce <= end;
  }


  abstract int coverage();


  abstract void doMerge(Range target);


  abstract void getRanges(Deque<Range> list);


  abstract Txi getTx(long nonce, Hash hash);


  abstract String toString(int depth);


  @Override
  public String toString() {
    return toString(0);
  }
}
