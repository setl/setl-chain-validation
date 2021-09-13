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
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.P2PType;

/**
 * @author Simon Greatrix on 2019-03-26.
 */
public class PeerRequest implements Message {

  private final MPWrappedArray message;


  public PeerRequest(MPWrappedArray message) {
    this.message = message;
  }


  public PeerRequest(int chainId) {
    this.message = new MPWrappedArrayImpl(new Object[]{chainId, P2PType.PEER_REQUEST.getId()});
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  @Override
  public int getChainId() {
    return message.asInt(0);
  }


  @Override
  public P2PType getType() {
    return P2PType.PEER_REQUEST;
  }
}
