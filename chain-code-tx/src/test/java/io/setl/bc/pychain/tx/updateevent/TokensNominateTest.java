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
package io.setl.bc.pychain.tx.updateevent;


import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.NewContractTx.UnsupportedContractException;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import java.time.Instant;
import org.junit.Test;

public class TokensNominateTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    final String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    final String publicKey1 = getRandomPublicKey();
    final String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    int party1Nonce = 0;

    // Establish basic Nominate Contract

    NewContractTx thisnewTX = new NewContractTx(
        state1.getChainId(),
        "",
        party1Nonce++,
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,            //  height
        "",
        6);           //  timestamp

    TokensNominateContractData testData = new TokensNominateContractData(thisnewTX.getContractAddress(), CONTRACT_NAME_TOKENS_NOMINATE, "namespace",
        "inputclass",
        "outputclass", 1L, 1L, Instant.now().getEpochSecond() + 1000, new String[]{"time"}, party1Address, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    StateSnapshot s0 = state1.createSnapshot();

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    // Event : missing contract entry, time event, Assumed to be a 'Ghost' event. Not important.

    assertEquals(SuccessType.PASS,
        TokensNominate.updateEvent(s0, thisnewTX.getTimestamp(), "MissingAddress", new EventData("", "", "time", ""), false).success);

    // Event : missing contract entry, non-time event, Raised as an error because it should not happen, though Nominate only has 'time' events.

    assertEquals(SuccessType.PASS,
        TokensNominate.updateEvent(s0, thisnewTX.getTimestamp(), "MissingAddress", new EventData("", "", "other", ""), false).success);

    // Add null Contract entry

    ContractEntry newContract = new ContractEntry("MissingAddress", null);
    s0.getContracts().add(newContract);

    // Event : null contract entry, now returns false.

    try {
      TokensNominate.updateEvent(s0, thisnewTX.getTimestamp(), "MissingAddress", new EventData("", "", "time", ""), false);
      fail();
    } catch (UnsupportedContractException e) {
      // Should throw an error, fails do decode Contract data.
      //
    }

    // It is not possible to test

    newContract = new ContractEntry("ContractAddress1",
        (new TokensNominateContractData("DuffAddress", CONTRACT_NAME_TOKENS_NOMINATE, "", "", "", 1, 1, 0, new String[]{}, "", "", "")).encode());
    s0.getContracts().add(newContract);

    // updateEvent never return FAIL, even when it does nothing.
    assertSame(SuccessType.PASS,
        TokensNominate.updateEvent(s0, thisnewTX.getTimestamp(), "ContractAddress1", new EventData("", "", "time", ""), false).success);

    // 'Valid' "update" event. Does nothing.

    assertSame(SuccessType.PASS, TokensNominate.updateEvent(s0, thisnewTX.getTimestamp(), thisnewTX.getContractAddress(), new EventData("", "", "update",
            Long.MAX_VALUE),
        false).success);

    assertNotNull(s0.getContracts().find(thisnewTX.getContractAddress()));

    // Expiry time event. Will delete contract data.

    assertSame(SuccessType.PASS, TokensNominate.updateEvent(s0, thisnewTX.getTimestamp(), thisnewTX.getContractAddress(),
        new EventData("", "", "time", Long.MAX_VALUE), false).success);

    assertNull(s0.getContracts().find(thisnewTX.getContractAddress()));

  }
}
