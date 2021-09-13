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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeSet;
import org.junit.Test;

/**
 * Most tests done by testing of higher level code that relies upon correct functioning of the Lane class. This covers the rare special cases.
 *
 * @author Simon Greatrix on 2019-05-10.
 */
public class LaneTest {

  private static final long NONCE = 10L;


  @Test
  public void rehash() {
    StateManager stateManager = mock(StateManager.class);
    State state = mock(State.class);
    Merkle<AddressEntry> merkle = mock(Merkle.class);
    AddressEntry addressEntry = mock(AddressEntry.class);

    when(addressEntry.getNonce()).thenReturn(NONCE);
    when(merkle.find(any())).thenReturn(addressEntry);
    when(state.getAssetBalances()).thenReturn(merkle);
    when(stateManager.getState()).thenReturn(state);

    Random random = new Random(0x7e57ab1e);
    HashMap<String, Series> map = new HashMap<>();

    Lane lane = new Lane();

    TreeSet<String> old = new TreeSet<>();
    for (int i = 0; i < 51; i++) {
      String address = "A" + Integer.toString(random.nextInt(0x4000_0000), 32);
      Series s = lane.findOrCreate(stateManager, address);
      assertNotNull(s);
      assertEquals(address, s.getAddress());
      map.put(address, s);
      assertEquals(map.size(), lane.size);

      // create replays, good TXs and futures
      s.addTx(SparseArrayTest.makeTx(NONCE - 1 + i % 3));
      if ((i % 3) == 0) {
        old.add(address);
      }
    }

    for (Entry<String, Series> e : map.entrySet()) {
      Series s = lane.findOrCreate(stateManager, e.getKey());
      assertSame(e.getValue(), s);
    }

    // this removes all the replays
    lane.cleanUp();
    assertEquals(34, lane.size);

    for (Entry<String, Series> e : map.entrySet()) {
      Series s = lane.findOrCreate(stateManager, e.getKey());
      s.bulkRemove(NONCE);
    }
    lane.cleanUp();
    assertEquals(17, lane.size);


  }
}