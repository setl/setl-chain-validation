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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.create.PoaTransferFromMany;
import io.setl.bc.pychain.tx.create.RegisterAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaTransferFromManyTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String xcStateFile = "src/test/resources/test-states/genesis/20/4e905d26446940930b87cf1b0ea817ba150548410390d0fdb3266ea5e3ad0a2d";
    String namespace = "NS1";
    String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    int xchainID = 20;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;
    final String sigPubKey1 = "mary had a little lamb";
    final Long sigAmount1 = 12345678L;
    final String sigPubKey2 = "mary lost a little lamb, but found it later. Hurrah!";
    final Long sigAmount2 = 87654321L;

    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";
    final String poaReference2 = "poaRef2";
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    String poaPubKey = getRandomPublicKey();
    String poaAddress = AddressUtil.publicKeyToAddress(poaPubKey, AddressType.NORMAL);

    String attorneyPubKey = getRandomPublicKey();
    final String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    final String sAddress1 = getRandomAddress();
    final String sAddress2 = getRandomAddress();
    final String sAddress3 = getRandomAddress();
    final String sAddress4 = getRandomAddress();

    int addressNonce = 0;

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    int newBlockHeight = 554466;
    long newChainParams = 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{sigPubKey1, sigAmount1});
    newChainSigNodes.add(new Object[]{sigPubKey2, sigAmount2});

    AbstractTx thisTX;
    StateSnapshot s0 = state1.createSnapshot();

    // Place initial holding ...
    thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = RegisterAddress.registerAddressUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, "", "", "");

    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, sAddress1, issueAmount, "", "", "");
    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, sAddress2, issueAmount, "", "", "");
    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    thisTX = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, poaPubKey, poaAddress, namespace, classname, sAddress3, issueAmount, "", "", "");
    UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false);

    s0.commit();
    s0 = state1.createSnapshot();

    // Check Issued amount and issuer -ve balance.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();

    assertEquals(assetBalances.find(sAddress1).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), issueAmount);
    assertEquals(assetBalances.find(sAddress2).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), issueAmount);
    assertEquals(assetBalances.find(sAddress3).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), issueAmount);
    assertEquals(assetBalances.find(poaAddress).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), (-3 * issueAmount));

    //
    Object[] subjectAddresses = new Object[]{new Object[]{sAddress1, transferAmount}, new Object[]{sAddress2, transferAmount},
        new Object[]{sAddress3, transferAmount}};

    Object[] badSubjectAddresses2 = new Object[]{new Object[]{sAddress1, transferAmount}, new Object[]{sAddress2, -1L},
        new Object[]{sAddress3, transferAmount}};

    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, subjectAddresses,
            transferAmount, protocol, metadata, poa);

    thisTX.setTimestamp(1L);
    // Fail on timestamp.
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce, attorneyPubKey, "Dross", poaAddress, poaReference, namespace, classname, subjectAddresses,
            transferAmount, protocol, metadata, poa);

    // Bad Attorney key
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce, attorneyPubKey, attorneyAddress, "Dross", poaReference, namespace, classname, subjectAddresses,
            transferAmount, protocol, metadata, poa);

    // Bad POA Address
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, subjectAddresses,
            -1, protocol, metadata, poa);

    // Bad Amount
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, subjectAddresses,
            transferAmount, protocol, metadata, poa);

    // No POA
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Grant POA

    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_FROM_MANY.getId(), (transferAmount * 3), new String[]{fullAssetID}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    assertNotNull(s0.getPowerOfAttorneys().find(poaAddress));

    // Should Not Work Now : bad amount.
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Bad From Address
    Object[] badSubjectAddresses1 = new Object[]{new Object[]{sAddress1, transferAmount}, new Object[]{"Dross", transferAmount},
        new Object[]{sAddress3, transferAmount}};

    thisTX = PoaTransferFromMany.poaTransferFromManyUnsigned(
        chainID,
        addressNonce++,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        namespace,
        classname,
        badSubjectAddresses1,
        transferAmount * 3,
        protocol,
        metadata,
        poa);

    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Should Work Now
    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference, namespace, classname, subjectAddresses,
            transferAmount * 3, protocol, metadata, poa);

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Should Not work, POA consumed
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    assertEquals(assetBalances.find(sAddress1).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), issueAmount - transferAmount);
    assertEquals(assetBalances.find(sAddress2).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), issueAmount - transferAmount);
    assertEquals(assetBalances.find(sAddress3).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), issueAmount - transferAmount);
    assertEquals(assetBalances.find(poaAddress).getClassBalance().getOrDefault(fullAssetID, BALANCE_ZERO), -3 * (issueAmount - transferAmount));

    assertNull(s0.getPowerOfAttorneys().find(poaAddress));

    itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_FROM_MANY.getId(), (issueAmount * 3), new String[]{fullAssetID}}};

    thisAddTX = PoaAdd
        .poaUnsigned(chainID, addressNonce, poaPubKey, poaAddress, poaReference2, attorneyAddress, startDate, endDate, itemsData, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    subjectAddresses = new Object[]{new Object[]{sAddress1, (issueAmount - transferAmount)}, new Object[]{sAddress2, (issueAmount - transferAmount)},
        new Object[]{sAddress3, (issueAmount - transferAmount)}};
    thisTX = PoaTransferFromMany
        .poaTransferFromManyUnsigned(chainID, addressNonce++, attorneyPubKey, attorneyAddress, poaAddress, poaReference2, namespace, classname,
            subjectAddresses, (issueAmount - transferAmount) * 3, protocol, metadata, poa);
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    // Balances gone.
    assertFalse(assetBalances.find(sAddress1).getClassBalance().containsKey(fullAssetID));
    assertFalse(assetBalances.find(sAddress2).getClassBalance().containsKey(fullAssetID));
    assertFalse(assetBalances.find(sAddress3).getClassBalance().containsKey(fullAssetID));
  }
}
