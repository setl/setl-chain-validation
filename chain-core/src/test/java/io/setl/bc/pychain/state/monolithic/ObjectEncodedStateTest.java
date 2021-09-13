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
package io.setl.bc.pychain.state.monolithic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import java.util.TreeMap;
import org.junit.Test;

/**
 * Created by wojciechcichon on 19/06/2017.
 */
public class ObjectEncodedStateTest {

  private static final String BLOCK_HASH = "b0b1b2b3b4";

  private static final int CHAIN_ID = 12;

  private static final MPWrappedMap<String, Object> CONFIG = new MPWrappedMap<>(new Object[]{});

  private static final Object[] EMPTY_ARR = new Object[0];

  private static final MPWrappedArray ASSETBALANCE_LIST = new MPWrappedArrayImpl(
      new Object[]{new Object[]{new Object[]{1, "", new Object[]{new MPWrappedMap(EMPTY_ARR), null}}}});

  private static final MPWrappedArray CONTRACTS_LIST = new MPWrappedArrayImpl(
      new Object[]{new Object[]{new Object[]{1, "", new Object[]{new MPWrappedMap(EMPTY_ARR), null}}}});

  private static final MPWrappedArray CONTRACT_TIMEEVENTS = new MPWrappedArrayImpl(EMPTY_ARR);

  private static final int HEIGHT = 12;

  private static final String LOADED_HASH = "1011121314";

  private static final MPWrappedArray NAMESPACE_LIST = new MPWrappedArrayImpl(
      new Object[]{new Object[]{new Object[]{1, "", new Object[]{new Object[]{1, "", new MPWrappedMap(EMPTY_ARR)}}}}});

  private static final MPWrappedArray SIGNODELIST = new MPWrappedArrayImpl(
      new Object[]{new Object[]{new Object[]{1, "", new Object[]{new Object[]{0d, "", 5L, 1, 4, 3, new Object[]{EMPTY_ARR}, 2d, 3f}}, 'a', 3L, 4f, 5d}}});

  private static final long TIMESTAMP = 123456L;

  private static final int TX_COUNT = 0;

  private static final int VERSION = 2;

  private static final int XCHAIN_LENGTH = 0;


  @Test
  public void constructor2() throws Exception {

    ObjectEncodedState state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(
        new Object[]{
            CHAIN_ID, VERSION, HEIGHT, LOADED_HASH, BLOCK_HASH, TIMESTAMP, CONFIG, SIGNODELIST, NAMESPACE_LIST, ASSETBALANCE_LIST, CONTRACTS_LIST,
            CONTRACT_TIMEEVENTS, TX_COUNT, XCHAIN_LENGTH
        }));

    // Copy constructor
    ObjectEncodedState state = new ObjectEncodedState(state1);

    teststate(state);

  }


  @Test
  public void decodeV2() throws Exception {
    ObjectEncodedState state = ObjectEncodedState.decode(new MPWrappedArrayImpl(
        new Object[]{
            CHAIN_ID, VERSION, HEIGHT, LOADED_HASH, BLOCK_HASH, TIMESTAMP, CONFIG, SIGNODELIST, NAMESPACE_LIST, ASSETBALANCE_LIST, CONTRACTS_LIST,
            CONTRACT_TIMEEVENTS, TX_COUNT, XCHAIN_LENGTH
        }));

    teststate(state);

  }


  void teststate(ObjectEncodedState state) {

    // Same tests
    assertEquals(CHAIN_ID, state.getChainId());
    assertEquals(VERSION, state.getVersion());
    assertEquals(HEIGHT, state.getHeight());
    assertEquals(LOADED_HASH, state.getLoadedHash().toHexString());
    assertEquals(BLOCK_HASH, state.getBlockHash().toHexString());
    assertEquals(TIMESTAMP, state.getTimestamp());

    // Check Config Maps are the same by toString();
    assertEquals(CONFIG.toString(), state.getEncodedConfig().toString());

    // Check Maps are the same by converting to Treemaps and using the 'equals' method.
    TreeMap<String, Object> configMap = new TreeMap<>();
    TreeMap<String, Object> checkedMap = new TreeMap<>();
    // look ma, no lambdas!
    CONFIG.iterate(configMap::put);
    state.getEncodedConfig().iterate(checkedMap::put);
    assertTrue(configMap.equals(checkedMap));

    assertEquals(new SignNodeList(SIGNODELIST, 2).computeRootHash(), state.getSignNodes().computeRootHash());
    assertEquals(new NamespaceList(NAMESPACE_LIST, 2).computeRootHash(), state.getNamespaces().computeRootHash());
    assertEquals(new AssetBalanceList(ASSETBALANCE_LIST, 2).computeRootHash(), state.getAssetBalances().computeRootHash());

    assertEquals(new ContractsList(CONTRACTS_LIST, 2).computeRootHash(), state.getContracts().computeRootHash());

    assertTrue(state.getContractTimeEvents().getEventsBefore(Long.MAX_VALUE - 1, Integer.MAX_VALUE).isEmpty());

  }

}
