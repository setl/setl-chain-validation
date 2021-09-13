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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.Memo;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.time.Instant;
import org.junit.Test;

public class MemoTest extends BaseTestClass {

  @Test
  public void updatestate() throws Exception {

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    String classname = "Class1";
    String metadata = "this metadata";
    final String fullAssetID = namespace + "|" + classname;
    int chainID = 16;
    long issueAmount = 1000;
    long issueTransferAmount = 100;
    long transferAmount = 400;

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    int addressNonce = 0;

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    String toAddress1 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    String toAddress2 = getRandomAddress();

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = Memo.memoUnsigned(chainID, addressNonce++, pubKey, "badaddress", "", "");

    // Fail on bad address.
    assertFalse(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = Memo.memoUnsigned(chainID, addressNonce++, pubKey, address, "", "");

    // Fail on bad time.
    assertFalse(UpdateState.update(thisTX, s0, 0, thisTX.getPriority(), false));

    // OK on bad priority.
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority() + 1, false));

    // OK Checkonly
    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), true));

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    thisTX = Memo.memoUnsigned(chainID, addressNonce, pubKey, address, "", "");
    thisTX.setTimestamp(1L);
    assertFalse(UpdateState.update(thisTX, s0, Instant.now().getEpochSecond(), thisTX.getPriority(), false));

  }

}
