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
package io.setl.bc.pychain.node.txpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.MissingTxIds;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.ProposedTxIds;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.block.ProposedTxList.Builder;
import io.setl.bc.pychain.block.TxIdList;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.bc.pychain.tx.create.NullTX;
import io.setl.util.PriorityExecutor;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Simon Greatrix on 2019-05-09.
 */
public class LargeTransactionPoolTest {

  static long timestamp = System.currentTimeMillis();


  static Txi makeTx(int addr, long nonce) {
    NullTX creator = new NullTX();
    creator.setAddress("A" + addr);
    creator.setNonce(nonce);
    creator.setTimestamp(timestamp++);
    return creator.create();
  }


  Txi[] transactions;

  LargeTransactionPool value;

  XChainTxPackageTx xChainTxPackageTx1 = new XChainTxPackageTx(
      101,
      "abcdef00",
      false,
      "version",
      42,
      102,
      "01234567",
      null,
      null,
      null,
      null,
      System.currentTimeMillis(),
      42,
      System.currentTimeMillis()
  );

  XChainTxPackageTx xChainTxPackageTx2 = new XChainTxPackageTx(
      101,
      "abcdef01",
      false,
      "version",
      43,
      103,
      "80234567",
      null,
      null,
      null,
      null,
      System.currentTimeMillis(),
      42,
      System.currentTimeMillis()
  );


  @Test
  public void addTx() {
    assertFalse(value.hasAvailableTransactions());
    value.addTx(xChainTxPackageTx1);
    assertEquals(1, value.getAvailableTransactionCount());
    assertTrue(value.hasAvailableTransactions());

    value.addTx(xChainTxPackageTx1);
    assertEquals(1, value.getAvailableTransactionCount());

    value.addTx(makeTx(1, 100));
    assertTrue(value.hasAvailableTransactions());
    assertEquals(2, value.getAvailableTransactionCount());

    // same nonce does nto count as new TX
    value.addTx(makeTx(1, 100));
    assertEquals(2, value.getAvailableTransactionCount());

    // nonce below address's nonce is ignored
    value.addTx(makeTx(1, 5));
    assertEquals(2, value.getAvailableTransactionCount());

    // different address works
    value.addTx(makeTx(2, 20));
    assertEquals(3, value.getAvailableTransactionCount());
  }


  private void bulkAdd() {

    transactions = new Txi[25];
    for (int i = 1; i <= 5; i++) {
      for (int j = 0; j < 5; j++) {
        Txi txi = makeTx(i, 10 + j);
        transactions[i * 5 + j - 5] = txi;
        value.addTx(txi);
      }
    }

    // add a future
    value.addTx(makeTx(1, 17));

    // add a replay
    value.addTx(makeTx(1, 9));
  }


  @Test
  public void bulkRemove() {
    bulkAdd();

    // 25 transactions and 1 future
    assertEquals(26, value.getAvailableTransactionCount());
    for (int i = 0; i < 5; i++) {
      value.addTx(makeTx(10, 100 + i));
    }

    // 25 + 1 + 5 = 31
    assertEquals(31, value.getAvailableTransactionCount());

    value.addTx(xChainTxPackageTx1);
    transactions[transactions.length - 1] = xChainTxPackageTx1;

    Block block = Mockito.mock(Block.class);
    when(block.getTransactions()).thenReturn(transactions);

    value.bulkRemove(block);

    // 25 + 1 + 5 + x-chain - 25 = 7
    assertEquals(7, value.getAvailableTransactionCount());

    for (int i = 0; i < transactions.length; i++) {
      Txi txi = transactions[i];
      assertNull("Tx " + i + " should be removed", value.getTx(txi.getNonceAddress(), txi.getNonce(), Hash.fromHex(txi.getHash())));
    }
  }


  @Test
  public void clear() {
    bulkAdd();
    assertTrue(value.hasAvailableTransactions());
    value.clear();
    assertEquals(0, value.getAvailableTransactionCount());
    assertFalse(value.hasAvailableTransactions());
  }


  @Test
  public void createProposal() {
    bulkAdd();
    value.addTx(xChainTxPackageTx1);
    value.addTx(xChainTxPackageTx2);

    ProposedTxList txList = value.createProposal(17);

    // 17 transactions plus two X-Chain
    assertEquals(19, txList.size());
  }


  @Test
  public void getAllTx() {
    ArrayList<Txi> expected = new ArrayList<>();
    for (int i = 100; i < 110; i++) {
      Txi tx = makeTx(10, i);
      expected.add(tx);
      value.addTx(tx);
    }

    List<Txi> actual = value.getAllTx("A10");
    assertEquals(expected, actual);

    assertTrue(value.getAllTx("ZZZ").isEmpty());
    assertTrue(value.getAllTx("").isEmpty());
  }


  @Test
  public void getTx() {
    assertNull(value.getTx("", 0, Hash.fromHex(xChainTxPackageTx1.getHash())));
    value.addTx(xChainTxPackageTx1);
    assertSame(xChainTxPackageTx1, value.getTx("", 0, Hash.fromHex(xChainTxPackageTx1.getHash())));

    Txi txi = makeTx(20, 200);
    assertNull(value.getTx(txi.getNonceAddress(), 200, Hash.fromHex(txi.getHash())));
    value.addTx(txi);
    assertSame(txi, value.getTx(txi.getNonceAddress(), 200, Hash.fromHex(txi.getHash())));
  }


  @Test
  public void matchTxs() {
    bulkAdd();
    Builder b = new Builder();
    for (Txi txi : transactions) {
      b.add(txi);
    }

    value.addTx(xChainTxPackageTx1);
    b.add(xChainTxPackageTx1);
    b.add(xChainTxPackageTx2);
    b.add(makeTx(100, 100));

    ProposedTxList proposedTxList = b.build();
    ProposedTxIds proposedTxIds = proposedTxList.asIdList();
    MissingTxIds unmatched = new MissingTxIds(UUID.randomUUID());
    ProposedTxList list2 = proposedTxIds.asTxList(value, unmatched);

    assertEquals(27,list2.getAllTx().length);
  }


  @Before
  public void setUp() {
    StateManager stateManager = Mockito.mock(StateManager.class);
    PriorityExecutor priorityExecutor = new PriorityExecutor(1);
    SecureRandom secureRandom = new SecureRandom();
    MeterRegistry meterRegistry = Mockito.mock(MeterRegistry.class);
    Counter counter = Mockito.mock(Counter.class);

    when(meterRegistry.counter("transactionPool_size")).thenReturn(counter);
    value = new LargeTransactionPool(stateManager, priorityExecutor, secureRandom, meterRegistry);

    State state = Mockito.mock(State.class);
    when(stateManager.getState()).thenReturn(state);
    Merkle<AddressEntry> merkle = mock(Merkle.class);
    when(state.getAssetBalances()).thenReturn(merkle);

    AddressEntry addressEntry = mock(AddressEntry.class);
    when(merkle.find(any())).thenReturn(addressEntry);
    when(addressEntry.getNonce()).thenReturn(10L);
  }
}