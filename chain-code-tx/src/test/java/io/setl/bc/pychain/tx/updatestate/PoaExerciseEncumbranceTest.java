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
package io.setl.bc.pychain.tx.updatestate;

import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.XChainTypes.CHAIN_ADDRESS_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.LockAsset;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaExerciseEncumbrance;
import io.setl.bc.pychain.tx.create.UnLockAsset;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaExerciseEncumbranceTest extends EncumberTest {

  @Test
  @Override
  public void updatestate() throws Exception {

    super.updatestate();

    String xcStateFile = "src/test/resources/test-states/genesis/20/4e905d26446940930b87cf1b0ea817ba150548410390d0fdb3266ea5e3ad0a2d";
    final ObjectEncodedState xcState = fileStateLoaded.loadStateFromFile(xcStateFile);
    final int xchainID = 20;

    String poaAddress = beneficiary.address;
    String poaReference = "thisReference";
    String protocol = "Proto";  // Base64(MsgPack(""))
    String metadata = "Meta";  // Base64(MsgPack(""))
    String poa = "poa";  // Base64(MsgPack(""))

    final String recipientAddress = getRandomAddress();

    String attorneyPubKey = getRandomPublicKey();
    String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);
    final long startDate = 0L;
    long endDate = Instant.now().getEpochSecond() + 999L;

    // Get 'state1' as established by the 'EncumberTest'.
    // Not really necessary, but just for clarity.
    StateSnapshot state1 = this.state1;
    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID,
        beneficiaryNonce++,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        namespace,
        classname,
        subject.address,
        reference,
        chainID,
        recipientAddress,
        exerciseAmount,
        "",
        "",
        "");

    // No POA
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad TxType
    Object[] itemsData = new Object[]{new Object[]{TxType.REGISTER_ASSET_CLASS.getId(), lockAmount, new String[]{fullAssetID}}};
    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, beneficiary.publicHex, beneficiary.address, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0 = state1.createSnapshot();

    // Bad Amount
    itemsData = new Object[]{new Object[]{TxType.REGISTER_ASSET_CLASS.getId(), lockAmount, new String[]{fullAssetID}},
        new Object[]{TxType.EXERCISE_ENCUMBRANCE.getId(), 1, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, beneficiary.publicHex, beneficiary.address, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // OK
    s0 = state1.createSnapshot();

    itemsData = new Object[]{new Object[]{TxType.REGISTER_ASSET_CLASS.getId(), lockAmount, new String[]{fullAssetID}},
        new Object[]{TxType.EXERCISE_ENCUMBRANCE.getId(), lockAmount, new String[]{fullAssetID}}};
    thisAddTX = PoaAdd
        .poaUnsigned(chainID, subjectNonce, beneficiary.publicHex, beneficiary.address, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol,
            metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    AddressEncumbrances thisAddressEncumbrance = s0.getEncumbrances().find(subject.address);
    AssetEncumbrances thisAssetEncumbrances = thisAddressEncumbrance.getAssetEncumbrance(fullAssetID);
    EncumbranceEntry thisEntry = thisAssetEncumbrances.getByReference(reference).get(0);

    assertEquals(thisAssetEncumbrances.getTotalAmount(0L), lockAmount - exerciseAmount);
    assertEquals(thisEntry.amount, lockAmount - exerciseAmount);

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.find(recipientAddress);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    Balance newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(newValue, exerciseAmount);

    // xChain example

    final String sigPubKey1 = "mary had a little lamb";
    final Long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final Long sigAmount2 = 87654321L;

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    // Set xChain.
    thisTX = AddXChain.addXChainUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, xchainID, newBlockHeight, newChainParams,
        newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Set xChain.
    StateSnapshot xc0 = xcState.createSnapshot();

    thisTX = AddXChain.addXChainUnsigned(xchainID, issuerNonce, issuer.publicHex, issuer.address, chainID, newBlockHeight, newChainParams,
        newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check Chain Address not exist.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    assertNull(fromAddressEntry);

    thisTX = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID,
        beneficiaryNonce++,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        namespace,
        classname,
        subject.address,
        reference,
        xchainID,
        recipientAddress,
        lockAmount - exerciseAmount,
        "",
        "",
        "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));
    assertTrue(UpdateState.update(thisTX, xc0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Check source address
    thisAddressEncumbrance = s0.getEncumbrances().find(subject.address);
    assertNull(thisAddressEncumbrance);
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, exerciseAmount);

    // Check Chain address has incremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, xchainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, lockAmount - exerciseAmount);

    // Check xcTo Address has changed
    assetBalances = xc0.getAssetBalances();
    fromAddressEntry = assetBalances.find(recipientAddress);
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, lockAmount - exerciseAmount);

    // Check Chain address has decremented.
    fromAddressEntry = assetBalances.find(String.format(CHAIN_ADDRESS_FORMAT, chainID));
    fromBalances = fromAddressEntry.getClassBalance();
    newValue = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    assertEquals(newValue, exerciseAmount - lockAmount);

    // Some to fail :

    // Bad toChain
    thisTX = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID,
        beneficiaryNonce,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        namespace,
        classname,
        subject.address,
        reference,
        chainID + 99,
        recipientAddress,
        lockAmount - exerciseAmount,
        "",
        "",
        "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad Address vs Public key
    thisTX = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID,
        beneficiaryNonce,
        attorneyPubKey,
        "dross",
        poaAddress,
        poaReference,
        namespace,
        classname,
        subject.address,
        reference,
        chainID,
        recipientAddress,
        lockAmount - exerciseAmount,
        "",
        "",
        "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = LockAsset.lockAssetUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, classname, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Locked Asset
    thisTX = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID,
        beneficiaryNonce,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        namespace,
        classname,
        subject.address,
        reference,
        chainID,
        recipientAddress,
        lockAmount - exerciseAmount,
        "",
        "",
        "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = io.setl.bc.pychain.tx.create.UnLockAsset.unlockAssetUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, classname,
        "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = LockAsset.lockAssetUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, "", "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Locked Namespace
    thisTX = PoaExerciseEncumbrance.exerciseEncumbranceUnsigned(
        chainID,
        beneficiaryNonce,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        namespace,
        classname,
        subject.address,
        reference,
        chainID,
        recipientAddress,
        lockAmount - exerciseAmount,
        "",
        "",
        "");

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = UnLockAsset.unlockAssetUnsigned(chainID, issuerNonce++, issuer.publicHex, issuer.address, namespace, "", "", "");
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

  }

}
