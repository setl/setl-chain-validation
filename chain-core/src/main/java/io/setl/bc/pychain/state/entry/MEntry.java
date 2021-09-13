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
package io.setl.bc.pychain.state.entry;

import io.setl.bc.pychain.common.ToObjectArray;

/**
 * An entry in a block-chain Merkle list.
 *
 * @author aanten
 */
public interface MEntry extends ToObjectArray, KeyedEntry<String>, ClonableEntry<MEntry> {

  /**
   * Encode the entry as an object array.
   *
   * @param index an irrelevant index
   *
   * @deprecated Encoding fix via an Object[] is no longer the only or best method.
   */
  @Deprecated(since = "28-11-2019")
  default Object[] encode(long index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the last block height this entry was updated.
   *
   * @return the height of the last update
   */
  long getBlockUpdateHeight();

  /**
   * Set the block height at which that is being updated.
   *
   * @param blockHeight the height
   */
  void setBlockUpdateHeight(long blockHeight);

}
