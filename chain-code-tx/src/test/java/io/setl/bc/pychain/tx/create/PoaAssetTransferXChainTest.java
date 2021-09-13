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
import io.setl.bc.pychain.state.tx.PoaAssetTransferXChainTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaAssetTransferXChainTest {

  @Test
  public void getTxType() {

    PoaAssetTransferXChain thisClass = new PoaAssetTransferXChain();

    assertEquals(TxType.POA_TRANSFER_ASSET_X_CHAIN, thisClass.getTxType());
  }


  @Test
  public void poaAssetTransferXChainUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    long amount = 42000000000L;
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "323a3a12364edfcdad84609cfbd4c0e97191d923663f1d11dfca223443e797fa";

    long timestamp = 1502285984L;

    PoaAssetTransferXChainTx rVal = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
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

    PoaAssetTransferXChainTx txCopy = new PoaAssetTransferXChainTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaAssetTransferXChain thisClass = new PoaAssetTransferXChain(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaAssetTransferXChainTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    amount += 1;

    PoaAssetTransferXChainTx rVal2 = PoaAssetTransferXChain.poaAssetTransferXChainUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
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


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


}