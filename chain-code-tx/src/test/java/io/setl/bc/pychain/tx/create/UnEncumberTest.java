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

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.UnEncumberTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnEncumberTest {

  @Test
  public void getTxType() {

    UnEncumber thisClass = new UnEncumber();

    assertEquals(TxType.UNENCUMBER_ASSET, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


  @Test
  public void unencumberUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String reference = "thisReference";
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    String subjectAddress = "demoAddress";

    Long amount = 4242424L;

    String goodHash = "c276200f86c0e6169638a7b9a83215003f0e613a6f3552e70135d75eed258894";

    UnEncumberTx rVal = UnEncumber.unencumberUnsigned(
        chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, subjectAddress, reference, amount, protocol, metadata, poa);

    rVal.setTimestamp(1502896019L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    UnEncumberTx txCopy = new UnEncumberTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    UnEncumber thisClass = new UnEncumber(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = UnEncumberTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }

}