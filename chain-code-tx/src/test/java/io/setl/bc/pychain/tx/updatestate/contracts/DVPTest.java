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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;
import static io.setl.bc.pychain.tx.updateevent.DVP.updateEvent;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CONTRACT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.junit.Test;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
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
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.NewContract;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.bc.pychain.tx.updatestate.Encumber;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.SuccessType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;

@SuppressWarnings("unlikely-arg-type")
public class DVPTest extends BaseTestClass {

  private final Entity entity1 = new Entity();

  private final Entity entity2 = new Entity();
  
  private final Entity entity3 = new Entity();
  
  private final Entity author = new Entity();

  private final Entity master = new Entity();

  private String contractFunction = CONTRACT_NAME_DVP_UK;

  private String encumbranceToUse = "enc.";

  private String[] events = new String[]{"time", "commit"};

  private long expiryDate = Instant.now().getEpochSecond() + 600L;

  private int isCompleted = 0;

  private String issuingAddress = getRandomAddress();

  // AddressUtil.verifyAddress(entity1.address);

  private long nextTimeEvent = 888888888L;

  private int party1Nonce = 0;

  private int party2Nonce = 0;

  // Party 1
  private String partyIdentifier1 = "party1";

  // Party 2
  private String partyIdentifier2 = "party2";

  // Party 3
  private String partyIdentifier3 = "party3";

  private String protocol = "Protocol";

  private long startDate = 1L;



  @Test
  public void dvpBlueScreen() throws Exception {
    /*
    Replicates the standard 'Blue Screen' Contract : 2 parties doing DvP plus a third taxman.
    Parties 1 & 2 are fully signed, party 3 (recipient only) does not need to sign.
    */

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long taxAmount = 50;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    final MessageSignerVerifier verifier = MessageVerifierFactory.get();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    //
    // Contract without signatures :

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        entity1.address,
        entity1.publicHex,
        "",
        false
    );

    DvpPayItem p1p1 = new DvpPayItem(
        entity1.address,
        namespace,
        classname,
        transferAmount,
        entity1.publicHex,
        "",
        false,
        "meta1",
        ""
    );

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        entity1.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        ""
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount - taxAmount
    ));

    DvpParty party3 = new DvpParty(
        partyIdentifier3,
        entity3.address,
        entity3.publicHex,
        "",
        false
    );

    party3.receiveList.add(new DvpReceiveItem(
        entity3.address,
        namespace,
        classname,
        taxAmount
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
        null
    );

    testData.addParty(party1);
    testData.addParty(party2);
    testData.addParty(party3);

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    NewContractTx contractTx = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        author.publicHex,
        author.address,
        contractDictionary,
        poa
    );

    String contractAddress = contractTx.getContractAddress();

    // Sign party 1
    party1.signature = verifier.createSignatureB64(contractAddress, entity1.privateHex);
    party2.signature = verifier.createSignatureB64(contractAddress, entity2.privateHex);

    p1p1.signature = verifier.createSignatureB64(computeHash(p1p1.objectToHashToSign(contractAddress)), entity1.privateHex);

    contractTx.setContractDictionary(new MPWrappedMap<>(testData.encodeToMapForTxParameter()));

    // P2P1 not signed, Tx completes OK, but is not 'ok to proceed' so there is no Contract event. :

    assertTrue(UpdateState.update(contractTx, s0, contractTx.getTimestamp(), contractTx.getPriority(), false));
    assertEquals(0, s0.getContractEvents().size());
    s0 = state1.createSnapshot();

    // Sign it.

    p2p1.signature = verifier.createSignatureB64(computeHash(p2p1.objectToHashToSign(contractAddress)), entity2.privateHex);
    contractTx.setContractDictionary(new MPWrappedMap<>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(contractTx, s0, contractTx.getTimestamp(), contractTx.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    //

    AddressEntry fromAddressEntry = assetBalances.find(entity2.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    //

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, contractTx.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - transferAmount));

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount - taxAmount));

    fromAddressEntry = assetBalances.find(entity3.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(taxAmount));

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - transferAmount2));

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount2));

    // Check Effective Tx Count. Could also check value !

    assertEquals(3, s0.getEffectiveTXList().size());

  }


  @Test
  public void dvpEncumbranceExchange() throws Exception {
    /*
    Standard 'exchange' Encumbrance use case : Two parties encumber in favour of a third party, the exchange, that organises
    and executes the DVP Contract. No further signing is required by the Asset-Exchanging parties.
     */

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    final String fullAssetID2 = namespace2 + "|" + classname2;
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // OK Add encumbrances

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    // Contract without signatures :

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        entity1.address,
        entity1.publicHex,
        "",
        false
    );

    DvpPayItem p1p1 = new DvpPayItem(
        entity1.address,
        namespace,
        classname,
        transferAmount,
        entity1.publicHex,
        "",
        false,
        "meta1",
        ""
    );

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        entity1.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        ""
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount
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

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        author.publicHex,
        author.address,
        contractDictionary,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    AddressEntry fromAddressEntry = assetBalances.find(entity1.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - transferAmount));

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount));

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount - transferAmount2));

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(transferAmount2));

    // Check Effective Tx Count. Could also check value !

    assertEquals(2, s0.getEffectiveTXList().size());

    // Check Encumbrances consumed

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertTrue(thisEntry.amount.equalTo(issueAmount - transferAmount));

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertTrue(thisEntry.amount.equalTo(issueAmount - transferAmount2));


  }


  @Test
  public void dvpEncumbranceExchangeSpecific() throws Exception {
    /*
    Standard 'exchange' Encumbrance use case : Two parties encumber in favour of a third party, the exchange, that organises
    and executes the DVP Contract. No further signing is required by the Asset-Exchanging parties.
    The Encumbrances in use are specified on the payment lines.
    Initially, the Encumbrances are not fulfillable but require additional assets.
     */

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    final String fullAssetID2 = namespace2 + "|" + classname2;
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    final String encumbranceToUseP1 = "enc1";
    final String encumbranceToUseP2 = "enc2";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // OK Add encumbrances

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};

    // NOTE : Encumbrances to 'encumbranceToUse' will mean that there is no asset available to 'encumbranceToUseP1' or 'encumbranceToUseP2'

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUseP1);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUseP2);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    // Contract without signatures :

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        entity1.address,
        entity1.publicHex,
        "",
        false
    );

    DvpPayItem p1p1 = new DvpPayItem(
        entity1.address,
        namespace,
        classname,
        transferAmount,
        entity1.publicHex,
        "",
        false,
        "meta1",
        encumbranceToUseP1
    );

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        entity1.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        encumbranceToUseP2
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount
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

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        author.publicHex,
        author.address,
        contractDictionary,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have not moved

    AddressEntry fromAddressEntry = assetBalances.find(entity1.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(0L));

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(0L));

    // More assets

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Try again.

    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals((issueAmount * 2) - transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals((issueAmount * 2) - transferAmount2, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(transferAmount2, newValue.longValue());

    // Check Effective Tx Count. Could also check value !

    assertEquals(2, s0.getEffectiveTXList().size());

    // Check Encumbrances consumed, or not, as the case may be.

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertTrue(thisEntry.amount.equalTo(issueAmount));

    thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUseP1);

    assertTrue(thisEntry.amount.equalTo(issueAmount - transferAmount));

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertTrue(thisEntry.amount.equalTo(issueAmount));

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUseP2);

    assertTrue(thisEntry.amount.equalTo(issueAmount - transferAmount2));


  }


  @Test
  public void dvpEncumbranceExchangeSpecificWithLock() throws Exception {
    /*
    Standard 'exchange' Encumbrance use case : Two parties encumber in favour of a third party, the exchange, that organises
    and executes the DVP Contract. No further signing is required by the Asset-Exchanging parties.
    The Encumbrances in use are specified on the payment lines.
    Initially, the Encumbrances are not fulfillable but require additional assets.
     */

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    final String fullAssetID2 = namespace2 + "|" + classname2;
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    final String encumbranceToUseP1 = "enc1";
    final String encumbranceToUseP2 = "enc2";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount, "",
        "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // OK Add encumbrances

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};

    // NOTE : Encumbrances to 'encumbranceToUse' will mean that there is no asset available to 'encumbranceToUseP1' or 'encumbranceToUseP2'

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "",
            "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUseP1);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUseP2);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    // Contract without signatures :

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        entity1.address,
        entity1.publicHex,
        "",
        false
    );

    DvpPayItem p1p1 = new DvpPayItem(
        entity1.address,
        namespace,
        classname,
        transferAmount,
        entity1.publicHex,
        "",
        false,
        "meta1",
        encumbranceToUseP1
    );

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        entity1.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        encumbranceToUseP2
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount
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

    DvpAddEncumbrance lockEncumbrance = new DvpAddEncumbrance(fullAssetID, "lockReference", transferAmount, entity2.address, "");
    lockEncumbrance.administrators.add(administrators[0]);

    testData.addParty(party1);
    testData.addParty(party2);
    testData.addAddEncumbrance(lockEncumbrance);

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        author.publicHex,
        author.address,
        contractDictionary,
        poa
    );

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have not moved

    AddressEntry fromAddressEntry = assetBalances.find(entity1.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(issueAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(new Balance(0L), newValue);

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(new Balance(0L), newValue);

    // More assets

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount,
            "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Try again.

    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals((issueAmount * 2) - transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals((issueAmount * 2) - transferAmount2, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(transferAmount2, newValue.longValue());

    // Check Effective Tx Count. Could also check value !

    assertEquals(2, s0.getEffectiveTXList().size());

    // Check Encumbrances consumed, or not, as the case may be.

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUseP1);

    assertEquals(issueAmount - transferAmount, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUseP2);

    assertEquals(issueAmount - transferAmount2, thisEntry.amount.longValue());

    // New 'Lock' Encumbrance

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getAggregateByReference("lockReference");

    assertEquals(transferAmount, thisEntry.amount.longValue());
    assertEquals(administrators[0], thisEntry.getAdministrators().get(0));
    assertTrue(thisEntry.getBeneficiaries().isEmpty());
    assertEquals(1, thisEntry.getAdministrators().size());

  }


  @Test
  public void dvpEncumbranceExchangeSpecificWithParameterLock() throws Exception {
    /*
    Standard 'exchange' Encumbrance use case : Two parties encumber in favour of a third party, the exchange, that organises
    and executes the DVP Contract. No further signing is required by the Asset-Exchanging parties.
    The Encumbrances in use are specified on the payment lines.
    Initially, the Encumbrances are not fulfillable but require additional assets.
     */

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    final String fullAssetID2 = namespace2 + "|" + classname2;
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    final String encumbranceToUseP1 = "enc1";
    final String encumbranceToUseP2 = "enc2";
    final String parameterName = "Param1";

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // OK Add encumbrances

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};

    // NOTE : Encumbrances to 'encumbranceToUse' will mean that there is no asset available to 'encumbranceToUseP1' or 'encumbranceToUseP2'

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUseP1);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUseP2);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    // Contract without signatures :

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        entity1.address,
        entity1.publicHex,
        "",
        false
    );

    DvpPayItem p1p1 = new DvpPayItem(
        entity1.address,
        namespace,
        classname,
        transferAmount,
        entity1.publicHex,
        "",
        false,
        "meta1",
        encumbranceToUseP1
    );

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        entity1.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        encumbranceToUseP2
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount
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

    DvpAddEncumbrance lockEncumbrance = new DvpAddEncumbrance(fullAssetID, "lockReference", "" + parameterName + "*2", entity2.address, "");
    lockEncumbrance.administrators.add(administrators[0]);

    testData.addParty(party1);
    testData.addParty(party2);
    testData.addAddEncumbrance(lockEncumbrance);

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        author.publicHex,
        author.address,
        contractDictionary,
        poa
    );

    // Will fail as the parameter is undefined.
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add parameter
    testData.addParameter(parameterName, new DvpParameter("", transferAmount / 2, 0, 0, 1, ""));
    contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        author.publicHex,
        author.address,
        contractDictionary,
        poa
    );

    // Try again
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have not moved

    AddressEntry fromAddressEntry = assetBalances.find(entity1.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(issueAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(new Balance(0L), newValue);

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(new Balance(0L), newValue);

    // More assets

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, entity1.address, issueAmount,
        "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Try again.

    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals((issueAmount * 2) - transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals((issueAmount * 2) - transferAmount2, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(transferAmount2, newValue.longValue());

    // Check Effective Tx Count. Could also check value !

    assertEquals(2, s0.getEffectiveTXList().size());

    // Check Encumbrances consumed, or not, as the case may be.

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(entity1.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUseP1);

    assertEquals(issueAmount - transferAmount, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUseP2);

    assertEquals(issueAmount - transferAmount2, thisEntry.amount.longValue());

    // New 'Lock' Encumbrance

    thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getAggregateByReference("lockReference");

    assertEquals(transferAmount, thisEntry.amount.longValue());
    assertEquals(administrators[0], thisEntry.getAdministrators().get(0));
    assertTrue(thisEntry.getBeneficiaries().isEmpty());
    assertEquals(1, thisEntry.getAdministrators().size());

  }


  @Test
  public void dvpIssuerEncumbranceVsEncumbrance() throws Exception {

    /*
    Tests a contract where the authoring address is an asset owner and they issue assets through
    the contract to a counterparty.
    In the presence of an encumbrance, the issuing party and payment need not be signed.
     */
    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    final String fullAssetID2 = namespace2 + "|" + classname2;
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();

    // OK Add encumbrances

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    // Contract without signatures :

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

    NewContractTx thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        author.publicHex,
        author.address,
        testData,
        poa
    );

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        master.address,
        master.publicHex,
        "",
        false
    );

    DvpPayItem p1p1 = new DvpPayItem(
        master.address,
        namespace,
        classname,
        transferAmount,
        master.publicHex,
        "",
        true,
        "meta1",
        ""
    );

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        master.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        ""
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount
    ));

    testData.addParty(party1);
    testData.addParty(party2);

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisnewTX.setContractDictionary(contractDictionary);

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    AddressEntry fromAddressEntry = assetBalances.find(master.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(-transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount - transferAmount2, newValue.longValue());

    fromAddressEntry = assetBalances.find(master.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(transferAmount2 - issueAmount, newValue.longValue());

    // Check Effective Tx Count. Could also check value !

    assertEquals(2, s0.getEffectiveTXList().size());

    // Check Encumbrances consumed

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount - transferAmount2, thisEntry.amount.longValue());

    thisAddressEncumbrance = s0.getEncumbrances().find(master.address);
    thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount - transferAmount, thisEntry.amount.longValue());

  }


  @Test
  public void dvpIssuerVsEncumbrance() throws Exception {

    /*
    Tests a contract where the authoring address is an asset owner and they issue assets through
    the contract to a counterparty.
    In the absence of an encumbrance, the issuing party and payment must be signed.
     */
    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    final String namespace = "NS1";
    final String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID2 = namespace2 + "|" + classname2;
    final int chainID = 16;
    final long issueAmount = 1000;
    final long transferAmount = 400;
    final long transferAmount2 = 450;
    int addressNonce = 0;

    final MessageSignerVerifier verifier = MessageVerifierFactory.get();

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();
    StateSnapshot s0 = state1.createSnapshot();

    // Register Namespaces & Classes, issue assets.

    AbstractTx thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, master.publicHex, master.address, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace, classname, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, metadata, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue
        .assetIssueUnsigned(chainID, addressNonce++, master.publicHex, master.address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // OK Add encumbrances

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};
    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(author.address, 0L, Instant.now().getEpochSecond() + 9999L)};

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", encumbranceToUse);
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    //
    // Contract with few signatures :

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

    NewContractTx thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        author.publicHex,
        author.address,
        testData,
        poa
    );

    String contractAddress = thisnewTX.getContractAddress();

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        master.address,
        master.publicHex,
        "",
        false
    );

    if (contractAddress != null) {
      party1.signature = verifier.createSignatureB64(contractAddress, master.privateHex);
    }

    DvpPayItem p1p1 = new DvpPayItem(
        master.address,
        namespace,
        classname,
        transferAmount,
        master.publicHex,
        "",
        true,
        "meta1",
        ""
    );

    if (contractAddress != null) {
      p1p1.signature = verifier.createSignatureB64(computeHash(p1p1.objectToHashToSign(contractAddress)), master.privateKey);
    }

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        master.address,
        namespace2,
        classname2,
        transferAmount2
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        namespace2,
        classname2,
        transferAmount2,
        entity2.publicHex,
        "",
        false,
        "meta1",
        ""
    );

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        namespace,
        classname,
        transferAmount
    ));

    testData.addParty(party1);
    testData.addParty(party2);

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData.encodeToMapForTxParameter());

    thisnewTX.setContractDictionary(contractDictionary);

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Apply Contract Event.
    s0.commit();
    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(1, s0.getContractEvents().size());

    ReturnTuple rt;
    for (EventData thisEvent : s0.getContractEvents()) {
      rt = updateEvent(s0, thisTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false);
      assertSame(SuccessType.PASS, rt.success);
    }

    // Check Assets have moved

    AddressEntry fromAddressEntry = assetBalances.find(master.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(-transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(transferAmount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount - transferAmount2, newValue.longValue());

    fromAddressEntry = assetBalances.find(master.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(transferAmount2 - issueAmount, newValue.longValue());

    // Check Effective Tx Count. Could also check value !

    assertEquals(2, s0.getEffectiveTXList().size());

    // Check Encumbrances consumed

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(entity2.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID2);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getAggregateByReference(encumbranceToUse);

    assertEquals(issueAmount - transferAmount2, thisEntry.amount.longValue());

  }


  @Test
  public void dvptidybalance() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    int chainID = 16;
    final long issueAmount = 1000;
    long asset1Amount = 432L;
    int addressNonce = 0;

    String pubKey = getRandomPublicKey();

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

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, entity1.address, asset1Amount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // Check Issued balances :

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(entity1.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertTrue(newValue.equalTo(asset1Amount));

    fromAddressEntry = assetBalances.findAndMarkUpdated(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertTrue(newValue.equalTo(issueAmount));

    // Contract without signatures :

    DvpUkContractData testData1 = getTestData1(null);

    NewContractTx thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        pubKey,
        address,
        testData1,
        poa
    );

    // Contract With signatures :
    // Get Signed test data.

    testData1 = getTestData1(thisnewTX.getContractAddress());

    thisnewTX.setContractDictionary(new MPWrappedMap<>(testData1.encodeToMapForTxParameter()));

    assertEquals(0, s0.getContractEvents().size());

    // Create Contract OK.

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Check Event has been lodged.

    assertEquals(1, s0.getContractEvents().size());
    assertEquals(((EventData) s0.getContractEvents().toArray()[0]).getEventAddress(), thisnewTX.getContractAddress());

    // Set State.

    s0.commit();

    assetBalances = s0.getAssetBalances();

    // Apply Contract Event.

    assertEquals(1, s0.getContractEvents().size());

    for (EventData thisEvent : s0.getContractEvents()) {
      assertSame(SuccessType.PASS, updateEvent(s0, thisnewTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success);
    }

    // Check Balances moved :
    // Check entity1.address, Balance has been removed. (Now null).

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.get(namespace + "|" + classname);
    assertNull(newValue);

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(asset1Amount, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount - 431L, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(new Balance(431L), newValue);

    assertEquals(2, s0.getEffectiveTXList().size());


  }


  private DvpUkContractData getTestData1(String contractAddress) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        entity1.address,
        entity1.publicHex,
        "",
        false
    );

    if (contractAddress != null) {
      party1.signature = verifier.createSignatureB64(contractAddress, entity1.privateHex);
    }

    DvpPayItem p1p1 = new DvpPayItem(
        entity1.address,
        "NS1",
        "Class1",
        432L,
        entity1.publicHex,
        "",
        false,
        "meta1",
        ""

    );

    if (contractAddress != null) {
      p1p1.signature = verifier.createSignatureB64(computeHash(p1p1.objectToHashToSign(contractAddress)), entity1.privateHex);
    }

    party1.payList.add(p1p1);

    party1.receiveList.add(new DvpReceiveItem(
        entity1.address,
        "NS2",
        "Class2",
        431L
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        entity2.address,
        entity2.publicHex,
        "",
        false
    );

    if (contractAddress != null) {
      party2.signature = verifier.createSignatureB64(contractAddress, entity2.privateHex);
    }

    DvpPayItem p2p1 = new DvpPayItem(
        entity2.address,
        "NS2",
        "Class2",
        431L,
        entity2.publicHex,
        "",
        false,
        "meta2",
        ""
    );

    if (contractAddress != null) {
      p2p1.signature = verifier.createSignatureB64(computeHash(p2p1.objectToHashToSign(contractAddress)), entity2.privateHex);
    }

    party2.payList.add(p2p1);

    party2.receiveList.add(new DvpReceiveItem(
        entity2.address,
        "NS1",
        "Class1",
        432L
    ));

    String metadata = "MetaData";
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

    DvpAddEncumbrance yy = new DvpAddEncumbrance("AssetID", "Reference", 100L, entity1.publicHex, "");
    yy.beneficiaries.add(new EncumbranceDetail("Address", 0L, 999999L));
    if (contractAddress != null) {
      yy.setSignature(verifier.createSignatureB64(computeHash(yy.objectToHashToSign(contractAddress)), entity1.privateHex));
    }
    testData.addAddEncumbrance(yy);

    DvpAuthorisation dvpAuthSig = new DvpAuthorisation(entity1.publicHex, "AuthID", "", "AuthMeta", false, 1);
    if (contractAddress != null) {
      dvpAuthSig.setSignature(verifier.createSignatureB64(dvpAuthSig.stringToHashToSign(contractAddress), entity1.privateHex));
    }
    testData.addAuthorisation(dvpAuthSig);

    DvpParameter par1 = new DvpParameter(entity1.publicHex, 7L, 0, 0, 0, "");
    if (contractAddress != null) {
      par1.setSignature(verifier.createSignatureB64(par1.stringToHashToSign("P1", contractAddress), entity1.privateHex));
    }
    testData.addParameter("P1", par1);

    testData.setEncumbrance(new DvpEncumbrance(true, contractAddress));
    return testData;
  }


  @Test
  public void updatestate() throws Exception {
    /*
    Test general Contract conditions :
        Namespace Locks
        Contract Event
        Encumbrances.
        Bad signatures
        Bad Address
     */

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    int chainID = 16;
    final long issueAmount = 1000;
    int addressNonce = 0;

    String pubKey = getRandomPublicKey();

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

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, entity1.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace2, classname2, entity2.address, issueAmount, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Issued balances :

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(entity1.address);
    Map<String, Balance> fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    Balance newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(issueAmount, newValue.longValue());

    fromAddressEntry = assetBalances.findAndMarkUpdated(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount, newValue.longValue());

    s0.commit();
    s0 = state1.createSnapshot();

    // Contract without signatures :

    DvpUkContractData testData1 = getTestData1(null);
    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<>(testData1.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractDictionary,
        poa
    );

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(address);
    fromAddressEntry.setAddressPermissions(AP_CLASS);

    // Wrong permission
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry.setAddressPermissions(AP_CONTRACT);

    // OK permission
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Reset.
    s0 = state1.createSnapshot();

    // Contract without signatures :

    testData1 = getTestData1(null);
    contractDictionary = new MPWrappedMap<>(testData1.encodeToMapForTxParameter());

    thisTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        contractDictionary,
        poa
    );

    // Test Namespace Lock
    // Lock Namespace

    AbstractTx lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    // Contract fails to Create because namespace is locked

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Unlock Namespace

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    // Bad timestamp.

    assertFalse(UpdateState.update(thisTX, s0, 1, thisTX.getPriority(), false));

    // Contract creates OK.

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    NewContractTx thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        testData1,
        poa
    );

    // Contract With signatures :
    // Get Signed test data.

    testData1 = getTestData1(thisnewTX.getContractAddress());

    thisnewTX.setContractDictionary(new MPWrappedMap<>(testData1.encodeToMapForTxParameter()));

    assertEquals(0, s0.getContractEvents().size());

    // Test Asset Lock
    // Lock Namespace

    lockTX = LockAsset.lockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    // Contract fails to Create because namespace is locked

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Unlock Asset

    lockTX = UnLockAsset.unlockAssetUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");
    assertTrue(UpdateState.update(lockTX, s0, lockTX.getTimestamp(), lockTX.getPriority(), false));

    // Create Contract OK.

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Check Event has been lodged.

    assertEquals(1, s0.getContractEvents().size());
    assertEquals(((EventData) s0.getContractEvents().toArray()[0]).getEventAddress(), thisnewTX.getContractAddress());

    // Set State.

    s0.commit();

    assetBalances = s0.getAssetBalances();

    // Apply Contract Event.

    assertEquals(1, s0.getContractEvents().size());

    for (EventData thisEvent : s0.getContractEvents()) {
      assertSame(SuccessType.PASS, updateEvent(s0, thisnewTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success);
    }

    // Check Balances moved :

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(issueAmount - 432L, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(new Balance(432L), newValue);

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount - 431L, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(new Balance(431L), newValue);

    assertEquals(2, s0.getEffectiveTXList().size());

    // New TX

    thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce++,
        pubKey,
        address,
        testData1,
        poa
    );

    // Put Encumbrances in place

    String administratorAddress = getRandomAddress();

    EncumbranceDetail[] administrators = new EncumbranceDetail[]{new EncumbranceDetail(administratorAddress, 0L, Instant.now().getEpochSecond() + 9999L)};

    EncumbranceDetail[] beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(address, 0L, Instant.now().getEpochSecond() + 9999L)};

    Map<String, Object> txMap = new TreeMap<>();
    txMap.put("reference", thisnewTX.getContractAddress());
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party1Nonce++, entity1.publicHex, entity1.address, namespace, classname, "", issueAmount, txMap, "", "", "");
    ReturnTuple rTup = Encumber.updatestate((EncumberTx) thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    assertEquals(SuccessType.PASS, rTup.success);

    administrators = new EncumbranceDetail[]{new EncumbranceDetail(administratorAddress, 0L, Instant.now().getEpochSecond() + 9999L)};
    beneficiaries = new EncumbranceDetail[]{new EncumbranceDetail(address, 0L, Instant.now().getEpochSecond() + 9999L)};

    txMap = new TreeMap<>();
    txMap.put("reference", thisnewTX.getContractAddress());
    txMap.put("administrators", administrators);
    txMap.put("beneficiaries", beneficiaries);
    thisTX = io.setl.bc.pychain.tx.create.Encumber
        .encumberUnsigned(chainID, party2Nonce++, entity2.publicHex, entity2.address, namespace2, classname2, "", issueAmount, txMap, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Get Test data

    testData1 = getTestData1(thisnewTX.getContractAddress());

    // Clear payment signatures

    testData1.getParties().get(0).payList.get(0).signature = "";
    testData1.getParties().get(1).payList.get(0).signature = "";

    thisnewTX.setContractDictionary(new MPWrappedMap<>(testData1.encodeToMapForTxParameter()));

    assertEquals(1, s0.getContractEvents().size());

    // Update :-

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    assertEquals(2, s0.getContractEvents().size());
    final NewContractTx tmpTX = thisnewTX;
    assertTrue(s0.getContractEvents().stream().anyMatch(ed -> ed.getEventAddress().equals(tmpTX.getContractAddress())));

    // Apply Contract Event.

    assertEquals(2, s0.getContractEvents().size());

    for (EventData thisEvent : s0.getContractEvents()) {
      assertSame(SuccessType.PASS, updateEvent(s0, thisnewTX.getTimestamp(), thisEvent.getEventAddress(), thisEvent, false).success);
    }

    // Check Balances moved :

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals( issueAmount - 2 * 432L, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace + "|" + classname, BALANCE_ZERO);
    assertEquals(new Balance(864L), newValue);

    fromAddressEntry = assetBalances.find(entity2.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(issueAmount - 2 * 431L, newValue.longValue());

    fromAddressEntry = assetBalances.find(entity1.address);
    fromBalances = Objects.requireNonNull(fromAddressEntry.getClassBalance(), "Balances were null");
    newValue = fromBalances.getOrDefault(namespace2 + "|" + classname2, BALANCE_ZERO);
    assertEquals(new Balance(862L), newValue);

    assertEquals(4, s0.getEffectiveTXList().size());

    // Bad Sigs.

    thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        pubKey,
        address,
        testData1,
        poa
    );

    // Get Bad Signed test data.
    testData1 = getTestData1("Dross");

    thisnewTX.setContractDictionary(new MPWrappedMap<>(testData1.encodeToMapForTxParameter()));

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Bad Time

    thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        pubKey,
        address,
        testData1,
        poa
    );

    // Get Bad Signed test data.
    testData1 = getTestData1(thisnewTX.getContractAddress());

    thisnewTX.setContractDictionary(new MPWrappedMap<>(testData1.encodeToMapForTxParameter()));
    thisnewTX.setTimestamp(1);

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Get Bad Address.

    thisnewTX = NewContract.newContractUnsigned(
        chainID,
        addressNonce,
        pubKey,
        "address",
        testData1,
        poa
    );

    testData1 = getTestData1(thisnewTX.getContractAddress());

    thisnewTX.setContractDictionary(new MPWrappedMap<>(testData1.encodeToMapForTxParameter()));

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));


  }

  @Test
  public void updateEvent__SetStatusWhenIssueInContract() {
    StateSnapshot mockStateSnapshot = mock(StateSnapshot.class);
    when(mockStateSnapshot.getVersion()).thenReturn(5); //Change only available in version 5 and up
    
    
    MutableMerkle<ContractEntry> mockMerkle = mock(MutableMerkle.class);
    when(mockStateSnapshot.getContracts()).thenReturn(mockMerkle);
    
    long updateTime = LocalDateTime.of(2020, 6, 8, 14, 10, 0).toEpochSecond(ZoneOffset.UTC);
    String contractAddress = "12dasnjdasnlafnjksank32n4j";
    EventData eventData = new EventData(contractAddress, "", "commit", "");
    
    ContractEntry mockContractEntry = mock(ContractEntry.class);
    when(mockMerkle.findAndMarkUpdated(contractAddress)).thenReturn(mockContractEntry);
    
    DvpUkContractData mockContractData = mock(DvpUkContractData.class);
    when(mockContractEntry.getContractData()).thenReturn(mockContractData);
    
    updateEvent(mockStateSnapshot, updateTime, contractAddress, eventData, false);
    
    verify(mockContractData).set__status(anyString());
  }
}
