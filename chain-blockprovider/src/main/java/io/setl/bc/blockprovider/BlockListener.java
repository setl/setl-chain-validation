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
package io.setl.bc.blockprovider;

import java.util.TreeMap;
import java.util.function.Consumer;

import io.setl.bc.pychain.block.Block;

/**
 * @author Simon Greatrix on 01/04/2021.
 */
class BlockListener implements Consumer<Block> {

  /** The actual consumer of the blocks. */
  final Consumer<Block> consumer;

  /** End of interest - inclusive. */
  private final int endHeight;

  /** Map of blocks that arrived out of sequence and cannot be sent to the consumer yet. */
  private final TreeMap<Integer, Block> unsentBlocks = new TreeMap<>();

  /**
   * Start of interest - inclusive. The magic value of '-1' is used to indicate the listener is interested in blocks as they come available, but not in
   * history.
   */
  private int startHeight;


  BlockListener(int startHeight, int endHeight, Consumer<Block> consumer) {
    this.startHeight = startHeight;
    this.endHeight = endHeight;
    this.consumer = consumer;
  }


  @Override
  public void accept(Block block) {
    synchronized (this) {
      int height = block.getHeight();
      if (startHeight == -1 || height < startHeight || height > endHeight) {
        return;
      }

      // Is this a future block? If so need to keep it for later
      if (height > startHeight) {
        unsentBlocks.put(height, block);
        return;
      }

      // This is the next block we want
      consumer.accept(block);
      startHeight++;

      // Send any postponed blocks.
      while ((block = unsentBlocks.remove(startHeight)) != null) {
        consumer.accept(block);
        startHeight++;
      }
    }
  }


  /**
   * Has this listener received all the blocks it is interested in?.
   *
   * @return true if the listener has had all the blocks it wanted
   */
  boolean isFinished() {
    return startHeight > endHeight;
  }


  /**
   * What is the next height this listener is interested in?.
   *
   * @param height the height being suggested.
   *
   * @return the suggested height, the starting height for this listener, or Integer.MAX_VALUE if not interested.
   */
  int nextInteresting(int height) {
    // Not interested if un-started, or above or end height.
    if (startHeight == -1 || endHeight < height) {
      return Integer.MAX_VALUE;
    }
    // interested in the proposed block, or the our starting block, whichever is highest.
    return Math.max(height, startHeight);
  }


  void startAt(int height) {
    if (startHeight == -1) {
      startHeight = height;
    }
  }

}
