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
import io.setl.bc.pychain.state.tx.PoaNamespaceDeleteTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaNamespaceDeleteTest {

  @Test
  public void getTxType() {

    PoaNamespaceDelete thisClass = new PoaNamespaceDelete();

    assertEquals(TxType.POA_DELETE_NAMESPACE, thisClass.getTxType());
  }


  @Test
  public void poaNamespaceDeleteUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String nameSpace = "thisNamespace";
    String protocol = "Proto";  // Base64(MsgPack(""))
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "ba271451a1164ddb2e20d62252c780ed3618f96f24b18ac9d9bc6d250fd90bda";

    long timestamp = 1502212836L;

    PoaNamespaceDeleteTx rVal = PoaNamespaceDelete.poaNamespaceDeleteUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        protocol,
        metadata,
        poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaNamespaceDeleteTx txCopy = new PoaNamespaceDeleteTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaNamespaceDelete thisClass = new PoaNamespaceDelete(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaNamespaceDeleteTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    nonce = 123457;

    rVal = PoaNamespaceDelete.poaNamespaceDeleteUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        protocol,
        metadata,
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
