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
import io.setl.bc.pychain.state.tx.PoaLockAssetTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaLockAssetTest {

  @Test
  public void getTxType() {

    PoaLockAsset thisClass = new PoaLockAsset();

    assertEquals(TxType.POA_LOCK_ASSET, thisClass.getTxType());
  }


  @Test
  public void poaLockAssetUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String lockDetails = "thisLockDetails";
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    // [20, 21, 123456, 'demoPublicKey', 'demoAddress', 1502788690, '', 'thisNamespace', 'thisClassID', 'thisLockDetails', 'Meta']

    String goodHash = "46dff09cb7f49c0a528a551338d73cc4d8e58a33d435c48643778b882b3dcd8f";

    PoaLockAssetTx rVal = PoaLockAsset.poaLockAssetUnsigned(chainID, nonce, fromPubKey, fromAddress, poaAddress, poaReference, nameSpace, classId,
        lockDetails, protocol, metadata, poa);

    rVal.setTimestamp(1502788690L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaLockAssetTx txCopy = new PoaLockAssetTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaLockAsset thisClass = new PoaLockAsset(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaLockAssetTx.decodeTX(txCopy.encodeTx());
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