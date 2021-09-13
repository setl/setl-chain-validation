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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import org.junit.Test;

public class ExchangeTest extends BaseTestClass {

  @Test
  public void simpleExchange() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot s0 = state1.createSnapshot();

    final String publicKey1 = getRandomPublicKey();
    String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);
    int party1Nonce = 0;

    Object[] inputs = new Object[]{new NominateAsset("namespace", "classid", 1L)};
    Object[] outputs = new Object[]{new NominateAsset("namespace", "classid", 1L)};
    Long minBlocks = 0L;
    Long maxBlocks = 0L;
    Long startDate = 0L;
    Long expiryDate = Long.MAX_VALUE;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),
        "",
        party1Nonce,
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,
        "",
        6);

    ExchangeContractData testData = new ExchangeContractData(
        thisnewTX.getContractAddress(),
        CONTRACT_NAME_EXCHANGE,
        inputs,
        outputs,
        minBlocks,
        maxBlocks,
        startDate,
        expiryDate,
        new String[]{"time"},
        party1Address, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    // A couple to fail

    assertFalse(UpdateState.update(thisnewTX, s0, Long.MAX_VALUE, thisnewTX.getPriority(), false));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority() + 1, false));

    assertNull(state1.getContracts().find(thisnewTX.getContractAddress()));

    // OK

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

    // Already exist...
    assertFalse(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

  }

}
