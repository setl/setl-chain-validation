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
import java.util.Arrays;
import java.util.Deque;

/**
 * A contiguous nonce range for transactions.
 *
 * @author Simon Greatrix on 2019-04-14.
 */
class Range extends Node {

  static final Range DUMMY = new Range(-1, -1);

  final Txi[] txis;


  Range(long start, long end) {
    super(start, end);
    txis = new Txi[1 + (int) (end - start)];
  }


  @Override
  Node add(Split mergePoint, long nonce, Txi txi, MutableBoolean didAdd) {
    int index = (int) (nonce - start);
    Txi current = txis[index];
    if (current == null) {
      txis[index] = txi;
      didAdd.set(true);
    } else {
      // do not count as a set
      txis[index] = ConfusedNonceTx.merge(current, txi);
    }
    return this;
  }


  @Override
  Node bulkRemove(long last) {
    if (last < start) {
      return this;
    }
    if (last >= end) {
      return null;
    }
    int index = (int) (last - start);
    boolean isEmpty = true;
    for (int i = index + 1; i < txis.length; i++) {
      if (txis[i] != null) {
        isEmpty = false;
        break;
      }
    }

    if (isEmpty) {
      return null;
    }

    if (txis.length == 8) {
      // Can't make an 8 smaller
      Arrays.fill(txis, 0, index + 1, null);
      return this;
    }

    long common = SparseArray.commonRoot(end, last) & ~0xfL;
    if (common == start) {
      // Exactly the same range
      Arrays.fill(txis, 0, index + 1, null);
      return this;
    }

    // Create new, smaller range
    int newLength = 1 + (int) (end - common);
    Range newRange = new Range(common, end);
    int toCopy = (int) (end - last);
    System.arraycopy(txis, txis.length - toCopy, newRange.txis, newLength - toCopy, toCopy);
    return newRange;
  }


  int coverage() {
    return txis.length;
  }


  @Override
  void doMerge(Range target) {
    int offset = (int) (start - target.start);
    int l = txis.length;
    System.arraycopy(txis, 0, target.txis, offset, l);
  }


  @Override
  void getRanges(Deque<Range> list) {
    list.addLast(this);
  }


  @Override
  Txi getTx(long nonce, Hash hash) {
    int index = (int) (nonce - start);
    Txi out = txis[index];
    if (out == null) {
      return null;
    }
    if (hash.equalsHex(out.getHash())) {
      return out;
    }
    if (out instanceof ConfusedNonceTx) {
      return ((ConfusedNonceTx) out).getTx(hash);
    }
    return null;
  }


  @Override
  String toString(int depth) {
    StringBuilder sb = new StringBuilder("Range{").append(Long.toHexString(start)).append('-').append(Long.toHexString(end))
        .append(" -> ").append(txis.length);
    int l = sb.length();
    char divider = ':';
    for (int i = 0; i < txis.length; i++) {
      if (txis[i] != null) {
        sb.setLength(l);
        sb.append(divider).append(start + i);
        if (divider != '-') {
          divider = '-';
          l = sb.length();
        }
      } else {
        if (divider == '-') {
          divider = ',';
          l = sb.length();
        }
      }
    }
    sb.append('}');
    return sb.toString();
  }
}
