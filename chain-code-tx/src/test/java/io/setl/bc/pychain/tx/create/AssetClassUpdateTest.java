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

import io.setl.bc.pychain.state.tx.AssetClassUpdateTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetClassUpdateTest {

  @Test
  public void assetClassUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "d20c468e49e680d91481c523876ca6a62e9fff8caa79b60ba293df31ae4175be";

    AssetClassUpdateTx rVal = AssetClassUpdate.assetClassUpdateUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, metadata, poa);

    rVal.setTimestamp(1502377819); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    AssetClassUpdateTx txCopy = new AssetClassUpdateTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    AssetClassUpdate thisClass = new AssetClassUpdate(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = AssetClassUpdateTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    poa = "fred";

    rVal = AssetClassUpdate.assetClassUpdateUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, metadata, poa);

    rVal.setTimestamp(1502377819); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @Test
  public void getTxType() {

    AssetClassUpdate thisClass = new AssetClassUpdate();

    assertEquals(TxType.UPDATE_ASSET_CLASS, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}