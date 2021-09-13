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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.common.CommonPy.P2PType;

/**
 * A wrapper for an unrecognised message.
 *
 * @author Simon Greatrix on 2019-03-26.
 */
public class UnknownMessage implements Message {

  private final MPWrappedArray message;


  public UnknownMessage(MPWrappedArray message) {
    this.message = message;
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  @Override
  public int getChainId() {
    if (message.isEmpty()) {
      return -1;
    }
    Object o = message.get(0);
    return (o instanceof Number) ? ((Number) o).intValue() : -1;
  }


  public MPWrappedArray getMessage() {
    return message;
  }


  @Override
  public P2PType getType() {
    return P2PType.UNKNOWN;
  }


  /**
   * Get the type assigned to this message.
   *
   * @return the message's type
   */
  public int getTypeId() {
    if (message.size() < 2) {
      return -1;
    }
    Object o = message.get(1);
    return (o instanceof Number) ? ((Number) o).intValue() : -1;
  }
}
