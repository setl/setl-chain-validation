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
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import org.junit.Test;

public class TNominateTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);
    StateSnapshot s0 = state1.createSnapshot();

    final String publicKey1 = getRandomPublicKey();
    String party1Address = AddressUtil.publicKeyToAddress(publicKey1, AddressType.NORMAL);
    int party1Nonce = 0;

    NewContractTx thisnewTX = new NewContractTx(state1.getChainId(),            //  chainId
        "",
        party1Nonce,            //  nonce
        true,
        publicKey1,
        party1Address,
        null,
        "",
        0,            //  height
        "",
        6);           //  timestamp

    TokensNominateContractData testData = new TokensNominateContractData(thisnewTX.getContractAddress(), CONTRACT_NAME_TOKENS_NOMINATE, "namespace",
        "inputclass", "outputclass", 1L, 1L, 999, new String[]{"time"}, party1Address, "", "");

    thisnewTX.setContractDictionary(new MPWrappedMap<String, Object>(testData.encodeToMapForTxParameter()));

    assertTrue(UpdateState.update(thisnewTX, s0, thisnewTX.getTimestamp(), thisnewTX.getPriority(), false));

    s0.commit();

    assertEquals(s0.getContracts().find(thisnewTX.getContractAddress()).getContractData(), testData);

  }

}
