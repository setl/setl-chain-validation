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
package io.setl.bc.pychain.state.ipfs;

import io.setl.bc.pychain.state.entry.MEntry;

/**
 * @author Simon Greatrix on 10/12/2018.
 */
class TestEntry implements MEntry {

  static String keyGen(int i) {
    return String.format("Key=%d", i);
  }

  private final int i;

  private long updateHeight = -1;

  public TestEntry(int i) {
    this.i = i;
  }


  public TestEntry(int i, String s, String s1) {
    this.i = i;
  }


  @Override
  public TestEntry copy() {
    return new TestEntry(this.i);
  }


  @Override
  public Object[] encode(long index) {
    return new Object[]{index, keyGen(i), "More stuff" + i};
  }


  @Override
  public String getKey() {
    return keyGen(i);
  }

  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }

  public long getBlockUpdateHeight() {
    return updateHeight;
  }

}
