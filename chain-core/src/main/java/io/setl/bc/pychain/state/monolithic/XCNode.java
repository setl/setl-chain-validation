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

//Used only for python compatible XCHASH  - remove this class.



/**
 * No longer used.
 * @deprecated - unused
 */
@Deprecated
public class XCNode {

  private final long chainId;

  private final long height;

  private final PyWrappedImmutableIndexedList signNodes;

  private final long unknown;

  private final long unknown1;


  /**
   * <p>Create a new node from its array specification. Expected entries are:</p>
   *
   * <ol start="0">
   * <li>Chain ID</li>
   * <li>Height</li>
   * <li>List of signing nodes</li>
   * <li>???</li>
   * <li>???</li>
   * </ol>
   */
  public XCNode(MPWrappedArray node) {
    this.signNodes = new PyWrappedImmutableIndexedList(node.asWrapped(2), 3);
    this.chainId = node.asLong(0);
    this.height = node.asLong(1);
    this.unknown = node.asLong(3);
    this.unknown1 = node.asLong(4);

  }


  public Object[] getHashable() {
    return new Object[]{chainId, height, signNodes.computeRootHash(), unknown, unknown1};
  }
}
