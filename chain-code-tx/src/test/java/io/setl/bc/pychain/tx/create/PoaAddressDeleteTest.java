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
import io.setl.bc.pychain.state.tx.PoaAddressDeleteTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaAddressDeleteTest {

  @Test
  public void getTxType() {

    PoaAddressDelete thisClass = new PoaAddressDelete();

    assertEquals(TxType.POA_DELETE_ADDRESS, thisClass.getTxType());
  }


  @Test
  public void poaAddressDeleteUnsigned() {

    int chainID = 20;
    long nonce = 42;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String protocol = "protocol";
    String metadata = "metadata";
    String poa = "";

    String goodHash = "fbad3a986d251d65af10abad28b69b722ff2db57b57ee58d454e927c2de378a8";

    PoaAddressDeleteTx rVal = PoaAddressDelete
        .poaAddressDeleteUnsigned(chainID, nonce, fromPubKey, fromAddress, poaAddress, poaReference, protocol, metadata, poa);

    rVal.setTimestamp(1501672669L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals(rVal.getHash(), goodHash);

    rVal.setHash(null);
    PoaAddressDelete thisClass = new PoaAddressDelete(rVal);

    PoaAddressDeleteTx txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = new PoaAddressDeleteTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaAddressDeleteTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @Before
  public void setUp() throws Exception {
  }


  @After
  public void tearDown() throws Exception {
  }
}