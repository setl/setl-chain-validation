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
package io.setl.bc.pychain.peer.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-12-04.
 */
public class MsgPackObjectEncoderTest {

  @Test
  public void encode() throws Exception {
    ChannelHandlerContext context = mock(ChannelHandlerContext.class);

    MsgPackObjectEncoder encoder = new MsgPackObjectEncoder();
    ArrayList<Object> list = new ArrayList<>();
    encoder.encode(context, new Object[]{"one", 2, "three"}, list);
    assertEquals(1, list.size());
    assertTrue(list.get(0) instanceof ByteBuf);
  }
}