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

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.ConfigMap;
import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashedMap;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.BlockVerifier;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.CTEventsImpl;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.structure.Metadata;
import io.setl.bc.pychain.state.structure.NamedData;
import io.setl.bc.pychain.state.structure.Root;
import io.setl.bc.pychain.state.structure.StateStructure;
import io.setl.util.CopyOnWriteMap;

/**
 * Represents a full state, with multiple individual Merkle structures. <p>Storage for SignodeList, NamespaceList, AssetBallanceList, ContractsList,
 * EncumbrancesList and PowerOfAttourneysList is in the specific implimentations of State (ObjectEncodedState and IpfsBasedState)</p> <p>The dictionary storage
 * objects (XChainSignodes, unusedKeys, LockedAssets and Config) are held in two potential forms : a TreeMap or a MPWrappedMap, access is through two sets of
 * get/set methods which will server to maintain consistency between the tow forms and the associated hash. It is vital that that, once aquired, the related map
 * object is not updated directly. Update via snapshot is handled correctly.</p>
 */
public abstract class AbstractState implements State {

  private static final Logger logger = LoggerFactory.getLogger(AbstractState.class);



  protected interface MutationHelper {

    default SnapshotMerkle<AddressEntry> getAssetBalances() {
      return getMerkle(AddressEntry.class);
    }

    default SnapshotMerkle<ContractEntry> getContracts() {
      return getMerkle(ContractEntry.class);
    }

    default SnapshotMerkle<AddressEncumbrances> getEncumbrances() {
      return getMerkle(AddressEncumbrances.class);
    }

    default SnapshotMerkle<LockedAsset> getLockedAssets() {
      return getMerkle(LockedAsset.class);
    }

    <X extends MEntry> SnapshotMerkle<X> getMerkle(Class<X> type);

    default SnapshotMerkle<NamespaceEntry> getNamespaces() {
      return getMerkle(NamespaceEntry.class);
    }

    default SnapshotMerkle<PoaEntry> getPowerOfAttorneys() {
      return getMerkle(PoaEntry.class);
    }

    SnapshotMerkle<SignNodeEntry> getSignNodes();

  }


  private static <K1 extends Comparable<? super K1>, V1, M1 extends HashedMap<K1, V1>> M1 updateMap(
      M1 current, Map<K1, V1> newValues,
      Set<K1> removed, boolean overwrite
  ) {
    if (overwrite) {
      current.clear();

      // Put to empty map
      current.putAll(newValues);
    } else {
      // Put to existing map
      current.putAll(newValues);
    }

    // Process removals
    if (removed != null) {
      for (K1 key : removed) {
        current.remove(key);
      }
    }
    return current;
  }


  private final int chainId;

  private final int version;

  protected ConfigMap config;

  protected CTEventsImpl contractTimeEvents;

  /** A state is immutable once initialisation is complete. */
  protected boolean isInitialised = false;

  protected Hash loadedHash;

  protected Map<Class<? extends MEntry>, SnapshotMerkle<?>> merkles = new CopyOnWriteMap<>(i -> new TreeMap<>(Comparator.comparing(Class::getName)));

  protected HashedMap<String, PrivilegedKey> privilegedKeys;

  protected HashedMap<Long, XChainDetails> xChainSignNodes;

  private Hash blockHash;

  private int height;

  private StateChangeListener stateChangeListener = NoOpChangeListener.INSTANCE;

  /** State configuration - created on demand. */
  private StateConfig stateConfig = null;

  private long timestamp;


  /**
   * AbstractState constructor.
   *
   * @param chainId :  Int, Chain identifier
   * @param version :  Int, State version identifier
   * @param height  :  Int, State height.
   */
  public AbstractState(int chainId, int version, int height) {
    this.chainId = chainId;
    this.version = version;
    this.height = height;

    config = new ConfigMap();
    contractTimeEvents = new CTEventsImpl();
  }


  /**
   * AbstractState constructor.
   *
   * @param chainId    :  Int, Chain identifier
   * @param version    :  Int, State version identifier
   * @param height     :  Int, State height.
   * @param loadedHash :  String, State hash
   * @param blockHash  :  String, Block Hash
   * @param timestamp  :  Long, time state was created
   */
  protected AbstractState(int chainId, int version, int height, Hash loadedHash, Hash blockHash, long timestamp) {
    this.chainId = chainId;
    this.version = version;
    this.height = height;
    this.loadedHash = loadedHash;
    this.blockHash = blockHash;
    this.timestamp = timestamp;

    config = new ConfigMap();
    contractTimeEvents = new CTEventsImpl();
  }


  protected AbstractState(StateStructure structure) {
    loadedHash = structure.getLoadedHash();

    Root root = structure.getRoot();
    version = root.version;

    Metadata metadata = structure.getMetadata();
    chainId = metadata.chainId;
    height = metadata.height;
    blockHash = metadata.getBlockHash();
    timestamp = metadata.timestamp;

    config = new ConfigMap();
    contractTimeEvents = new CTEventsImpl();
  }


  /**
   * Copy constructor.
   *
   * @param source source state
   */
  protected AbstractState(AbstractState source) {
    // Initialise the primitive properties owned by this class. The representation used for these values is fixed so sub-classes should not need to set them in
    // a different way.
    blockHash = source.getBlockHash();
    chainId = source.getChainId();
    height = source.getHeight();
    isInitialised = false;
    loadedHash = source.getLoadedHash();
    timestamp = source.getTimestamp();
    version = source.getVersion();
    stateChangeListener = source.stateChangeListener;

    // Initialise the complex properties one at a time. Having a separate initialiser for each one allows sub-classes to specify the implementations they want
    // to use.
    initialiseConfigFrom(source);
    initialisePrivilegedKeysFrom(source);
    initialiseNamedDataFrom(source);

    initialiseSignNodesFrom(source);
    initialiseXChainSignNodesFrom(source);

    // Finally initialise the Merkle structure that hold the state data.
    initialiseMerklesFrom(source);
  }


  @Override
  public boolean anyPendingEventTime(long eventTime) {
    long firstTime = getContractTimeEvents().firstTime();
    // NB: "firstTime" returns 0 if there are no events
    return (firstTime != 0) && firstTime <= eventTime;
  }


  protected void checkCanWrite() {
    if (isInitialised) {
      throw new IllegalStateException("State is immutable");
    }
  }


  protected abstract void computeRootHash();


  /**
   * Create a new Merkle in state and provide a way to write  to it from a Snapshot.
   *
   * @param name the new Merkle's name
   * @param <X>  the new Merkle's leaf type
   *
   * @return the Merkle
   */
  protected abstract <X extends MEntry> SnapshotMerkle<X> createMerkle(Class<X> name);


  @Override
  public StateSnapshot createSnapshot() {
    return new StateSnapshotImplementation(getMutableCopy(), stateChangeListener);
  }


  protected void finalizeBlock(Block block) {
    checkCanWrite();

    height++;
    timestamp = block.getTimeStamp();
    blockHash = new BlockVerifier().computeHash(block);

    saveNamedData();
    computeRootHash();
    isInitialised = true;
  }


  protected void finalizeCopy() {
    checkCanWrite();

    saveNamedData();

    Hash requiredHash = getLoadedHash();
    computeRootHash();
    if (!getLoadedHash().equals(requiredHash)) {
      throw new IllegalStateException("State hash was required to be " + requiredHash + " but is actually " + getLoadedHash());
    }

    isInitialised = true;
  }


  @Override
  public Merkle<AddressEntry> getAssetBalances() {
    return getMerkle(AddressEntry.class);
  }


  @Override
  public Hash getBlockHash() {
    return blockHash;
  }


  @Override
  public int getChainId() {
    return chainId;
  }


  /**
   * Get all the configuration values. The values mapped to the keys are not guaranteed beyond that fact that the appropriate <code>Setting</code> instance
   * can convert it into the expected type.
   *
   * @return Configuration values
   */
  public Map<String, Object> getConfig() {
    return Collections.unmodifiableMap(config.asMap());
  }


  public Hash getConfigHash(int type) {
    return config.getHash(type);
  }


  @Override
  public <Q> Q getConfigValue(Setting<Q> keyName) {
    return config.get(keyName);
  }


  @Override
  public Merkle<ContractEntry> getContracts() {
    return getMerkle(ContractEntry.class);
  }


  @Override
  public Merkle<AddressEncumbrances> getEncumbrances() {
    return getMerkle(AddressEncumbrances.class);
  }


  @Override
  public int getHeight() {
    return height;
  }


  @Override
  public Hash getLoadedHash() {
    return loadedHash;
  }


  @Override
  public Merkle<LockedAsset> getLockedAssets() {
    return getMerkle(LockedAsset.class);
  }


  @Override
  public <X extends MEntry> Merkle<X> getMerkle(Class<X> leafType) {
    Merkle<?> merkle = merkles.get(leafType);
    Merkle<X> xMerkle = Merkle.cast(merkle, leafType);
    return xMerkle != null ? xMerkle : new EmptyMerkle<>(leafType);
  }


  @Override
  public Set<Class<? extends MEntry>> getMerkleTypes() {
    return merkles.keySet();
  }


  protected abstract AbstractState getMutableCopy();


  protected MutationHelper getMutationHelper() {
    final BooleanSupplier isLocked = () -> isInitialised;
    return new MutationHelper() {
      @Override
      public SnapshotMerkle<AddressEntry> getAssetBalances() {
        return new LockableMerkle<>((SnapshotMerkle<AddressEntry>) AbstractState.this.getAssetBalances(), isLocked);
      }


      @Override
      public SnapshotMerkle<ContractEntry> getContracts() {
        return new LockableMerkle<>((SnapshotMerkle<ContractEntry>) AbstractState.this.getContracts(), isLocked);
      }


      @Override
      public SnapshotMerkle<AddressEncumbrances> getEncumbrances() {
        return new LockableMerkle<>((SnapshotMerkle<AddressEncumbrances>) AbstractState.this.getEncumbrances(), isLocked);
      }


      @Override
      public SnapshotMerkle<LockedAsset> getLockedAssets() {
        return new LockableMerkle<>((SnapshotMerkle<LockedAsset>) AbstractState.this.getLockedAssets(), isLocked);
      }


      @Override
      @SuppressWarnings("unchecked")
      public <X extends MEntry> SnapshotMerkle<X> getMerkle(Class<X> type) {
        return new LockableMerkle<>((SnapshotMerkle<X>) merkles.get(type), isLocked);
      }


      @Override
      public SnapshotMerkle<NamespaceEntry> getNamespaces() {
        return new LockableMerkle<>((SnapshotMerkle<NamespaceEntry>) AbstractState.this.getNamespaces(), isLocked);
      }


      @Override
      public SnapshotMerkle<PoaEntry> getPowerOfAttorneys() {
        return new LockableMerkle<>((SnapshotMerkle<PoaEntry>) AbstractState.this.getPowerOfAttorneys(), isLocked);
      }


      @Override
      public SnapshotMerkle<SignNodeEntry> getSignNodes() {
        return new LockableMerkle<>((SnapshotMerkle<SignNodeEntry>) AbstractState.this.getSignNodes(), isLocked);
      }
    };
  }


  /**
   * Get the map of datum name to value hash. Changes to the returned map do not affect state.
   *
   * @return the map
   */
  public abstract NamedData getNamedDataHashes();


  @Override
  public Merkle<NamespaceEntry> getNamespaces() {
    return getMerkle(NamespaceEntry.class);
  }


  @Override
  public Merkle<PoaEntry> getPowerOfAttorneys() {
    return getMerkle(PoaEntry.class);
  }


  @Override
  public PrivilegedKey getPrivilegedKey(String keyName) {
    if (keyName == null || keyName.isEmpty()) {
      return null;
    }

    // Try by address
    HashedMap<String, PrivilegedKey> map = getPrivilegedKeys();
    PrivilegedKey key = map.get(keyName);
    if (key != null) {
      return key;
    }

    // Try by name
    for (PrivilegedKey pk : map.values()) {
      if (keyName.equals(pk.getName())) {
        return pk;
      }
    }

    // not found
    return null;
  }


  @Override
  public HashedMap<String, PrivilegedKey> getPrivilegedKeys() {
    return privilegedKeys;
  }


  public StateChangeListener getStateChangeListener() {
    return stateChangeListener;
  }


  @Override
  public StateConfig getStateConfig() {
    if (stateConfig == null) {
      stateConfig = new StateConfig(this);
    }
    return stateConfig;
  }


  @Override
  public long getTimestamp() {
    return timestamp;
  }


  @Override
  public int getVersion() {
    return version;
  }


  @Override
  public HashedMap<Long, XChainDetails> getXChainSignNodes() {
    return xChainSignNodes;
  }


  protected void initialiseConfigFrom(AbstractState source) {
    config = new ConfigMap(source.config);
  }


  /**
   * Initialise a specific Merkle list from the source state. The default implementation removes all items currently in the target list, adds all items from the
   * source, and then recomputes the target list's hash
   *
   * @param rawSource the source Merkle
   * @param target    the target Merkle
   * @param <V>       the Merkle's leaf type.
   */
  protected <V extends MEntry> void initialiseListFrom(Merkle<?> rawSource, SnapshotMerkle<V> target) {
    // First clear out target
    LinkedList<V> currentContents = new LinkedList<>();
    target.forEach(currentContents::addFirst);
    for (V v : currentContents) {
      String k = v.getKey();
      target.remove(k);
    }

    // Now add source to target
    Merkle<V> source = Merkle.cast(rawSource, target.getLeafType());
    source.forEach(e -> target.update(e.getKey(), e));
    target.computeRootHash();
  }


  /**
   * Initialise all the state data Merkle structures from the source state.
   *
   * @param source the source state
   */
  protected void initialiseMerklesFrom(AbstractState source) {
    // Copy all the Merkles via a snapshot
    MutationHelper helper = getMutationHelper();
    Set<Class<? extends MEntry>> merkleTypes = source.getMerkleTypes();
    for (Class<? extends MEntry> type : merkleTypes) {
      initialiseListFrom(source.getMerkle(type), helper.getMerkle(type));
    }
  }


  protected abstract void initialiseNamedDataFrom(AbstractState source);


  /**
   * Initialise the map storing the privileged keys.
   *
   * @param source the source state
   */
  protected abstract void initialisePrivilegedKeysFrom(AbstractState source);


  /**
   * Initialise the list of network signing nodes. The default implementation copies the list from the source.
   *
   * @param source the source state
   */
  protected void initialiseSignNodesFrom(AbstractState source) {
    MutationHelper helper = getMutationHelper();
    initialiseListFrom(source.getSignNodes(), helper.getSignNodes());
  }


  /**
   * Initialise the map storing the cross chain signing nodes.
   *
   * @param source the source state
   */
  protected abstract void initialiseXChainSignNodesFrom(AbstractState source);


  /**
   * isAssetLocked(). Returns 'true' if the given AssetName (NS|Class) is locked. May be used for Namespaces also by specifying just the Namespace name.
   *
   * @param assetName : Full Asset Class or just the Namespace.
   *
   * @return : boolean
   */
  @Override
  public boolean isAssetLocked(String assetName) {
    // Assumes 'locked' if the Asset name exists in the lockedAssetsMap. May need to be refined.
    return getLockedAssets().itemExists((assetName));
  }


  @Override
  public long nextPendingEventTime() {
    return getContractTimeEvents().firstTime();
  }


  @Override
  public SortedSet<String> pendingEventTimeAddresses(long eventTime) {
    long limit = getStateConfig().getMaxTimersPerBlock();
    return getContractTimeEvents().getEventsBefore(eventTime, (int) limit);
  }


  protected boolean removeContractEventTime(String address, long eventTime) {
    checkCanWrite();
    return getContractTimeEvents().remove(address, eventTime);
  }


  protected abstract void saveNamedData();


  /**
   * setConfigMap(). <p>configMap should ALWAYS be updated using this method in order to ensure that this.config is cleared. If this.config is not cleared, then
   * config Hash or Encoded methods could be out of date! </p>
   *
   * @param newConfigMap : Map with which to update State Config.
   */
  protected void setConfigMap(@Nonnull Map<String, Object> newConfigMap, boolean overwrite) {
    checkCanWrite();

    if (overwrite) {
      config.replace(newConfigMap);
    } else {
      config.putAll(newConfigMap);
    }
  }


  /**
   * Mark this state as fully initialised. A state is immutable once fully initialised.
   */
  protected void setInitialised() {
    isInitialised = true;
  }


  protected void setLoadedHash(Hash newLoadedHash) {
    checkCanWrite();
    loadedHash = newLoadedHash;
  }


  /**
   * setUnusedKeys(). <p> unusedKeysMap should ALWAYS be updated using this method in order to ensure that this.unusedKeys is cleared. If this.unusedKeys is
   * not cleared, then unusedKeys Hash or Encoded methods could be out of date! </p>
   *
   * @param newUnusedKeys : Map with which to update State unusedKeys.
   */
  protected void setPrivilegedKeys(Map<String, PrivilegedKey> newUnusedKeys, Set<String> removedUnusedKeys, boolean overwrite) {
    checkCanWrite();
    privilegedKeys = updateMap(privilegedKeys, newUnusedKeys, removedUnusedKeys, overwrite);
  }


  public void setStateChangeListener(StateChangeListener newListener) {
    stateChangeListener = newListener != null ? newListener : NoOpChangeListener.INSTANCE;
  }


  /**
   * setXChainSignNodes(). <p> xChainSignNodesMap should ALWAYS be updated using this method in order to ensure that this.xChainSignNodes is cleared. If
   * this.xChainSignNodes is not cleared, then xChainSignNodes Hash or Encoded methods could be out of date! </p>
   *
   * @param newXChainSignNodes : Map with which to update State XChainSignodes.
   */
  protected void setXChainSignNodes(Map<Long, XChainDetails> newXChainSignNodes, Set<Long> removedXChainSignodes, boolean overwrite) {
    checkCanWrite();
    xChainSignNodes = updateMap(xChainSignNodes, newXChainSignNodes, removedXChainSignodes, overwrite);
  }

}
