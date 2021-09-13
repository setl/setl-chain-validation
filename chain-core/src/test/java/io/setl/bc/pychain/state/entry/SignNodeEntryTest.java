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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;

import java.io.IOException;
import java.security.MessageDigest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.Content;
import io.setl.bc.pychain.serialise.factories.SignNodeFactory;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Balance;

/**
 * @author Simon Greatrix on 2019-04-10.
 */
public class SignNodeEntryTest {

  MessageDigest digest;

  SignNodeEntry entry;

  FileStateLoader fileStateLoaded;

  SerialiseToByte hashSerialiser;

  String stateFile;


  @Before
  public void before() {
    String hex1 = "3059301306072a8648ce3d020106082a8648ce3d0301070342000473cde7710bdbbfc457a59a1292ea2ca30b8dff1227b0fbd96b8833de9e40b905010dac5bc2375f"
        + "c91a29ff3e968b473b7900d47dc5fcc13645b48b68fc6dc893";
    entry = new SignNodeEntry(hex1, "returnTo", 1_000_000, 5);
  }


  @Test
  public void copy() {
    SignNodeEntry c = entry.copy();
    assertEquals(entry, c);
    assertEquals(entry.hashCode(), c.hashCode());
  }


  @Test
  public void decrementBalance() {
    assertEquals(new Balance(1_000_000), entry.getBalance());
    entry.decrementBalance(1_000);
    assertEquals(new Balance(999_000), entry.getBalance());
  }


  @Test
  public void encode() {
    Object[] d = entry.encode(1);
    SignNodeEntry e = new SignNodeEntry.Decoder().decode(new MPWrappedArrayImpl(d));
    assertEquals(entry, e);
  }


  @Test
  public void factory() {
    SignNodeFactory f = new SignNodeFactory();

    Content c = f.asContent(Digest.TYPE_SHA_256, entry);
    SignNodeEntry e = f.asValue(c.getData());
    assertEquals(entry, e);
  }


  @Test
  public void getKey() {
    assertEquals(entry.getHexPublicKey(), entry.getKey());
  }


  @Test
  public void getNonce() {
    assertEquals(5L, entry.getNonce());
  }


  @Test
  public void incrementBalance() {
    assertEquals(new Balance(1_000_000), entry.getBalance());
    entry.incrementBalance(1_000);
    assertEquals(new Balance(1_001_000), entry.getBalance());
  }


  @Test
  public void incrementNonce() {
    assertEquals(5L, entry.getNonce());
    entry.incrementNonce();
    assertEquals(6L, entry.getNonce());
  }


  @Test
  public void pack() throws IOException {
    byte[] binary = MsgPackUtil.pack(entry);
    SignNodeEntry c = new SignNodeEntry(MsgPackUtil.newUnpacker(binary));
    assertEquals(entry.getHexPublicKey(), c.getHexPublicKey());
    assertEquals(entry, c);
    assertEquals(entry.hashCode(), c.hashCode());
  }


  @Test
  public void setBalance() {
    assertEquals(new Balance(1_000_000), entry.getBalance());
    entry.setBalance(2_000_000);
    assertEquals(new Balance(2_000_000), entry.getBalance());
  }


  @Test
  public void setHexPublicKey() {
    String hex2 = "3059301306072a8648ce3d020106082a8648ce3d0301070342000405443c5b3d6b0cc609abc1e234034b174376394d8b6f5cd74e35b8377b060c0bec69fa0ca86332"
        + "05701da69506bf89a1b7a9a911e02a2ea6beaa14367b1c2e7a";
    entry.setHexPublicKey(hex2);
    assertEquals(hex2, entry.getHexPublicKey());
  }


  @Test
  public void setReturnAddress() {
    entry.setReturnAddress("newReturnFrom");
    assertEquals("newReturnFrom", entry.getReturnAddress());
  }


  @Before
  public void setUp() throws Exception {

    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();

    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
    stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
  }


  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void signNodeEntryConstructor() throws Exception {

    SignNodeEntry thisSignNodeEntry1 = new SignNodeEntry("HexPublic", "ReturnAddress", 999L, 1, 2, 3);
    SignNodeEntry thisSignNodeEntry2 = (new SignNodeEntry.Decoder()).decode(new MPWrappedArrayImpl(thisSignNodeEntry1.encode(0)));
    SignNodeEntry thisSignNodeEntry3 = thisSignNodeEntry1.copy();

    assertEquals(thisSignNodeEntry1, thisSignNodeEntry2);
    assertEquals(thisSignNodeEntry1, thisSignNodeEntry3);

    assertTrue(thisSignNodeEntry1.getBalance().equals(999L));
    assertEquals("HexPublic", thisSignNodeEntry1.getHexPublicKey());
    assertEquals("HexPublic", thisSignNodeEntry1.getKey());
    assertEquals("ReturnAddress", thisSignNodeEntry1.getReturnAddress());
    assertEquals(-1L, thisSignNodeEntry1.getBlockUpdateHeight());
    assertEquals(1, thisSignNodeEntry1.getNonce());

    thisSignNodeEntry1.incrementBalance(1L);
    assertTrue(thisSignNodeEntry1.getBalance().equals(1000L));
  }


  @Test
  public void signNodeEntryUpdateHeight() throws Exception {

    // Decoder().decode(new MPWrappedArrayImpl(encode(-1)));

    SignNodeEntry thisSignNodeEntry = new SignNodeEntry("HexPublic", "ReturnAddress", 999L, 0, 0, 0);

    assertEquals(thisSignNodeEntry.getBlockUpdateHeight(), -1);

    thisSignNodeEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisSignNodeEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    SignNodeEntry newSignNodeEntry = new SignNodeEntry(thisSignNodeEntry);

    assertEquals(newSignNodeEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    newSignNodeEntry = (new SignNodeEntry.Decoder()).decode(new MPWrappedArrayImpl(thisSignNodeEntry.encode(0L)));

    assertEquals(newSignNodeEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<SignNodeEntry> SignNodeEntryTree = stateSnapshot.getSignNodes();

    SignNodeEntryTree.add(new SignNodeEntry("HexPublic", "ReturnAddress", 999L, 0, 0, 0));

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot
        .finalizeBlock(new Block(state1.getChainId(), state1.getHeight(), state1.getLoadedHash(), null, new Object[0], new Object[0],
            new Object[0], 0, "",
            null, new Object[0], new Object[0], new Object[0], new Object[0], new Object[0]
        ));

    stateSnapshot = state1.createSnapshot();

    SignNodeEntryTree = stateSnapshot.getSignNodes();

    newSignNodeEntry = SignNodeEntryTree.find("HexPublic");

    assertEquals(42, newSignNodeEntry.getBlockUpdateHeight());
  }


  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }


}