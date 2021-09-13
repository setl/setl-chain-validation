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

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;

/**
 * A generic implementation which should be replaced by a more specific class.
 */
public class DummyEntry implements MEntry {

  private long updateHeight = -1;

  public static class Decoder implements EntryDecoder<DummyEntry> {
    @Override
    public DummyEntry decode(MPWrappedArray va) {
      return new DummyEntry(va);
    }
  }

  private MPWrappedArray wrapped;

  public String getValueForTest() {

    return valueForTest;
  }

  public void setValueForTest(String valueForTest) {

    this.valueForTest = valueForTest;
  }

  private String valueForTest = "";  // Only exists to help with Snapshot testing.

  public DummyEntry(MPWrappedArray va) {
    this.wrapped = va;
  }

  @Override
  public Object[] encode(long index) {

    Object[] rVal = wrapped.unwrap();
    rVal[0] = index;
    return rVal;

  }

  @Override
  public String getKey() {
    return wrapped.asString(1);
  }

  @Override
  public DummyEntry copy() {
    return new Decoder().decode(new MPWrappedArrayImpl(encode(-1)));
  }

  public long getBlockUpdateHeight() {
    return updateHeight;
  }

  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }

}
