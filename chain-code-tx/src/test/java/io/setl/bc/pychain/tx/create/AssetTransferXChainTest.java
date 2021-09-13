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

import io.setl.bc.pychain.state.tx.AssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetTransferXChainTest {

  @Test
  public void assetTransferXChainUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    long amount = 42000000000L;
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "90ebc00d2207bea51a30e903f4edb513d1a38e941760c759aec897f0b02a6464";

    long timestamp = 1502285984L;

    AssetTransferXChainTx rVal = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        chainID,
        toAddress,
        amount,
        protocol,
        metadata,
        poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    Object o2 = rVal.encodeTx();

    assertEquals("", rVal.getHash(), goodHash);

    AssetTransferXChainTx txCopy = new AssetTransferXChainTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    AssetTransferXChain thisClass = new AssetTransferXChain(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = AssetTransferXChainTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    amount += 1;

    AssetTransferXChainTx rVal2 = AssetTransferXChain.assetTransferXChainUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        chainID,
        toAddress,
        amount,
        protocol,
        metadata,
        poa);

    rVal2.setTimestamp(timestamp); // Test value

    rVal2.setHash(Hash.computeHash(rVal2));

    assertFalse(rVal2.getHash().equals(goodHash));

  }


  @Test
  public void getTxType() {

    AssetTransferXChain thisClass = new AssetTransferXChain();

    assertEquals(TxType.TRANSFER_ASSET_X_CHAIN, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}