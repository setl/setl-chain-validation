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

/**
 * @author Simon Greatrix on 2019-03-19.
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.InputStream;
import java.util.List;

public class MsgPackWrappedDecoder extends MessageToMessageDecoder<ByteBuf> {

  public MsgPackWrappedDecoder() {
    super(ByteBuf.class);
  }


  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    try (InputStream inputStream = new ByteBufInputStream(msg)) {
      out.add(MsgPackUtil.unpackWrapped(MsgPackUtil.newUnpacker(inputStream)));
    }
  }
}
