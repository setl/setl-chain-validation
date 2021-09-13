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

import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-12.
 */
public class RootTest {

  @Test(expected = UnsupportedOperationException.class)
  public void add() {
    Root root = new Root(-1);
    root.add(null,1, null, null);
  }


  @Test(expected = UnsupportedOperationException.class)
  public void doMerge() {
    Root root = new Root(-1);
    root.doMerge(null);
  }
}