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
import java.util.Arrays;
import io.setl.common.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestUnBondTx {
  
  @Before
  public void setUp() throws Exception {
  
  }
  
  @After
  public void tearDown() throws Exception {
  
  }
  
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
    final String protocol = "proto 1";
    final String metadata = "meta X";
    final String poa = "poa";
    String hash = "HASH";
    
    UnBondTx tx = new UnBondTx(1, 2, hash, 3, true, fromPubKey, fromAddress, returnAddress, amount, signature, protocol, metadata, 5, poa, 6);
  
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("faa676e3ac5100d11829f511235cb5b55c9b653bf829a78ee38b6bbc4397bf42"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.REVOKE_VOTING_POWER));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.REVOKE_VOTING_POWER));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();
    UnBondTx tx2 = UnBondTx.decodeTX(encoded);
    UnBondTx tx3 = new UnBondTx(tx);
    UnBondTx tx4 = (UnBondTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(UnBondTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (UnBondTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.REVOKE_VOTING_POWER.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("toAddress does not match", thisTx.getToAddress(), returnAddress);
      assertEquals("amount does not match", (long) thisTx.getAmount(), (long) amount);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("metadata does not match", thisTx.getMetadata(), metadata);
      assertEquals("protocol does not match", thisTx.getProtocol(), protocol);
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
  
    }
    
  }
  
  
}