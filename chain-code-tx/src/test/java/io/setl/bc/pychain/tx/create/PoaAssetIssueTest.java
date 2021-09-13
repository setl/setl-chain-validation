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

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaAssetIssueTx;
import io.setl.common.CommonPy.TxType;
import java.math.BigInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaAssetIssueTest {

  @Test
  public void getTxType() {

    PoaAssetIssue thisClass = new PoaAssetIssue(BigInteger.ONE,"","","","","","");

    assertEquals(TxType.POA_ISSUE_ASSET, thisClass.getTxType());
  }


  @Test
  public void poaAssetIssueUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 42;
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

    String goodHash = "4f533ce4ae088f2d0d79e2853dc0c09a11a1b11d7c83244d5219a3cb42b8d0de";

    long timestamp = 1501689022L;

    PoaAssetIssueTx rVal = PoaAssetIssue
        .poaAssetIssueUnsigned(chainID, nonce, fromPubKey, fromAddress, poaAddress, poaReference, nameSpace, classId, toAddress, amount, protocol, metadata,
            poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaAssetIssueTx txCopy = new PoaAssetIssueTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaAssetIssue thisClass = new PoaAssetIssue(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaAssetIssueTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    poa = "changed";

    rVal = PoaAssetIssue
        .poaAssetIssueUnsigned(chainID, nonce, fromPubKey, fromAddress, poaAddress, poaReference, nameSpace, classId, toAddress, amount, protocol, metadata,
            poa);

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertNotEquals(goodHash,rVal.getHash());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


}