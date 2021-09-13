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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADMINISTRATORS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.BENEFICIARIES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.IS_CUMULATIVE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaEncumberTx;
import io.setl.common.CommonPy.TxType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaEncumberTest {

  @Test
  public void assetTransferUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    String subjectAddress = "";

    Long amount = 4242424L;

    final String goodHash = "82f089c03fb2e7ea6fc2236d2468bd93a2dba0151e3204f2a4de64f093938ef2";

    Object[] beneficiaries = new Object[]{(new EncumbranceDetail("Address1", 0L, 1000L)).encode()};
    Object[] administrators = new Object[]{(new EncumbranceDetail("Address2", 10L, 2000L)).encode()};

    Map<String, Object> thisMap = new TreeMap<>();

    thisMap.put(REFERENCE, "thisreference");
    thisMap.put(BENEFICIARIES, beneficiaries);
    thisMap.put(ADMINISTRATORS, administrators);
    thisMap.put(IS_CUMULATIVE, true);

    // Build kv store (like MP Wrapped) to test alternative constructor.

    Object[] kvs = new Object[2 * thisMap.size()];
    int i = 0;
    for (Entry e : thisMap.entrySet()) {
      kvs[i++] = e.getKey();
      kvs[i++] = e.getValue();
    }

    // OK...

    PoaEncumberTx rVal = PoaEncumber.encumberUnsigned(
        chainID, nonce, fromPubKey, fromAddress, poaAddress,
        poaReference, nameSpace, classId, subjectAddress, amount, kvs, protocol, metadata, poa);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    rVal = PoaEncumber.encumberUnsigned(
        chainID, nonce, fromPubKey, fromAddress, poaAddress,
        poaReference, nameSpace, classId, subjectAddress, amount, thisMap, protocol, metadata, poa);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    PoaEncumberTx txCopy = new PoaEncumberTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaEncumber thisClass = new PoaEncumber(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaEncumberTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

    rVal = PoaEncumber.encumberUnsigned(
        chainID, nonce, fromPubKey, fromAddress, poaAddress,
        poaReference, nameSpace, classId, subjectAddress, amount, new MPWrappedMap<String, Object>(thisMap), protocol, metadata, poa);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

  }


  @Test
  public void getTxType() {

    PoaEncumber thisClass = new PoaEncumber();

    assertEquals(TxType.POA_ENCUMBER_ASSET, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


}