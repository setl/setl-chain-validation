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
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EncumberTest {

  @Test
  public void encumberUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoAddressToo";
    String nameSpace = "thisNamespace";
    String classId = "thisClassID";
    String protocol = "Proto";
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "";
    String subjectAddress = "";

    Long amount = 4242424L;

    final String goodHash = "d6a5fddc30daecdd8142027de850df0b57b3510bda94c3e3ff6aba0df97d9731";

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

    EncumberTx rVal = Encumber
        .encumberUnsigned(chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, subjectAddress, amount, kvs, protocol, metadata, poa);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    rVal = Encumber.encumberUnsigned(
        chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, subjectAddress, amount, new MPWrappedMap<>(kvs), protocol, metadata, poa);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    rVal = Encumber.encumberUnsigned(
        chainID, nonce, fromPubKey, fromAddress, nameSpace, classId, subjectAddress, amount, thisMap, protocol, metadata, poa);

    rVal.setTimestamp(1502818611L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    EncumberTx txCopy = new EncumberTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    Encumber thisClass = new Encumber(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = EncumberTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

  }


  @Test
  public void getTxType() {

    Encumber thisClass = new Encumber();

    assertEquals(TxType.ENCUMBER_ASSET, thisClass.getTxType());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}