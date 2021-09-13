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
package io.setl.bc.blockprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.state.test.BlockBuilder;

/**
 * @author Simon Greatrix on 01/04/2021.
 */
public class BlockListenerTest {

  static class TestConsumer implements Consumer<Block> {

    List<Block> received = new ArrayList<>();


    @Override
    public void accept(Block block) {
      received.add(block);
    }

  }



  TestConsumer testConsumer = new TestConsumer();

  BlockListener blockListener = new BlockListener(-1, 20, testConsumer);

  @Test
  public void testInterested() {
    assertEquals(Integer.MAX_VALUE,blockListener.nextInteresting(8));
    assertFalse(blockListener.isFinished());

    blockListener.startAt(10);
    assertFalse(blockListener.isFinished());
    assertEquals(10,blockListener.nextInteresting(8));

    blockListener.startAt(15);
    assertEquals(10,blockListener.nextInteresting(8));

    assertEquals(20,blockListener.nextInteresting(20));
    assertEquals(Integer.MAX_VALUE,blockListener.nextInteresting(21));
  }

  @Test
  public void testAcceptNotStarted() {
    Block block = new BlockBuilder().withHeight(10).build();
    blockListener.accept(block);
    assertTrue(testConsumer.received.isEmpty());
  }


  @Test
  public void testAcceptTooLowStarted() {
    Block block = new BlockBuilder().withHeight(10).build();
    blockListener.startAt(11);
    blockListener.accept(block);
    assertTrue(testConsumer.received.isEmpty());
  }


  @Test
  public void testAcceptTooHighStarted() {
    Block block = new BlockBuilder().withHeight(21).build();
    blockListener.startAt(10);
    blockListener.accept(block);
    assertTrue(testConsumer.received.isEmpty());
  }



  @Test
  public void testAcceptHappy() {
    Block block = new BlockBuilder().withHeight(10).build();
    blockListener.startAt(10);
    blockListener.accept(block);
    assertEquals(1,testConsumer.received.size());
  }


  @Test
  public void testAcceptHappyMultipleBlocks() {
    blockListener.startAt(10);
    blockListener.accept(new BlockBuilder().withHeight(10).build());
    blockListener.accept(new BlockBuilder().withHeight(11).build());
    blockListener.accept(new BlockBuilder().withHeight(11).build());
    blockListener.accept(new BlockBuilder().withHeight(11).build());
    blockListener.accept(new BlockBuilder().withHeight(12).build());
    assertEquals(3,testConsumer.received.size());
  }


  @Test
  public void testAcceptOutOfOrderBlocks() {
    blockListener.startAt(10);
    blockListener.accept(new BlockBuilder().withHeight(15).build());
    blockListener.accept(new BlockBuilder().withHeight(12).build());
    blockListener.accept(new BlockBuilder().withHeight(11).build());
    blockListener.accept(new BlockBuilder().withHeight(11).build());
    blockListener.accept(new BlockBuilder().withHeight(9).build());
    blockListener.accept(new BlockBuilder().withHeight(10).build());
    assertEquals(3,testConsumer.received.size());
    assertEquals(10,testConsumer.received.get(0).getHeight());
    assertEquals(11,testConsumer.received.get(1).getHeight());
    assertEquals(12,testConsumer.received.get(2).getHeight());
  }
}