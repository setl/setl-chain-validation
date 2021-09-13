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
package io.setl.bc.pychain.state.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileBlockLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.MemoTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.create.BaseTransaction;
import io.setl.bc.pychain.tx.create.Memo;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @author Simon Greatrix on 09/01/2020.
 */
public class BlockBuilderTest {


  @Test
  public void getBaseStateHash() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertEquals(Hash.fromHex("12fc3dced794ffd50f22e651d352e01fe2221b3980b6d2d3c01a19f5fa1746f5"), bb.getBaseStateHash());
  }


  @Test
  public void getChainId() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertEquals(20, bb.getChainId());
  }


  @Test
  public void getContractEvents() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertTrue(bb.getContractEvents().isEmpty());
  }


  @Test
  public void getCrossChainTxs() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertTrue(bb.getCrossChainTxs().isEmpty());
  }


  @Test
  public void getEffectiveTxList() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertTrue(bb.getEffectiveTxList().isEmpty());
  }


  @Test
  public void getEffectiveTxiList() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertTrue(bb.getEffectiveTxiList().isEmpty());
  }


  @Test
  public void getHeight() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertEquals(5, bb.getHeight());
  }


  @Test
  public void getNodeName() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertEquals("network6363.local", bb.getNodeName());
  }


  @Test
  public void getPreviousBlockHash() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertEquals(Hash.fromHex("36c7b1b2554d98939b828043eb67e83be9fcea3eea2df0e4c2090784ad0bb2c4"), bb.getPreviousBlockHash());
  }


  @Test
  public void getTimeEvents() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    Set<String> trans = bb.getTimeEvents();
    assertTrue(trans.isEmpty());
  }


  @Test
  public void getTimeStamp() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    assertEquals(1495116815, bb.getTimeStamp());
  }


  @Test
  public void getTxList() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    List<BaseTransaction> trans = bb.getTxList();
    assertEquals(144, trans.size());
    HashSet<String> hashes = new HashSet<>();
    for (BaseTransaction bt : trans) {
      hashes.add(bt.getHash());
    }
    assertEquals(trans.size(), hashes.size());
  }


  @Test
  public void getTxiList() {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    List<Txi> trans = bb.getTxiList();
    assertEquals(144, trans.size());
    HashSet<String> hashes = new HashSet<>();
    for (Txi bt : trans) {
      hashes.add(bt.getHash());
    }
    assertEquals(trans.size(), hashes.size());
  }


  private Block loadBlock() {
    FileBlockLoader blockLoader = new FileBlockLoader();
    try {
      return blockLoader.loadBlockFromFile("src/test/resources/test-transitions/mono/task/5-6/47cc7420bda671f748b8b61bcf06ef275ebbb6236bf868ddab00efe9fff40bfb");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load test block", e);
    }
  }


  @Test
  public void setBaseStateHash() {
    BlockBuilder bb = new BlockBuilder();
    Hash h = Hash.fromHex("01234567");
    bb.setBaseStateHash(h);
    Block b = bb.build();
    assertEquals(h, b.getBaseStateHash());
  }


  @Test
  public void setChainId() {
    BlockBuilder bb = new BlockBuilder();
    bb.setChainId(56);
    Block b = bb.build();
    assertEquals(56, b.getChainId());
  }


  @Test
  public void setContractEvents() {
    BlockBuilder bb = new BlockBuilder();
    bb.setContractEvents(List.of(
        new EventData("a", "b", "c", "d"),
        new EventData("b", "b", "c", "d"),
        // this event data has a duplicate ID, so is ignored
        new EventData("a", "z", "c", "z")
    ));
    assertEquals(2, bb.getContractEvents().size());
  }


  @Test
  public void setCrossChainTxs() {
    Set<XCTxId> txIds = Set.of(
        new XCTxId(12, Hash.fromHex("01")),
        new XCTxId(12, Hash.fromHex("23")),
        new XCTxId(12, Hash.fromHex("45"))
    );
    BlockBuilder bb = new BlockBuilder();
    bb.setCrossChainTxs(txIds);
    Block b = bb.build();
    Set<XCTxId> actual = BlockBuilder.decodeXChainTxs(b);
    assertEquals(txIds, actual);
  }


  @Test
  public void setEffectiveTxList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();

    BlockBuilder bb = new BlockBuilder();
    bb.setEffectiveTxList(null);
    Block b = bb.build();
    Txi[] txis = b.getEffectiveTransactions();
    assertEquals(0, txis.length);

    bb.setEffectiveTxList(List.of(memo));
    b = bb.build();
    txis = b.getEffectiveTransactions();
    assertEquals(1, txis.length);
    assertEquals(tx.getHash(), txis[0].getHash());
  }


  @Test
  public void setEffectiveTxiList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();

    BlockBuilder bb = new BlockBuilder();
    bb.setEffectiveTxiList(null);
    Block b = bb.build();
    Txi[] txis = b.getEffectiveTransactions();
    assertEquals(0, txis.length);

    bb.setEffectiveTxiList(List.of(tx));
    b = bb.build();
    txis = b.getEffectiveTransactions();
    assertEquals(1, txis.length);
    assertEquals(tx.getHash(), txis[0].getHash());
  }


  @Test
  public void setHeight() {
    BlockBuilder bb = new BlockBuilder();
    bb.setHeight(123);
    Block b = bb.build();
    assertEquals(123, b.getHeight());
  }


  @Test
  public void setNodeName() {
    BlockBuilder bb = new BlockBuilder();
    bb.setNodeName("wibble");
    Block b = bb.build();
    assertEquals("wibble", b.getNodeName());
  }


  @Test
  public void setPreviousBlockHash() {
    BlockBuilder bb = new BlockBuilder();
    Hash h = Hash.fromHex("89abcdef");
    bb.setPreviousBlockHash(h);
    Block b = bb.build();
    assertEquals(h, b.getPreviousBlockHash());

  }


  @Test
  public void setTimeEvents() {
    BlockBuilder bb = new BlockBuilder();
    bb.setTimeEvents(Set.of("a", "b", "c"));
    Block b = bb.build();
    MPWrappedArray array = b.getTimeEvents();
    assertEquals(3, array.size());
    assertEquals("b", array.asString(1));
  }


  @Test
  public void setTimeStamp() {
    BlockBuilder bb = new BlockBuilder();
    bb.setTimeStamp(1234567890L);
    Block b = bb.build();
    assertEquals(1234567890L, b.getTimeStamp());
  }


  @Test
  public void setTxList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();

    BlockBuilder bb = new BlockBuilder();
    bb.setTxList(null);
    Block b = bb.build();
    Set<String> txis = b.getTxHashes();
    assertEquals(0, txis.size());

    bb.setTxList(List.of(memo));
    b = bb.build();
    txis = b.getTxHashes();
    assertEquals(Set.of(tx.getHash()), txis);
  }


  @Test
  public void setTxiList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();

    BlockBuilder bb = new BlockBuilder();
    bb.setTxiList(null);
    Block b = bb.build();
    Set<String> txis = b.getTxHashes();
    assertEquals(0, txis.size());

    bb.setTxiList(List.of(tx));
    b = bb.build();
    txis = b.getTxHashes();
    assertEquals(Set.of(tx.getHash()), txis);
  }


  @Test
  public void testWithBaseStateHash() {
    BlockBuilder bb = new BlockBuilder();
    Hash h = Hash.fromHex("0123456789");
    bb.withBaseStateHash(h);
    Block b = bb.build();
    assertEquals(h, b.getBaseStateHash());
  }


  @Test
  public void testWithTxList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();
    memo.setNonce(500);
    MemoTx t2 = memo.create();

    BlockBuilder bb = new BlockBuilder();
    Block b = bb.withTxList().add(tx).add(memo).finish().build();
    Set<String> txis = b.getTxHashes();
    assertEquals(Set.of(tx.getHash(), t2.getHash()), txis);
  }


  @Test
  public void withChainId() {
    BlockBuilder bb = new BlockBuilder().withChainId(56);
    Block b = bb.build();
    assertEquals(56, b.getChainId());
  }


  @Test
  public void withContractEvents() {
    BlockBuilder bb = new BlockBuilder();
    bb.withContractEvents(List.of(
        new EventData("a", "b", "c", "d"),
        new EventData("v", "w", "x", 123)
    ));
    assertEquals(2, bb.getContractEvents().size());
  }


  @Test
  public void withCrossChainTxs() {
    Set<XCTxId> txIds = Set.of(
        new XCTxId(12, Hash.fromHex("01")),
        new XCTxId(12, Hash.fromHex("23")),
        new XCTxId(12, Hash.fromHex("45"))
    );
    BlockBuilder bb = new BlockBuilder();
    bb.withCrossChainTxs(txIds);
    Block b = bb.build();
    Set<XCTxId> actual = BlockBuilder.decodeXChainTxs(b);
    assertEquals(txIds, actual);
  }


  @Test
  public void withEffectiveTxList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();

    BlockBuilder bb = new BlockBuilder();
    bb.withEffectiveTxList(null);
    Block b = bb.build();
    Txi[] txis = b.getEffectiveTransactions();
    assertEquals(0, txis.length);

    bb.withEffectiveTxList(List.of(memo));
    b = bb.build();
    txis = b.getEffectiveTransactions();
    assertEquals(1, txis.length);
    assertEquals(tx.getHash(), txis[0].getHash());
  }


  @Test
  public void withEffectiveTxList2() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();
    memo.setNonce(500);
    MemoTx tx2 = memo.create();

    BlockBuilder bb = new BlockBuilder();
    Block b = bb.withEffectiveTxList().finish().build();
    Txi[] txis = b.getEffectiveTransactions();
    assertEquals(0, txis.length);

    b = bb.withEffectiveTxList().add(tx).add(memo).finish().build();
    txis = b.getEffectiveTransactions();
    assertEquals(2, txis.length);
    assertEquals(tx.getHash(), txis[0].getHash());
    assertEquals(tx2.getHash(), txis[1].getHash());
  }


  @Test
  public void withEffectiveTxiList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();

    BlockBuilder bb = new BlockBuilder();
    bb.withEffectiveTxiList(null);
    Block b = bb.build();
    Txi[] txis = b.getEffectiveTransactions();
    assertEquals(0, txis.length);

    bb.withEffectiveTxiList(List.of(tx));
    b = bb.build();
    txis = b.getEffectiveTransactions();
    assertEquals(1, txis.length);
    assertEquals(tx.getHash(), txis[0].getHash());
  }


  @Test
  public void withHeight() {
    BlockBuilder bb = new BlockBuilder().withHeight(1234);
    Block b = bb.build();
    assertEquals(1234, b.getHeight());
  }


  @Test
  public void withNodeName() {
    BlockBuilder bb = new BlockBuilder().withNodeName("wobble");
    Block b = bb.build();
    assertEquals("wobble", b.getNodeName());
  }


  @Test
  public void withPreviousBlockHash() {
    BlockBuilder bb = new BlockBuilder();
    Hash hash = Hash.fromHex("ffff");
    Block b = bb.withPreviousBlockHash(hash).build();
    assertEquals(hash, b.getPreviousBlockHash());
  }


  @Test
  public void withTimeEvent() {
    BlockBuilder bb = new BlockBuilder();
    Block b = bb.withTimeEvents(Set.of("a", "b", "c")).build();
    MPWrappedArray array = b.getTimeEvents();
    assertEquals(3, array.size());
    assertEquals("b", array.asString(1));
  }


  @Test
  public void withTimeStamp() {
    BlockBuilder bb = new BlockBuilder().withTimeStamp(4321567890L);
    Block b = bb.build();
    assertEquals(4321567890L, b.getTimeStamp());
  }

  @Test
  public void withTxList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    String hash = memo.create().getHash();

    BlockBuilder bb = new BlockBuilder();
    Block b = bb.withTxList(List.of(memo)).build();
    Set<String> txis = b.getTxHashes();
    assertEquals(Set.of(hash), txis);
  }

  @Test
  public void withTxiList() {
    Memo memo = new Memo();
    memo.setAddress("address");
    memo.setMetadata("My memo");
    memo.setPublicKey("my-key");
    memo.setNonce(400);
    MemoTx tx = memo.create();
    memo.setNonce(500);
    MemoTx t2 = memo.create();

    BlockBuilder bb = new BlockBuilder();
    Block b = bb.withTxiList(List.of(tx,t2)).build();
    Set<String> txis = b.getTxHashes();
    assertEquals(Set.of(tx.getHash(), t2.getHash()), txis);
  }
}