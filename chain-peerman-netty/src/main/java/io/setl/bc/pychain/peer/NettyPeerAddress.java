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
package io.setl.bc.pychain.peer;

import io.netty.channel.Channel;

public class NettyPeerAddress implements PeerAddress {

  private final Channel channel;



  public NettyPeerAddress(Channel channel) {
    this.channel = channel;
  }


  public Channel getCtx() {
  return channel;
  }


  @Override
  public String getAddressString() {
    return this.toString();
  }


  @Override
  public String toString() {
    if (channel != null && channel.remoteAddress() != null) {
      String uuid = "";
      if (channel.hasAttr(NettyPeerManager.ORIGIN_KEY)) {
        uuid = channel.attr(NettyPeerManager.ORIGIN_KEY).get();
      }

      return uuid + "=" + channel.remoteAddress().toString().replace("/", "");
    }

    return "Peer not connected.";
  }

}
