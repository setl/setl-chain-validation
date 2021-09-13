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
import io.setl.bc.pychain.state.tx.LockAssetTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LockAssetTest {

  @Test
  public void getTxType() {

    LockAsset thisClass = new LockAsset();

    assertEquals(TxType.LOCK_ASSET, thisClass.getTxType());
  }


  @Test
  public void lockAssetUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String lockDetails = "thisLockDetails";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    // [20, 21, 123456, 'demoPublicKey', 'demoAddress', 1502788690, '', 'thisNamespace', 'thisClassID', 'thisLockDetails', 'Meta']

    String goodHash = "35391e26415709b2415d73c104ea2f5282571147d017b61a71c5bde5a233ec68";

    LockAssetTx rVal = LockAsset.lockAssetUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, lockDetails, metadata, poa);

    rVal.setTimestamp(1502788690L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    LockAssetTx txCopy = new LockAssetTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    LockAsset thisClass = new LockAsset(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = LockAssetTx.decodeTX(txCopy.encodeTx());
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