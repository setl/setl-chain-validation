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
package io.setl.bc.pychain.state.tx.contractdataclasses;

import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData.AssetIn;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.util.HashMap;
import io.setl.common.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class NominateCommitDataTest {

  String publicKey2 = Hex.encode(Type.ED25519.generate().getPublic().getEncoded());

  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


  @Test
  public void equals() throws Exception {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();
    final HashMap<String, Object> dataMap = new HashMap<>();

    NominateCommitData testData1 = getCommitData2();
    NominateCommitData testData2 = new NominateCommitData(new Object[] {});

    assertTrue(!testData1.equals(null));
    assertTrue(!testData1.equals("Fred"));
    assertTrue(!testData1.equals(testData2));

    dataMap.put("contractaddress", testData1.contractAddress);
    testData2 = new NominateCommitData(new MPWrappedMap(dataMap));
    assertTrue(!testData1.equals(testData2));

    dataMap.put("namespace", testData1.getNamespace());
    testData2 = new NominateCommitData(new MPWrappedMap(dataMap));
    assertTrue(!testData1.equals(testData2));

    dataMap.put("assetclass", testData1.getAssetclass());
    testData2 = new NominateCommitData(new MPWrappedMap(dataMap));
    assertTrue(!testData1.equals(testData2));

    dataMap.put("protocol", testData1.protocol);
    testData2 = new NominateCommitData(new MPWrappedMap(dataMap));
    assertTrue(!testData1.equals(testData2));

    dataMap.put("metadata", testData1.metadata);
    testData2 = new NominateCommitData(new MPWrappedMap(dataMap));
    assertTrue(!testData1.equals(testData2));

    testData2.setAssetIn(new AssetIn(11L, publicKey2, "Signature2"));
    assertTrue(!testData1.equals(testData2));

    testData2.getAssetsIn().clear();
    testData2.setAssetIn(new AssetIn(42L, publicKey2, "Signature1"));
    assertTrue(!testData1.equals(testData2));

    testData2.getAssetsIn().clear();
    testData2.setAssetIn(new AssetIn(42L, publicKey2, "Signature2"));
    assertTrue(testData1.equals(testData2));

    String j1 = testData1.toJSON().toJSONString();

    assertTrue(testData1.toJSON().toJSONString().equals(testData2.toJSON().toJSONString()));

  }

  @Test
  public void encode() throws Exception {

    NominateCommitData testData1 = getCommitData2();

    MPWrappedMap<String, Object> encoded = testData1.encode();
    NominateCommitData testData2 = new NominateCommitData(encoded);
    NominateCommitData testData3 = new NominateCommitData(testData1);

    assertTrue(testData1.equals(testData2));
    assertTrue(testData1.equals(testData3));
    assertTrue(testData3.equals(testData2));
    assertTrue(testData2.equals(testData1));
    assertTrue(testData3.equals(testData1));
    assertTrue(testData2.equals(testData3));

  }


  private NominateCommitData getCommitData2() {
    NominateCommitData rVal = new NominateCommitData("namespace", "assetclass", "protocol", "metadata", "contractaddress");
    rVal.setAssetIn(new AssetIn(42L, publicKey2, "Signature2"));
    return rVal;
  }


}
