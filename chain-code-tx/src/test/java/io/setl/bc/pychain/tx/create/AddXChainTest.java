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

import io.setl.bc.pychain.state.tx.AddXChainTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AddXChainTest {

  @Test
  public void addXChainUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poa = "";
    int newChainId = 21;
    int newBlockHeight = 22;
    long newParameter = 23;
    List<Object[]> newSignodes = Arrays.<Object[]>asList(new Object[]{"sigPub", 42});

    String goodHash = "6a840840d83f04723c6bae8405820f21e5d98ca0fd147485f31898e6cc992790";
    long timestamp = 1503059878L;

    AddXChainTx rVal = AddXChain.addXChainUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        newChainId,
        newBlockHeight,
        newParameter,
        newSignodes,
        poa);

    rVal.setTimestamp(timestamp); // Test value

    // [20, 130, 123456, 'demoPublicKey', 'demoAddress', 1503059878, '', 21, 22, 23, [['sigPub', 42]]]

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    AddXChainTx txCopy = new AddXChainTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    AddXChain thisClass = new AddXChain(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = AddXChainTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    nonce = 123457;

    rVal = AddXChain.addXChainUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        newChainId,
        newBlockHeight,
        newParameter,
        newSignodes,
        poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @Test
  public void getTxType() {

    AddXChain thisClass = new AddXChain();

    assertEquals(TxType.ADD_X_CHAIN, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}