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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.PoaAddTx;
import io.setl.bc.pychain.state.tx.PoaNewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.PoaAdd;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;
import java.time.Instant;
import org.junit.Test;

public class PoaTNominateTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot s0 = state1.createSnapshot();
    final long startDate = 0L;
    final long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";
    final String protocol = "prot";
    final String metadata = "meta";
    final String poa = "poa";

    final String poaPubKey = getRandomPublicKey();
    String poaAddress = AddressUtil.publicKeyToAddress(poaPubKey, AddressType.NORMAL);
    int party1Nonce = 0;

    String attorneyPubKey = getRandomPublicKey();
    String attorneyAddress = AddressUtil.publicKeyToAddress(attorneyPubKey, AddressType.NORMAL);

    PoaNewContractTx thisnewTX = new PoaNewContractTx(state1.getChainId(),            //  chainId
        "",
        party1Nonce,            //  nonce
        true,
        attorneyPubKey,
        attorneyAddress,
        poaAddress,
        poaReference,
        null,
        "",
        protocol,
        metadata,
        0,            //  height
        "",
        6);           //  timestamp

    TokensNominateContractData testData = new TokensNominateContractData(thisnewTX.getContractAddress(), CONTRACT_NAME_TOKENS_NOMINATE, "namespace",
        "inputclass", "outputclass", 1L, 1L, 999, new String[]{"time"}, poaAddress, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // Grant POA, bad Tx Type
    String fullAssetID1 = "namespace|inputclass";
    String fullAssetID2 = "namespace|outputclass";

    Object[] itemsData = new Object[]{
        new Object[]{TxType.TRANSFER_ASSET_AS_ISSUER.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2}}};

    PoaAddTx thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    // Bad Tx Type in POA

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0 = state1.createSnapshot();

    // Grant POA, bad assets
    fullAssetID1 = "namespace|inputclass";
    fullAssetID2 = "namespace|differentclass";

    itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    //

    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0 = state1.createSnapshot();

    // Grant POA, good assets
    fullAssetID1 = "namespace|inputclass";
    fullAssetID2 = "namespace|outputclass";

    itemsData = new Object[]{
        new Object[]{TxType.NEW_CONTRACT.getId(), (1L), new String[]{"Crud", fullAssetID1, fullAssetID2}}};

    thisAddTX = PoaAdd
        .poaUnsigned(state1.getChainId(), party1Nonce, poaPubKey, poaAddress, poaReference, attorneyAddress, startDate, endDate, itemsData, protocol, metadata,
            poa);
    assertTrue(UpdateState.update(thisAddTX, s0, thisAddTX.getTimestamp(), thisAddTX.getPriority(), false));

    //

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

  }

}
