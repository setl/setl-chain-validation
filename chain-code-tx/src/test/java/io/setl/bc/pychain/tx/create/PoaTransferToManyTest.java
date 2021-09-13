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
import io.setl.bc.pychain.state.tx.PoaTransferToManyTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaTransferToManyTest {

  @Test
  public void getTxType() {

    PoaTransferToMany thisClass = new PoaTransferToMany();

    assertEquals(TxType.POA_TRANSFER_ASSET_TO_MANY, thisClass.getTxType());
  }


  @Test
  public void poaTransferToManyUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    Long amount1 = 4242424L;
    Long amount2 = 121212L;
    String protocol = "Proto";
    Object[] toAddresses = new Object[]{
        new Object[]{"Address1", amount1},
        new Object[]{"Address2", amount2}
    };
    String goodHash = "0cb44d6332f4074885a4a906cdab214b39005ca6477859edbc8ed3b1abbdd69f";

    PoaTransferToManyTx rVal = PoaTransferToMany.poaTransferToManyUnsigned(
        chainID, nonce, fromPubKey, fromAddress, poaAddress,
        poaReference, nameSpace, classId, chainID, toAddresses, (amount1 + amount2), protocol, metadata, poa);

    rVal.setTimestamp(1502618341L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaTransferToManyTx txCopy = new PoaTransferToManyTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaTransferToMany thisClass = new PoaTransferToMany(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaTransferToManyTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

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