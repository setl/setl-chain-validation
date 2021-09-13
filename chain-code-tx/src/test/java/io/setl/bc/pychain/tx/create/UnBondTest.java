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
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.UnBondTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnBondTest {

  @Test
  public void getTxType() {

    UnBond thisClass = new UnBond();

    assertEquals(TxType.REVOKE_VOTING_POWER, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


  @Test
  public void unBondUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String signodePublicKey = "thisPulicKey"; // sic.
    String returnAddress = "thisReturnAddress";
    String protocol = "proto";
    String metadata = "meta";
    String poa = "";
    Long amount = 4242424L;

    String goodHash = "61144609541d3988730aacd7bd34df6f89b3ab33fe05d2a020c02b4166b551f8";

    AbstractTx rVal = UnBond.unBondUnsigned(chainID, nonce, fromPubKey, fromAddress, returnAddress, amount, protocol, metadata, poa);

    rVal.setTimestamp(1502728038L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    UnBondTx txCopy = new UnBondTx((UnBondTx) rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    UnBond thisClass = new UnBond((UnBondTx) rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = UnBondTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }

}