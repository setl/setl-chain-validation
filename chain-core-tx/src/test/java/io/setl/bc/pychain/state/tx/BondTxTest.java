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
import io.setl.crypto.SHA256;
import io.setl.common.Hex;
import org.junit.Test;

public class BondTxTest {
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {
    final String fromPubKey = "fromPubKey";
    final String fromAddress = "fromAddress";
    final String signodePubKey = "toKey2";
    final String returnAddress = "returnAddress";
    final String signature = "signature";
    final Long amount = 65748L;
    final String poa = "poa";
    String hash = "HASH";
    
    BondTx tx = new BondTx(1, 2, hash, 3, true, fromPubKey, fromAddress, signodePubKey, returnAddress, amount, signature, 5, poa, 6);

    assertTrue(tx.getTxType().equals(TxType.GRANT_VOTING_POWER));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("6ea3539753de37b178c2c439a967c91105ea779b2454608397367016edbe9f76"));
    tx.setHash(hash);
  
    Object[] encoded = tx.encodeTx();
    BondTx tx2 = BondTx.decodeTX(encoded);
    BondTx tx3 = new BondTx(tx);
    BondTx tx4 = (BondTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(BondTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (BondTx thisTx : new BondTx[] {tx, tx2, tx3, tx4}) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.GRANT_VOTING_POWER.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("getToSignodePubKey does not match", thisTx.getToSignodePubKey(), signodePubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("returnAddress does not match", thisTx.getReturnAddress(), returnAddress);
      assertEquals("amount does not match", thisTx.getAmount().longValue(), (long) amount);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
    
    }
  }
  
}