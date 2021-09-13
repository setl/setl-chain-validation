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
 *  Created by emh on 2017.Jun.20.
 */
public class TestNullTx {

  
  /**
   *  Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {

    final String  fromPubKey  = "fromPubKey";
    final String  fromAddress = "fromAddress";
    final String  signature   = "signature";
    final String  poa         = "poa";
    String  hash        = "HASH";


    NullTx tx = new NullTx( 1,
                            2,              //  int1
                            hash,
                            3,
                            true,
                            fromPubKey,
                            fromAddress,
                            signature,
                            4,              //  NB. set by class to -1
                            poa,
                            5
                          );
  
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("ebe7636fa8d0d9474416adddcea1ef11950cfbe1aede2bbd3c2298e57d3799f2"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.DO_NOTHING));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.DO_NOTHING));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();

    NullTx tx2 = NullTx.decodeTX( new MPWrappedArrayImpl( encoded));
    NullTx tx3 = new NullTx(tx);
    NullTx tx4 = (NullTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(NullTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (NullTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.DO_NOTHING.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
  
      //  int1 is not tested as it is not transmitted.
  
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
  
      //  height is not passed on encode/decode, so no test - it will fail
      //
      //assertEquals( "height does not match",        thisTx.getHeight(),          4);
  
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", thisTx.getTimestamp(), 5);
    }
  }

}
