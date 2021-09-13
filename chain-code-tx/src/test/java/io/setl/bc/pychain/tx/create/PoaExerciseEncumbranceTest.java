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
import io.setl.bc.pychain.state.tx.PoaExerciseEncumbranceTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaExerciseEncumbranceTest {

  @Test
  public void assetTransferUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    String subjectAddress = "";
    String reference = "thisReference";

    Long amount = 4242424L;

    String goodHash = "74c9a0bea1a952011fd39d115f3d1e0dc415f23bd52c193d7633d25986f9c13e";

    PoaExerciseEncumbranceTx rVal = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID, nonce, fromPubKey, fromAddress, poaAddress, poaReference, nameSpace, classId, fromAddress, reference, chainID, toAddress, amount, protocol,
        metadata, poa);

    rVal.setTimestamp(1502912412L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaExerciseEncumbranceTx txCopy = new PoaExerciseEncumbranceTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaExerciseEncumbrance thisClass = new PoaExerciseEncumbrance(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaExerciseEncumbranceTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @Test
  public void getTxType() {

    PoaExerciseEncumbrance thisClass = new PoaExerciseEncumbrance();

    assertEquals(TxType.POA_EXERCISE_ENCUMBRANCE, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


}
  
