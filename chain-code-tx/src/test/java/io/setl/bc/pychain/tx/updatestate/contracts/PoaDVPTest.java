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
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CONTRACT;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POA_EXERCISE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
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
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaNewContract;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import io.setl.common.Hex;
import io.setl.crypto.KeyGen.Type;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaDVPTest extends BaseTestClass {


  final String poa = "poa";

  final String poaReference = "poaRef1";

  final PrivateKey privateKey1;

  final String publicKey1;

  final String publicKey2;

  String attorneyPubKey = getRandomPublicKey();

  String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

  String contractFunction = CONTRACT_NAME_DVP_UK;

  String encumbranceToUse = "enc.";

  long endDate = Instant.now().getEpochSecond() + 999L;

  String[] events = new String[]{"time", "commit"};

  long expiryDate = Instant.now().getEpochSecond() + 600L;

  int isCompleted = 0;

  String issuingAddress = getRandomAddress();

  // AddressUtil.verifyAddress(party1Address);

  String metadata = "MetaData";

  boolean mustSign1 = false;

  boolean mustSign2 = false;

  long nextTimeEvent = 888888888L;

  String party1Address;

  int party1Nonce = 0;

  String party2Address;

  // Party 1
  String partyIdentifier1 = "party1";

  // Party 2
  String partyIdentifier2 = "party2";

  PrivateKey privateKey2;

  String protocol = "Protocol";

  String signature1 = "";

  String signature2 = "";

  long startDate = 1L;

  {
    KeyPair keyPair = Type.ED25519.generate();
    privateKey1 = keyPair.getPrivate();
    publicKey1 = Hex.encode((keyPair.getPublic().getEncoded()));
    party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    keyPair = Type.ED25519.generate();
    privateKey2 = keyPair.getPrivate();
    publicKey2 = Hex.encode((keyPair.getPublic().getEncoded()));
    party2Address = AddressUtil.publicKeyToAddress(publicKey2, AddressType.NORMAL);
  }


  private DvpUkContractData getTestData1(String contractAddress) {

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
        432L,
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
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    final String namespace2 = "NS2";
    final String classname2 = "Class2";
    final String metadata = "this metadata";
    final String poa = "this poa";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    final long issueAmount = 1000;
    long issueTransferAmount = 100;
    long transferAmount = 400;
    int addressNonce = 0;

    String pubKey = getRandomPublicKey();

    String pubKey2 = getRandomPublicKey();

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String poaPubKey = pubKey;
    final String poaAddress = address;

    ObjectEncodedState baseState = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot state1 = baseState.createSnapshot();
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

    s0.commit();
    s0 = state1.createSnapshot();

    // -----------------------------------------------------
    // Contract without signatures, Test Address Permissions :

    // Grant POA
    DvpUkContractData testData1 = getTestData1(null);
    String fullAssetID1 = testData1.getParties().get(0).payList.get(0).getFullAssetID();

    Object[] itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{fullAssetID1}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    MPWrappedMap<String, Object> contractDictionary = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    thisTX = PoaNewContract.poaNewContractUnsigned(
        chainID,
        addressNonce++,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        contractDictionary,
        protocol,
        metadata,
        poa
    );

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Add permission. The address has already been implicitly created.
    assetBalances = s0.getAssetBalances();
    fromAddressEntry = assetBalances.find(attorneyAddress);
    fromAddressEntry.setAddressPermissions(AP_POA_EXERCISE);

    fromAddressEntry = assetBalances.findAndMarkUpdated(poaAddress);
    fromAddressEntry.setAddressPermissions(AP_CLASS);

    // Wrong permission
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    fromAddressEntry.setAddressPermissions(AP_CONTRACT);

    // OK permission
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // -----------------------------------------------------
    // Reset.
    s0 = state1.createSnapshot();

    // Contract without signatures :

    testData1 = getTestData1(null);
    contractDictionary = new MPWrappedMap<String, Object>(testData1.encodeToMapForTxParameter());

    thisTX = PoaNewContract.poaNewContractUnsigned(
        chainID,
        addressNonce++,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        contractDictionary,
        protocol,
        metadata,
        poa
    );

    // no POA
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();

    // Grant POA, bad Tx Type
    fullAssetID1 = testData1.getParties().get(0).payList.get(0).getFullAssetID();

    itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_AS_ISSUER.getId(), (1L), new String[]{fullAssetID1}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Bad Tx Type in POA

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();

    // Grant POA, bad assets

    itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{"Dross"}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Bad asset in POA, Works because Contract creation (without Party payment POA) does not require a matching Asset.

    assertEquals(0, s0.getContractEvents().size());

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

  }

}
