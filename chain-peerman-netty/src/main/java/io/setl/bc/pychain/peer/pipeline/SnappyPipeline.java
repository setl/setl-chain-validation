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


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "p2p.pipeline", havingValue = "snappy")
public class SnappyPipeline implements FrameDecoder, HealthIndicator {

  private double compressed = 0d;
  private double uncompressed = 0d;
  private Object lock = new Object();

  @Override
  public void addPipeline(ChannelPipeline pipeline) {
    pipeline.addLast("frameDecoder", new SnappyFrameDecoder());
    pipeline.addLast("frameEncoder", new SnappyFrameEncoder() {

      @Override
      protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        int reader0 = in.readerIndex();
        int writer0 = out.writerIndex();
        super.encode(ctx, in, out);
        int reader1 = in.readerIndex() - reader0;
        int writer1 = out.writerIndex() - writer0;
        synchronized (lock) {
          compressed = compressed + writer1;
          uncompressed = uncompressed + reader1;
        }
      }
    });
  }


  @Override
  public Health health() {
    synchronized (lock) {
      return Health.up().withDetail("Compressed data", compressed)
          .withDetail("Uncompressed data", uncompressed)
          .withDetail("Ratio", compressed * uncompressed == 0 ? 0 : uncompressed / compressed).build();
    }
  }
}
