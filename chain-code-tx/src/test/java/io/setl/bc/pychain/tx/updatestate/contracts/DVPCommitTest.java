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
package io.setl.bc.pychain.tx.updatestate.contracts;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;
import static io.setl.bc.pychain.tx.updateevent.DVP.updateEvent;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_COMMITS;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitAuthorise;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitCancel;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitment;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.CommitToContract;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.NewContract;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.Pair;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class DVPCommitTest extends BaseTestClass {

  String contractFunction = CONTRACT_NAME_DVP_UK;

  final String privateKey1;

  final String publicKey1;

  String encumbranceToUse = "enc.";

  String[] events = new String[]{"time", "commit"};

  long expiryDate = Instant.now().getEpochSecond() + 600L;

  int isCompleted = 0;

  String issuingAddress = getRandomAddress();

  String metadata = "MetaData";

  boolean mustSign1 = false;

  boolean mustSign2 = false;

  long nextTimeEvent = 888888888L;

  final String party1Address;

  int party1Nonce = 0;

  // AddressUtil.verifyAddress(party1Address);

  int party2Nonce = 0;

  // Party 1
  String partyIdentifier1 = "party1";

  // Party 2
  String partyIdentifier2 = "party2";

  final String privateKey2;

  String protocol = "Protocol";

  final String publicKey2;

  final String party2Address;

  String signature1 = "";

  String signature2 = "";

  long startDate = 1L;

  /** New instance. */
  public DVPCommitTest() {
    Pair<String,String> hexPair = getRandomHexKeyPair();
    publicKey1 = hexPair.left();
    privateKey1 = hexPair.right();
    party1Address = AddressUtil.publicKeyToAddress(publicKey1,AddressType.NORMAL);

    hexPair = getRandomHexKeyPair();
    publicKey2 = hexPair.left();
    privateKey2 = hexPair.right();
    party2Address = AddressUtil.publicKeyToAddress(publicKey2,AddressType.NORMAL);
  }


  private DvpUKCommitData getCommitData1(String contractAddress, DvpUkContractData contractData) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[]{});

    rVal.setParty(new DvpCommitParty(partyIdentifier1, publicKey1, verifier.createSignatureB64(contractAddress, privateKey1)));

    rVal.setCommitment(
        new DvpCommitment(
            0L,
            publicKey1,
            verifier.createSignatureB64(computeHash(contractData.getParties().get(0).payList.get(0).objectToHashToSign(contractAddress)), privateKey1)
        )
    );

    return rVal;
  }


  private DvpUKCommitData getCommitData2(String contractAddress, DvpUkContractData contractData) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[]{});

    rVal.setParty(new DvpCommitParty(partyIdentifier2, publicKey2, verifier.createSignatureB64(contractAddress, privateKey2)));

    rVal.setCommitment(
        new DvpCommitment(
            0L,
            publicKey2,
            verifier.createSignatureB64(computeHash(contractData.getParties().get(1).payList.get(0).objectToHashToSign(contractAddress)), privateKey2)
        )
    );

    DvpCommitEncumbrance yy = new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "");
    yy.signature = verifier.createSignatureB64(computeHash(yy.objectToHashToSign(contractAddress)), privateKey1);
    rVal.setEncumbrance(yy);

    DvpCommitAuthorise aa = new DvpCommitAuthorise(publicKey1, "AuthID", "", "AuthMeta", false, 1);
    aa.signature = verifier.createSignatureB64(aa.stringToHashToSign(contractAddress), privateKey1);
    rVal.setAuthorise(aa);

    DvpCommitParameter pp = new DvpCommitParameter("P1", 7L, 0, publicKey1, "");
    pp.signature = verifier.createSignatureB64(pp.stringToHashToSign(contractAddress), privateKey1);
    rVal.setParameter(pp);

    return rVal;
  }


  private DvpUKCommitData getCommitData3NoParams(String contractAddress, DvpUkContractData contractData) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[]{});

    rVal.setParty(new DvpCommitParty(partyIdentifier2, publicKey2, verifier.createSignatureB64(contractAddress, privateKey2)));

    rVal.setCommitment(
        new DvpCommitment(
            0L,
            publicKey2,
            verifier.createSignatureB64(computeHash(contractData.getParties().get(1).payList.get(0).objectToHashToSign(contractAddress)), privateKey2)
        )
    );

    DvpCommitEncumbrance yy = new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "");
    yy.signature = verifier.createSignatureB64(computeHash(yy.objectToHashToSign(contractAddress)), privateKey1);
    rVal.setEncumbrance(yy);

    DvpCommitAuthorise aa = new DvpCommitAuthorise(publicKey1, "AuthID", "", "AuthMeta", false, 1);
    aa.signature = verifier.createSignatureB64(aa.stringToHashToSign(contractAddress), privateKey1);
    rVal.setAuthorise(aa);

    return rVal;
  }


  private DvpUKCommitData getCommitData4ParamOnly(String contractAddress, String authID) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[]{});

    DvpCommitParameter pp = new DvpCommitParameter("P1", 7L, 0, publicKey1, "");
    pp.signature = verifier.createSignatureB64(pp.stringToHashToSign(contractAddress), privateKey1);
    rVal.setParameter(pp);

    DvpCommitAuthorise thisAuthorise = new DvpCommitAuthorise(publicKey1, authID, "", "", false, 0);
    thisAuthorise.signature = verifier.createSignatureB64(thisAuthorise.stringToHashToSign(contractAddress), privateKey1);
    rVal.setAuthorise(thisAuthorise);
    return rVal;
  }


  private DvpUkContractData getContractTestData(String contractAddress) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        party1Address,
        publicKey1,
        signature1,
        mustSign1
    );

    if (contractAddress != null) {
      party1.signature = verifier.createSignatureB64(contractAddress, privateKey1);
    }

    DvpPayItem p1p1 = new DvpPayItem(
        party1Address,
        "NS1",
        "Class1",
        "432+0",
        publicKey1,
        "",
        false,
        "meta1",
        ""

    );

    if (contractAddress != null) {
      p1p1.signature = verifier.createSignatureB64(computeHash(p1p1.objectToHashToSign(contractAddress)), privateKey1);
    }

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        party1Address,
        "NS2",
        "Class2",
        431L
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        party2Address,
        publicKey2,
        signature2,
        mustSign2
    );

    if (contractAddress != null) {
      party2.signature = verifier.createSignatureB64(contractAddress, privateKey2);
    }

    DvpPayItem p2p1 = new DvpPayItem(
        party2Address,
        "NS2",
        "Class2",
        431L,
        publicKey2,
        "",
        false,
        "meta2",
        ""
    );

    if (contractAddress != null) {
      p2p1.signature = verifier.createSignatureB64(computeHash(p2p1.objectToHashToSign(contractAddress)), privateKey2);
    }

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        party2Address,
        "NS1",
        "Class1",
        432L
    ));

    DvpUkContractData testData = new DvpUkContractData(
        "",
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

    DvpAddEncumbrance yy = new DvpAddEncumbrance("AssetID", "Reference", 100L, publicKey1, "");
    yy.beneficiaries.add(new EncumbranceDetail("Address", 0L, 999999L));
    if (contractAddress != null) {
      yy.setSignature(verifier.createSignatureB64(computeHash(yy.objectToHashToSign(contractAddress)), privateKey1));
    }
    testData.addAddEncumbrance(yy);

    DvpAuthorisation dvpAuthSig = new DvpAuthorisation(publicKey1, "AuthID", "", "AuthMeta", false, 1);
    if (contractAddress != null) {
      dvpAuthSig.setSignature(verifier.createSignatureB64(dvpAuthSig.stringToHashToSign(contractAddress), privateKey1));
    }
    testData.addAuthorisation(dvpAuthSig);

    DvpParameter par1 = new DvpParameter(publicKey1, 7L, 0, 0, 0, "");
    if (contractAddress != null) {
      par1.setSignature(verifier.createSignatureB64(par1.stringToHashToSign("P1", contractAddress), privateKey1));
    }
    testData.addParameter("P1", par1);

    testData.setEncumbrance(new DvpEncumbrance(true, contractAddress));
    return testData;
  }


  @Test
  public void updatestate1() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;
    int addressNonce = 0;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    String toAddress2 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot().createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, party1Address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, party2Address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();
    int savedNonce = addressNonce;

    // Check Issued balances :

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(party1Address);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    // Contract without signatures :

    DvpUkContractData testData1 = getContractTestData(null);
    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    NewContractTx contractTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractDictionary,
        poa
    );

    assertTrue(UpdateState.update(contractTX, s0, contractTX.getTimestamp(), contractTX.getPriority(), false));

    DvpUKCommitData testCommitData1 = getCommitData1(contractTX.getContractAddress(), testData1);

    MutableMerkle<ContractEntry> contractsList = s0.getContracts();
    ContractEntry thisContractEntry = contractsList.find(contractTX.getContractAddress());
    DvpUkContractData resultData = (DvpUkContractData) thisContractEntry.getContractData();

    assertNotEquals(resultData.getParties().get(0).signature, testCommitData1.getParty().signature);
    assertNotEquals(resultData.getParties().get(0).payList.get(0).signature, testCommitData1.getCommitment().get(0).signature);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertEquals(1, s0.getContractEvents().size());
    resultData = (DvpUkContractData) thisContractEntry.getContractData();

    assertEquals(resultData.getParties().get(0).signature, testCommitData1.getParty().signature);
    assertEquals(resultData.getParties().get(0).payList.get(0).signature, testCommitData1.getCommitment().get(0).signature);

    for (EventData thisEvent : s0.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    resultData = (DvpUkContractData) thisContractEntry.getContractData();
    assertEquals(0, resultData.get__completed());

    // Some to fail :

    testCommitData1 = getCommitData2(contractTX.getContractAddress(), testData1);

    // Bad ContractAddress

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        "BadcontractAddress",
        testCommitData1,
        poa
    );

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    // Bad TX Time
    assertFalse(UpdateState.update(thisTX, s0, 1, thisTX.getPriority(), false));

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey2,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    // Address / PubKey mismatch
    assertFalse(UpdateState.update(thisTX, s0, 1, thisTX.getPriority(), false));

    // Finish off

    testCommitData1 = getCommitData2(contractTX.getContractAddress(), testData1);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();

    // Pre Event false.

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertNotEquals(newValue, issueAmount - 432L);

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertNotEquals(432L, newValue);

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertNotEquals(newValue, issueAmount - 431L);

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertNotEquals(431L, newValue);

    assertTrue(s0.getEffectiveTXList().size() != 2);

    s0.commit();
    s0 = state1.createSnapshot();

    // Check Issued balances :

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(party1Address);

    for (EventData thisEvent : state1.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    // Check Balances moved :

    assetBalances = s0.getAssetBalances();

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(newValue, new Balance(issueAmount - 432L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(new Balance(432L), newValue);

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(newValue, new Balance(issueAmount - 431L));

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(new Balance(431L), newValue);

    assertEquals(2, s0.getEffectiveTXList().size());

    // Now some Events that don't work.
    // s0 = state1.createSnapshot();

    // For 'time' does not care that the contract is not there
    assertSame(updateEvent(s0, thisTX.getTimestamp(), "BadAddress", new EventData("BadAddress", "", "time", ""), false).success, SuccessType.PASS);

    // For !'time' does care that the contract is not there, but updateEvent returns PASS anyway!
    assertSame(updateEvent(s0, thisTX.getTimestamp(), "BadAddress", new EventData("BadAddress", "", "space", ""), false).success, SuccessType.PASS);

    ContractEntry contractEntry = s0.getContracts().findAndMarkUpdated(contractTX.getContractAddress());
    assertNotNull(contractEntry);

    // 'time' event on a completed contract will remove it.
    assertSame(updateEvent(s0, thisTX.getTimestamp(), contractTX.getContractAddress(), new EventData(contractTX.getContractAddress(), "", "time",
        ""), false).success, SuccessType.PASS);

    // time event will have deleted the Contract. Check.

    contractEntry = s0.getContracts().findAndMarkUpdated(contractTX.getContractAddress());
    assertNull(contractEntry);
  }


  @Test
  public void updatestate2() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;
    int addressNonce = 0;

    final MessageSignerVerifier verifier = MessageVerifierFactory.get();

    Pair<String,String> hexPair = getRandomHexKeyPair();
    final String hPrivHey = hexPair.right();
    String pubKey = hexPair.left();

    String pubKey2 = getRandomPublicKey();

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    String toAddress2 = getRandomAddress();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, party1Address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, party2Address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();
    int savedNonce = addressNonce;

    // Check Issued balances :

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(party1Address);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    fromAddressEntry = assetBalances.findAndMarkUpdated(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(newValue, issueAmount);

    // Contract without signatures :

    DvpUkContractData testData1 = getContractTestData(null);
    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    NewContractTx contractTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractDictionary,
        poa
    );

    assertTrue(UpdateState.update(contractTX, s0, contractTX.getTimestamp(), contractTX.getPriority(), false));

    DvpUKCommitData testCommitData1 = getCommitData1(contractTX.getContractAddress(), testData1);

    MutableMerkle<ContractEntry> contractsList = s0.getContracts();
    ContractEntry thisContractEntry = contractsList.find(contractTX.getContractAddress());
    DvpUkContractData resultData = (DvpUkContractData) thisContractEntry.getContractData();

    assertNotEquals(resultData.getParties().get(0).signature, testCommitData1.getParty().signature);
    assertNotEquals(resultData.getParties().get(0).payList.get(0).signature, testCommitData1.getCommitment().get(0).signature);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertEquals(1, s0.getContractEvents().size());
    resultData = (DvpUkContractData) thisContractEntry.getContractData();

    assertEquals(resultData.getParties().get(0).signature, testCommitData1.getParty().signature);
    assertEquals(resultData.getParties().get(0).payList.get(0).signature, testCommitData1.getCommitment().get(0).signature);

    for (EventData thisEvent : s0.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    resultData = (DvpUkContractData) thisContractEntry.getContractData();
    assertEquals(0, resultData.get__completed());

    s0.commit();
    s0 = state1.createSnapshot();
    assetBalances = s0.getAssetBalances();

    // Some to fail :

    testCommitData1 = getCommitData2(contractTX.getContractAddress(), testData1);

    // Bad ContractAddress

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        "BadcontractAddress",
        testCommitData1,
        poa
    );

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad TX Time

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertFalse(UpdateState.update(thisTX, s0, 1, thisTX.getPriority(), false));

    // Address / PubKey mismatch

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey2,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertFalse(UpdateState.update(thisTX, s0, 1, thisTX.getPriority(), false));

    // Address permissions

    s0.setConfigValue("authorisebyaddress", 1);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission

    fromAddressEntry = assetBalances.find(address);
    fromAddressEntry.setAddressPermissions(AP_COMMITS);

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();

    // Test Cancel

    testCommitData1 = new DvpUKCommitData(new Object[]{});

    DvpCommitCancel thisCancel = new DvpCommitCancel(pubKey, "");

    thisCancel.signature = verifier.createSignatureB64("Bad Sig Data", hPrivHey);

    testCommitData1.setCancel(thisCancel);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    // Bad sig data.

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Good Sig data :

    thisCancel.signature = verifier.createSignatureB64(thisCancel.stringToHashToSign(contractTX.getContractAddress()), hPrivHey);
    testCommitData1.setCancel(thisCancel);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    contractsList = s0.getContracts();
    thisContractEntry = contractsList.findAndMarkUpdated(contractTX.getContractAddress());

    assertTrue(((DvpUkContractData) thisContractEntry.getContractData()).get__completed() != 0);
    assertEquals(((DvpUkContractData) thisContractEntry.getContractData()).get__canceltime(), thisTX.getTimestamp());

    // Finish off

    s0 = state1.createSnapshot();

    testCommitData1 = getCommitData2(contractTX.getContractAddress(), testData1);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();

    // Pre Event false.

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertFalse(newValue.equalTo(issueAmount - 432L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertFalse(newValue.equalTo(432L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertFalse(newValue.equalTo(issueAmount - 431L));

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertFalse(newValue.equalTo(431L));

    assertTrue(s0.getEffectiveTXList().size() != 2);

    s0.commit();
    s0 = state1.createSnapshot();

    // Check Issued balances :

    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.findAndMarkUpdated(party1Address);

    for (EventData thisEvent : state1.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    // Check Balances moved :

    assetBalances = s0.getAssetBalances();

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - 432L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(432L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - 431L));

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(431L));

    assertEquals(2, s0.getEffectiveTXList().size());

  }


  @Test
  public void updatestate_commit_to_multiple() throws Exception {
    /*
    Test to try to check the commit-to-multiple-contracts functionality of the DVPCommit transaction
    This functionality allow the user to specify multiple contract addresses when (and only when) setting parameter
    values. The parameters need to have been defined as 'not contract specific' as this means that the parameter signature does
    not include the contract address.

    Having been created the contracts are committed to by CommitData1 and CommitData3. This leaves them with only the parameters
    to be provided. This is done for both contracts by a single Tx using CommitData4.
     */

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String authID = "MultiAuth";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;
    int addressNonce = 0;


    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, pubKey, address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, party1Address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, party2Address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();
    int savedNonce = addressNonce;

    // Check Issued balances :

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(party1Address);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    fromAddressEntry = assetBalances.findAndMarkUpdated(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    // Contract without signatures :

    DvpUkContractData testData1 = getContractTestData(null);

    testData1.addAuthorisation(new DvpAuthorisation(party1Address, authID, "", metadata, false, 0));

    // Contract 1

    MPWrappedMap<String, Object> contractDictionary1 = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    NewContractTx contractTX1 = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractDictionary1,
        poa
    );

    assertTrue(UpdateState.update(contractTX1, s0, contractTX1.getTimestamp(), contractTX1.getPriority(), false));

    // Contract 2

    MPWrappedMap<String, Object> contractDictionary2 = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    NewContractTx contractTX2 = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractDictionary1,
        poa
    );

    assertTrue(UpdateState.update(contractTX2, s0, contractTX2.getTimestamp(), contractTX2.getPriority(), false));

    // Commit 1.1

    DvpUKCommitData testCommitData1 = getCommitData1(contractTX1.getContractAddress(), testData1);

    MutableMerkle<ContractEntry> contractsList = s0.getContracts();
    ContractEntry thisContractEntry1 = contractsList.find(contractTX1.getContractAddress());
    DvpUkContractData resultData1 = (DvpUkContractData) thisContractEntry1.getContractData();

    assertNotEquals(resultData1.getParties().get(0).signature, testCommitData1.getParty().signature);
    assertNotEquals(resultData1.getParties().get(0).payList.get(0).signature, testCommitData1.getCommitment().get(0).signature);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX1.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertEquals(1, s0.getContractEvents().size());
    resultData1 = (DvpUkContractData) thisContractEntry1.getContractData();

    assertEquals(resultData1.getParties().get(0).signature, testCommitData1.getParty().signature);
    assertEquals(resultData1.getParties().get(0).payList.get(0).signature, testCommitData1.getCommitment().get(0).signature);

    resultData1 = (DvpUkContractData) thisContractEntry1.getContractData();
    assertEquals(0, resultData1.get__completed());

    // Commit 1.2

    DvpUKCommitData testCommitData2 = getCommitData1(contractTX2.getContractAddress(), testData1);

    ContractEntry thisContractEntry2 = contractsList.find(contractTX2.getContractAddress());
    DvpUkContractData resultData = (DvpUkContractData) thisContractEntry2.getContractData();

    assertNotEquals(resultData.getParties().get(0).signature, testCommitData2.getParty().signature);
    assertNotEquals(resultData.getParties().get(0).payList.get(0).signature, testCommitData2.getCommitment().get(0).signature);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX2.getContractAddress(),
        testCommitData2,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertEquals(2, s0.getContractEvents().size());
    resultData = (DvpUkContractData) thisContractEntry2.getContractData();

    assertEquals(resultData.getParties().get(0).signature, testCommitData2.getParty().signature);
    assertEquals(resultData.getParties().get(0).payList.get(0).signature, testCommitData2.getCommitment().get(0).signature);

    resultData = (DvpUkContractData) thisContractEntry2.getContractData();
    assertEquals(0, resultData.get__completed());

    //

    for (EventData thisEvent : s0.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    // Ok, First commit done....

    s0.commit();
    s0 = state1.createSnapshot();
    assetBalances = s0.getAssetBalances();

    // Commit 2.1

    testCommitData1 = getCommitData3NoParams(contractTX1.getContractAddress(), testData1);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX1.getContractAddress(),
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Commit 2.2

    testCommitData2 = getCommitData3NoParams(contractTX2.getContractAddress(), testData1);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractTX2.getContractAddress(),
        testCommitData2,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assetBalances = s0.getAssetBalances();

    // Process events : To check that assets will not yet be moved.

    for (EventData thisEvent : state1.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    // Commit 3
    // Commit to multiple contracts from a single commit Tx.

    testCommitData1 = getCommitData4ParamOnly(contractTX1.getContractAddress(), authID);

    thisTX = CommitToContract.newCommitmentUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        new String[]{contractTX1.getContractAddress(), contractTX2.getContractAddress()},
        testCommitData1,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Pre Event : Assets have Not moved.

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(0L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(0L));

    // Should be No Effective transactions now...

    assertEquals(0, s0.getEffectiveTXList().size());

    s0.commit();
    s0 = state1.createSnapshot();

    // Process events :

    assetBalances = s0.getAssetBalances();

    for (EventData thisEvent : state1.getContractEvents()) {
      assertSame(updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success, SuccessType.PASS);
    }

    // Check Balances have moved :

    assetBalances = s0.getAssetBalances();

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - (2 * 432L)));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(2 * 432L));

    fromAddressEntry = assetBalances.find(party2Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - (2 * 431L)));

    fromAddressEntry = assetBalances.find(party1Address);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(2 * 431L));

    // Should be 4 Effective transactions now...

    assertEquals(4, s0.getEffectiveTXList().size());


  }

}
