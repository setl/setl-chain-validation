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
package io.setl.bc.pychain.state.entry;

import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.utils.ByteUtil;
import java.security.MessageDigest;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ContractEntryTest {

  private MessageDigest digest;

  private FileStateLoader fileStateLoaded;

  private SerialiseToByte hashSerialiser;

  String stateFile;

  private void checkContract(String stateFile, String contractAddress) throws Exception {

    // Checks the Hash of the Contract data (MPWrappedMap style data) taken directly from State vs the Encoded data from the contract data once
    // instantiated as an IContractData.

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    MutableMerkle<ContractEntry> cList = s0.getContracts();

    String stateHash1 = new DefaultHashableHashComputer().computeHashAsHex(state1);

    ContractEntry thisContract = cList.find(contractAddress);

    Object[] object1 = thisContract.encode(-1);
    String cHash1 = ByteUtil.bytesToHex(digest.digest(hashSerialiser.serialise(object1)));

    IContractData thisData = thisContract.getContractData();

    s0.commit();

    String stateHash2 = new DefaultHashableHashComputer().computeHashAsHex(state1);

    Object[] object2 = thisContract.encode(-1);
    String cHash2 = ByteUtil.bytesToHex(digest.digest(hashSerialiser.serialise(object2)));

    Assert.assertTrue(stateFile + " " + contractAddress, cHash1.equals(cHash2));
    Assert.assertTrue(stateFile + " " + contractAddress, stateHash1.equals(stateHash2));
  }


  @Test
  public void copy() throws Exception {

    ContractEntry thisEntry = getEntry("src/test/resources/test-states/contracts/433e87afd549188f5636777d4c0d33cfbfc36377d463c88f441f5fe1d2e81cef",
        "33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF");

    ContractEntry copyEntry = thisEntry.copy();

    Assert.assertTrue(copyEntry.getContractData().equals(thisEntry.getContractData()));

    assertEquals(thisEntry.getBlockUpdateHeight(), -1);

    thisEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    ContractEntry newCopy = new ContractEntry(thisEntry);

    assertEquals(newCopy.getBlockUpdateHeight(), Long.MAX_VALUE);

    newCopy = (new ContractEntry.Decoder()).decode(new MPWrappedArrayImpl(thisEntry.encode(0L)));

    assertEquals(newCopy.getBlockUpdateHeight(), Long.MAX_VALUE);

  }


  @Test
  public void encode() throws Exception {

    // NOTE : State data that includes Maps (dictionaries) needs to be written using spackb in Python as, by default, saved files do not
    // have sorting ON.

    checkContract("src/test/resources/test-states/contracts/433e87afd549188f5636777d4c0d33cfbfc36377d463c88f441f5fe1d2e81cef",
        "33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF");
    checkContract("src/test/resources/test-states/contracts/6f4656ea8a81f6780d56f25c96b9e611ad258518bd06a086674f07134e518b8f",
        "33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF");
    checkContract("src/test/resources/test-states/contracts/7efb92339d645276b54c022545307b108f9fa2273bd75bddb1885c93fa54b45d",
        "33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF");
    checkContract("src/test/resources/test-states/contracts/549aa28443d924813ba7bbb02a61f7a1ec9e87370f671ee9f1e66a152a125edf",
        "3GqEHQnzfKeiLJCyNR9pZw58UrN4oLzaaz");
    checkContract("src/test/resources/test-states/contracts/529993ee98496910d61a6553f335340bf2e914c35ffad77e4d90104b6c59a462",
        "3MLdPnVHSuoAyhu222pMhpvMcXuSU6LFvB");
    checkContract("src/test/resources/test-states/contracts/20f169cbd0875945fb2b87dc341025adab9210fe2700366621792c2a0c0f0400",
        "3NStrNo1XbNiPesc7RRyhBgQyaq2NN9f1y");
    checkContract("src/test/resources/test-states/contracts/d08eb331ebcccde32612e7b30ee2142f63fdc89f1e8ec5e61b75ae86a9df9bd6",
        "3PSgYCA3HTp3DwkExfr4kpv2jbUgYnQnyk");

  }


  @Test
  public void getContractData() throws Exception {

    ContractEntry thisEntry = getEntry("src/test/resources/test-states/contracts/433e87afd549188f5636777d4c0d33cfbfc36377d463c88f441f5fe1d2e81cef",
        "33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF");

    Map<String, Object> details = thisEntry.getContractData().encodeToMap();

    Assert.assertTrue("33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF".equalsIgnoreCase((String) details.get("__address")));
    Assert.assertTrue("dvp_uk".equalsIgnoreCase((String) details.get("__function")));
    Assert.assertTrue("19YLbGamqHJZgVGqJVStBxU3n26qNh7Qto".equalsIgnoreCase((String) details.get("issuingaddress")));
    Assert.assertTrue("meta data".equalsIgnoreCase((String) details.get("metadata")));
    Assert.assertTrue("dvp".equalsIgnoreCase((String) details.get("protocol")));
    Assert.assertTrue(((Object[]) details.get("parties")).length == 4);

  }


  private ContractEntry getEntry(String stateFile, String contractAddress) throws Exception {

    // Checks the Hash of the Contract data (MPWrappedMap style data) taken directly from State vs the Encoded data from the contract data once
    // instantiated as an IContractData.

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot s0 = state1.createSnapshot();

    MutableMerkle<ContractEntry> cList = s0.getContracts();

    String stateHash1 = new DefaultHashableHashComputer().computeHashAsHex(state1);

    return cList.find(contractAddress);

  }


  @Test
  public void getKey() throws Exception {

    ContractEntry thisEntry = getEntry("src/test/resources/test-states/contracts/433e87afd549188f5636777d4c0d33cfbfc36377d463c88f441f5fe1d2e81cef",
        "33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF");

    Assert.assertTrue(thisEntry.getKey().equalsIgnoreCase("33v6ZfMmRLM8wHsPDNvmivawrPoSAdLBaF"));

  }


  @Test
  public void contractEntryUpdateHeight() throws Exception {

    ContractEntry thisContractEntry = new ContractEntry();

    assertEquals(thisContractEntry.getBlockUpdateHeight(), -1);

    thisContractEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisContractEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    ContractEntry newContractEntry = new ContractEntry(thisContractEntry);

    assertEquals(newContractEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    newContractEntry = (new ContractEntry.Decoder()).decode(new MPWrappedArrayImpl(thisContractEntry.encode(0L)));

    assertEquals(newContractEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<ContractEntry> contractsList = stateSnapshot.getContracts();

    ContractEntry newContract = new ContractEntry("newAddress", null);
    contractsList.add(newContract);

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot.finalizeBlock(new Block(state1.getChainId(), state1.getHeight(), state1.getLoadedHash(), null, new Object[0], new Object[0],
        new Object[0], 0, "",
        null, new Object[0], new Object[0], new Object[0], new Object[0], new Object[0]));

    stateSnapshot = state1.createSnapshot();

    contractsList = stateSnapshot.getContracts();

    newContractEntry = contractsList.find("newAddress");

    assertTrue(newContractEntry.getBlockUpdateHeight() == 42);
  }

  @Before
  public void setUp() throws Exception {

    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();

    Defaults.reset();
    fileStateLoaded = new FileStateLoader();

    stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

  }


  @After
  public void tearDown() throws Exception {
    Defaults.reset();

  }

}
