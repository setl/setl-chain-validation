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
import io.setl.bc.pychain.state.tx.NamespaceRegisterTx;
import io.setl.common.CommonPy.TxType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NamespaceRegisterTest {

  @Test
  public void getTxType() {

    NamespaceRegister thisClass = new NamespaceRegister();

    assertEquals(TxType.REGISTER_NAMESPACE, thisClass.getTxType());
  }


  @Test
  public void namespaceRegisterUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String nameSpace = "thisNamespace";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "8e8fe492311f55bf5e4f828f130fdcb40d13a5491cb36f950bc2b56a0507be03";

    long timestamp = 1502212836L;

    NamespaceRegisterTx rVal = NamespaceRegister.namespaceRegisterUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        nameSpace,
        metadata,
        poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    NamespaceRegisterTx txCopy = new NamespaceRegisterTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    NamespaceRegister thisClass = new NamespaceRegister(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = NamespaceRegisterTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    nonce = 123457;

    rVal = NamespaceRegister.namespaceRegisterUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        nameSpace,
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