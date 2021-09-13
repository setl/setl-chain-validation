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

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.NamespaceTransferTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NamespaceTransferTest {

  @Test
  public void getTxType() {

    NamespaceTransfer thisClass = new NamespaceTransfer();

    assertEquals(TxType.TRANSFER_NAMESPACE, thisClass.getTxType());
  }


  @Test
  public void namespaceTransferUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "b1297c638d4d33d183321a457b5e0df5221c4b98a52cad5f6d04510964e5919d";

    long timestamp = 1502380502L;

    NamespaceTransferTx rVal = NamespaceTransfer.namespaceTransferUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        nameSpace,
        toAddress,
        poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    NamespaceTransferTx txCopy = new NamespaceTransferTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    NamespaceTransfer thisClass = new NamespaceTransfer(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = NamespaceTransferTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    nonce++;

    rVal = NamespaceTransfer.namespaceTransferUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        nameSpace,
        toAddress,
        poa);

    rVal.setTimestamp(timestamp); // Test value

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