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

import static org.junit.Assert.*;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author Simon Greatrix on 09/01/2020.
 */
public class MemoryHashStoreTest {
  Hash h0;
  Hash h1;
  Hash h2 = Hash.fromHex("2222");
  Hash h3 = Hash.fromHex("3333");

  MemoryHashStore store = new MemoryHashStore();

  @Mock
  AbstractState state;

  @Before
  public void before() {
    h0 = store.insert(h2.get());
    h1 = store.insert(h3.get());
  }
  @Test
  public void flush() {
    // method does nothing, so what to test?
    store.flush(state);
    assertNull(store.get(h3));
  }


  @Test
  public void get() {
    assertNull(store.get(h3));
    assertArrayEquals(h2.get(),store.get(h0));
  }



  @Test
  public void shutdown() throws DBStoreException {
    // method does nothing, so what to test?
    store.shutdown();
    assertNull(store.get(h3));
  }
}