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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaDeleteTx;
import io.setl.common.CommonPy.TxType;
import org.junit.Test;

public class PoaDeleteTest {

  @Test
  public void getTxType() {

    PoaDelete thisClass = new PoaDelete();

    assertEquals(TxType.REVOKE_POA, thisClass.getTxType());
  }


  @Test
  public void poaUnsigned() throws Exception {

    String fromPubKey = "3ba59635e0e09086f85ca3cfa8f8b68b17c636a53750c5f2219fed4ba8a2eec3";
    String fromAddress = "1KoYHqdQ7qQFZs2qjzhKq2c5jfv27tBBBt";
    String issuingAddress = "157XPPiV4Nc4xdrFUDJeec2W2RrSTe3fHo";

    int chainID = 20;
    long nonce = 123456;
    final String reference = "toKey2";
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    final String goodHash = "61e8fabf3b3f69b3dd560333f0ed1862fc9b03db6af3d440295f7a0412d3f816";

    PoaDeleteTx rVal = PoaDelete
        .poaDeleteUnsigned(chainID, nonce, fromPubKey, fromAddress, issuingAddress, reference, protocol, metadata, poa);

    assertNotNull(rVal);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaDeleteTx txCopy = new PoaDeleteTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaDelete thisClass = new PoaDelete(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaDeleteTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


}
