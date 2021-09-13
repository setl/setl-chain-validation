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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.Content;
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.factories.NamespaceFactory;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.NamespaceEntry.Asset;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.util.MsgPackUtil;


/**
 * @author Simon Greatrix on 2019-04-10.
 */
public class NamespaceEntryTest {

  NamespaceEntry entry;

  FileStateLoader fileStateLoaded;

  String stateFile;


  @Test
  public void asset() throws IOException {
    Asset a1 = new Asset("thing", "wotsit");
    assertEquals("thing", a1.getAssetId());
    assertEquals("wotsit", a1.getMetadata());
    assertNotNull(a1.toString());

    byte[] binary = MsgPackUtil.pack(a1);
    Asset a2 = new Asset(MsgPackUtil.newUnpacker(binary));
    assertEquals(a1, a2);
    assertEquals(a1.hashCode(), a2.hashCode());
  }


  @Before
  public void before() {
    HashMap<String, Asset> classes = new HashMap<>();
    classes.put("c1", new Asset("c1", null));
    classes.put("c2", new Asset("c2", "an asset"));
    entry = new NamespaceEntry("name", "address", classes, null);
  }


  @Test
  public void copy() {
    NamespaceEntry c = entry.copy();
    assertEquals(entry, c);
  }


  @Test
  public void encode() {
    Object[] e = entry.encode(1);
    NamespaceEntry.Decoder d = new NamespaceEntry.Decoder();
    NamespaceEntry e2 = d.decode(new MPWrappedArrayImpl(e));
    assertEquals(entry, e2);
  }


  @Test
  public void factory() {
    ContentFactory<NamespaceEntry> factory = new NamespaceFactory();
    Content c = factory.asContent(Digest.TYPE_SHA_256, entry);
    assertEquals("Ih9ozEjtswPMAu43C7TrXljfnzsUZv2QhIJBm0_d8rw", c.getKey().toB64());
    NamespaceEntry e = factory.asValue(c.getData());
    assertEquals(entry, e);
  }


  @Test
  public void getClassMetadata() {
    assertNull(entry.getClassMetadata("c1"));
    assertEquals("an asset", entry.getClassMetadata("c2"));
    assertNull(entry.getClassMetadata("zzzzz"));
  }


  @Test
  public void getClasses() {
    Map<String, Asset> map = entry.getClasses();
    assertEquals(2, map.size());
    assertTrue(map.containsKey("c1"));
  }


  @Test
  public void getKey() {
    assertEquals("name", entry.getKey());
  }


  @Test
  public void getMetadata() {
    // NB - metadata was set as null
    assertEquals("", entry.getMetadata());

    entry.setMetadata("new metadata");
    assertEquals("new metadata", entry.getMetadata());
  }


  @Test
  public void jackson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    NamespaceEntry entry = new NamespaceEntry("Namespace1", "fromAddress1", "Metadata");
    entry.setBlockUpdateHeight(50);
    entry.setAsset(new Asset("Class1", "ClassMetadata"));
    entry.setAsset(new Asset("Other", "Something"));
    String text = mapper.writeValueAsString(entry);

    NamespaceEntry readBack = mapper.readValue(text, NamespaceEntry.class);
    assertEquals(entry, readBack);
  }


  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void namespaceEntryConstruct() {

    NamespaceEntry thisNamespaceEntry1 = new NamespaceEntry("Namespace1", "fromAddress1", "Metadata");
    thisNamespaceEntry1.setAsset(new Asset("Class1", "ClassMetadata"));
    NamespaceEntry thisNamespaceEntry2 = new NamespaceEntry(thisNamespaceEntry1);

    // Check that Class data is independent after a clone / copy
    thisNamespaceEntry2.setAsset(new Asset("Class2", "ClassMetadata"));
    thisNamespaceEntry2.setMetadata("NewMeta");
    assertEquals("Class1", thisNamespaceEntry2.getAsset("Class1").getAssetId());
    assertEquals("Class2", thisNamespaceEntry2.getAsset("Class2").getAssetId());
    assertEquals(thisNamespaceEntry1.getMetadata(), "Metadata");
    assertEquals(thisNamespaceEntry2.getMetadata(), "NewMeta");

    assertNotEquals(null, thisNamespaceEntry1);
    assertNotEquals("", thisNamespaceEntry1);
    assertNotEquals(thisNamespaceEntry1, thisNamespaceEntry2);

    thisNamespaceEntry2 = new NamespaceEntry(thisNamespaceEntry1);
    assertEquals(thisNamespaceEntry1, thisNamespaceEntry2);

    // Shameless coverage
    assertNotNull(thisNamespaceEntry1.toString());
  }


  @Test
  public void namespaceEntryUpdateHeight() throws Exception {

    NamespaceEntry thisNamespaceEntry = new NamespaceEntry("", "", "");

    assertEquals(thisNamespaceEntry.getBlockUpdateHeight(), -1);

    thisNamespaceEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisNamespaceEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    NamespaceEntry newNamespaceEntry = new NamespaceEntry(thisNamespaceEntry);

    assertEquals(newNamespaceEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    newNamespaceEntry = (new NamespaceEntry.Decoder()).decode(new MPWrappedArrayImpl(thisNamespaceEntry.encode(0L)));

    assertEquals(newNamespaceEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<NamespaceEntry> namespaceEntryTree = stateSnapshot.getNamespaces();

    namespaceEntryTree.add(new NamespaceEntry("newAddress", "", ""));

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot
        .finalizeBlock(new Block(state1.getChainId(), state1.getHeight(), state1.getLoadedHash(), null, new Object[0], new Object[0],
            new Object[0], 0, "",
            null, new Object[0], new Object[0], new Object[0], new Object[0], new Object[0]
        ));

    stateSnapshot = state1.createSnapshot();

    namespaceEntryTree = stateSnapshot.getNamespaces();

    newNamespaceEntry = namespaceEntryTree.find("newAddress");

    assertEquals(42, newNamespaceEntry.getBlockUpdateHeight());
  }


  @Test
  public void setAddress() {
    assertEquals("address", entry.getAddress());
    entry.setAddress("new address");
    assertEquals("new address", entry.getAddress());
  }


  @Before
  public void setUp() throws Exception {
    Defaults.reset();
    fileStateLoaded = new FileStateLoader();

    stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";
  }


  @After
  public void tearDown() {

    Defaults.reset();

  }

}
