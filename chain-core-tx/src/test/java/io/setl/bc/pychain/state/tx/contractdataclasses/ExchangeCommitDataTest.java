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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE_COMMIT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.common.Balance;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class ExchangeCommitDataTest {

  String contractAddress = "thisAddress";

  String contractFunction = CONTRACT_NAME_EXCHANGE_COMMIT;

  String[] events = new String[]{"time"};

  long expiryDate = 123456L;

  Number inputAmount1 = 123L;

  Number inputAmount2 = new Balance(986756L);

  String inputClass1 = "ClassName1";

  String inputClass2 = "ClassName2";

  String inputNamespace1 = "NS1";

  String inputNamespace2 = "NS2";

  List<NominateAsset> inputs;

  Number maxblocks = 100;

  String metadata = "thisMeta";

  Number minblocks = 10;

  String outputAddress1 = "outAddress1";

  Number outputAmount1 = 321;

  Number outputAmount2 = new Balance(11111);

  String outputClass1 = "ClassName1";

  String outputClass2 = "ClassName2";

  String outputNamespace1 = "NS1";

  String outputNamespace2 = "NS2";

  String outputPublicKey1 = "outpubkey1";

  String outputReference1 = "ourref1";

  String outputSignature1 = "outsig1";

  List<NominateAsset> outputs;

  String protocol = "thisProto";

  long startDate = 0;

  String toAddress = "issuingAddress";


  @Test
  public void constructors() {

    ExchangeCommitData ecd1 = new ExchangeCommitData(
        "",
        outputs,
        toAddress,
        protocol,
        metadata
    );

    assertTrue(ecd1.contractAddress.equals(""));
    ecd1.contractAddress = contractAddress;

    assertTrue(ecd1.get__function().equals(CONTRACT_NAME_EXCHANGE_COMMIT));

    assertTrue(ecd1.setNextTimeEvent(42L, true) == 0L);
    assertTrue(ecd1.get__function().equals(contractFunction));
    assertTrue(ecd1.metadata.equals(metadata));
    assertTrue(ecd1.protocol.equals(protocol));
    assertTrue(Objects.equals(ecd1.getAssetsIn(), outputs));

    MPWrappedMap<String, Object> encoded = ecd1.encode();
    final ExchangeCommitData ecd2 = new ExchangeCommitData(encoded);
    final ExchangeCommitData ecd3 = new ExchangeCommitData(ecd2);
    final ExchangeCommitData ecd4 = new ExchangeCommitData(ecd2.encodeToMap());
    final ExchangeCommitData ecd5 = (ExchangeCommitData) ecd1.copy();

    ecd1.toJSON();  // Just for code coverage.
    ecd1.toString();  // Just for code coverage.

    assertTrue(ecd1.addresses().contains(toAddress));
    assertTrue(ecd1.addresses().contains(outputAddress1));
    assertTrue(ecd1.addresses().size() == 2);

    // Equals tests

    assertFalse(ecd1.equals(null));
    assertFalse(ecd1.equals(""));

    ExchangeCommitData ecdcompare = new ExchangeCommitData(
        "",
        inputs,
        toAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeCommitData(
        contractAddress,
        null,
        toAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeCommitData(
        contractAddress,
        inputs,
        "",
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeCommitData(
        contractAddress,
        inputs,
        toAddress,
        "",
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeCommitData(
        contractAddress,
        inputs,
        toAddress,
        protocol,
        ""
    );
    assertFalse(ecd1.equals(ecdcompare));

    // OK, now good compares..

    for (ExchangeCommitData thisECD : new ExchangeCommitData[]{ecd2, ecd3, ecd4, ecd5}) {

      assertTrue(thisECD.equals(ecd1));

      assertTrue(ecd1.contractAddress.equals(thisECD.contractAddress));
      assertTrue(ecd1.toaddress.equals(thisECD.toaddress));
      assertTrue(ecd1.get__function().equals(thisECD.getContractType()));
      assertTrue(ecd1.metadata.equals(thisECD.metadata));
      assertTrue(ecd1.protocol.equals(thisECD.protocol));

      assertTrue(Objects.equals(ecd1.getAssetsIn(), thisECD.getAssetsIn()));

    }

  }


  @Before
  public void setUp() throws Exception {

    outputs = new ArrayList<NominateAsset>();
    outputs.add(new NominateAsset(outputNamespace1, outputClass1, outputAmount1, outputAddress1, outputReference1, outputPublicKey1, outputSignature1));
    outputs.add(new NominateAsset(outputNamespace2, outputClass2, outputAmount2));
  }

}