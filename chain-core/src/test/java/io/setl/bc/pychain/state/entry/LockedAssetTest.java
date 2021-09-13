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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.LockedAsset.Decoder;
import io.setl.bc.pychain.state.entry.LockedAsset.Type;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class LockedAssetTest {

  LockedAsset asset = new LockedAsset("asset", Type.FULL, 1);

  FileStateLoader fileStateLoaded;

  String stateFile;


  @Test
  public void construct() throws Exception {

    LockedAsset thisLockedAsset = new LockedAsset("Asset", Type.FULL, 99);
    LockedAsset thisLockedAsset2 = thisLockedAsset.copy();

    assertTrue(thisLockedAsset.getKey().equals("Asset"));
    assertTrue(thisLockedAsset.getType().equals(Type.FULL));
    assertTrue(thisLockedAsset.getBlockUpdateHeight() == 99);

    assertTrue(thisLockedAsset.getKey().equals(thisLockedAsset2.getKey()));
    assertTrue(thisLockedAsset.getType().equals(thisLockedAsset2.getType()));
    assertTrue(thisLockedAsset.getBlockUpdateHeight() == thisLockedAsset2.getBlockUpdateHeight());

    thisLockedAsset = new LockedAsset("Asset", Type.NO_LOCK, -2);
    thisLockedAsset2 = thisLockedAsset.copy();

    assertTrue(thisLockedAsset.getKey().equals("Asset"));
    assertTrue(thisLockedAsset.getType().equals(Type.NO_LOCK));
    assertTrue(thisLockedAsset.getBlockUpdateHeight() == -1);

    assertTrue(thisLockedAsset.getKey().equals(thisLockedAsset2.getKey()));
    assertTrue(thisLockedAsset.getType().equals(thisLockedAsset2.getType()));
    assertTrue(thisLockedAsset.getBlockUpdateHeight() == thisLockedAsset2.getBlockUpdateHeight());

    thisLockedAsset.toString(); // Shameless code coverage.
  }


  @Test
  public void copy() {
    LockedAsset c = asset.copy();
    assertEquals(asset, c);
  }


  @Test
  public void encode() {
    Object[] encoded = asset.encode(1);
    Decoder decoder = new Decoder();
    LockedAsset a2 = decoder.decode(new MPWrappedArrayImpl(encoded));
    assertEquals(asset, a2);
  }


  @Test
  public void getKey() {
    assertEquals("asset", asset.getKey());
  }


  @Test
  public void lockedAssetUpdateHeight() throws Exception {

    LockedAsset thisLockedAsset = new LockedAsset("Asset", Type.FULL, -1);

    assertEquals(thisLockedAsset.getBlockUpdateHeight(), -1);

    thisLockedAsset.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisLockedAsset.getBlockUpdateHeight(), Long.MAX_VALUE);

    LockedAsset newLockedAsset = thisLockedAsset.copy();

    assertEquals(newLockedAsset.getBlockUpdateHeight(), Long.MAX_VALUE);

    newLockedAsset = new LockedAsset(thisLockedAsset);

    assertEquals(newLockedAsset.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    stateSnapshot.setAssetLockValue("newAsset", Type.FULL);

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot
        .finalizeBlock(new Block(state1.getChainId(), state1.getHeight(), state1.getLoadedHash(), null, new Object[0], new Object[0],
            new Object[0], 0, "",
            null, new Object[0], new Object[0], new Object[0], new Object[0], new Object[0]));

    Merkle<LockedAsset> theseLocks = state1.getLockedAssets();
    newLockedAsset = theseLocks.find("newAsset");

    assertTrue(newLockedAsset.getBlockUpdateHeight() == 42);
  }


  @Test
  public void setType() {
    assertEquals(Type.FULL, asset.getType());
    asset.setType(Type.NO_LOCK,5);
    assertEquals(Type.NO_LOCK, asset.getType());
  }


  @Before
  public void setUp() throws Exception {
    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
    stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
  }


  @Test
  public void string() {
    assertNotNull(asset.toString());
  }


  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }


}