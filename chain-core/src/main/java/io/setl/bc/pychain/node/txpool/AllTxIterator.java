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
 * An iterator over a sparse array which matches a nonce range.
 *
 * @author Simon Greatrix on 2019-04-19.
 */
class AllTxIterator implements Iterator<Txi> {

  private final long lastNonce;

  private final Deque<Range> rangeDeque;

  private int index;

  private Txi next;

  private Range range;

  private int size;


  /**
   * Create new iterator
   *
   * @param array      the array
   * @param firstNonce the first nonce to return
   * @param lastNonce  the last nonce (exclusive). That is, the first nonce not to return
   */
  AllTxIterator(SparseArray array, long firstNonce, long lastNonce) {
    this.lastNonce = lastNonce;
    rangeDeque = array.getRanges();

    next = null;
    if (lastNonce <= firstNonce || rangeDeque.isEmpty()) {
      return;
    }

    // find the first TX
    while (!rangeDeque.isEmpty()) {
      range = rangeDeque.removeFirst();
      size = range.txis.length;
      index = 0;
      while (index < size) {
        Txi txi = range.txis[index];

        if (txi != null) {
          long n = txi.getNonce();
          if (firstNonce <= n) {
            if (n < lastNonce) {
              // Found first transaction
              next = txi;
              index++;
            }
            return;
          } else {
            // Skip forward appropriate amount
            index += firstNonce - n;
          }
        } else {
          // Next entry might be non-null
          index++;
        }
      }

      // finished range without finding first TX
    }
    // finished all ranges without finding first TX
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

    next = null;
    do {
      // still in the same range?
      if (index < size) {
        next = range.txis[index];
        index++;
        continue;
      }

      // are we out of ranges?
      if (rangeDeque.isEmpty()) {
        next = null;
        return txi;
      }

      range = rangeDeque.removeFirst();
      size = range.txis.length;
      next = range.txis[0];
      index = 1;

      } while (next == null);

    // check if we have reached the last nonce
    if (next.getNonce() >= lastNonce) {
      next = null;
    }
    return txi;
  }
}
