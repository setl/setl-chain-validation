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
import io.setl.bc.pychain.msgpack.MPWrappedMap;

/**
 * Created by aanten on 30/06/2017.
 */
public class PeerMessage {

  private final MPWrappedArray peer;

  public PeerMessage(MPWrappedArray peer) {
    this.peer = peer;
  }

  public String getIP() {
    return peer.asString(0);

  }

  public int getPort() {
    return peer.asInt(1);
  }

  public String getNodeName() {
    return peer.asString(2);
  }

  public MPWrappedMap<Object, Object> getMap() {
    return peer.asWrappedMap(3);
  }

  public int getNodeType() {
    return peer.asInt(4);
  }

  public int getConnectionCount() {
    return peer.asInt(5);
  }

  public int getUnknown() {
    return peer.asInt(6);
  }
}
