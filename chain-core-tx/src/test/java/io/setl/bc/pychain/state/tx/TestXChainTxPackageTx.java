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
package io.setl.bc.pychain.state.tx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.common.Hex;
import io.setl.common.Pair;
import io.setl.crypto.SHA256;
import java.util.Arrays;
import org.junit.Test;

public class TestXChainTxPackageTx {

  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {

    final int toChainID = 20;
    final boolean updated = true;
    final String blockVersion = "ABC";
    final int blockHeight = 99999;
    final int fromChainID = 42;
    final String baseHash = "now is the time for all good men to come to the aid of their country.";
    final Long blockTimestamp = 123456789L;
    final int height = 98765432;
    final Long timestamp = 546372819L;
    String hash = "HASH";

    XChainTxPackageTx tx = new XChainTxPackageTx(
        toChainID,
        hash,
        updated,
        blockVersion,
        blockHeight,
        fromChainID,
        baseHash,
        new Object[]{},
        new Pair[]{},
        new Object[]{},
        new Object[]{},
        blockTimestamp,
        height,
        timestamp
    );

    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash, hash.equals("db11ff8e3378a451e60c0fb7f547585ef4c68e82e078c23883c9adf3b426aff6"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.X_CHAIN_TX_PACKAGE));
    assertTrue(((AbstractTx) tx).getTxType().equals(TxType.X_CHAIN_TX_PACKAGE));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();

    XChainTxPackageTx tx2 = XChainTxPackageTx.decodeTX(new MPWrappedArrayImpl(encoded));
    XChainTxPackageTx tx3 = new XChainTxPackageTx(tx);
    XChainTxPackageTx tx4 = (XChainTxPackageTx) TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(XChainTxPackageTx.decodeTX(new MPWrappedArrayImpl(encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (XChainTxPackageTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.X_CHAIN_TX_PACKAGE.getPriority());

      assertEquals("chainId does not match", toChainID, thisTx.getChainId());
      assertEquals("tochainId does not match", fromChainID, thisTx.getFromChainID());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", thisTx.getNonce(), blockHeight);
      assertEquals("version does not match", thisTx.getVersion(), blockVersion);
      assertEquals("fromChainID does not match", thisTx.getFromChainID(), fromChainID);
      assertEquals("basehash does not match", thisTx.getBasehash(), baseHash);
      assertEquals("blockTimestamp does not match", thisTx.getBlockTimestamp(), blockTimestamp);
      assertEquals("updated does not match", thisTx.isGood(), updated);
      assertEquals("timestamp does not match", (long) thisTx.getTimestamp(), (long) timestamp);
    }
  }

}
