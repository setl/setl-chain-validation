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

import static io.setl.bc.pychain.node.txpool.LargeTransactionPoolTest.makeTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.TxIdList;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-05-13.
 */
public class SeriesTest {

  StateManager stateManager;


  @Test
  public void addTx1() {
    Txi txi = makeTx(20, 1099);
    Series series = new Series(stateManager, "A20");
    series.addTx(txi);
    assertFalse(series.isEmpty());

    assertSame(txi, series.getTx(txi.getNonce(), Hash.fromHex(txi.getHash())));
    assertNull(series.getTx(txi.getNonce() + 1, Hash.fromHex(txi.getHash())));

    TxIterator iterator = series.iterate(50);
    assertFalse(iterator.hasNext());

    txi = makeTx(20, 1000);
    series.addTx(txi);
    iterator = series.iterate(50);
    assertSame(txi, iterator.next());
    assertFalse(iterator.hasNext());
  }


  @Test
  public void addTx2() {
    Txi txi = makeTx(21, 1099);
    Series series = new Series(stateManager, "A21");
    series.addTx(txi);
    assertFalse(series.isEmpty());
  }


  @Before
  public void before() {
    stateManager = mock(StateManager.class);
    State state = mock(State.class);
    Merkle<AddressEntry> entries = mock(Merkle.class);
    AddressEntry entry = mock(AddressEntry.class);
    when(entry.getNonce()).thenReturn(1000L);
    when(entries.find(endsWith("0"))).thenReturn(entry);
    when(state.getAssetBalances()).thenReturn(entries);
    when(stateManager.getState()).thenReturn(state);
  }


  @Test
  public void bulkRemove() {
    Series series = new Series(stateManager, "A5");
    for (int i = 0; i < 5; i++) {
      series.addTx(makeTx(5, i));
    }
    assertFalse(series.isEmpty());
    series.bulkRemove(4);
    assertTrue(series.isEmpty());
  }


  @Test
  public void rejectReplay() {
    Txi txi = makeTx(20, 99);
    Series series = new Series(stateManager, "A20");
    series.addTx(txi);
    assertTrue(series.isEmpty());
  }


  @Test
  public void testAddresses() {
    Series series = new Series(stateManager, "myAddress");
    assertEquals("myAddress", series.getAddress());
    assertEquals("myAddress".hashCode(), series.getAddressCode());
  }


  @Test
  public void testGetAll() {
    Series series = new Series(stateManager, "A1");
    Txi[] txis = new Txi[5];
    for (int i = 0; i < 5; i++) {
      txis[i] = makeTx(1, i * 2);
      series.addTx(txis[i]);
    }

    // Confuse some of the TXs
    for (int i = 2; i < 5; i++) {
      series.addTx(makeTx(1, i * 2));
    }

    List<Txi> list = series.getAllTXs();
    assertEquals(5, list.size());
    for (int i = 0; i < 5; i++) {
      assertSame(txis[i], list.get(i));
    }
  }


  @Test
  public void testMatchTxs() {
    Series series = new Series(stateManager, "A1");

    ArrayList<Txi> inSeries = new ArrayList<>();
    ArrayList<Txi> inList = new ArrayList<>();
    ArrayList<Txi> expectedTxi = new ArrayList<>();
    HashSet<NonceAndHash> expectedUnmatched = new HashSet<>();

    // both sides have Txi 0
    Txi txi = makeTx(1, 0);
    inSeries.add(txi);
    inList.add(txi);
    expectedTxi.add(txi);

    // both sides have Txi 1
    txi = makeTx(1, 1);
    inSeries.add(txi);
    inList.add(txi);
    expectedTxi.add(txi);

    // sides have different versions of Txi 2
    txi = makeTx(1, 2);
    inSeries.add(txi);
    txi = makeTx(1, 2);
    inList.add(txi);

    expectedUnmatched.add(new NonceAndHash(2, Hash.fromHex(txi.getHash())));
    expectedTxi.add(null);

    // both sides have Txi 3, but series has an extra one
    txi = makeTx(1, 3);
    inSeries.add(txi);
    txi = makeTx(1, 3);
    inSeries.add(txi);
    inList.add(txi);
    expectedTxi.add(txi);

    // series has multiple Txi 4's, but list is different
    txi = makeTx(1, 4);
    inSeries.add(txi);
    txi = makeTx(1, 4);
    inSeries.add(txi);
    txi = makeTx(1, 4);
    inList.add(txi);
    expectedUnmatched.add(new NonceAndHash(4, Hash.fromHex(txi.getHash())));
    expectedTxi.add(null);

    // only list has txi 5
    txi = makeTx(1, 5);
    inList.add(txi);
    expectedUnmatched.add(new NonceAndHash(5, Hash.fromHex(txi.getHash())));
    expectedTxi.add(null);

    // both sides have Txi 6
    txi = makeTx(1, 6);
    inSeries.add(txi);
    inList.add(txi);
    expectedTxi.add(txi);

    // only list has txi 7
    txi = makeTx(1, 7);
    inList.add(txi);
    expectedUnmatched.add(new NonceAndHash(7, Hash.fromHex(txi.getHash())));
    expectedTxi.add(null);

    TxIdList txIds = new TxIdList(
        new Object[]{
            "A1",
            0L,
            inList.stream().map(t -> Hash.fromHex(t.getHash()).get()).toArray()
        }
    );

    for (Txi t : inSeries) {
      series.addTx(t);
    }

    ArrayList<Txi> transactions = new ArrayList<>();
    HashSet<NonceAndHash> unmatched = new HashSet<>();

    series.matchTxs(transactions, unmatched, txIds);

    assertEquals(expectedTxi, transactions);
    assertEquals(expectedUnmatched, unmatched);
  }
}