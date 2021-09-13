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

public class PoaAddTxTest {
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {
    final String fromPubKey = "fromPubKey";
    final String fromAddress = "fromAddress";
    final String reference = "toKey2";
    final String returnAddress = "returnAddress";
    final String signature = "signature";
    final long startDate = 0L;
    final long endDate = 0L;
    final Object[] itemsData = new Object[] {new Object[] {TxType.TRANSFER_ASSET.getId(), 1000L, new String[] {"Asset1", "Asset2"}}};
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";
    String hash = "HASH";
  
    PoaAddTx tx = new PoaAddTx(
        1,
        hash,
        3,
        true,
        fromPubKey,
        fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, signature, 5, poa, 6);
    
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertEquals("294c8666f4ff6453350a105fd33abc0e20535d9cc32d9b68aa6d74e73bd3f5cd",hash);
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.GRANT_POA));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.GRANT_POA));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();
    PoaAddTx tx2 = PoaAddTx.decodeTX(encoded);
    PoaAddTx tx3 = new PoaAddTx(tx);
    PoaAddTx tx4 = (PoaAddTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(PoaAddTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (PoaAddTx thisTx : Arrays.asList(new PoaAddTx[] {tx, tx2, tx3, tx4})) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.GRANT_POA.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("reference does not match", thisTx.getReference(), reference);
      assertEquals("attorneyAddress does not match", thisTx.getAttorneyAddress(), returnAddress);
      assertEquals("startDate does not match", thisTx.getStartDate(), startDate);
      assertEquals("endDate does not match", thisTx.getEndDate(), endDate);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("protocol does not match", thisTx.getProtocol(), protocol);
      assertEquals("metadata does not match", thisTx.getMetadata(), metadata);
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
      assertTrue(tx.getPoaItems().equals(thisTx.getPoaItems()));
    }
  }
  
}