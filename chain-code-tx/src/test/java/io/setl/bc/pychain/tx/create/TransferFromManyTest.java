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
import io.setl.bc.pychain.state.tx.TransferFromManyTx;
import io.setl.common.CommonPy.TxType;
import java.math.BigInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransferFromManyTest {

  @Test
  public void getTxType() {

    TransferFromMany thisClass = new TransferFromMany();

    assertEquals(TxType.TRANSFER_ASSET_FROM_MANY, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


  @Test
  public void transferFromManyUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    Long amount1 = 4242424L;
    Long amount2 = 121212L;
    String protocol = "Proto";
    Object[] fromAddresses = new Object[]{
        new Object[]{"Address1", amount1},
        new Object[]{"Address2", amount2}
    };
    String goodHash = "28262f6db865e41cdbed44a6e86152dcc00a2d9f22735b4415484310b565ce2d";

    TransferFromManyTx rVal = TransferFromMany.transferFromManyUnsigned(
        chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, fromAddresses, (amount1 + amount2), protocol, metadata, poa);

    rVal.setTimestamp(1502618341L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    TransferFromManyTx txCopy = new TransferFromManyTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    TransferFromMany thisClass = new TransferFromMany(rVal);

    BigInteger i1 = thisClass.getAmount();
    thisClass.setAmount(null);
    thisClass.fixAmount();

    assertEquals(i1, thisClass.getAmount());

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = TransferFromManyTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


}