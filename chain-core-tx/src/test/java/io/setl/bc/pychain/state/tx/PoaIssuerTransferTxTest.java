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
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.SHA256;
import java.util.Arrays;
import io.setl.common.Hex;
import org.junit.Test;


/**
 * Created by emh on 2017.Jun.20.
 */
@SuppressWarnings("unlikely-arg-type")
public class PoaIssuerTransferTxTest {
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseDeserialise() throws Exception {
    
    final String fromPubKey = "fromPubKey";
    final String fromAddress = "fromAddress";
    final String poaAddress   = "thisPoaAddress";
    final String poaReference = "mary had a lamb";
    final String sourceAddress = "sourceAddress1";
    final String nameSpace = "namespace";
    final String classId = "classId";
    final String toAddress = "toAddress";
    final String signature = "signature";
    final String protocol = "protocol";
    final String meta = "meta";
    final String poa = "poa";
    String hash = "HASH";
    
    PoaIssuerTransferTx tx = new PoaIssuerTransferTx(1,            //  chainId
        hash,
        3,            //  nonce
        true,         //  updated
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        classId,
        sourceAddress,
        7,
        toAddress,
        4,            //  amount
        signature,
        protocol,
        meta,
        5,            //  height
        poa,
        6);           //  timestamp
    
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("ceb7952968d5565f27d3dd9c541c1fdc29dbae94b4bd3accffafb0fc10cbf3f5"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.POA_TRANSFER_ASSET_AS_ISSUER));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.POA_TRANSFER_ASSET_AS_ISSUER));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();
    
    PoaIssuerTransferTx tx2 = PoaIssuerTransferTx.decodeTX(encoded);
    PoaIssuerTransferTx tx3 = new PoaIssuerTransferTx(tx);
    PoaIssuerTransferTx tx4 = (PoaIssuerTransferTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(PoaIssuerTransferTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (PoaIssuerTransferTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.POA_TRANSFER_ASSET_AS_ISSUER.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("toChainId does not match", 7, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("poaAddress does not match", thisTx.getPoaAddress(), poaAddress);
      assertEquals("poaReference does not match", thisTx.getPoaReference(), poaReference);
      assertEquals("sourceAddress does not match", thisTx.getSourceAddress(), sourceAddress);
      assertEquals("namespace does not match", thisTx.getNameSpace(), nameSpace);
      assertEquals("classId does not match", thisTx.getClassId(), classId);
      assertEquals("toAddress does not match", thisTx.getToAddress(), toAddress);
      assertTrue("amount does not match", (new Balance(thisTx.getAmount())).equals(4));
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("protocol does not match", thisTx.getProtocol(), protocol);
      assertEquals("meta does not match", thisTx.getMetadata(), meta);
      assertEquals("height does not match", 5, thisTx.getHeight());
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
      
    }
  }
  
}
