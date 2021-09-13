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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaNewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.common.CommonPy.TxType;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import io.setl.common.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaNewContractTest {

  String contractAddress = "";

  String contractFunction = CONTRACT_NAME_DVP_UK;

  String encumbranceToUse = "enc.";

  String[] events = new String[]{"EventType1", "EventType2"};

  long expiryDate = 99999999L;

  int isCompleted = 0;

  String issuingAddress = "Address1";

  String metadata = "MetaData";

  boolean mustSign1 = false;

  boolean mustSign2 = false;

  long nextTimeEvent = 8888888L;

  // Party 1
  String partyIdentifier1 = "party1";

  // Party 2
  String partyIdentifier2 = "party2";

  String protocol = "Protocol";

  String publicKey1 = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijABCD";

  String publicKey2 = "ABCDabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";

  String sigAddress1 = "party1sigaddress";

  String sigAddress2 = "party2sigaddress";

  String signature1 = "party1sig";

  String signature2 = "party2sig";

  long startDate = 1L;


  private DvpUkContractData getTestData1() {

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        sigAddress1,
        publicKey1,
        signature1,
        mustSign1
    );

    party1.payList.add(new DvpPayItem(
        "payadd1",
        "payNs1",
        "payClass",
        "43",
        publicKey1,
        "paySig1",
        true,
        "meta1",
        "enc1"

    ));

    party1.receiveList.add(new DvpReceiveItem(
        "payadd1",
        "payNs2",
        "payClass2",
        432L
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        sigAddress2,
        publicKey2,
        signature2,
        mustSign2
    );

    party2.payList.add(new DvpPayItem(
        "payadd2",
        "payNs2",
        "payClass2",
        432L,
        "payPubKey2",
        "paySig2",
        true,
        "meta2",
        "enc2"
    ));

    party1.receiveList.add(new DvpReceiveItem(
        "payadd2",
        "payNs1",
        "payClass1",
        43L
    ));

    DvpUkContractData testData = new DvpUkContractData(
        contractAddress,
        isCompleted,
        contractFunction,
        nextTimeEvent,
        startDate,
        expiryDate,
        issuingAddress,
        protocol,
        metadata,
        events,
        encumbranceToUse
    );

    testData.addParty(party1);
    testData.addParty(party2);

    DvpAddEncumbrance yy = new DvpAddEncumbrance("AssetID", "Reference", 100L, publicKey1, "sig5");
    yy.beneficiaries.add(new EncumbranceDetail("Address", 0L, 999999L));

    testData.addAddEncumbrance(yy);
    testData.addAuthorisation(new DvpAuthorisation(publicKey1, "AuthID", "Address2", "AuthMeta", false, 1));
    testData.addParameter("P1", new DvpParameter(publicKey1, 7L, 0, 0, 0, ""));

    return testData;
  }


  @Test
  public void getTxType() {

    PoaNewContract thisClass = new PoaNewContract();

    assertEquals(TxType.POA_NEW_CONTRACT, thisClass.getTxType());
  }


  @Test
  public void poaNewContractUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = Hex.encode("demoPublicKey___.___.___.___.___".getBytes(StandardCharsets.US_ASCII));
    ;
    String fromAddress = "demoAddress";
    String poa = "";
    int newChainId = 21;
    int newBlockHeight = 22;
    long newParameter = 23;
    List<Object[]> newSignodes = Arrays.<Object[]>asList(new Object[]{"sigPub", 42});
    String poaAddress = "demoAddress2";
    String poaReference = "thisReference";
    String protocol = "Proto";  // Base64(MsgPack(""))
    String metadata = "Meta";  // Base64(MsgPack(""))

    final String goodHash = "6f40d12c3f67c3acfcb078d88832ceef2929ee14a3faa1d00c524b176d29f82a";
    long timestamp = 1503405529L;

    IContractData testData1 = getTestData1();

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    JSONHashAccumulator hashList = new JSONHashAccumulator();
    hashList.add(testData1.encodeToMapForTxParameter());
    String s1 = new String(hashList.getBytes());

    AbstractTx rVal = PoaNewContract.poaNewContractUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractDictionary,
        protocol,
        metadata,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("Hashes must match", goodHash, rVal.getHash());

    // Alternative constructor

    rVal = PoaNewContract.poaNewContractUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        new DvpUkContractData(contractDictionary),
        protocol,
        metadata,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("Hashes must match", goodHash, rVal.getHash());

    // Alternative constructor

    rVal = PoaNewContract.poaNewContractUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractDictionary.toMap(),
        protocol,
        metadata,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    HashAccumulator hashlist = new JSONHashAccumulator();
    rVal.buildHash(hashlist);
    String h1 = hashlist.toString();

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("Hashes must match", goodHash, rVal.getHash());

    AbstractTx txCopy = new PoaNewContractTx((PoaNewContractTx) rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("Hashes must match", goodHash, txCopy.getHash());

    rVal.setHash(null);
    PoaNewContract thisClass = new PoaNewContract((PoaNewContractTx) rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaNewContractTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("Hashes must match", goodHash, txCopy.getHash());

  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}