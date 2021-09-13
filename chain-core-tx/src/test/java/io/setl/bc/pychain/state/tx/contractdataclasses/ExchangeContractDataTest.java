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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTO_SIGN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__STATUS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__TIMEEVENT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEvent;
import io.setl.common.Balance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class ExchangeContractDataTest {

  String contractAddress = "thisAddress";

  String contractFunction = "exchange";

  String[] events = new String[]{"time"};

  long expiryDate = 123456L;

  Number inputAmount1 = 123L;

  Number inputAmount2 = new Balance(986756L);

  String inputClass1 = "ClassName1";

  String inputClass2 = "ClassName2";

  String inputNamespace1 = "NS1";

  String inputNamespace2 = "NS2";

  List<NominateAsset> inputs;

  String issuingAddress = "issuingAddress";

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


  @Test
  public void constructors() {

    // Test 'default' constructor.
    ExchangeContractData ecdNull = null;
    ExchangeContractData ecd0 = new ExchangeContractData(ecdNull);

    assertTrue(ecd0.getIssuingaddress().equals(""));
    assertTrue(ecd0.get__address().equals(""));
    assertTrue(ecd0.getProtocol().equals(""));
    assertTrue(ecd0.getMetadata().equals(""));
    assertTrue(ecd0.getStatus().equals(""));
    assertTrue(ecd0.getInputs().size() == 0);
    assertTrue(ecd0.getOutputs().size() == 0);
    assertTrue(ecd0.get__timeevent() == 0L);
    assertTrue(ecd0.getStartdate() == 0L);
    assertTrue(ecd0.getExpiry() == 0L);
    assertTrue(ecd0.getEvents().encode(0).length == 0);

    //

    ExchangeContractData ecd1 = new ExchangeContractData(
        "",
        "",
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );

    assertTrue(ecd1.get__address().equals(""));
    ecd1.set__address(contractAddress);

    assertTrue(ecd1.get__function().equals(""));
    ecd1.set__function(contractFunction);

    assertTrue(ecd1.setNextTimeEvent(42L, true) == 42L);
    assertTrue(ecd1.setNextTimeEvent(0L, false) == expiryDate);

    ecd1.setIssuingaddress(issuingAddress);

    ecd1.setAutosign(false);
    assertFalse(ecd1.getAutosign());
    ecd1.toJSON();  // Just for code coverage.
    ecd1.setAutosign(true);
    assertTrue(ecd1.getAutosign());

    assertTrue(ecd1.getExpiry() == expiryDate);
    assertTrue(ecd1.getIssuingaddress().equals(issuingAddress));
    assertTrue(ecd1.getStatus().equals(""));
    assertTrue(ecd1.get__address().equals(contractAddress));
    assertTrue(ecd1.get__function().equals(contractFunction));
    assertTrue(ecd1.getEvents().equals(new DvpEvent(events)));
    assertTrue(ecd1.getMaxblocks().equalTo(maxblocks));
    assertTrue(ecd1.getMinblocks().equalTo(minblocks));
    assertTrue(ecd1.getMetadata().equals(metadata));
    assertTrue(ecd1.getProtocol().equals(protocol));
    assertTrue(ecd1.getStartdate().equals(startDate));
    assertTrue(ecd1.get__timeevent() == expiryDate);

    assertTrue(Objects.equals(ecd1.getInputs(), inputs));
    assertTrue(Objects.equals(ecd1.getOutputs(), outputs));

    assertFalse(ecd1.encodeToMapForTxParameter().containsKey(__ADDRESS));
    assertFalse(ecd1.encodeToMapForTxParameter().containsKey(__TIMEEVENT));
    assertFalse(ecd1.encodeToMapForTxParameter().containsKey(CONTRACT_FUNCTION));

    MPWrappedMap<String, Object> encoded = ecd1.encode();
    final ExchangeContractData ecd2 = new ExchangeContractData(encoded);
    final ExchangeContractData ecd3 = new ExchangeContractData(ecd2);
    Map<String, Object> tempMap = ecd2.encodeToMap();

    tempMap.put(__STATUS, "testStatus"); // Test parts of the Map Constructor.
    tempMap.put(AUTO_SIGN, false);
    final ExchangeContractData ecd4 = new ExchangeContractData(tempMap);
    assertTrue(ecd4.getStatus().equals("testStatus"));
    assertTrue(ecd4.getAutosign() == false);
    assertTrue(ecd4.encodeToMap().getOrDefault(__STATUS, "").equals("testStatus"));
    assertTrue(((boolean)ecd4.encodeToMap().getOrDefault(AUTO_SIGN, true)) == false);

    ecd4.setAutosign(true); // So that it is equal to all the others...
    ecd4.setStatus(""); // So that it is equal to all the others...

    final ExchangeContractData ecd5 = (ExchangeContractData) ecd1.copy();

    ecd1.toJSON();  // Just for code coverage.
    ecd1.encodeEventsJson();  // Just for code coverage.
    ecd1.toString();  // Just for code coverage.

    assertTrue(ecd1.addresses().contains(issuingAddress));
    assertTrue(ecd1.addresses().contains(outputAddress1));
    assertTrue(ecd1.addresses().size() == 2);

    // Equals tests

    assertFalse(ecd1.equals(null));
    assertFalse(ecd1.equals(""));

    ExchangeContractData ecdcompare = new ExchangeContractData(
        contractAddress,
        "badfunction",
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        "BadAddress",
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate - 1,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        "Bad address",
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate + 1,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        "Not Protocol",
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        "not meta"
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        -1,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        -1,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        new Object[]{},
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        new Object[]{},
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        events,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    ecdcompare = new ExchangeContractData(
        contractAddress,
        contractFunction,
        inputs,
        outputs,
        minblocks,
        maxblocks,
        startDate,
        expiryDate,
        null,
        issuingAddress,
        protocol,
        metadata
    );
    assertFalse(ecd1.equals(ecdcompare));

    // OK, now good compares..

    for (ExchangeContractData thisECD : new ExchangeContractData[]{ecd2, ecd3, ecd4, ecd5}) {

      assertTrue(thisECD.equals(ecd1));

      assertTrue(ecd1.getExpiry().equals(thisECD.getExpiry()));
      assertTrue(ecd1.getIssuingaddress().equals(thisECD.getIssuingaddress()));
      assertTrue(ecd1.getStatus().equals(thisECD.getStatus()));
      assertTrue(ecd1.get__address().equals(thisECD.get__address()));
      assertTrue(ecd1.get__function().equals(thisECD.getContractType()));
      assertTrue(ecd1.getIssuingaddress().equals(thisECD.getIssuingaddress()));
      assertTrue(ecd1.getEvents().equals(thisECD.getEvents()));
      assertTrue(ecd1.getMaxblocks().equalTo(thisECD.getMaxblocks()));
      assertTrue(ecd1.getMinblocks().equalTo(thisECD.getMinblocks()));
      assertTrue(ecd1.getMetadata().equals(thisECD.getMetadata()));
      assertTrue(ecd1.getProtocol().equals(thisECD.getProtocol()));
      assertTrue(ecd1.getStartdate().equals(thisECD.getStartdate()));
      assertTrue(ecd1.get__timeevent() == thisECD.get__timeevent());

      assertTrue(Objects.equals(ecd1.getInputs(), thisECD.getInputs()));
      assertTrue(Objects.equals(ecd1.getOutputs(), thisECD.getOutputs()));

    }

    // test equals vs autosign
    ecd2.setAutosign(!ecd1.getAutosign());
    assertFalse(ecd1.equals(ecd2));


  }


  @Before
  public void setUp() throws Exception {

    inputs = new ArrayList<NominateAsset>();
    inputs.add(new NominateAsset(inputNamespace1, inputClass1, inputAmount1));
    inputs.add(new NominateAsset(inputNamespace2, inputClass2, inputAmount2));

    outputs = new ArrayList<NominateAsset>();
    outputs.add(new NominateAsset(outputNamespace1, outputClass1, outputAmount1, outputAddress1, outputReference1, outputPublicKey1, outputSignature1));
    outputs.add(new NominateAsset(outputNamespace2, outputClass2, outputAmount2));
  }

}