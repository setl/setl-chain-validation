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

import static io.setl.bc.pychain.state.entry.LockedAsset.Type.FULL;
import static io.setl.bc.pychain.state.entry.LockedAsset.Type.NO_LOCK;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileBlockLoader;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.state.DbgCompareState;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.NamespaceDeleteTx;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.bc.pychain.tx.create.AddXChain;
import io.setl.bc.pychain.tx.create.NamespaceDelete;
import io.setl.bc.pychain.tx.create.NamespaceRegister;
import io.setl.bc.pychain.tx.create.PoaNamespaceDelete;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.XChainParameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceDeleteTest extends BaseTestClass {

  private static final Logger logger = LoggerFactory.getLogger(NamespaceDeleteTest.class);


  /**
   * singleTransitionTest.
   *
   * @param from  :
   * @param to    :
   * @param block :
   *
   * @throws Exception :
   */
  public void singleTransitionTest(String from, String to, String block) throws Exception {

    FileStateLoader sl = new FileStateLoader();
    FileBlockLoader bl = new FileBlockLoader();

    Block b = bl.loadBlockFromFile(block);

    ObjectEncodedState s0 = sl.loadStateFromFile(from);

    if (!s0.verifyAll()) {
      throw new RuntimeException(String.format("Loaded state(%d) does not verify", s0.getHeight()));
    }

    ObjectEncodedState s1 = sl.loadStateFromFile(to);
    if (!s1.verifyAll()) {
      throw new RuntimeException(String.format("Loaded state(%d) does not verify", s1.getHeight()));
    }

    validateTransition(s0, s1, b);
  }


  @Test
  public void updatestate() throws Exception {

    // Init

    String stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
    String namespace = "NS1";
    int chainID = 16;
    int addressNonce = 0;

    String pubKey = getRandomPublicKey();
    String address = AddressUtil.publicKeyToAddress(pubKey, AddressType.NORMAL);

    StateSnapshot state1 = fileStateLoaded.loadStateFromFile(stateFile).createSnapshot();

    StateSnapshot s0 = state1.createSnapshot();

    AbstractTx thisTX = NamespaceRegister.namespaceRegisterUnsigned(chainID, addressNonce++, pubKey, address, namespace, "", "");

    assertTrue(UpdateState.update(thisTX, s0, thisTX.getTimestamp(), thisTX.getPriority(), false));

    s0.commit();
    s0 = state1.createSnapshot();

    // Test :

    ReturnTuple rVal;

    // Bad Address
    thisTX = NamespaceDelete.namespaceDeleteUnsigned(chainID, addressNonce, pubKey, "BadAddressVsKey", namespace, "", "");
    rVal = UpdateState.doUpdate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.FAIL) && (rVal.status.equals("`From` Address and Public key do not match.")));

    // bad POA Address
    thisTX = PoaNamespaceDelete.poaNamespaceDeleteUnsigned(chainID, addressNonce, pubKey, address, "BadPoaAddress", "reference", namespace, "", "", "");
    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.FAIL) && (rVal.status.equals("Invalid POA address BadPoaAddress")));

    // No Namespace
    thisTX = NamespaceDelete.namespaceDeleteUnsigned(chainID, addressNonce, pubKey, address, "", "", "");
    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.FAIL) && (rVal.status.equals("Namespace is not given.")));

    // Lock it.

    s0.setAssetLockValue(namespace, FULL);

    thisTX = NamespaceDelete.namespaceDeleteUnsigned(chainID, addressNonce, pubKey, address, namespace, "", "");
    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.FAIL) && (rVal.status.equals("Namespace is locked : " + namespace)));

    s0.setAssetLockValue(namespace, NO_LOCK);

    //

    // Test Authorise By Address
    s0.setConfigValue("authorisebyaddress", 1);

    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.FAIL) && (rVal.status.equals("Inadequate Address permissioning")));

    s0.setConfigValue("authorisebyaddress", 0);

    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertSame(rVal.success, SuccessType.PASS);

    // Reset

    s0 = state1.createSnapshot();

    // Try unknown xChain : Silent fail.

    int chainID2 = chainID + 1;
    int newBlockHeight = 554466;
    long newChainParams = XChainParameters.AcceptNamespaces; // 998833L;
    List<Object[]> newChainSigNodes = new ArrayList<>();
    newChainSigNodes.add(new Object[]{"PubKey", 1000});

    thisTX = NamespaceDelete.namespaceDeleteUnsigned(chainID2, addressNonce, pubKey, address, namespace, "", "");
    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.PASS) && (rVal.status.equals("")));

    // Make it known

    // Register Chain

    AbstractTx thisTXxc = AddXChain.addXChainUnsigned(chainID, addressNonce++, pubKey, address, chainID2, newBlockHeight, newChainParams, newChainSigNodes, "");
    assertTrue(UpdateState.update(thisTXxc, s0, thisTXxc.getTimestamp(), thisTXxc.getPriority(), false));

    assertNotNull(s0.getNamespaces().find(namespace));

    // Try again

    rVal = io.setl.bc.pychain.tx.updatestate.NamespaceDelete.updatestate((NamespaceDeleteTx) thisTX, s0, thisTX.getTimestamp(),
        thisTX.getPriority(), false);

    assertTrue((rVal.success == SuccessType.PASS) && (rVal.status.equals("")));

    // Check Namespace gone.

    assertNull(s0.getNamespaces().find(namespace));

  }


  @Test
  public void updatestatetransition() throws Exception {

    // Transition involving the sucessful creation of a Namespace.
    singleTransitionTest(
        "src/test/resources/test-states/nsdelete/20/Balance/c870ce1bc5b08af67070560448e1f495f307eae7eaedb0a7fb083d3adbcded43",
        "src/test/resources/test-states/nsdelete/20/Balance/8c8de1056be92af2544f88069b3f832ced8810d5a811acbc2091cd2f0c32bed8",
        "src/test/resources/test-states/nsdelete/20/Block/4a8e36db7d43cda6fa47b532248db4a7a614997e6ed7764e5a1e08d2446d2362");

    // Transition involving the sucessful deletion of a Namespace.
    singleTransitionTest(
        "src/test/resources/test-states/nsdelete/20/Balance/8c8de1056be92af2544f88069b3f832ced8810d5a811acbc2091cd2f0c32bed8",
        "src/test/resources/test-states/nsdelete/20/Balance/8be59580746cde202f7fa04351713043098d02b76ae7688c16946c69cf2341aa",
        "src/test/resources/test-states/nsdelete/20/Block/1130764e11dfff7030531ffcb89d82c78bfedff52643a80a7775289f76364ec2");

  }


  void validateTransition(ObjectEncodedState s0, ObjectEncodedState s1, Block block)
      throws IOException, Exception {

    StateSnapshot newSnap = s0.createSnapshot();

    boolean oldStyleForDebug = false;

    if (!DefaultProcessor.getInstance().processTransactions(
        newSnap, block.getTransactions(), block.getTimeStamp(), true)) {
      throw new RuntimeException("Process Transactions Failed");
    }

    newSnap.commit();

    s0 = (ObjectEncodedState) newSnap.finalizeBlock(block);

    if (!s0.verifyAll()) {
      if (!s1.verifyAll()) {
        logger.error("New state also fails");
      } else {
        logger.info("Loaded state is ok :=transform failed");
      }

      logger.info("FAILED");
      DbgCompareState.debugCompare(s0, s1);
      throw new RuntimeException(
          String.format("State transform failed from %d to %d", s0.getHeight(), s1.getHeight()));
    } else {
      logger.info("Transition ok {}->{}", s0.getHeight(), s1.getHeight());
    }

  }


}
