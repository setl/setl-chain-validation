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

public class PoaUnEncumberTxTest {
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {
    
    final String fromPubKey = "fromPubKey";
    final String fromAddress = "fromAddress";
    final String toAddress = "toAddress";
    final String nameSpace = "namespace";
    final String classId = "classId";
    final String signature = "signature";
    final String meta = "meta";
    final String poa = "poa";
    final String proto = "proto";
    final String reference = "reference";
    String hash = "HASH";
    
    PoaUnEncumberTx tx = new PoaUnEncumberTx(1, hash, 3, true, fromPubKey, fromAddress,toAddress,
        reference, "", reference, nameSpace, classId, 42, proto, meta, signature, -1, poa, 6);
    
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("3802599612512a65a1f1dcd98fae29ec2dd8179692385abe9243352c8656edb6"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.POA_UNENCUMBER_ASSET));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.POA_UNENCUMBER_ASSET));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();
    
    PoaUnEncumberTx tx2 = PoaUnEncumberTx.decodeTX(encoded);
    PoaUnEncumberTx tx3 = new PoaUnEncumberTx(tx);
    PoaUnEncumberTx tx4 = (PoaUnEncumberTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(PoaUnEncumberTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (PoaUnEncumberTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.POA_UNENCUMBER_ASSET.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("reference does not match", thisTx.getReference(), reference);
      assertEquals("namespace does not match", thisTx.getNameSpace(), nameSpace);
      assertEquals("classId does not match", thisTx.getClassId(), classId);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
      assertEquals("protocol does not match", thisTx.getProtocol(), proto);
      assertEquals("meta does not match", thisTx.getMetadata(), meta);
    }
  }
}
