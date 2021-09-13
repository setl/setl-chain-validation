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
package io.setl.bc.pychain.state.monolithic;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.IndexedList;
import io.setl.bc.pychain.state.hashtree.FullFastMerkleHashCalculator;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.utils.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PyWrappedImmutableIndexedList implements IndexedList<Object[]> {

  private static final SerialiseToByte hashSerialiser = HashSerialisation.getInstance();

  private static final Logger logger = LoggerFactory.getLogger(PyWrappedImmutableIndexedList.class);

  protected MPWrappedArray wrappedMerkleArray;


  /**
   * <p>Create a new indexed list from the provided array. For versions of 3 or greater, the expected encoding is:</p>
   *
   * <ol start="0">
   * <li>Ignored</li>
   * <li>Ignored</li>
   * <li>Ignored</li>
   * <li>Ignored</li>
   * <li>The encoded list</li>
   * </ol>
   *
   * @param v       the array of data
   * @param version the encoding version
   */
  public PyWrappedImmutableIndexedList(MPWrappedArray v, int version) {
    if (v == null) {
      // simply create as empty
      wrappedMerkleArray = new MPWrappedArrayImpl(new Object[0]);
      return;
    }
    if (version <= 2) {
      wrappedMerkleArray = v.asWrapped(0);

      // MPWrappedMap lookupdict=v.asWrappedMap(1)
      // Note merkle hashlists are straight string.getbytes
      // MPWrappedArray hashlist=v.asWrapped(2)
    } else {
      // First 4 values are object-type, M version, data-type, and max offset.
      wrappedMerkleArray = v.asWrapped(4);
      // MPWrappedMap lookupdict=v.asWrappedMap(5)

      // Note merkle hashlists are straight string.getbytes
      // MPWrappedArray hashlist=v.asWrapped(6)
    }
  }


  protected PyWrappedImmutableIndexedList() {
    wrappedMerkleArray = new MPWrappedArrayImpl(new Object[0]);
  }


  /**
   * Compute the root hash of the Merkle Tree.
   *
   * @return the hash as a hexadecimal String. Empty string if this list is empty.
   */
  public String computeRootHash() {
    if (wrappedMerkleArray == null || wrappedMerkleArray.size() == 0) {
      logger.info("{} Blank roothash", this.getClass().getName());
      return "";
    }
    byte[][][] b = new FullFastMerkleHashCalculator().computeHashTreeFast(this, hashSerialiser);
    return ByteUtil.bytesToHex(b[b.length - 1][0]);
  }


  public MPWrappedArray debugGetWrappedMerkleArray() {
    return wrappedMerkleArray;
  }


  @Override
  public long getEntryCount() {
    return wrappedMerkleArray.size();
  }


  @Override
  public Object[] getHashableEntry(long i) {
    return wrappedMerkleArray.asObjectArray((int) i);
  }


  @Override
  public String toString() {
    try {
      int size = wrappedMerkleArray == null ? 0 : wrappedMerkleArray.size();
      return String.format("merkle:%d:%s", size, wrappedMerkleArray);
    } catch (Exception ex) {
      logger.error("ToString:", ex);
    }
    return "";
  }
}
