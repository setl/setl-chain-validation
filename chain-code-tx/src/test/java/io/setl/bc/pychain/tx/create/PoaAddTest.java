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
package io.setl.bc.pychain.tx.create;

import static io.setl.common.CommonPy.TxType.TRANSFER_ASSET_X_CHAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.common.CommonPy.TxType;
import org.junit.Test;

public class PoaAddTest {

  @Test
  public void getTxType() {

    PoaAdd thisClass = new PoaAdd();

    assertEquals(TxType.GRANT_POA, thisClass.getTxType());
  }


  @Test
  public void poaUnsigned() throws Exception {

    String fromPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String fromAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    String returnAddress = "157XPPiV4Nc4xdrFUDJeec2W2RrSTe3fHo";

    int chainID = 20;
    long nonce = 123456;
    final String reference = "toKey2";
    final String signature = "signature";
    final Long amount = 65748L;
    final long startDate = 123L;
    final long endDate = 456L;
    final Object[] itemsData = new Object[]{new Object[]{TRANSFER_ASSET_X_CHAIN.getId(), 1000L, new String[]{"Asset1", "Asset2"}}};
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";
    String hash = "HASH";

    final String goodHash = "9ec6f066605517bab2fa17cf4b0f4d93762fd309e8d643490889082f05d792e3";

    PoaAddTx rVal = PoaAdd
        .poaUnsigned(chainID, nonce, fromPubKey, fromAddress, reference, returnAddress, startDate, endDate, itemsData, protocol, metadata, poa);

    assertNotNull(rVal);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals(goodHash, rVal.getHash());

    rVal.setHash(null);
    PoaAdd thisClass = new PoaAdd(rVal);

    PoaAddTx txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(goodHash, txCopy.getHash());

    txCopy = new PoaAddTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(goodHash, txCopy.getHash());

    txCopy = PoaAddTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(goodHash, txCopy.getHash());

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertNotEquals(goodHash, rVal.getHash());

  }


}