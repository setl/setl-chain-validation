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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_MANAGER_CONTROL;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.SHA256;
import java.util.Arrays;
import io.setl.common.Hex;
import org.junit.Test;

public class AddressPermissionsTxTest {
  
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
    final String toAddress    = "toAddress";
    final String signature    = "signature";
    final long   permissions  = AP_MANAGER_CONTROL;
    final String meta         = "meta";
    final String poa          = "poa";
    String hash         = "HASH";
    
    
    AddressPermissionsTx tx = new AddressPermissionsTx( 1,            //  chainId
        2,            //  int1
        hash,
        3,            //  nonce
        true,
        fromPubKey,
        fromAddress,
        toAddress,
        permissions,
        null,
        meta,
        signature,
        5,            //  height
        poa,
        6);           //  timestamp
    
    assertTrue(tx.getTxType().equals(TxType.UPDATE_ADDRESS_PERMISSIONS));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.UPDATE_ADDRESS_PERMISSIONS));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("222ffa248f81fef75606ec546c15d64fa61fab97bbd7ae86c568d4f0ce1e662c"));
    tx.setHash(hash);
    
    Object[] encoded = tx.encodeTx();
    
    AddressPermissionsTx tx2 = AddressPermissionsTx.decodeTX( new MPWrappedArrayImpl( encoded));
    AddressPermissionsTx tx3 = new AddressPermissionsTx(tx);
    AddressPermissionsTx tx4 = (AddressPermissionsTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(AddressPermissionsTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (AddressPermissionsTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.UPDATE_ADDRESS_PERMISSIONS.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("toAddress does not match", thisTx.getToAddress(), toAddress);
      assertEquals("permissions does not match", thisTx.getAddressPermissions(), permissions);
      assertEquals("metadata does not match", thisTx.getMetadata(), meta);
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("height does not match", 5, thisTx.getHeight());
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
    }
  }
  
}