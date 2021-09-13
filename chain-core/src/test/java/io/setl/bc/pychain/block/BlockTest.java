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
package io.setl.bc.pychain.block;

import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.tx.MemoTx;
import io.setl.common.Pair;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wojciechcichon on 15/06/2017.
 */
public class BlockTest {


  private static final int CHAIN_ID = 20;

  private static final Object[] CONTRACT_EVENTS = new Object[]{};

  private static final Object[] EFFECTIVE_TX_LIST = new Object[]{};

  private static final Object FUTURE_USE = new Object[]{};

  private static final int HEIGHT = 6;

  private static final MemoTx MEMOTX = new MemoTx(CHAIN_ID, 1, 1, false, "fromPubKey", "fromAddress", null, "", 0);

  private static final String NODENAME = "local node";

  private static final Hash PREV_BLOCK_HASH = Hash.fromHex("45a4");

  private static final Object[] SIGN_NODES = new Object[]{};

  private static final Object[] SIG_LIST = new Object[]{};

  private static final Object[] SIG_LIST_XCHAIN = new Object[]{};

  private static final Hash STATE_HASH = Hash.fromHex("4a54");

  private static final long TIMESTAMP = 1L;

  private static final Object[] TIME_EVENTS = new Object[]{};

  private static final Object[] TX_LIST = new Object[]{MEMOTX.encodeTx()};

  private static final Object[] TX_LIST_WRAPPED = new Object[]{new MPWrappedArrayImpl(MEMOTX.encodeTx())};

  private static final int VERSION_2 = 2;

  private static final List<Pair<String, Integer>> XCHAIN_TXS = Collections.emptyList();

  private static final Object[] XC_HEIGHTS = new Object[]{};


  @Test
  public void blockVersion2Test() throws IOException {
    Block block = new Block(new MPWrappedArrayImpl(
        new Object[]{CHAIN_ID, VERSION_2, HEIGHT, STATE_HASH, PREV_BLOCK_HASH,
            new MPWrappedArrayImpl(TX_LIST),
            new MPWrappedArrayImpl(SIG_LIST), SIG_LIST_XCHAIN, TIMESTAMP, NODENAME, XCHAIN_TXS.toArray(),
            SIGN_NODES, XC_HEIGHTS, TIME_EVENTS, CONTRACT_EVENTS
        }));
    Assert.assertEquals(STATE_HASH, block.getBaseStateHash());
    Assert.assertEquals(CHAIN_ID, block.getChainId());
    //
    Assert.assertArrayEquals(CONTRACT_EVENTS,
        ((MPWrappedArrayImpl) block.getContractEvents()).unwrap());
    Assert.assertEquals(HEIGHT, block.getHeight());
    Assert.assertEquals(NODENAME, block.getNodeName());
    Assert.assertEquals(PREV_BLOCK_HASH, block.getPreviousBlockHash());
    Assert.assertArrayEquals(SIG_LIST, ((MPWrappedArrayImpl) block.getSigList()).unwrap());
    Assert.assertArrayEquals(SIG_LIST_XCHAIN,
        ((MPWrappedArrayImpl) block.getSigListXChain()).unwrap());
    Assert.assertArrayEquals(SIGN_NODES, ((MPWrappedArrayImpl) block.getSigNodes()).unwrap());
    Assert.assertArrayEquals(XC_HEIGHTS, ((MPWrappedArrayImpl) block.getXChainHeights()).unwrap());
    Assert.assertEquals(XCHAIN_TXS, block.getXChainTxs());
    Assert.assertEquals(VERSION_2, block.getVersion());
    Assert.assertEquals(TX_LIST_WRAPPED, block.getTxList().toArray());
    Assert.assertEquals(TIMESTAMP, block.getTimeStamp());
    Assert.assertArrayEquals(TIME_EVENTS, ((MPWrappedArrayImpl) block.getTimeEvents()).unwrap());
    Assert.assertEquals(1, block.getTransactions().length);
    Assert.assertArrayEquals(MEMOTX.encodeTx(), ((MemoTx) block.getTransactions()[0]).encodeTx());
  }


  @Test
  public void blockVersion4Test() throws IOException {
    Block block = new Block(CHAIN_ID, HEIGHT, STATE_HASH, PREV_BLOCK_HASH,
        TX_LIST, SIG_LIST, SIG_LIST_XCHAIN, TIMESTAMP, NODENAME,
        new Pair[0], SIGN_NODES, XC_HEIGHTS, TIME_EVENTS, CONTRACT_EVENTS, EFFECTIVE_TX_LIST);
    Assert.assertEquals(STATE_HASH, block.getBaseStateHash());
    Assert.assertEquals(CHAIN_ID, block.getChainId());
    Assert.assertArrayEquals(CONTRACT_EVENTS,
        ((MPWrappedArrayImpl) block.getContractEvents()).unwrap());
    Assert.assertEquals(HEIGHT, block.getHeight());
    Assert.assertEquals(NODENAME, block.getNodeName());
    Assert.assertEquals(PREV_BLOCK_HASH, block.getPreviousBlockHash());
    Assert.assertArrayEquals(SIG_LIST, ((MPWrappedArrayImpl) block.getSigList()).unwrap());
    Assert.assertArrayEquals(SIG_LIST_XCHAIN,
        ((MPWrappedArrayImpl) block.getSigListXChain()).unwrap());
    Assert.assertArrayEquals(SIGN_NODES, ((MPWrappedArrayImpl) block.getSigNodes()).unwrap());
    Assert.assertArrayEquals(XC_HEIGHTS, ((MPWrappedArrayImpl) block.getXChainHeights()).unwrap());
    Assert.assertEquals(XCHAIN_TXS, block.getXChainTxs());
    Assert.assertEquals(4, block.getVersion());
    Assert.assertEquals(TX_LIST_WRAPPED, block.getTxList().toArray());
    Assert.assertEquals(TIMESTAMP, block.getTimeStamp());
    Assert.assertArrayEquals(TIME_EVENTS, ((MPWrappedArrayImpl) block.getTimeEvents()).unwrap());
    Assert.assertEquals(1, block.getTransactions().length);
    Assert.assertArrayEquals(MEMOTX.encodeTx(), ((MemoTx) block.getTransactions()[0]).encodeTx());
  }


  @Test
  public void getTransactionsTest() throws IOException {
    Block block = new Block(CHAIN_ID, HEIGHT, STATE_HASH, PREV_BLOCK_HASH,
        TX_LIST, SIG_LIST, SIG_LIST_XCHAIN, TIMESTAMP, NODENAME,
        new Pair[0], SIGN_NODES, XC_HEIGHTS, TIME_EVENTS, CONTRACT_EVENTS, EFFECTIVE_TX_LIST);
    Object[] encoded = block.encode();

    assertEquals(17, encoded.length);

    Assert.assertEquals(CHAIN_ID, encoded[0]);
    Assert.assertEquals(4, encoded[1]);
    Assert.assertEquals(HEIGHT, encoded[2]);
    Assert.assertEquals(STATE_HASH, Hash.fromHex((String) encoded[3]));
    Assert.assertEquals(PREV_BLOCK_HASH, Hash.fromHex((String) encoded[4]));
    Object[] transactions = ((List<?>) encoded[5]).toArray();
    Assert.assertEquals(TX_LIST.length, transactions.length);
    Assert.assertEquals(TX_LIST_WRAPPED, transactions);
    Assert.assertArrayEquals(SIG_LIST, ((MPWrappedArrayImpl) encoded[6]).unwrap());
    Assert.assertArrayEquals(SIG_LIST_XCHAIN, ((MPWrappedArrayImpl) encoded[7]).unwrap());
    Assert.assertEquals(TIMESTAMP, encoded[8]);
    Assert.assertEquals(NODENAME, encoded[9]);
    Assert.assertEquals(0, ((Object[]) encoded[10]).length);
    Assert.assertArrayEquals(SIGN_NODES, ((MPWrappedArrayImpl) encoded[11]).unwrap());
    Assert.assertArrayEquals(XC_HEIGHTS, ((MPWrappedArrayImpl) encoded[12]).unwrap());
    Assert.assertArrayEquals(TIME_EVENTS, ((MPWrappedArrayImpl) encoded[13]).unwrap());
    Assert.assertArrayEquals(CONTRACT_EVENTS, ((MPWrappedArrayImpl) encoded[14]).unwrap());
    Assert.assertArrayEquals(EFFECTIVE_TX_LIST, ((MPWrappedArrayImpl) encoded[15]).unwrap());


  }
}
