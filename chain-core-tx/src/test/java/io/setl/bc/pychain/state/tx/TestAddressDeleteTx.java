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


/**
 * TestAddressDeleteTx.
 */
public class TestAddressDeleteTx {
  
  /**
   * Test that a serialise/deserialised pair.
   */
  
  //  this is here so that an error occurs and this code
  //  is re-evaluated when the  encode  method is implemented
  //
  @Test()
  
  public void testSerialiseDeserialise() throws Exception {
    
    final String fromPubKey   = "fromPubKey";
    final String fromAddress  = "fromAddress";
    final String signature    = "signature";
    final String protocol     = "protocol";
    final String meta         = "meta";
    final String poa          = "poa";
    String hash               = "HASH";
    
    
    AddressDeleteTx tx = new AddressDeleteTx( 1,            //  chainId
        2,            //  int1
        hash,
        3,            //  nonce
        true,
        fromPubKey,
        fromAddress,
        protocol,
        meta,
        signature,
        5,            //  height
        poa,
        6);           //  timestamp
    
    
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("a621a498d8515f118f1a96ec3c312136a4e24f12bd6ad526394f7b1c2c0c2df3"));
    tx.setHash(hash);
    
    assertTrue(tx.getTxType().equals(TxType.DELETE_ADDRESS));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.DELETE_ADDRESS));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));
    
    Object[] encoded = tx.encodeTx();
    
    AddressDeleteTx tx2 = AddressDeleteTx.decodeTX( new MPWrappedArrayImpl( encoded));
    AddressDeleteTx tx3 = new AddressDeleteTx(tx);
    AddressDeleteTx tx4 = (AddressDeleteTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(AddressDeleteTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (AddressDeleteTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.DELETE_ADDRESS.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("protocol does not match", thisTx.getProtocol(), protocol);
      assertEquals("meta does not match", thisTx.getMetadata(), meta);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("height does not match", 5, thisTx.getHeight());
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
    }
  }
  
}
