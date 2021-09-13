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
package io.setl.bc.pychain.state;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.monolithic.LockedAssetsList;
import io.setl.common.TypeSafeMap;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 21/08/2017.
 */
public class AbstractStateTest {

  public static class TestState extends AbstractEncodedState {

    public TestState() {
      super(1, 2, 3, new Hash("1234".getBytes(UTF_8)), new Hash("5678".getBytes(UTF_8)), 1234567890L);
    }


    public TestState(int chainId, int version, int height) {
      super(chainId, version, height);
    }


    @Override
    protected <X extends MEntry> SnapshotMerkle<X> createMerkle(Class<X> name) {
      return null;
    }


    @Override
    public Merkle<AddressEntry> getAssetBalances() {
      return null;
    }


    @Override
    public Merkle<ContractEntry> getContracts() {
      return null;
    }


    @Override
    public Merkle<AddressEncumbrances> getEncumbrances() {
      return null;
    }


    @Override
    public Merkle<LockedAsset> getLockedAssets() {
      return null;
    }


    @Override
    protected AbstractState getMutableCopy() {
      throw new UnsupportedOperationException("Not implemented");
    }


    @Override
    protected MutationHelper getMutationHelper() {
      return null;
    }


    @Override
    public Merkle<NamespaceEntry> getNamespaces() {
      return null;
    }


    @Override
    public Merkle<PoaEntry> getPowerOfAttorneys() {
      return null;
    }


    @Override
    public Merkle<SignNodeEntry> getSignNodes() {
      return null;
    }


    @Override
    public SortedSet<String> pendingEventTimeAddresses(long eventTime) {
      return null;
    }
  }


  /**
   * Add a contract event time to the state.
   *
   * @param state     the state
   * @param address   the address
   * @param eventTime the time
   */
  public static void addContractEventTime(State state, String address, long eventTime) {
    state.getContractTimeEvents().add(address, eventTime);
  }


  AbstractEncodedState instance;


  @Test
  public void constructor1() throws Exception {
    AbstractState a1 = new TestState(1, 2, 3);

    assertTrue(a1.getChainId() == 1);
    assertTrue(a1.getVersion() == 2);
    assertTrue(a1.getHeight() == 3);
  }


  @Test
  public void getBlockHash() throws Exception {
    assertEquals("35363738", instance.getBlockHash().toHexString());
  }


  @Test
  public void getChainId() throws Exception {
    assertEquals(1, instance.getChainId());
  }


  @Test
  public void getConfigHash() throws Exception {
    instance.setEncodedConfig(new MPWrappedMap<>(new Object[]{"foo", "bar"}));
    assertEquals("4c128a3b684d83293df4b078bb6b7bfeae83865c8eb3a82bff04111681e13387", instance.getConfigHash().toHexString());
    instance.setEncodedConfig(new MPWrappedMap<>(new Object[]{"baz", "bat"}));
    assertEquals("3b7b59db44b6e193eacd1f7db3a4187cd98425a6b6a9f0094c41d592b6c15ac7", instance.getConfigHash().toHexString());
  }


  @Test
  public void getConfigMap() throws Exception {
    instance.setEncodedConfig(new MPWrappedMap<>(new Object[]{"zzz", 3, "aaa", 1, "cat", "dog"}));
    Setting<String> cat = new Setting<>("cat", TypeSafeMap::asString);
    assertNotNull(instance.getConfigValue(cat));
    assertEquals("dog", instance.getConfigValue(cat));
  }


  @Test
  public void getEncodedPrivilegedKeys() throws Exception {
    MPWrappedMap<String, Object[]> map = new MPWrappedMap<>(
        new Object[]{
            "Charlie",
            new Object[]{
                "address", 5L, Instant.now().getEpochSecond() + 5678L, "0011223344", "Charlie", new String[]{"bar", "foo"}
            }
        }
    );
    instance.setEncodedPrivilegedKeys(map);
    assertEquals(map.toJSONString(), instance.getEncodedPrivilegedKeys().toJSONString());
  }


  @Test
  public void getHeight() throws Exception {
    assertEquals(3, instance.getHeight());
  }


  @Test
  public void getLockedAssetsHash() throws Exception {
    AbstractState spy = spy(instance);
    when(spy.getLockedAssets()).thenReturn(new LockedAssetsList());
    assertEquals("", spy.getLockedAssets().getHash().toHexString());
  }


  @Test
  public void getPrivilegedKeys() throws Exception {
    instance.setEncodedPrivilegedKeys(new MPWrappedMap<>(new Object[]{
        "Charlie", new Object[]{
        "address", 5L, 12345678L, "0011223344", "Charlie", Arrays.asList("foo", "bar")
    }
    }));
    Map<String, PrivilegedKey> map = instance.getPrivilegedKeys();
    assertNotNull(map);
    assertTrue(map.containsKey("Charlie"));
    assertEquals(1, map.size());
  }


  @Test
  public void getPrivilegedKeysHash() throws Exception {
    PrivilegedKey privilegedKey = new PrivilegedKey("address", 12345678L, "0011223344", "Charlie", Arrays.asList("foo", "bar"), 5);
    PrivilegedKey.Encoded store = new PrivilegedKey.Encoded();
    store.put(privilegedKey.getKey(), privilegedKey);
    MPWrappedMap<String, Object[]> map = store.getEncoded();

    instance.setEncodedPrivilegedKeys(map);
    assertEquals("c6d271b1038ecc2bafd9a26954d63be5aaed9e09cbda1f3588ca6488d0c422c8", instance.getPrivilegedKeys().getHash(Digest.TYPE_SHA_256).toHexString());
  }


  @Test
  public void getTimestamp() throws Exception {
    assertEquals(1234567890L, instance.getTimestamp());
  }


  @Test
  public void getVersion() throws Exception {
    assertEquals(2, instance.getVersion());
  }


  @Test
  public void getXChainSignodesHash() throws Exception {
    assertTrue(instance.getXChainSignNodesHash().equals("76be8b528d0075f7aae98d6fa57a6d3c83ae480a8469e668d7b0af968995ac71"));
  }


  @Test
  public void isAssetLocked() throws Exception {
    AbstractState spy = spy(instance);
    when(spy.getLockedAssets()).thenReturn(new LockedAssetsList());
    assertTrue(!spy.isAssetLocked("fred"));
  }


  @Test
  public void nextPendingEventTime() {

    assertTrue(instance.nextPendingEventTime() == 0L);
    assertFalse(instance.anyPendingEventTime(0L));

    addContractEventTime(instance, "Ad1", 42L);

    assertFalse(instance.anyPendingEventTime(0L));
    assertTrue(instance.anyPendingEventTime(90L));
    assertTrue(instance.nextPendingEventTime() == 45L); // Rounded up to 5

    instance.removeContractEventTime("Ad2", 42L);

    assertTrue(instance.anyPendingEventTime(90L));
    assertTrue(instance.nextPendingEventTime() == 45L);

    instance.removeContractEventTime("Ad1", 42L);
    assertTrue(instance.nextPendingEventTime() == 0L);
    assertFalse(instance.anyPendingEventTime(90L));

  }


  @Before
  public void setUp() throws Exception {
    instance = spy(TestState.class);
  }


  @After
  public void tearDown() throws Exception {
  }


}
