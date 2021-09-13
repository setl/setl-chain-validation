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

import static io.setl.bc.pychain.state.entry.AddressEntry.addDefaultAddressEntry;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_CLASSES;
import static io.setl.common.CommonPy.VersionConstants.VERSION_SET_ADDRESS_TIME;
import static io.setl.common.CommonPy.VersionConstants.VERSION_USE_UPDATE_HEIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({"ConstantConditions", "unlikely-arg-type"})
public class AddressEntryTest {

  String address1 = "TestAddress1";

  String address2 = "TestAddress2";

  MessageDigest digest;

  FileStateLoader fileStateLoaded;

  SerialiseToByte hashSerialiser;

  String stateFile;


  @Test
  public void addBalanceOnlyAddressEntryTest() throws Exception {

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    // addBalanceOnlyAddressEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
    // version 'VERSION_SET_FULL_ADDRESS' (4).

    AddressEntry testEntry = AddressEntry.addBalanceOnlyAddressEntry(assetBalanceTree, address1, 3);

    MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

    AddressEntry thisAddressEntry = assetBalances.findAndMarkUpdated(address1);

    assertTrue(thisAddressEntry != null);

    assertTrue(thisAddressEntry.isNonceUnset());
    assertTrue(thisAddressEntry.getAddress().equals(address1));
    assertTrue(thisAddressEntry.getClassBalance() != null);
    assertTrue(thisAddressEntry.getClassBalance().size() == 0);

  }


  @Test
  public void addDefaultAddressEntryTest() throws Exception {

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    AddressEntry testEntry = AddressEntry.addDefaultAddressEntry(assetBalanceTree, address1, VERSION_SET_ADDRESS_TIME - 1);

    MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

    AddressEntry thisAddressEntry = assetBalances.findAndMarkUpdated(address1);

    assertTrue(thisAddressEntry != null);

    assertFalse(thisAddressEntry.isNonceUnset());
    assertTrue(thisAddressEntry.getAddress().equals(address1));
    assertTrue(thisAddressEntry.getClassBalance() != null);
    assertTrue(thisAddressEntry.getClassBalance().size() == 0);
    assertTrue(thisAddressEntry.getAddressPermissions() == 0);
    assertTrue(thisAddressEntry.getAddressMetadata() == null);
    assertTrue(thisAddressEntry.getUpdateTime() == null);

  }


  @Test
  public void addDefaultAddressEntryVersion4Test() throws Exception {

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    AddressEntry testEntry = AddressEntry.addDefaultAddressEntry(assetBalanceTree, address1, VERSION_SET_ADDRESS_TIME);

    MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

    AddressEntry thisAddressEntry = assetBalances.findAndMarkUpdated(address1);

    assertTrue(thisAddressEntry != null);

    assertFalse(thisAddressEntry.isNonceUnset());
    assertTrue(thisAddressEntry.getAddress().equals(address1));
    assertTrue(thisAddressEntry.getClassBalance() != null);
    assertTrue(thisAddressEntry.getAddressMetadata() == null);
    assertTrue(thisAddressEntry.getUpdateTime().equals(0L));
    assertTrue(thisAddressEntry.getClassBalance().size() == 0);
    assertTrue(thisAddressEntry.getAddressPermissions() == 0);

  }


  @Test
  public void addDefaultNonceEntryTest() throws Exception {

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    // addDefaultNonceEntry is called for legacy reasons. Note that this method is version aware and will add a fully formed Address as of
    // version 'VERSION_SET_FULL_ADDRESS' (4).

    AddressEntry testEntry = AddressEntry.addDefaultNonceEntry(assetBalanceTree, address1, 3);

    MutableMerkle<AddressEntry> assetBalances = stateSnapshot.getAssetBalances();

    AddressEntry thisAddressEntry = assetBalances.findAndMarkUpdated(address1);

    assertTrue(thisAddressEntry != null);

    assertFalse(thisAddressEntry.isNonceUnset());
    assertTrue(thisAddressEntry.getAddress().equals(address1));
    assertTrue(thisAddressEntry.getClassBalance() == null);
    assertTrue(thisAddressEntry.getAddressPermissions() == 0);

  }


  @Test
  public void addressEntryConstructor1() throws Exception {

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    AddressEntry thisAddressEntry = new AddressEntry();

    assertTrue(thisAddressEntry != null);

    assertTrue(thisAddressEntry.isNonceUnset());
    assertTrue(thisAddressEntry.getAddress().isEmpty());
    assertTrue(thisAddressEntry.getClassBalance() != null);
    assertTrue(thisAddressEntry.getAddressPermissions() == 0);

  }


  @Test
  public void addressEntryEquals() throws Exception {

    int index = 42;
    long nonce = 100;
    long highNonce = 200;
    long lowNonce = 300;
    long addressPerms = 1357908642L;
    EnumSet<TxType> txPerms = EnumSet.of(TxType.ADD_X_CHAIN, TxType.REGISTER_ASSET_CLASS, TxType.UPDATE_ASSET_CLASS);

    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    AddressEntry thisAddressEntry = new AddressEntry(address1, nonce, highNonce, lowNonce);
    thisAddressEntry.makeClassBalance();
    thisAddressEntry.setAssetBalance("a1", new Balance(4242L));
    thisAddressEntry.setAddressPermissions(addressPerms);
    thisAddressEntry.setAuthorisedTx(txPerms);
    thisAddressEntry.setUpdateTime(42L);
    thisAddressEntry.setAddressMetadata("somedata");
    assertTrue(thisAddressEntry != null);

    AddressEntry copyAddressEntry;

    assertFalse(thisAddressEntry.equals(null));
    assertFalse(thisAddressEntry.equals(""));

    copyAddressEntry = new AddressEntry(address2, nonce, highNonce, lowNonce);
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry = new AddressEntry(address1, nonce + 1, highNonce, lowNonce);
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry = new AddressEntry(address1, nonce, highNonce + 1, lowNonce);
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry = new AddressEntry(address1, nonce, highNonce, lowNonce + 1);
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry = new AddressEntry(address1, nonce, highNonce, lowNonce);
    copyAddressEntry.makeClassBalance();
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry.setAssetBalance("a1", new Balance(99L));
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry = new AddressEntry(address1, nonce, highNonce, lowNonce);
    copyAddressEntry.makeClassBalance();
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry.setAssetBalance("a1", new Balance(4242L));
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry.setAddressPermissions(addressPerms);
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry.setAddressMetadata("somedata");
    assertFalse(thisAddressEntry.equals(copyAddressEntry));

    copyAddressEntry.setUpdateTime(42L);
    assertTrue(thisAddressEntry.equals(copyAddressEntry));

    //
    assertTrue(thisAddressEntry.getAssetBalance("a1").longValue() == 4242L);
    assertEquals(highNonce, thisAddressEntry.getHighPriorityNonce());
    assertEquals(lowNonce, thisAddressEntry.getLowPriorityNonce());
    assertTrue(thisAddressEntry.canUseTx(TxType.REGISTER_ASSET_CLASS));
    assertFalse(thisAddressEntry.canUseTx(TxType.CANCEL_CONTRACT));

  }


  @Test
  public void addressEntryUpdateHeight() throws Exception {

    AddressEntry thisAddressEntry = new AddressEntry();

    assertEquals(thisAddressEntry.getBlockUpdateHeight(), -1);

    thisAddressEntry.setBlockUpdateHeight(Long.MAX_VALUE);

    assertEquals(thisAddressEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    AddressEntry newAddressEntry = new AddressEntry(thisAddressEntry);

    assertEquals(newAddressEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    newAddressEntry = (new AddressEntry.Decoder()).decode(new MPWrappedArrayImpl(thisAddressEntry.encode(0L)));

    assertEquals(newAddressEntry.getBlockUpdateHeight(), Long.MAX_VALUE);

    // Create a state with version : VERSION_USE_UPDATE_HEIGHT
    ObjectEncodedState state1 = fileStateLoaded.loadStateFromFile(stateFile);

    Object[] encoded = state1.encode();
    encoded[1] = VERSION_USE_UPDATE_HEIGHT; // version
    encoded[2] = 42; // height
    state1 = ObjectEncodedState.decode(new MPWrappedArrayImpl(encoded));

    // Phew...

    StateSnapshot stateSnapshot = state1.createSnapshot();

    MutableMerkle<AddressEntry> assetBalanceTree = stateSnapshot.getAssetBalances();

    newAddressEntry = addDefaultAddressEntry(assetBalanceTree, "newAddress", stateSnapshot.getVersion());

    stateSnapshot.commit();

    state1 = (ObjectEncodedState) stateSnapshot.finalizeBlock(new Block(state1.getChainId(),
        state1.getHeight(),
        state1.getLoadedHash(),
        null,
        new Object[0],
        new Object[0],
        new Object[0],
        0,
        "",
        null,
        new Object[0],
        new Object[0],
        new Object[0],
        new Object[0],
        new Object[0]
    ));

    stateSnapshot = state1.createSnapshot();

    assetBalanceTree = stateSnapshot.getAssetBalances();

    newAddressEntry = assetBalanceTree.find("newAddress");

    assertTrue(newAddressEntry.getBlockUpdateHeight() == 42);
  }


  @Test
  public void balanceConstructors() throws Exception {

    int index = 42;
    final int biggerIndex = 420;

    Balance thisBalance = new Balance(index);

    assertTrue(thisBalance.longValue() == index);

    thisBalance = new Balance(BigInteger.valueOf(index));

    assertTrue(thisBalance.longValue() == index);

    // CompareTo

    assertTrue(thisBalance.compareTo(Long.MIN_VALUE) > 0);
    assertTrue(thisBalance.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) > 0);
    assertTrue(thisBalance.compareTo(1) > 0);
    assertTrue(thisBalance.compareTo(1L) > 0);
    assertTrue(thisBalance.compareTo(1D) > 0);
    assertTrue(thisBalance.compareTo(index) == 0);
    assertTrue(thisBalance.compareTo((long) index) == 0);
    assertTrue(thisBalance.compareTo((double) index) == 0);
    assertTrue(thisBalance.compareTo(BigInteger.valueOf(index)) == 0);
    assertTrue(thisBalance.compareTo(index + 1) < 0);
    assertTrue(thisBalance.compareTo(biggerIndex) < 0);
    assertTrue(thisBalance.compareTo(Long.MAX_VALUE) < 0);
    assertTrue(thisBalance.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0);

    // Test add()

    thisBalance = thisBalance.add(index);

    assertTrue(thisBalance.longValue() == (index * 2));

    // Upgrade to BigInt.

    assertTrue(thisBalance.bigintValue().equals(BigInteger.valueOf((index * 2))));

    thisBalance = thisBalance.add(Long.MAX_VALUE);

    BigInteger testBigInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(index * 2));

    assertTrue(thisBalance.getValue().equals(testBigInt));

    // More compare

    assertTrue(thisBalance.compareTo(Long.MIN_VALUE) > 0);
    assertTrue(thisBalance.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) > 0);
    assertTrue(thisBalance.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
    assertTrue(thisBalance.compareTo(1) > 0);
    assertTrue(thisBalance.compareTo(1L) > 0);
    assertTrue(thisBalance.compareTo(1D) > 0);
    assertTrue(thisBalance.compareTo(testBigInt) == 0);

    BigInteger testBiggerInt = testBigInt.add(BigInteger.valueOf(1));

    assertTrue(thisBalance.compareTo(testBiggerInt) < 0);

    // Revert Balance to Long

    thisBalance = thisBalance.add(-index * 2);

    testBigInt = testBigInt.add(BigInteger.valueOf(-index * 2));

    assertTrue(thisBalance.getValue().equals(Long.MAX_VALUE));
    assertTrue(thisBalance.bigintValue().equals(testBigInt));
    assertTrue(thisBalance.compareTo(testBigInt) == 0);

  }


  @Test
  public void jackson() throws IOException {
    AddressEntry entry = new AddressEntry("Ipswich");
    entry.setAuthorisedTx(EnumSet.of(TxType.REGISTER_ADDRESS, TxType.REGISTER_ASSET_CLASS, TxType.WFL_HOLD13, TxType.DELETE_NAMESPACE,
        TxType.ISSUE_AND_ENCUMBER_ASSET
    ));
    Map<String, Balance> balances = entry.makeClassBalance();
    balances.put("asset", new Balance(99));
    balances.put("thing", new Balance(1234567890));
    balances.put("bigThing", new Balance("123456789012345678901234567890"));
    entry.setUpdateTime(1_000_000_000L);
    entry.setAddressMetadata("Fee Fi Fo Fum");
    entry.setAddressPermissions(AP_CLASSES);
    entry.setNonce(100);

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(entry);

    AddressEntry other = mapper.readValue(json, AddressEntry.class);
    String json2 = mapper.writeValueAsString(other);
    assertEquals(json,json2);
    assertEquals(entry, other);
  }


  @Test
  public void pack() throws IOException {
    AddressEntry entry = new AddressEntry("Ipswich");
    entry.setAuthorisedTx(EnumSet.of(TxType.REGISTER_ADDRESS, TxType.REGISTER_ASSET_CLASS, TxType.WFL_HOLD13, TxType.DELETE_NAMESPACE,
        TxType.ISSUE_AND_ENCUMBER_ASSET
    ));
    byte[] binary = MsgPackUtil.pack(entry);
    AddressEntry copy = new AddressEntry(MsgPackUtil.newUnpacker(binary));
    assertEquals(entry, copy);

    entry = new AddressEntry("Ipswich", 5, 4, 3);
    entry.setUpdateTime(Instant.now().getEpochSecond());
    binary = MsgPackUtil.pack(entry);
    copy = new AddressEntry(MsgPackUtil.newUnpacker(binary));
    assertEquals(entry, copy);

    entry = new AddressEntry();
    entry.setAddressMetadata("Fee Fi Fo Fum");
    Map<String, Balance> balances = entry.makeClassBalance();
    balances.put("asset", new Balance(99));
    balances.put("thing", new Balance(1234567890));
    binary = MsgPackUtil.pack(entry);
    copy = new AddressEntry(MsgPackUtil.newUnpacker(binary));
    assertEquals(entry, copy);
  }


  @Ignore
  @Test
  public void perfTest() throws Exception {

    long l1 = 0L;
    Long v1 = 0L;
    Random r1 = new Random();
    Balance b1 = new Balance(0L);
    Balance b2 = new Balance(Long.MAX_VALUE);

    long t1 = java.lang.System.currentTimeMillis();

    for (int i = 0; i < 100000000; i++) {
      v1 = v1 + r1.nextInt(1000);
    }

    long t2 = java.lang.System.currentTimeMillis();

    for (int i = 0; i < 100000000; i++) {
      b1 = b1.add(r1.nextInt(1000));
    }

    long t3 = java.lang.System.currentTimeMillis();

    for (int i = 0; i < 100000000; i++) {
      l1 += r1.nextInt(1000);
    }

    long t4 = java.lang.System.currentTimeMillis();

    for (int i = 0; i < 100000000; i++) {
      b2 = b2.add(r1.nextLong());
    }

    long t5 = java.lang.System.currentTimeMillis();

    Long o1 = t2 - t1;
    Long o2 = t3 - t2;
    Long o3 = t4 - t3;
    Long o4 = t5 - t4;

    System.out.print("\nO1 Long\n");
    System.out.print(o1.toString());
    System.out.print("\nO2 Balance\n");
    System.out.print(o2.toString());
    System.out.print("\nO3 long\n");
    System.out.print(o3.toString());
    System.out.print("\nO4 Balance BigInt\n");
    System.out.print(o4.toString());
    System.out.print("\nv1 Long\n");
    System.out.print(v1.toString());
    System.out.print("\nv2 Balance\n");
    System.out.print(b1.getValue().toString());
    System.out.print("\nv3 long\n");
    System.out.print(((Long) l1).toString());
    System.out.print("\nv4 Balance\n");
    System.out.print(b2.getValue().toString());

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


  @Test
  public void toJson() {
    AddressEntry addressEntry = new AddressEntry("Manchester");
    addressEntry.setUpdateTime(100000L);
    Map<String, Balance> balances = addressEntry.makeClassBalance();
    balances.put("asset", new Balance(99));

    String jsonText = addressEntry.toJSON().toJSONString();
    assertEquals("{\"address\":\"Manchester\",\"addressmetadata\":null,\"addresspermissions\":0,\"balances\":"
        + "{\"asset\":99},\"nonce\":null,\"noncehighpriority\":null,\"noncelowpriority\":null,\"permissionedtxs\":null,"
        + "\"updatetime\":100000}", jsonText);

    addressEntry.setLowPriorityNonce(5L);
    addressEntry.setHighPriorityNonce(5L);
    addressEntry.setNonce(5L);

    // Setting update time backwards should have no effect
    addressEntry.setUpdateTime(99999L);

    jsonText = addressEntry.toJSON().toJSONString();
    assertEquals("{\"address\":\"Manchester\",\"addressmetadata\":null,\"addresspermissions\":0,\"balances\":"
        + "{\"asset\":99},\"nonce\":5,\"noncehighpriority\":5,\"noncelowpriority\":5,\"permissionedtxs\":null,"
        + "\"updatetime\":100000}", jsonText);

  }
}
