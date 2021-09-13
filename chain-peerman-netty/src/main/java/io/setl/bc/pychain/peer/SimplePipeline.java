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

import io.netty.channel.ChannelPipeline;
import io.setl.bc.pychain.peer.pipeline.MsgPackObjectEncoder;

// squid:S1118 - have a private constructor to suppress default public zero-arg constructor
@SuppressWarnings("squid:S1118")
public class SimplePipeline {

  /**
   * Maximum packet size in bytes.
   */
  protected static final int MAX_PACKET = 2000000000;


  /**
   * Build the standard SETL parts of the communications pipeline.
   *
   * @param pipeline the pipeline to configure
   */
  public static void build(ChannelPipeline pipeline) {
    pipeline.addLast("msgPackDecoder", new MsgPackWrappedDecoder());
    pipeline.addLast("msgPackEncoder", new MsgPackObjectEncoder());
  }

}