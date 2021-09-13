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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.SHA256;
import java.util.Arrays;
import io.setl.common.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPoaLockHoldingTx {

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
    final String poaAddress = "poaAddress";
    final String poaReference = "poaReference";
    final String nameSpace = "namespace";
    final String classId = "classId";
    final String subjectAddress = "subjectAddress";
    final String signature = "signature";
    final String meta = "meta";
    final String poa = "poa";
    final String proto = "proto";
    final String reference = "reference";
    String hash = "HASH";

    EncumbranceDetail ed1 = new EncumbranceDetail("Address1", 0L, 1000L);
    EncumbranceDetail ed2 = new EncumbranceDetail(ed1);
    assertTrue(ed1.equals(ed2));

    Object[] administrators = new Object[]{ (new EncumbranceDetail("Address2", 10L, 2000L)).encode() };

    PoaLockHoldingTx tx = new PoaLockHoldingTx(
        1,
        hash,
        3,
        true,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        subjectAddress,
        nameSpace,
        classId,
        42,
        proto, meta, signature, poa, 6);

    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("e0cdcefd5f04d41bb9c06edbd87e93d092422d7189c52345494b35a8372d16dd"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.POA_LOCK_ASSET_HOLDING));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.POA_LOCK_ASSET_HOLDING));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();

    PoaLockHoldingTx tx2 = PoaLockHoldingTx.decodeTX(encoded);
    PoaLockHoldingTx tx3 = new PoaLockHoldingTx(tx);
    PoaLockHoldingTx tx4 = (PoaLockHoldingTx)TxFromList.txFromList(new MPWrappedArrayImpl(encoded));

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(PoaLockHoldingTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (PoaLockHoldingTx thisTx : Arrays.asList(tx, tx2, tx3, tx4)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.POA_LOCK_ASSET_HOLDING.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("poaAddress does not match", thisTx.getPoaAddress(), poaAddress);
      assertEquals("poaReference does not match", thisTx.getPoaReference(), poaReference);
      assertEquals("subjectAddress does not match", thisTx.getSubjectaddress(), subjectAddress);
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