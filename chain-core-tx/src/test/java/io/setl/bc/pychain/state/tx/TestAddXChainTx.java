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
import org.junit.Test;

public class TestAddXChainTx {
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {
    
    final String fromPubKey   = "fromPubKey";
    final String fromAddress  = "fromAddress";
    final String nameSpace    = "namespace";
    final String classId      = "classId";
    final String toAddress    = "toAddress";
    final String signature    = "signature";
    final String protocol     = "protocol";
    final String meta         = "meta";
    final String poa          = "poa";
    final String proto        = "proto";
    String hash               = "HASH";
    
    AddXChainTx tx = new AddXChainTx(
        1,            //  chainId
        hash,
        3,            //  nonce
        true,
        fromPubKey,
        fromAddress,
        4,
        7,
        8,
        Arrays.<Object[]>asList(new Object[]{"sigPub", 42}),
        signature,
        5,            //  height
        poa,
        6);           //  timestamp
  
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("279ce3cc89794b848b5f1a78cb127420c46d985254fa6da0241192124f7dfe20"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.ADD_X_CHAIN));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.ADD_X_CHAIN));
    assertTrue(!tx.getTxType().equals(TxType.UPDATE_ADDRESS_PERMISSIONS));

    Object[] encoded = tx.encodeTx();
  
    AddXChainTx tx2 = AddXChainTx.decodeTX( new MPWrappedArrayImpl( encoded));
    AddXChainTx tx3 = new AddXChainTx(tx);
    AddXChainTx tx4 = (AddXChainTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(AddXChainTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (AddXChainTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.ADD_X_CHAIN.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 4, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("newChainID does not match", thisTx.getNewChainId(), 4);
      assertEquals("blockHeight does not match", thisTx.getNewBlockHeight(), 7);
      assertEquals("chainParams does not match", (long)thisTx.getNewChainParams(), 8L);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("height does not match", 5, thisTx.getHeight());
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
      
    }
    
  }
  
}
