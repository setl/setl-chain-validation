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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashableObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.serialise.Content;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.entry.CTEventsImpl;
import io.setl.bc.pychain.state.entry.ContractTimeEvents;
import io.setl.bc.pychain.state.entry.DataName;
import io.setl.bc.pychain.state.entry.NamedDatum;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.ipfs.MerkleStoreWriter;
import io.setl.bc.pychain.state.monolithic.EncodedHashedMap;
import io.setl.bc.pychain.state.monolithic.KeyedIndexedEntryList;
import io.setl.bc.pychain.state.structure.NamedData;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a full state, with multiple individual Merkle structures. <p>Storage for SignodeList, NamespaceList, AssetBallanceList, ContractsList,
 * EncumbrancesList and PowerOfAttourneysList is in the specific implimentations of State (ObjectEncodedState and IpfsBasedState)</p> <p>The dictionary storage
 * objects (XChainSignodes, unusedKeys, LockedAssets and Config) are held in two potential forms : a TreeMap or a MPWrappedMap, access is through two sets of
 * get/set methods which will server to maintain consistency between the tow forms and the associated hash. It is vital that that, once aquired, the related map
 * object is not updated directly. Update via snapshot is handled correctly.</p>
 */
public abstract class AbstractEncodedState extends AbstractState implements EncodedState, HashableObjectArray {

  private static final Logger logger = LoggerFactory.getLogger(AbstractEncodedState.class);

  protected CTEventsImpl contractTimeEvents = new CTEventsImpl();


  /**
   * AbstractState constructor.
   *
   * @param chainId :  Int, Chain identifier
   * @param version :  Int, State version identifier
   * @param height  :  Int, State height.
   */
  public AbstractEncodedState(int chainId, int version, int height) {
    super(chainId, version, height);
    privilegedKeys = new PrivilegedKey.Encoded();
    xChainSignNodes = new XChainDetails.Encoded();
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
  protected AbstractEncodedState(int chainId, int version, int height, Hash loadedHash, Hash blockHash, long timestamp) {
    super(chainId, version, height, loadedHash, blockHash, timestamp);
    privilegedKeys = new PrivilegedKey.Encoded();
    xChainSignNodes = new XChainDetails.Encoded();
  }


  /**
   * Copy constructor.
   *
   * @param source source state
   */
  protected AbstractEncodedState(AbstractState source) {
    super(source);
    // This class only adds contractTimeEvents, so it does not have to construct and initialise anything else.
  }


  protected void computeRootHash() {
    loadedHash = new DefaultHashableHashComputer().computeHash(this);
  }


  /**
   * getConfigHash. <p>Get hash for StateConfig State component.</p>
   *
   * @return : String, Hash.
   */
  public Hash getConfigHash() {
    return config.getHash(Digest.TYPE_SHA_256);
  }


  @Override
  public ContractTimeEvents getContractTimeEvents() {
    return contractTimeEvents;
  }


  @Override
  public <N extends NamedDatum<N>> N getDatum(DataName<N> name) {
    if (name.equals(ContractTimeEvents.NAME)) {
      return (N) getContractTimeEvents();
    }
    throw new NoSuchElementException(name.toString());
  }


  /**
   * getEncodedConfig(). Returns MPWrappedMap for Config Map. Used when persisting / serialising state. <p>A MPWrappedMap is essentialy a simple list of
   * Key/Value details, not useful when querying state. </p>
   *
   * @return MPWrappedMap(Config)
   */
  @Override
  public MPWrappedMap<String, Object> getEncodedConfig() {
    return config.getEncoded();
  }


  /**
   * getEncodedLockedAssets(). Returns MPWrappedMap for LockedAssets Map. Used when persisting / serialising state. <p> A MPWrappedMap is essentially a simple
   * list of Key/Value details, not useful when querying state. </p>
   *
   * @return MPWrappedMap(LockedAssets)
   */
  @Override
  public MPWrappedMap<String, Object[]> getEncodedLockedAssets() {
    TreeMap<String, Object[]> map = new TreeMap<>();
    getLockedAssets().forEach(a -> map.put(a.getKey(), new Object[]{a.getType().code(), null}));
    return new MPWrappedMap<>(map);
  }


  /**
   * getEncodedUnusedKeys(). Returns MPWrappedMap for unusedKeys Map. Used when persisting / serialising state. <p>A MPWrappedMap is essentialy a simple list of
   * Key/Value details, not useful when querying state. </p>
   *
   * @return MPWrappedMap(unusedKeys)
   */
  @Override
  public MPWrappedMap<String, Object[]> getEncodedPrivilegedKeys() {
    return getPrivilegedKeys().getEncoded();
  }


  /**
   * getEncodedXChainSignodes(). Returns MPWrappedMap for XChainSignodes Map. Used when persisting / serialising state. <p>A MPWrappedMap is essentialy a simple
   * list of Key/Value details, not useful when querying state. </p>
   *
   * @return MPWrappedMap(XChainSignodes)
   */
  @Override
  public MPWrappedMap<Long, Object[]> getEncodedXChainSignNodes() {
    return getXChainSignNodes().getEncoded();
  }


  @Override
  public Object[] getHashableObject() {

    /*
      Note : contractTimeEvents does not go into the hash, as all info comes from the contracts tree, which does.
             Also, the contractTimeEvents dict may be subject to checking and update which would lead to inconsistent
             hashes if it was included.
     */

    String signodelisth = getSignNodes().computeRootHash();
    String namespacelisth = getNamespaces().computeRootHash();
    String assetbalancelisth = getAssetBalances().computeRootHash();

    String contractslisth = getContracts().computeRootHash();

    String thisXChainSignodesHash;
    MPWrappedMap<Long, Object[]> thisEncodedXChainSignodes = this.getEncodedXChainSignNodes();

    if (thisEncodedXChainSignodes == null || thisEncodedXChainSignodes.size() == 0) {

      thisXChainSignodesHash = "";
    } else {

      Hash hash = HashSerialisation.getInstance().hash(thisEncodedXChainSignodes);
      thisXChainSignodesHash = hash.toHexString();
    }

    if (this.getVersion() <= 2) {
      return new Object[]{
          getChainId(), getTimestamp(), getHeight(), getBlockHash(), getConfigHash().toHexString(),
          signodelisth, namespacelisth, assetbalancelisth, contractslisth, thisXChainSignodesHash
      };
    }

    String poaHash = getPowerOfAttorneys() == null ? "" : getPowerOfAttorneys().computeRootHash();

    final String thisunusedKeysHash;
    MPWrappedMap<String, Object[]> thisEncodedUnusedKeys = this.getEncodedPrivilegedKeys();

    if (thisEncodedUnusedKeys == null || thisEncodedUnusedKeys.size() == 0) {

      thisunusedKeysHash = "";
    } else {
      Hash hash = HashSerialisation.getInstance().hash(getEncodedPrivilegedKeys());
      thisunusedKeysHash = hash.toHexString();
    }

    final String thisLockedAssetsHash;

    if (this.getEncodedLockedAssets() == null || this.getEncodedLockedAssets().size() == 0) {

      thisLockedAssetsHash = "";
    } else {
      Hash hash = HashSerialisation.getInstance().hash(getEncodedLockedAssets());
      thisLockedAssetsHash = hash.toHexString();
    }

    String encumbranceslisth = getEncumbrances().computeRootHash();

    String combinedHash = thisXChainSignodesHash + thisunusedKeysHash + thisLockedAssetsHash + poaHash;

    return new Object[]{
        getChainId(), getTimestamp(), getHeight(), getBlockHash().toHexString(), getConfigHash().toHexString(),
        signodelisth, namespacelisth, assetbalancelisth, contractslisth, encumbranceslisth,
        combinedHash
    };

  }


  @Override
  public NamedData getNamedDataHashes() {
    // Assume ContractTimeEvents do not need a hash store.
    Content content = ContractTimeEvents.NAME.getFactory(null).asContent(Digest.getRecommended(this), contractTimeEvents);
    NamedData namedData = new NamedData();
    namedData.put(ContractTimeEvents.NAME.getName(), content.getHash());
    return namedData;
  }


  @Override
  public EncodedHashedMap<String, PrivilegedKey, Object[]> getPrivilegedKeys() {
    return (EncodedHashedMap<String, PrivilegedKey, Object[]>) super.getPrivilegedKeys();
  }


  @Override
  public EncodedHashedMap<Long, XChainDetails, Object[]> getXChainSignNodes() {
    return (EncodedHashedMap<Long, XChainDetails, Object[]>) super.getXChainSignNodes();
  }


  /**
   * Get the hash of the XChain Sign Nodes component.
   *
   * @return the hash
   */
  public String getXChainSignNodesHash() {
    return getXChainSignNodes().getHash(Digest.TYPE_SHA_256).toHexString();
  }


  protected void initialiseNamedDataFrom(AbstractState source) {
    contractTimeEvents = (CTEventsImpl) source.getDatum(ContractTimeEvents.NAME);
  }


  @Override
  protected void initialisePrivilegedKeysFrom(AbstractState source) {
    privilegedKeys = new PrivilegedKey.Encoded();
    privilegedKeys.replace(source.privilegedKeys);

  }


  @Override
  protected void initialiseXChainSignNodesFrom(AbstractState source) {
    xChainSignNodes = new XChainDetails.Encoded();
    xChainSignNodes.replace(source.xChainSignNodes);
  }


  /**
   * Helper method to iterate over all merkle structures.
   */
  public void iterateAllMerkles(MerkleStoreWriter<Object[]> msw) {
    Consumer<Merkle<?>> iterator = l -> {
      if (l instanceof KeyedIndexedEntryList<?>) {
        ((KeyedIndexedEntryList<?>) l).iterate(msw);
      }
    };
    iterator.accept(getSignNodes());
    iterator.accept(getNamespaces());
    iterator.accept(getAssetBalances());
    iterator.accept(getContracts());
    iterator.accept(getEncumbrances());
    iterator.accept(getPowerOfAttorneys());
    iterator.accept(getLockedAssets());
  }


  @Override
  protected void saveNamedData() {
    // do nothing, as named data is not supported here.
  }


  /**
   * setEncodedConfig.
   *
   * @param newConfig : MPWrappedMap Config options.
   */
  public void setEncodedConfig(MPWrappedMap<String, Object> newConfig) {
    checkCanWrite();
    config.replace(newConfig);
  }


  /**
   * setEncodedContractTimeEvents.
   *
   * @param encoded : MPWrappedMap ContractTimeEvents options.
   */
  protected void setEncodedContractTimeEvents(MPWrappedArray encoded) {
    checkCanWrite();
    contractTimeEvents = new CTEventsImpl(encoded);
  }


  /**
   * setEncodedPrivilegedKeys.
   *
   * @param privilegedKeys : MPWrappedMap unusedKeys options.
   */
  protected void setEncodedPrivilegedKeys(MPWrappedMap<String, Object[]> privilegedKeys) {
    checkCanWrite();
    EncodedHashedMap<String, PrivilegedKey, Object[]> m = new ObjectMapper().convertValue(privilegedKeys, PrivilegedKey.Encoded.class);
    setPrivilegedKeys(m, Collections.emptySet(), true);
  }


  /**
   * setEncodedXChainSignodes.
   *
   * @param xChainSignodes : MPWrappedMap XChainSignodes options.
   */
  protected void setEncodedXChainSignodes(MPWrappedMap<Long, Object[]> xChainSignodes) {
    checkCanWrite();
    EncodedHashedMap<Long, XChainDetails, Object[]> m = new XChainDetails.Encoded(xChainSignodes);
    setXChainSignNodes(m, Collections.emptySet(), true);
  }


  /**
   * verifyAll, Verify state hash.
   *
   * @return : Boolean, Success or Failure.
   */
  public boolean verifyAll() {
    Hash hash = new DefaultHashableHashComputer().computeHash(this);

    if (hash.equals(loadedHash)) {
      logger.info("State verified OK hash:{}", hash);
      return true;
    } else {
      if (logger.isErrorEnabled()) {
        logger.error(Arrays.toString(this.getHashableObject()));
      }
    }
    logger.error("Bad hash, original/loaded:{} computed:{}", loadedHash, hash);
    return false;
  }
}
