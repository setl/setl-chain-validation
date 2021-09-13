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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.AssetIssueTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import java.math.BigInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetIssueTest {

  @Test
  public void assetIssueUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 42;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    long amount = 42000000000L;
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";

    String goodHash = "ecdb93b881b0386cf962f1faa25fa0e55cc5304a7849101db1128c65e911d97f";

    long timestamp = 1501689022L;

    AssetIssueTx rVal = AssetIssue.assetIssueUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, toAddress, amount, protocol, metadata, poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    AssetIssueTx txCopy = new AssetIssueTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    AssetIssue thisClass = new AssetIssue(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = AssetIssueTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    poa = "changed";

    rVal = AssetIssue.assetIssueUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, toAddress, amount, protocol, metadata, poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertNotEquals(goodHash,rVal.getHash());

  }


  @Test
  public void getTxType() {

    AssetIssue thisClass = new AssetIssue(BigInteger.ONE,"","","","");

    assertEquals(TxType.ISSUE_ASSET, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


}