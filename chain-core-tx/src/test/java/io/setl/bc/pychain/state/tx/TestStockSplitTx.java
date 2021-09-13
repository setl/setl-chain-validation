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
 * Created by edmcnair-hayward on 2017.Jul.28.
 */
public class TestStockSplitTx {
  
  /**
   * Test that a serialise/deserialised object equates.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {
    
    final String fromPubKey = "fromPubKey";
    final String fromAddress = "fromAddress";
    final String nameSpace = "namespace";
    final String classId = "classId";
    final String strProperty = "strProperty";
    final String toAddress = "toAddress";
    final String signature = "signature";
    final String protocol = "protocol";
    final String meta = "meta";
    final String poa = "poa";
    String hash = "HASH";
  
    StockSplitTx tx = new StockSplitTx(1,              //  chainId
        2,              //  int1
        hash,
        3,              //  nonce
        true,           //  updated
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        strProperty,
        4.0,            //  ratio
        meta,
        signature,
        5,              //  height, ignored
        poa,
        6);             //  timestamp
  
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertEquals("6414082d5b568504dcdeba8cbadc44aeac65f02516f38a8cf3a07d53c75fd04a", hash);
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.SPLIT_STOCK));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.SPLIT_STOCK));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    //  encode/serialise
    //
    Object[] encoded = tx.encodeTx();
    
    //  decode/desirialise
    //
    StockSplitTx tx2 = StockSplitTx.decodeTX(encoded);
    StockSplitTx tx3 = new StockSplitTx(tx);
    StockSplitTx tx4 = (StockSplitTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(StockSplitTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (StockSplitTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {
  
      //  Evaluate equality
      //
      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.SPLIT_STOCK.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("namespace does not match", thisTx.getNameSpace(), nameSpace);
      assertEquals("classId does not match", thisTx.getClassId(), classId);
      assertEquals("toAddress does not match", thisTx.getReferenceStateHash(), strProperty);
  
      //  direct double comparisons deprecated, hence casts to object
      //
      //assertEquals( "amount does not match",        thisTx.getRatio(),          4);
      assertEquals("amount does not match", (Object) thisTx.getRatio(), (Object) 4.0);
  
      assertEquals("meta does not match", thisTx.getMetadata(), meta);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
  
      //  height is not passed in encode/decode process
      //
      //assertEquals( "height does not match",        thisTx.getHeight(),          5);
  
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
    }
  }
}
