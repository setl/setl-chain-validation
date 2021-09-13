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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.common.Balance;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class AddressEncumbrancesTest {

  private ArrayList<EncumbranceDetail> administrators;

  private ArrayList<EncumbranceDetail> administrators2;

  private ArrayList<EncumbranceDetail> beneficiaries;

  private ArrayList<EncumbranceDetail> beneficiaries2;

  private ArrayList<EncumbranceDetail> beneficiaries3;

  private FileStateLoader fileStateLoaded;

  private String stateFile;

  private AssetEncumbrances testAssetEncumbrances;

  private AddressEncumbrances testEncumbrance;


  @Test
  public void constructEncumbranceEntry() throws Exception {

    EncumbranceEntry e1 = new EncumbranceEntry();

    assertTrue(e1.reference.equals(""));
    assertTrue(e1.amount.equalTo(0L));
    assertTrue(e1.getBeneficiaries().isEmpty());
    assertTrue(e1.getAdministrators().isEmpty());
    assertTrue(e1.priority == Integer.MAX_VALUE);
    assertTrue(e1.expiryDate == 0);

    assertTrue(e1.equals(new EncumbranceEntry(e1.encode())));
    assertFalse(e1.equals(null));
    assertFalse(e1.equals("null"));

    e1 = new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators);
    EncumbranceEntry e2 = new EncumbranceEntry("thisReferenceX", 100L, beneficiaries, administrators);

    assertFalse(e1.equals(e2));

    e2 = new EncumbranceEntry("thisReference", 101L, beneficiaries, administrators);
    assertFalse(e1.equals(e2));

    e2 = new EncumbranceEntry(e1.encode());
    e2.priority = e1.priority + 1;
    assertFalse(e1.equals(e2));

    e2 = new EncumbranceEntry("thisReference", 101L, beneficiaries3, administrators);
    assertFalse(e1.equals(e2));

    // Check that it's not been messed up
    e2 = new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators);
    assertTrue(e1.equals(e2));

    e2 = new EncumbranceEntry("thisReference", 101L, beneficiaries2, administrators);
    assertFalse(e1.equals(e2));

    e2 = new EncumbranceEntry("thisReference", 101L, beneficiaries, administrators2);
    assertFalse(e1.equals(e2));

    //
    assertFalse(e1.hasExpired(0));
    assertFalse(e1.hasExpired(10000));
    assertTrue(e1.hasExpired(999999));

    //
    assertFalse(e1.isAdministratorValid("admin1", 42L));
    assertFalse(e1.isAdministratorValid("admin1", 420000L));
    assertTrue(e1.isAdministratorValid("admin1", 3425L));
    assertTrue(e1.isAdministratorValid("admin1", 77135L));
    assertTrue(e1.isAdministratorValid("admin2", 77135L));
    assertFalse(e1.isAdministratorValid("admin3", 3425L));
    assertFalse(e1.isAdministratorValid("admin3", 77135L));

    assertFalse(e1.isBeneficiaryValid("ben1", 42L));
    assertFalse(e1.isBeneficiaryValid("ben1", 420000L));
    assertTrue(e1.isBeneficiaryValid("ben1", 12345L));
    assertTrue(e1.isBeneficiaryValid("ben1", 56789L));
    assertTrue(e1.isBeneficiaryValid("ben2", 56789L));
    assertFalse(e1.isBeneficiaryValid("ben3", 12345L));
    assertFalse(e1.isBeneficiaryValid("ben3", 56789L));

  }


  @Test
  public void consumeEncumbrance() throws Exception {

    AddressEncumbrances thisTestEncumbrance = new AddressEncumbrances(testEncumbrance);

    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(1L), 321L, new EncumbranceEntry("thisReference", 1L, beneficiaries, administrators), true, false);
    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(1L), 321L, new EncumbranceEntry("dross", 1L, beneficiaries, administrators), true, false);
    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(122L), 321L, new EncumbranceEntry("thisReference", 99L, beneficiaries, administrators), true, false);
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(101L));

    thisTestEncumbrance.getAssetEncumbrance("AB|C").consumeEncumbrance("thisReference", 10L);
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(91L));

    thisTestEncumbrance.getAssetEncumbrance("AB|C").reduceEncumbrance("thisReference", 10L);
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(81L));

    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(123L), 321L, new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators), true, false);
    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(123L), 321L, new EncumbranceEntry("thisReference2", 100L, beneficiaries, administrators), true, false);
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(281L));

    thisTestEncumbrance.getAssetEncumbrance("AB|C").reduceEncumbrance("thisReference", 10L);
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(271L));

    assertTrue(thisTestEncumbrance.getAssetEncumbrance("AB|C").getEncumbranceAmountByReference("thisReference").equalTo(170L));
    assertTrue(thisTestEncumbrance.getAssetEncumbrance("AB|C").getEncumbranceTotal().equalTo(271L));

    //
    thisTestEncumbrance = new AddressEncumbrances(testEncumbrance);

    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(1L), 321L, new EncumbranceEntry("thisReference", 1L, beneficiaries, administrators), false, true);
    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(1L), 321L, new EncumbranceEntry("dross", 1L, beneficiaries, administrators), false, true);

    // Not cumulative, will be ignored...
    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(122L), 321L, new EncumbranceEntry("thisReference", 99L, beneficiaries, administrators), false, true);

    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(2L));
    thisTestEncumbrance.hashCode(); // Just for coverage.
  }


  @Test
  public void contractEntryUpdateHeight() throws Exception {

    AddressEncumbrances thisAddressEncumbrances = new AddressEncumbrances();

    assertEquals(thisAddressEncumbrances.getBlockUpdateHeight(), -1);

    thisAddressEncumbrances.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisAddressEncumbrances.getBlockUpdateHeight(), Long.MAX_VALUE);

    AddressEncumbrances newAddressEncumbrances = new AddressEncumbrances(thisAddressEncumbrances);

    assertEquals(newAddressEncumbrances.getBlockUpdateHeight(), Long.MAX_VALUE);

    newAddressEncumbrances = (new AddressEncumbrances.Decoder()).decode(new MPWrappedArrayImpl(thisAddressEncumbrances.encode(0L)));

    assertEquals(newAddressEncumbrances.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEncumbrances> addressEncumbrancesList = stateSnapshot.getEncumbrances();

    AddressEncumbrances newEncumbrance = new AddressEncumbrances("newAddress");
    addressEncumbrancesList.add(newEncumbrance);

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot.finalizeBlock(new Block(state1.getChainId(), state1.getHeight(), state1.getLoadedHash(), null, new Object[0],
        new Object[0], new Object[0], 0, "", null, new Object[0], new Object[0], new Object[0], new Object[0], new Object[0]));

    stateSnapshot = state1.createSnapshot();

    addressEncumbrancesList = stateSnapshot.getEncumbrances();

    newAddressEncumbrances = addressEncumbrancesList.find("newAddress");

    assertEquals(newAddressEncumbrances.getBlockUpdateHeight(), 42);
  }


  @Test
  public void copy() throws Exception {

    AddressEncumbrances newCopy = testEncumbrance.copy();

    assertTrue(newCopy.equals(testEncumbrance));


  }


  @Test
  public void encode() throws Exception {

    Object[] encoded = testEncumbrance.encode();

    AddressEncumbrances reHydrated = new AddressEncumbrances(encoded);

    assertTrue(testEncumbrance.equals(reHydrated));
  }


  @Test
  public void encumbranceUpdateHeight() throws Exception {

    AddressEncumbrances copy = testEncumbrance.copy();

    assertEquals(copy.getBlockUpdateHeight(), -1);

    copy.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(copy.getBlockUpdateHeight(), Long.MAX_VALUE);

    AddressEncumbrances newCopy = new AddressEncumbrances(copy);

    assertEquals(newCopy.getBlockUpdateHeight(), Long.MAX_VALUE);

    newCopy = (new AddressEncumbrances.Decoder()).decode(new MPWrappedArrayImpl(copy.encode(0L)));

    assertEquals(newCopy.getBlockUpdateHeight(), Long.MAX_VALUE);

  }


  @Test
  public void equals() throws Exception {

    AddressEncumbrances reHydrated = new AddressEncumbrances("");

    assertFalse(testEncumbrance.equals(null));
    assertFalse(testEncumbrance.equals(0L));
    assertFalse(testEncumbrance.equals(reHydrated)); // Index

    reHydrated = new AddressEncumbrances("");
    assertFalse(testEncumbrance.equals(reHydrated)); // Address

    reHydrated = new AddressEncumbrances(testEncumbrance.getAddress());
    assertFalse(testEncumbrance.equals(reHydrated)); // Encumbrances

    reHydrated = new AddressEncumbrances(testEncumbrance);
    assertTrue(reHydrated.setEncumbranceEntry("AB|C",
        new Balance(123L), 321L, new EncumbranceEntry("thisReference", 1L, beneficiaries, administrators), false, false));
    assertFalse(testEncumbrance.equals(reHydrated)); // Encumbrances not the same

    reHydrated = new AddressEncumbrances(testEncumbrance);
    assertTrue(testEncumbrance.equals(reHydrated)); // Encumbrances

  }


  @Test
  public void getAggregateByReference() {

    AddressEncumbrances thisCopy = new AddressEncumbrances(testEncumbrance);
    EncumbranceEntry newEncumbranceentry = new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators);

    // Add Cumulative new entry. "thisReference"
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, true, false);

    newEncumbranceentry = new EncumbranceEntry("reference2", 100L, beneficiaries, administrators);

    // Add Cumulative new entry. "reference2"
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, true, false);

    // Test Cumulative entry.
    EncumbranceEntry aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "thisReference");

    EncumbranceEntry testEncumbranceentry = new EncumbranceEntry("thisReference", 200L, beneficiaries, administrators);
    testEncumbranceentry.priority = 0;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Test new entry added as a Cumulative entry
    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "reference2");

    testEncumbranceentry = new EncumbranceEntry("reference2", 100L, beneficiaries, administrators);
    testEncumbranceentry.priority = 10;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Add Cumulative new entry.
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, true, false);

    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "reference2");

    testEncumbranceentry = new EncumbranceEntry("reference2", 200L, beneficiaries, administrators);
    testEncumbranceentry.priority = 10;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Add Not Cumulative new entry. (Fails)
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, false, false);

    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "reference2");

    testEncumbranceentry = new EncumbranceEntry("reference2", 200L, beneficiaries, administrators);
    testEncumbranceentry.priority = 10;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    //
    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Missing", "reference2");
    assertTrue(aggregateEntry == null);

  }


  @Test
  public void getAggregateByReference2() {

    // Adds Encumbrances in different order so that they do not consolidate.

    AddressEncumbrances thisCopy = new AddressEncumbrances(testEncumbrance);

    EncumbranceEntry newEncumbranceentry = new EncumbranceEntry("reference2", 100L, beneficiaries, administrators);

    // Add Cumulative new entry. "reference2"
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, true, false);

    newEncumbranceentry = new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators);

    // Add Cumulative new entry. "thisReference"
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, true, false);

    // Test Cumulative entry.
    EncumbranceEntry aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "thisReference");

    EncumbranceEntry testEncumbranceentry = new EncumbranceEntry("thisReference", 200L, beneficiaries, administrators);
    testEncumbranceentry.priority = 0;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Test new entry added as a Cumulative entry
    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "reference2");

    testEncumbranceentry = new EncumbranceEntry("reference2", 100L, beneficiaries, administrators);
    testEncumbranceentry.priority = 10;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Add Cumulative new entry.
    newEncumbranceentry = new EncumbranceEntry("reference2", 100L, beneficiaries, administrators);
    thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, true, false);

    // Check Availabilities, should reflect interweaved Encumbrances

    AssetEncumbrances thisAss = thisCopy.getEncumbranceList().get("Namespace1|Asset1");
    assertTrue(thisAss.availableToEncumbrance(200L, null, 0L).equalTo(0));
    assertTrue(thisAss.availableToEncumbrance(0L, "reference2", 0L).equalTo(0));
    assertTrue(thisAss.availableToEncumbrance(100L, "reference2", 0L).equalTo(0));
    assertTrue(thisAss.availableToEncumbrance(200L, "reference2", 0L).equalTo(100L));
    assertTrue(thisAss.availableToEncumbrance(250L, "reference2", 0L).equalTo(100L));
    assertTrue(thisAss.availableToEncumbrance(300L, "reference2", 0L).equalTo(100L));
    assertTrue(thisAss.availableToEncumbrance(350L, "reference2", 0L).equalTo(150L));
    assertTrue(thisAss.availableToEncumbrance(500L, "reference2", 0L).equalTo(200L));

    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "reference2");

    testEncumbranceentry = new EncumbranceEntry("reference2", 200L, beneficiaries, administrators);
    testEncumbranceentry.priority = 10;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Try to add Not Cumulative new entry. This will not be added as the reference exists.
    newEncumbranceentry = new EncumbranceEntry("reference2", 100L, beneficiaries, administrators);
    assertFalse(thisCopy.getEncumbranceList().get("Namespace1|Asset1").addEncumbrance(newEncumbranceentry, 0L, 0, false, false));

    aggregateEntry = thisCopy.getAggregateByReference("Namespace1|Asset1", "reference2");

    testEncumbranceentry = new EncumbranceEntry("reference2", 200L, beneficiaries, administrators);
    testEncumbranceentry.priority = 10;
    assertTrue(aggregateEntry.equals(testEncumbranceentry));

    // Test availabilities
    // Check Availabilities, should reflect consolidated Encumbrances

    thisAss = thisCopy.getEncumbranceList().get("Namespace1|Asset1");
    assertTrue(thisAss.availableToEncumbrance(0L, "reference2", 0L).equalTo(0));
    assertTrue(thisAss.availableToEncumbrance(75L, "reference2", 0L).equalTo(0));
    assertTrue(thisAss.availableToEncumbrance(100L, "reference2", 0L).equalTo(0));
    assertTrue(thisAss.availableToEncumbrance(250L, "reference2", 0L).equalTo(100));
    assertTrue(thisAss.availableToEncumbrance(300L, "reference2", 0L).equalTo(100));
    assertTrue(thisAss.availableToEncumbrance(350L, "reference2", 0L).equalTo(150));

    // Check getAggregateAvailableByReference()

    assertNull(thisCopy.getAggregateAvailableByReference("Namespace1|Asset1", "xxx", 0L));
    // If hold zero, zero is available
    assertEquals(0L, thisCopy.getAggregateAvailableByReference("Namespace1|Asset1", "reference2", 0L).amount.longValue());

    // If has 250, we have 100 encumbered to thisReference, 100 to reference2, and then 100 encumbered to thisReference, so only 100 available
    assertEquals(100L, thisCopy.getAggregateAvailableByReference("Namespace1|Asset1", "reference2", 250L).amount.longValue());

    // If has 300, we have 100 encumbered to thisReference, 100 to reference2, and then 100 encumbered to thisReference, so still only 100 available
    assertEquals(100L, thisCopy.getAggregateAvailableByReference("Namespace1|Asset1", "reference2", 300L).amount.longValue());

    // If has 350, we have 100 encumbered to thisReference, 100 to reference2, and then 100 encumbered to thisReference, then another to reference2 so now has
    // only 150 available
    assertEquals(150L, thisCopy.getAggregateAvailableByReference("Namespace1|Asset1", "reference2", 350L).amount.longValue());
  }


  @Test
  public void getEncumbranceAmountByReference() {

    AddressEncumbrances thisCopy = new AddressEncumbrances(testEncumbrance);

    assertTrue(thisCopy.getEncumbranceAmountByReference("Namespace1", "Asset1", "thisReference").equalTo(100L));

  }


  @Test
  public void getEncumbranceByReference() throws Exception {

    AddressEncumbrances thisCopy = new AddressEncumbrances(testEncumbrance);

    EncumbranceEntry foundEntry = thisCopy.getEncumbranceByReference("Namespace1", "Asset1", "thisReference").get(0);
    EncumbranceEntry copythis = new EncumbranceEntry("acopy", foundEntry.amount, new ArrayList<>(), new ArrayList<>());

    thisCopy.getAssetEncumbrance("Namespace1", "Asset1").addEncumbrance(copythis, 0L, 0, false, false);

    EncumbranceEntry thisEntry = thisCopy.getEncumbranceByReference("Namespace1", "Asset1", "thisReference").get(0);
    assertTrue(thisEntry.reference.equals("thisReference"));
    assertTrue(thisEntry.amount.equalTo(100L));

    thisEntry = thisCopy.getEncumbranceByReference("Namespace1", "Asset1", "acopy").get(0);
    assertTrue(thisEntry.reference.equals("acopy"));
    assertTrue(thisEntry.amount.equalTo(100L));

  }


  @Test
  public void getEncumbranceTotal() throws Exception {

    AddressEncumbrances thisCopy = new AddressEncumbrances(testEncumbrance);

    assertTrue(thisCopy.getEncumbranceTotal("testaddress", "Namespace1", "Asset1", 0).equalTo(100L));

    EncumbranceEntry foundEntry = thisCopy.getEncumbranceByReference("Namespace1", "Asset1", "thisReference").get(0);
    EncumbranceEntry copythis = new EncumbranceEntry("acopy", foundEntry.amount, new ArrayList<>(), new ArrayList<>());

    thisCopy.getAssetEncumbrance("Namespace1", "Asset1").addEncumbrance(copythis, 0L, 0, false, false);

    assertTrue(thisCopy.getAssetEncumbrance("Namespace1|Asset1").getEncumbranceAmountByReference("acopy").equalTo(100L));

    assertTrue(thisCopy.getEncumbranceTotal("testaddress", "Namespace1", "Asset1", 0).equalTo(200L));

  }


  @Test
  public void getKey() throws Exception {

    String thisKey = testEncumbrance.getKey();

    assertTrue(thisKey.equals("testaddress"));
  }


  @Test
  public void setAssetEncumbrance() throws Exception {

    AddressEncumbrances thisTestEncumbrance = new AddressEncumbrances(testEncumbrance);
    AssetEncumbrances testAssetEncumbrances = new AssetEncumbrances();
    testAssetEncumbrances.addEncumbrance(new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators), 0L, 0, true, true);

    // AssetEncumbrances Constructor test
    AssetEncumbrances testAssetEncumbrancesCopy =
        new AssetEncumbrances(testAssetEncumbrances.getTotalAmount(44444L),
            testAssetEncumbrances.getByReference("thisReference"));

    assertTrue(testAssetEncumbrancesCopy.equals(testAssetEncumbrances));

    //
    testAssetEncumbrancesCopy = new AssetEncumbrances((ArrayList<EncumbranceEntry>) null);
    assertTrue(testAssetEncumbrancesCopy.getTotalAmount(0L).equalTo(0));

    //

    assertFalse(thisTestEncumbrance.setAssetEncumbrance("Namespace1|Asset1", null));
    assertTrue(thisTestEncumbrance.setAssetEncumbrance("Namespace1|Asset1", testAssetEncumbrances));

    AssetEncumbrances testAssetEncumbrances2 = new AssetEncumbrances(testAssetEncumbrances.getByReference("thisReference"));

    assertTrue(testAssetEncumbrances2.equals(testAssetEncumbrances));

    testAssetEncumbrances.removeEncumbrance("thisReference");
    testAssetEncumbrances2.removeEncumbrance("thisReference");

    assertTrue(testAssetEncumbrances2.equals(testAssetEncumbrances));

    // Some failing comparisons
    assertFalse(testAssetEncumbrances2.equals(null));
    assertFalse(testAssetEncumbrances2.equals("null"));

    // Test some fails
    assertFalse(testAssetEncumbrances.addEncumbrance(null, 100L, 0, true, false));
    assertFalse(testAssetEncumbrances.addEncumbrance(new EncumbranceEntry(null, 100L, beneficiaries, administrators), 100L, 0, true, false));

    //
    testAssetEncumbrances.addEncumbrance(new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators), 100L, 0, true, false);
    testAssetEncumbrances.addEncumbrance(new EncumbranceEntry("thisReference2", 100L, beneficiaries, administrators), 100L, 0, true, false);
    assertFalse(testAssetEncumbrances2.equals(testAssetEncumbrances));

    testAssetEncumbrances2.addEncumbrance(new EncumbranceEntry("thisReference2", 100L, beneficiaries, administrators), 50L, 0, true, false);
    assertFalse(testAssetEncumbrances2.equals(testAssetEncumbrances));

    testAssetEncumbrances2.addEncumbrance(new EncumbranceEntry("thisReference", 50L, beneficiaries, administrators), 50L, 0, true, false);
    assertFalse(testAssetEncumbrances2.equals(testAssetEncumbrances));

    // Encumbrances in the 'wrong' order
    testAssetEncumbrances2.addEncumbrance(new EncumbranceEntry("thisReference", 50L, beneficiaries, administrators), 100L, 0, true, false);
    assertFalse(testAssetEncumbrances2.equals(testAssetEncumbrances));
    assertTrue(testAssetEncumbrances.getAggregateByReference("thisReference").amount.equalTo(100L));
    assertTrue(testAssetEncumbrances.getAggregateByReference("thisReference2").amount.equalTo(100L));
    assertTrue(testAssetEncumbrances2.getAggregateByReference("thisReference").amount.equalTo(100L));
    assertTrue(testAssetEncumbrances2.getAggregateByReference("thisReference2").amount.equalTo(100L));

    assertTrue(testAssetEncumbrances.getEncumbranceTotal(false).equalTo(200L));
    assertTrue(testAssetEncumbrances.getEncumbranceTotal(true).equalTo(200L));
    assertTrue(testAssetEncumbrances2.getEncumbranceTotal(false).equalTo(200L));
    assertTrue(testAssetEncumbrances2.getEncumbranceTotal(true).equalTo(200L));

    // Now in the right order. Need to consume existing and re-add
    testAssetEncumbrances.consumeEncumbrance("thisReference2", 100);
    testAssetEncumbrances.consumeEncumbrance("thisReference", 100);
    testAssetEncumbrances.addEncumbrance(new EncumbranceEntry("thisReference2", 100L, beneficiaries, administrators), 100L, 0, false, false);
    testAssetEncumbrances.addEncumbrance(new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators), 100L, 0, false, false);
    assertTrue(testAssetEncumbrances2.equals(testAssetEncumbrances));

  }


  @Test
  public void setEncumbranceEntry() throws Exception {

    AddressEncumbrances thisTestEncumbrance;
    thisTestEncumbrance = new AddressEncumbrances(testEncumbrance);

    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(123L), 321L, new EncumbranceEntry("thisReference", -1L, beneficiaries, administrators), false, false);

    // It was a bad EncumbranceEntry.
    assertTrue(thisTestEncumbrance.getEncumbranceByReference("AB|C", "thisReference") == null);

    thisTestEncumbrance.setEncumbranceEntry("AB|C",
        new Balance(123L), 321L, new EncumbranceEntry("thisReference", 1L, beneficiaries, administrators), false, false);

    // Now OK.
    assertTrue(thisTestEncumbrance.getEncumbranceByReference("AB|C", "thisReference") != null);

    assertTrue(thisTestEncumbrance.getEncumbranceTotal("DROSS", "AB|C", 0).equalTo(0));
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|C", 0).equalTo(1L));
    assertTrue(thisTestEncumbrance.getEncumbranceTotal("testaddress", "AB|D", 0).equalTo(0L));

    // Remove it
    thisTestEncumbrance.removeAssetEncumbrance("AB", "C");

    // Not there any more.
    assertTrue(thisTestEncumbrance.getEncumbranceByReference("ABC", "thisReference") == null);

  }


  /**
   * setUp.
   *
   * @throws Exception :
   */
  @Before
  public void setUp() throws Exception {

    fileStateLoaded = new FileStateLoader();

    stateFile = "src/test/resources/test-states/genesis/16/a4aa9a8fa4a8b07848bc5e9635c7b39d8f9692687d354fc03d7d403bdcdba6c0";

    Long totalAmount;

    beneficiaries = new ArrayList<>();
    totalAmount = 100L;
    beneficiaries.add(new EncumbranceDetail("ben1", 12345L, 56789L));
    beneficiaries.add(new EncumbranceDetail("ben2", 23456L, 67890L));

    beneficiaries2 = new ArrayList<>();
    totalAmount = 100L;
    beneficiaries2.add(new EncumbranceDetail("ben1.2", 12345L, 56789L));
    beneficiaries2.add(new EncumbranceDetail("ben2.2", 23456L, 67890L));

    beneficiaries3 = new ArrayList<>();
    totalAmount = 100L;
    beneficiaries3.add(new EncumbranceDetail("ben1", 12345L, 56789L));
    beneficiaries3.add(new EncumbranceDetail("ben2", 23456L, 67890L));
    beneficiaries3.add(new EncumbranceDetail("ben3", 23456L, 67890L));

    administrators = new ArrayList<>();
    administrators.add(new EncumbranceDetail("admin1", 3425L, 77135L));
    administrators.add(new EncumbranceDetail("admin2", 4321L, 87890L));

    administrators2 = new ArrayList<>();
    administrators2.add(new EncumbranceDetail("admin1.2", 3425L, 77135L));
    administrators2.add(new EncumbranceDetail("admin2.2", 4321L, 87890L));

    testAssetEncumbrances = new AssetEncumbrances();
    testAssetEncumbrances.addEncumbrance(new EncumbranceEntry("thisReference", 100L, beneficiaries, administrators), 0L, 0, true, false);

    testEncumbrance = new AddressEncumbrances("testaddress");
    testEncumbrance.setAssetEncumbrance("Namespace1|Asset1", testAssetEncumbrances);
  }


  @Test
  public void jackson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(testEncumbrance);

    AddressEncumbrances other = mapper.readValue(json,AddressEncumbrances.class);
    String json2 = mapper.writeValueAsString(other);
    assertEquals(json,json2);
    assertEquals(testEncumbrance,other);
  }

  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }

}