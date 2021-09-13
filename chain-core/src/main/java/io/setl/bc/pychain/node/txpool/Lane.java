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

import io.setl.bc.pychain.block.ProposedTxList.Builder;
import io.setl.bc.pychain.node.StateManager;

/**
 * A lane within the pool. The lane contains a hash-table which uses linear-probing and a load factor from 0.25 to 0.5. For each address we hold a series
 * of transactions.
 */
class Lane {

  /** Number of series in this lane. */
  int size;

  /** The buckets that store the series in this lane. */
  private Series[] buckets = new Series[4];

  /** Bit mask for identifying the bucket. */
  private int mask = 3;


  synchronized void bulkRemove(String address, long max) {
    int hc = address.hashCode();
    int l = buckets.length;

    for (int i = 0; i < l; i++) {
      int j = (hc + i) & mask;
      Series b = buckets[j];
      if (b == null) {
        // definitely not found
        return;
      }
      if (b.getAddressCode() == hc && b.getAddress().equals(address)) {
        // Found the series
        b.bulkRemove(max);
      }
    }

    // We will never get here, as there will always be a null bucket.
    throw new IllegalStateException("No gaps in hash table");
  }


  synchronized void cleanUp() {
    int oldSize = size;
    for (int i = 0; i < buckets.length; i++) {
      Series s = buckets[i];
      if (s != null && s.isEmpty()) {
        buckets[i] = null;
        size--;
      }
    }
    if (size != oldSize) {
      rehash();
    }
  }


  synchronized void clear() {
    buckets = new Series[4];
    mask = 3;
    size = 0;
  }


  synchronized Series findOrCreate(StateManager stateManager, String address) {
    int hc = address.hashCode();
    int l = buckets.length;

    for (int i = 0; i < l; i++) {
      int j = (hc + i) & mask;
      Series b = buckets[j];
      if (b == null) {
        // definitely not found
        b = buckets[j] = new Series(stateManager, address);
        size++;
        if (size * 2 > l) {
          rehash();
        }
        return b;
      }
      if (b.getAddressCode() == hc && b.getAddress().equals(address)) {
        // Found the series
        return b;
      }
    }

    // As at least half our entries should be null, we should never get here.
    throw new IllegalStateException("No gaps in hash table");
  }


  synchronized int propose(Builder builder, int maxTxPerBlock) {
    for (int i = buckets.length - 1; i >= 0 && maxTxPerBlock > 0; i--) {
      Series s = buckets[i];
      if (s != null) {
        TxIterator iterator = s.iterate(maxTxPerBlock);
        while (iterator.hasNext()) {
          builder.add(iterator.next());
        }
        maxTxPerBlock = iterator.getRemaining();
      }
    }
    return maxTxPerBlock;
  }


  private void rehash() {
    if (size == 0) {
      buckets = new Series[4];
      mask = 3;
      return;
    }

    int newLength = 4 * Integer.highestOneBit(size);
    int newMask = newLength - 1;
    Series[] newBuckets = new Series[newLength];
    for (Series s : buckets) {
      if (s == null) {
        continue;
      }

      int p = s.getAddressCode() & newMask;
      for (int i = 0; i < newLength; i++) {
        int j = (i + p) & newMask;
        if (newBuckets[j] == null) {
          // This will always happen at some point
          newBuckets[j] = s;
          break;
        }
      }
    }

    buckets = newBuckets;
    mask = newMask;
  }
}
