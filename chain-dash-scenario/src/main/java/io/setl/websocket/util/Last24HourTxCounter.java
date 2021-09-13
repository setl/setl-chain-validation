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
package io.setl.websocket.util;

public class Last24HourTxCounter {
  private static final int SECONDS_PER_DAY = 86400;
  private int[] counter;
  private int total = 0;
  private int lastUpdatedIdx = 0;
  private boolean initialised = false;

  private void init(int delay) {
    counter = new int[SECONDS_PER_DAY / delay];
    initialised = true;
  }

  /**
   * Update total of last 24 hours transactions.
   * @param timestamp timestamp in seconds
   * @param txs number of transactions
   * @param delay interval in seconds
   */
  public void update(long timestamp, int txs, int delay) {
    if (!initialised) {
      init(delay);
    }

    int currentIdx = (int) (timestamp % SECONDS_PER_DAY) / delay;
    if (currentIdx == lastUpdatedIdx) {
      total = total + txs;
    } else {
      resetMissedSlots(currentIdx);
      total = total - counter[currentIdx] + txs;
    }

    lastUpdatedIdx = currentIdx;
    counter[currentIdx] = txs;
  }

  private void resetMissedSlots(int currentIdx) {
    if (currentIdx > lastUpdatedIdx) {
      // When idx of the current slot > last updated slot, then reset all slots between the two indices.
      for (int idx = lastUpdatedIdx + 1; idx < currentIdx; ++idx) {
        total -= counter[idx];
        counter[idx] = 0;
      }
    } else {
      // When idx of the current slot < last updated slot:
      // 1) Reset all slots after last updated slot, until the end of the array.
      for (int idx = lastUpdatedIdx + 1; idx < counter.length; ++idx) {
        total -= counter[idx];
        counter[idx] = 0;
      }
      // 2) Reset all slots from the beginning of the array, until the slot before the current one.
      for (int idx = 0; idx < currentIdx; ++idx) {
        total -= counter[idx];
        counter[idx] = 0;
      }
    }
  }

  public int getTotal() {
    return total;
  }
}
