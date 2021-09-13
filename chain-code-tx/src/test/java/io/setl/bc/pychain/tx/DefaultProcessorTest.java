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
package io.setl.bc.pychain.tx;

import static io.setl.common.Balance.BALANCE_ZERO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.create.AssetClassRegister;
import io.setl.bc.pychain.tx.create.AssetIssue;
import io.setl.bc.pychain.tx.create.AssetTransfer;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class DefaultProcessorTest extends BaseTestClass {

  @Test
  public void processTransactionsOrdered() throws Exception {
    /*
    Test to check that the transactions in a block are processed in Priority order.

    To test this : Create a 'block' of transactions where the address has created the transactions in the wrong order. If the TXs are
    not ordered by priority this set of transactions will fail.
     */

    DefaultProcessor thisProcessor = new DefaultProcessor();

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    int chainID = 16;
    final long issueAmount = 1000;
    final long issueTransferAmount = 100;
    final long transferAmount = 400;
    final String namespace = "NS1";
    final String classname = "Class1";
    final String fullAssetID = namespace + "|" + classname;

    ArrayList<Txi> transactionsList = new ArrayList<>();

    long updateTime = (new Date()).getTime() / 1000;

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    String pubKey = getRandomPublicKey();
    String pubKey2 = getRandomPublicKey();

    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);
    final String toAddress2 = AddressUtil.publicKeyToAddress(pubKey2, AddressType.NORMAL);
    final String toAddress3 = getRandomAddress();

    int addressNonce = 0;

    // OK Build TX List

    // A test of transaction ordering would be to have an address issue an asset before creating it. Given the TX priorities, if
    // these TXs are in the same block, then they should work OK.

    AbstractTx thisTX0 = AssetTransfer
        .assetTransferUnsigned(chainID, 0, pubKey2, toAddress2, namespace, classname, toAddress3, transferAmount, "", "", "");

    thisTX0.setGood(true);

    transactionsList.add(thisTX0);

    AbstractTx thisTX1 = AssetIssue.assetIssueUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, toAddress2, issueAmount, "", "", "");
    thisTX1.setGood(true);

    transactionsList.add(thisTX1);

    AbstractTx thisTX2 = AssetClassRegister.assetClassRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, classname, "", "");
    thisTX2.setGood(true);

    transactionsList.add(thisTX2);

    AbstractTx thisTX3 = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");
    thisTX3.setGood(true);

    transactionsList.add(thisTX3);

    // Process list

    Txi[] transactions = new Txi[transactionsList.size()];
    transactionsList.toArray(transactions);

    thisProcessor.processTransactions(s0, transactions, updateTime, true);

    // Verify changes.

    MutableMerkle<AddressEntry> assetBalances = s0.getAssetBalances();
    AddressEntry fromAddressEntry = assetBalances.findAndMarkUpdated(toAddress2);
    Map<String, Balance> fromBalances = fromAddressEntry.getClassBalance();
    AddressEntry toAddressEntry = assetBalances.findAndMarkUpdated(toAddress3);
    Map<String, Balance> toBalances = toAddressEntry.getClassBalance();

    Balance newValue1 = fromBalances.getOrDefault(fullAssetID, BALANCE_ZERO);
    Balance newValue2 = toBalances.getOrDefault(fullAssetID, BALANCE_ZERO);

    assertEquals(newValue1, issueAmount - transferAmount);
    assertEquals(newValue2, transferAmount);

    assertTrue(thisTX3.getTxType().getPriority() < thisTX2.getTxType().getPriority());
    assertTrue(thisTX2.getTxType().getPriority() < thisTX1.getTxType().getPriority());
    assertTrue(thisTX1.getTxType().getPriority() < thisTX0.getTxType().getPriority());

  }


}
