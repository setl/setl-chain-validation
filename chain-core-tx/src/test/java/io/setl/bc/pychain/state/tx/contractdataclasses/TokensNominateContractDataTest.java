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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import io.setl.common.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class TokensNominateContractDataTest {


  // Party 1
  KeyPair keyPair1 = Type.ED25519.generate();
  PublicKey publicKey1 = keyPair1.getPublic();
  String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

  // Party 2
  KeyPair keyPair2 = Type.ED25519.generate();
  PublicKey publicKey2 = keyPair2.getPublic();

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void equals() throws Exception {

    final TokensNominateContractData testData1 = getCommitData2("thisContractAddress");
    final TokensNominateContractData testData2 = new TokensNominateContractData(new MPWrappedMap<>(new Object[]{}));
    final TokensNominateContractData testData3 = new TokensNominateContractData(testData1);

    String j1 = testData1.toJSON().toJSONString();
    assertTrue(testData1.toJSON().toJSONString().equals(testData3.toJSON().toJSONString()));

    assertTrue(testData1.equals(testData3));

    assertTrue(!testData1.equals(null));
    assertTrue(!testData1.equals("Fred"));
    assertTrue(!testData1.equals(testData2));

    HashMap<String, Object> params = new HashMap<>();

    params.put("__function", CONTRACT_NAME_TOKENS_NOMINATE);
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("__address", new String("thisContractAddress"));
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("__timeevent", testData1.get__timeevent());
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("issuingaddress", party1Address);
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("expiry", 999L);
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("blocksizein", 20L);
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("blocksizeout", 40L);
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("namespace", new String("thisNamespace"));
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("inputtokenclass", new String("thisInputtokenclass"));
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("outputtokenclass", new String("thisOutputtokenclass"));
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("protocol", new String("protocol"));
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("metadata", new String("metadata"));
    assertTrue(!testData1.equals(new TokensNominateContractData(params)));

    params.put("events", new String[] {"time"});
    assertTrue(testData1.equals(new TokensNominateContractData(params)));

  }

  @Test
  public void encode() throws Exception {

    TokensNominateContractData testData1 = getCommitData2("thisContractAddress");

    MPWrappedMap<String, Object> encoded = testData1.encode();
    TokensNominateContractData testData2 = new TokensNominateContractData(encoded);
    TokensNominateContractData testData3 = new TokensNominateContractData(testData1);
    TokensNominateContractData testData4 = new TokensNominateContractData(testData1.encodeToMap());

    assertTrue(testData1.equals(testData2));
    assertTrue(testData1.equals(testData3));
    assertTrue(testData1.equals(testData4));
    assertTrue(testData2.equals(testData1));
    assertTrue(testData2.equals(testData3));
    assertTrue(testData2.equals(testData4));
    assertTrue(testData3.equals(testData1));
    assertTrue(testData3.equals(testData2));
    assertTrue(testData3.equals(testData4));
    assertTrue(testData4.equals(testData1));
    assertTrue(testData4.equals(testData2));
    assertTrue(testData4.equals(testData3));

  }

  private TokensNominateContractData getCommitData2(String constractAddress) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    return new TokensNominateContractData(
        constractAddress,
        CONTRACT_NAME_TOKENS_NOMINATE,
        "thisNamespace",
        "thisInputtokenclass",
        "thisOutputtokenclass",
        20L,
        40L,
        999L,
        new String[] {"time"},
        party1Address,
        "protocol",
        "metadata"
    );
  }


}
