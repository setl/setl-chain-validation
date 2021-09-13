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
import io.setl.bc.pychain.state.tx.StockSplitTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by nicholas on 17/08/2017.
 */
public class StockSplitTest {

  @Before
  public void setUp() throws Exception {
  }


  @Test
  public void stockSplitUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    final long amount = 42000000000L;
    double ratio = 2.0;
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    // [20, 132, 123456, 'demoPublicKey', 'demoAddress', 1502981035, '', 'thisNamespace', 'thisClassID', 2.0, '', 'Meta']

    String goodHash = "dff9ac813e5ac4d0e21fcb3853c9f2bcf71bfcd667678f61f0feb58f15ca7384";
    long timestamp = 1502981035L;

    StockSplitTx rVal = StockSplit.stockSplitUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, "", ratio, metadata, poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    StockSplitTx txCopy = new StockSplitTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    txCopy = StockSplitTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    poa = "changed";

    rVal = StockSplit.stockSplitUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, "", amount, metadata, poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @After
  public void tearDown() throws Exception {
  }


}