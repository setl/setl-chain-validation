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
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaCommitToContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitReceive;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitment;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoaCommitToContractTest {


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



  private DvpUKCommitData getTestData1() {

    // {'receive': [[0, 'demoAddress'], [1, 'demoAddress']], 'party': ['2', '', ''], '__function': 'dvp_uk_commit', 'commitment': [[0, '', '']]}

    String fromPubKey = "demoPublicKey";
    String fromAddress = "Address1";

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[0]);

    rVal.setReceive(new Object[]{(new DvpCommitReceive(0L, "demoAddress")).encode(0), (new DvpCommitReceive(1L, "demoAddress")).encode(0)});
    rVal.setParty(new DvpCommitParty("2", "", ""));
    rVal.setCommitment(new DvpCommitment(0L, "", ""));

    return rVal;
  }


  @Test
  public void getTxType() {

    PoaCommitToContract thisClass = new PoaCommitToContract();

    assertEquals(TxType.POA_COMMIT_TO_CONTRACT, thisClass.getTxType());
  }


  @Test
  public void newContractUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "0000000000000000000000000000000000000000000000000000000000000000";
    String fromAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.NORMAL);
    String poaAddress = AddressUtil.publicKeyToAddress("0000000000000000000000000000000000000000000000000000000000000001", AddressType.NORMAL);
    String poaReference = "thisReference";
    String contractAddress = "3LxedBEGDVqR968QEfYcBai2m82Scr67NC";
    String poa = "";
    int newChainId = 21;
    int newBlockHeight = 22;
    long newParameter = 23;
    List<Object[]> newSignodes = Arrays.<Object[]>asList(new Object[]{"sigPub", 42});

    final String goodHash = "8eaf0f889020f9022ea171572fb8177bc5561fedeb0b558ff55d43b0795f2c51";
    long timestamp = 1510246814L;

    IContractData testData1 = getTestData1();

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    JSONHashAccumulator hashList = new JSONHashAccumulator();
    hashList.add(testData1.encodeToMapForTxParameter());
    String s1 = new String(hashList.getBytes());

    AbstractTx rVal = PoaCommitToContract.newCommitmentUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
        contractDictionary,
        protocol,
        metadata,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("Hashes must match", goodHash, rVal.getHash());

    // Alternative constructor

    rVal = PoaCommitToContract.newCommitmentUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
        new DvpUKCommitData(contractDictionary),
        protocol,
        metadata,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("Hashes must match", goodHash, rVal.getHash());

    // Alternative constructor

    rVal = PoaCommitToContract.newCommitmentUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
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

    assertEquals("", rVal.getHash(), goodHash);

    AbstractTx txCopy = new PoaCommitToContractTx((PoaCommitToContractTx) rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    PoaCommitToContract thisClass = new PoaCommitToContract((PoaCommitToContractTx) rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = PoaCommitToContractTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}