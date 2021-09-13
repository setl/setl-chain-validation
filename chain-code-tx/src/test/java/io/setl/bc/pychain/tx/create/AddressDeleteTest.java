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

import io.setl.bc.pychain.state.tx.AddressDeleteTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AddressDeleteTest {

  @Test
  public void addressDeleteUnsigned() {

    int chainID = 20;
    long nonce = 42;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String protocol = "protocol";
    String metadata = "metadata";
    String poa = "";

    String goodHash = "4466bf25e75f8411916ac6f1e8b5d4a4df5276fcf0646dee24440f540150a420";

    AddressDeleteTx rVal = AddressDelete.addressDeleteUnsigned(chainID, nonce, fromPubKey, fromAddress, protocol, metadata, poa);

    rVal.setTimestamp(1501672669L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals(rVal.getHash(), goodHash);

    rVal.setHash(null);
    AddressDelete thisClass = new AddressDelete(rVal);

    AddressDeleteTx txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = new AddressDeleteTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = AddressDeleteTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @Test
  public void getTxType() {

    AddressDelete thisClass = new AddressDelete();

    assertEquals(TxType.DELETE_ADDRESS, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {
  }


  @After
  public void tearDown() throws Exception {
  }
}