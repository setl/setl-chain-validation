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
import static org.junit.Assert.assertFalse;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.CommonPy.TxType;
import java.security.MessageDigest;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaEntryTest {
  
  private MessageDigest digest;
  private SerialiseToByte hashSerialiser;
  private FileStateLoader fileStateLoaded;
  private String stateFile;


  @Before
  public void setUp() throws Exception {

    stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();
    
    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
    
  }
  
  @After
  public void tearDown() throws Exception {
    
    Defaults.reset();
    
  }
  
  @Test
  public void copy() throws Exception {

    PoaEntry thisEntry = new PoaEntry(1, "fred");

    assertEquals(thisEntry.getBlockUpdateHeight(), -1);

    thisEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    PoaEntry newCopy = new PoaEntry(thisEntry);

    assertEquals(newCopy.getBlockUpdateHeight(), Long.MAX_VALUE);

    newCopy = (new PoaEntry.Decoder()).decode(new MPWrappedArrayImpl(thisEntry.encode(0L)));

    assertEquals(newCopy.getBlockUpdateHeight(), Long.MAX_VALUE);
  }

  @Test
  public void construct() throws Exception {

    PoaEntry thisEntry = new PoaEntry(1, "fred");
    
    assertTrue(thisEntry.index == 1);
    assertTrue(thisEntry.itemKey.equalsIgnoreCase("fred"));
    assertTrue(thisEntry.getKey().equalsIgnoreCase("fred"));
    
    ArrayList<String> assets = new ArrayList<>();
    assets.add("Asset1");
    assets.add("Asset2");
    
    PoaItem item1 = new PoaItem(TxType.CREATE_MEMO, 10000, assets);
    
    thisEntry.setDetail("Reference", "Address", "AttorneyAddress", 0, Integer.MAX_VALUE, new PoaItem[] {item1});
    
    PoaEntry copyEntry = new PoaEntry(new MPWrappedArrayImpl(thisEntry.encode(1)));
    PoaEntry copyEntry2 = new PoaEntry(thisEntry);
    
    assertTrue(thisEntry.getFullReference("X").equalsIgnoreCase(copyEntry.getFullReference("X")));
    assertTrue(thisEntry.getFullReference("X").equalsIgnoreCase(copyEntry2.getFullReference("X")));
    
    assertTrue(thisEntry.getPoaDetail().equals(copyEntry.getPoaDetail()));
    assertTrue(thisEntry.getFullReference("X").equalsIgnoreCase(copyEntry2.getFullReference("X")));
    
    // Is a details item, does not have references
    assertTrue(!thisEntry.hasReference("Reference"));
    assertTrue(thisEntry.getReference("Reference") == null);
    assertTrue(thisEntry.removeReference("Reference") == false);
    assertTrue(thisEntry.getReferenceCount() == 0);
    
    
    thisEntry = new PoaEntry(1, "fred");
    thisEntry.setReference("Reference", 0, 1000L);
    
    copyEntry = new PoaEntry(new MPWrappedArrayImpl(thisEntry.encode(1)));
    copyEntry2 = new PoaEntry(thisEntry);
    PoaEntry copyEntry4 = null;
    PoaEntry copyEntry3 = new PoaEntry(copyEntry4);
    
    assertTrue(copyEntry3.index == -1L);
    assertTrue(copyEntry3.itemKey.equals(""));
    assertTrue(copyEntry2.getReferenceCount() == 1);
    assertTrue(copyEntry2.hasReference("Reference"));
    assertTrue(!copyEntry2.hasReference("Referenc2"));
    assertTrue(copyEntry2.getReference("Reference").equals(new PoaHeader("Reference", "", 0, 1000L)));
    assertTrue(copyEntry2.removeReference("Reference"));
    assertFalse(copyEntry2.hasReference("Reference"));
  
    // This causes getPoaMap() to be exercised.
    copyEntry = new PoaEntry(new MPWrappedArrayImpl(thisEntry.encode(1)));
    assertTrue(copyEntry.getReference("Reference").expiryDate == 1000L);
  
  }
  
  @Test
  public void poaHeader() throws Exception {
  
    PoaHeader p1 = new PoaHeader("Ref", "Hash", 1L, 1000L);
    assertTrue(p1.reference.equals("Ref"));
    assertTrue(p1.hash.equals("Hash"));
    assertTrue(p1.startDate == 1L);
    assertTrue(p1.expiryDate == 1000L);
  
    // Default
    PoaHeader p2 = new PoaHeader(new Object[0]);
    assertTrue(p2.reference.equals("<Bad Constructor>"));
    assertTrue(p2.hash.equals(""));
    assertTrue(p2.startDate == 0L);
    assertTrue(p2.expiryDate == 0L);
  
    p2 = new PoaHeader(p1.encode());
    assertTrue(p2.reference.equals("Ref"));
    assertTrue(p2.hash.equals("Hash"));
    assertTrue(p2.startDate == 1L);
    assertTrue(p2.expiryDate == 1000L);
  
    PoaHeader p3 = null;
    p2 = new PoaHeader(p3);
    assertTrue(p2.reference.equals("<Bad Constructor>"));
    assertTrue(p2.hash.equals(""));
    assertTrue(p2.startDate == 0L);
    assertTrue(p2.expiryDate == 0L);
    
    // Equals
    assertFalse(p1.equals(null));
    assertFalse(p1.equals("null"));
  
    p2 = new PoaHeader("RefX", "Hash", 1L, 1000L);
    assertFalse(p1.equals(p2));
  
    p2 = new PoaHeader("Ref", "HashX", 1L, 1000L);
    assertFalse(p1.equals(p2));
  
    p2 = new PoaHeader("Ref", "Hash", 2L, 1000L);
    assertFalse(p1.equals(p2));
  
    p2 = new PoaHeader("Ref", "Hash", 1L, 1001L);
    assertFalse(p1.equals(p2));
  
    p2 = new PoaHeader("Ref", "Hash", 1L, 1000L);
    assertTrue(p1.equals(p2));
  
    //
    assertTrue(p1.hashCode() == p1.reference.hashCode());
    
  }

  @Test
  public void contractEntryUpdateHeight() throws Exception {

    PoaEntry thisPoaEntry = new PoaEntry(0L, "");

    assertEquals(thisPoaEntry.getBlockUpdateHeight(), -1);

    thisPoaEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisPoaEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    PoaEntry newPoaEntry = new PoaEntry(thisPoaEntry);

    assertEquals(newPoaEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    newPoaEntry = (new PoaEntry.Decoder()).decode(new MPWrappedArrayImpl(thisPoaEntry.encode(0L)));

    assertEquals(newPoaEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<PoaEntry> poaList = stateSnapshot.getPowerOfAttorneys();

    PoaEntry newPoa = new PoaEntry(0L, "newAddress");
    poaList.add(newPoa);

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot.finalizeBlock(new Block(state1.getChainId(), state1.getHeight(), state1.getLoadedHash(), null, new Object[0],
        new Object[0], new Object[0], 0, "", null, new Object[0], new Object[0], new Object[0], new Object[0], new Object[0]));

    stateSnapshot = state1.createSnapshot();

    poaList = stateSnapshot.getPowerOfAttorneys();

    newPoaEntry = poaList.find("newAddress");

    assertEquals(newPoaEntry.getBlockUpdateHeight() , 42);
  }

}