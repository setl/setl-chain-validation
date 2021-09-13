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

import static io.setl.bc.pychain.node.txpool.SparseArrayTest.makeTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.tx.Txi;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-05-09.
 */
public class ConfusedNonceTxTest {

  ConfusedNonceTx tx = (ConfusedNonceTx) ConfusedNonceTx.merge(makeTx(6), makeTx(6));


  @Test(expected = UnsupportedOperationException.class)
  public void addresses() {
    tx.addresses();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void buildHash() {
    tx.buildHash(null);
  }


  @Test(expected = UnsupportedOperationException.class)
  public void encodeTx() {
    tx.encodeTx();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getFromAddress() {
    tx.getFromAddress();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getFromPublicKey() {
    tx.getFromPublicKey();
  }


  @Test
  public void getHash() {
    assertEquals(ConfusedNonceTx.CONFUSED_HASH, tx.getHash());
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getHeight() {
    tx.getHeight();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getMetadata() {
    tx.getMetadata();
  }


  @Test
  public void getNonce() {
    assertEquals(6, tx.getNonce());
  }


  @Test
  public void getNonceAddress() {
    assertEquals("myAddress", tx.getNonceAddress());
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getPriority() {
    tx.getPriority();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getSignature() {
    tx.getSignature();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getTimestamp() {
    tx.getTimestamp();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getToChainId() {
    tx.getToChainId();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void getTxType() {
    tx.getTxType();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void isGood() {
    tx.isGood();
  }


  @Test
  public void merge() {
    Txi t1 = makeTx(5);
    Txi t2 = makeTx(5);
    Txi t3 = makeTx(5);

    Txi c = ConfusedNonceTx.merge(t1, t1);
    assertSame(c, t1);

    c = ConfusedNonceTx.merge(t1, t2);
    assertTrue(c instanceof ConfusedNonceTx);
    ConfusedNonceTx c2 = (ConfusedNonceTx) c;
    assertSame(t1, c2.getPrimaryTx());
    assertSame(t1, c2.getTx(Hash.fromHex(t1.getHash())));
    assertSame(t2, c2.getTx(Hash.fromHex(t2.getHash())));
    assertNull(c2.getTx(Hash.fromHex(t3.getHash())));
  }


  @Test(expected = UnsupportedOperationException.class)
  public void setGood() {
    tx.setGood(true);
  }


  @Test(expected = UnsupportedOperationException.class)
  public void setHeight() {
    tx.setHeight(4);
  }


  @Test(expected = UnsupportedOperationException.class)
  public void setSignature() {
    tx.setSignature("sig");
  }


  @Test(expected = UnsupportedOperationException.class)
  public void setUpdated() {
    tx.setUpdated(true);
  }

  @Test
  public void toString1() {
    assertNotNull(tx.toString());
  }
}