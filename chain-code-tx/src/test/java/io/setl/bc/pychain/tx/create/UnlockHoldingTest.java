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

import static io.setl.common.CommonPy.EncumbranceConstants.ISSUER_LOCK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.UnlockHoldingTx;
import io.setl.common.CommonPy.TxType;
import org.junit.Test;

public class UnlockHoldingTest {

  @Test
  public void getTxType() {

    UnlockHolding thisClass = new UnlockHolding();

    assertEquals(TxType.UNLOCK_ASSET_HOLDING, thisClass.getTxType());
  }


  @Test
  public void unlockHoldingUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String reference = ISSUER_LOCK;
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    String subjectAddress = "demoAddress";

    Long amount = 4242424L;

    String goodHash = "edf884dba223bf81ddc0fd6af6cfac2c0c7d5e753f77ebf741ff6705edc848a3";

    UnlockHoldingTx rVal = UnlockHolding.unlockHoldingUnsigned(
        chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, subjectAddress, amount, protocol, metadata, poa);

    rVal.setTimestamp(1502896019L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    UnlockHoldingTx txCopy = new UnlockHoldingTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    UnlockHolding thisClass = new UnlockHolding(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = UnlockHoldingTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


}