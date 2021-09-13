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

import io.setl.bc.pychain.state.tx.Txi;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over the transaction pool. Does not support remove.
 *
 * @author Simon Greatrix on 2019-04-19.
 */
class TxIterator implements Iterator<Txi> {

  private final Deque<Range> rangeDeque;

  private int count;

  private int index;

  private Txi next;

  private Range range;

  private int size;


  TxIterator(SparseArray array, long firstNonce, int count) {
    rangeDeque = array.getRanges();
    this.count = count;

    next = null;
    if (count <= 0 || rangeDeque.isEmpty()) {
      return;
    }

    // find the first TX
    while (!rangeDeque.isEmpty()) {
      range = rangeDeque.removeFirst();
      size = range.txis.length;
      index = 0;
      while (index < size) {
        Txi txi = range.txis[index];
        index++;

        if (txi != null) {
          if (txi.getNonce() == firstNonce) {
            // Found first transaction
            next = txi;
            return;
          }
          if (txi.getNonce() > firstNonce) {
            // We only have futures
            return;
          }
        }
      }

      // finished range without finding first TX
    }
    // finished all ranges without finding first TX
  }


  /**
   * Get maximum number of remaining TXs.
   *
   * @return the maximum number remaining
   */
  public int getRemaining() {
    return count;
  }


  @Override
  public boolean hasNext() {
    return next != null;
  }


  @Override
  public Txi next() {
    Txi txi = next;
    if (txi == null) {
      throw new NoSuchElementException();
    }
    if (txi instanceof ConfusedNonceTx) {
      txi = ((ConfusedNonceTx) txi).getPrimaryTx();
    }

    // have we reached our counter?
    count--;
    if (count <= 0) {
      next = null;
      return txi;
    }

    // still in the same range?
    if (index < size) {
      next = range.txis[index];
      index++;
      return txi;
    }

    // are we out of ranges?
    if (rangeDeque.isEmpty()) {
      next = null;
      return txi;
    }

    range = rangeDeque.removeFirst();
    size = range.txis.length;
    index = 1;

    // the next TX has to be the first one in the range, or we have a gap
    next = range.txis[0];
    if (next != null && next.getNonce() != txi.getNonce() + 1) {
      next = null;
    }

    return txi;
  }
}
