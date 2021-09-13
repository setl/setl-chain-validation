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
package io.setl.bc.pychain.state.ipfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.AbstractEncodedState;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.SnapshotMerkle;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.CTEventsImpl;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.monolithic.LockedAssetsList;
import io.setl.common.CommonPy.VersionConstants;

public class IpfsBasedState extends AbstractEncodedState {

  private static final Logger logger = LoggerFactory.getLogger(IpfsBasedState.class);


  private static Hash asHash(MPWrappedArray array, int index) {
    Object o = array.get(index);
    if (o instanceof Hash) {
      return (Hash) o;
    }
    if (o instanceof byte[]) {
      return new Hash((byte[]) o);
    }
    if (o == null) {
      return Hash.NULL_HASH;
    }
    throw new IllegalArgumentException("Cannot cast " + o.getClass() + " to a Hash");
  }


  /**
   * Recreate a state from its message packed array representation and the store which contains all the referenced objects.
   *
   * @param v     the representation
   * @param store the store
   *
   * @return the state
   */
  @SuppressWarnings("unchecked")
  public static IpfsBasedState decodeRootFromIPFsStorage(
      MPWrappedArray v,
      MerkleStore<Object> store
  ) {
    IpfsBasedState r = new IpfsBasedState(v.asInt(0), v.asInt(1), v.asInt(2), Hash.fromHex(v.asString(3)), Hash.fromHex(v.asString(4)), v.asLong(5));

    logger.trace("Loaded state hash:{}", r.getLoadedHash());
    // loadedHash = ""; // untrusted

    r.setEncodedConfig((MPWrappedMap<String, Object>) store.get(asHash(v, 6)));

    r.signNodeList = new SignNodeIpfs(asHash(v, 7), store);
    r.namespaceList = new NamespaceIpfs(asHash(v, 8), store);
    r.assetBalanceList = new AssetBalanceIpfs(asHash(v, 9), store);
    r.contractsList = new ContractsIpfs(asHash(v, 10), store);
    r.encumbrancesList = new EncumbrancesIpfs(asHash(v, 11), store);
    r.poaList = new PowerOfAttorneyIpfs(asHash(v, 12), store);
    r.setEncodedPrivilegedKeys(v.asWrappedMap(13));

    if (r.getVersion() < VersionConstants.VERSION_LOCKED_ASSET_AS_MERKLE) {
      r.setEncodedLockedAssets(store, v.asWrappedMap(14));
    } else {
      r.lockedAssetsList = new LockedAssetIpfs(asHash(v, 14), store);
    }

    if (v.size() > 15) {
      r.setEncodedXChainSignodes(v.asWrappedMap(15));
    }

    if (v.size() > 16) {
      r.setEncodedContractTimeEvents(v.asWrapped(16));
    }
    r.isInitialised = true;
    return r;
  }


  /**
   * Convert a state into an array of objects and by-hash references that can later be used to recreate the state.
   *
   * @param s the state
   *
   * @return the array of objects and by-hash references
   */
  public static Object[] encodeRootForIPFsStorage(AbstractEncodedState s) {
    Object[] r = new Object[17];
    r[0] = s.getChainId();
    r[1] = s.getVersion();
    r[2] = s.getHeight();
    r[3] = s.getLoadedHash().toHexString();
    r[4] = s.getBlockHash().toHexString();
    r[5] = s.getTimestamp();
    r[6] = s.getConfigHash().get();
    r[7] = s.getSignNodes().getHash();
    r[8] = s.getNamespaces().getHash();
    r[9] = s.getAssetBalances().getHash();
    r[10] = s.getContracts().getHash();
    r[11] = s.getEncumbrances().getHash();
    r[12] = s.getPowerOfAttorneys().getHash();
    //TODO - consider storing these as hashes, as per config above
    r[13] = s.getEncodedPrivilegedKeys();
    if (s.getVersion() < VersionConstants.VERSION_LOCKED_ASSET_AS_MERKLE) {
      r[14] = s.getEncodedLockedAssets();
    } else {
      r[14] = s.getLockedAssets().getHash();
    }
    r[15] = s.getEncodedXChainSignNodes();
    r[16] = ((CTEventsImpl) s.getContractTimeEvents()).encode();

    return r;
  }


  private IpfsList<AddressEntry> assetBalanceList;

  private IpfsList<ContractEntry> contractsList;

  private IpfsList<AddressEncumbrances> encumbrancesList;

  private IpfsList<LockedAsset> lockedAssetsList;

  private IpfsList<NamespaceEntry> namespaceList;

  private IpfsList<PoaEntry> poaList;

  private IpfsList<SignNodeEntry> signNodeList;


  private IpfsBasedState(int chainId, int version, int height, Hash loadedHash, Hash blockHash, long timestamp) {
    super(chainId, version, height, loadedHash, blockHash, timestamp);
  }


  /**
   * Create a brand new, empty state.
   *
   * @param chainId the chain this state commences
   * @param version the state version
   * @param ms      where the state will be stored
   */
  public IpfsBasedState(int chainId, int version, MerkleStore<Object> ms, long timestamp) {
    super(chainId, version, -1                                                                                                                                                                                                                                                , null, null, timestamp);
    assetBalanceList = new AssetBalanceIpfs(Hash.NULL_HASH, ms);
    contractsList = new ContractsIpfs(Hash.NULL_HASH, ms);
    encumbrancesList = new EncumbrancesIpfs(Hash.NULL_HASH, ms);
    lockedAssetsList = new LockedAssetIpfs(Hash.NULL_HASH, ms);
    namespaceList = new NamespaceIpfs(Hash.NULL_HASH, ms);
    poaList = new PowerOfAttorneyIpfs(Hash.NULL_HASH, ms);
    signNodeList = new SignNodeIpfs(Hash.NULL_HASH, ms);
    initMerkleNames();
  }


  public IpfsBasedState(IpfsBasedState source) {
    super(source);
    initMerkleNames();
  }


  @Override
  protected <X extends MEntry> SnapshotMerkle<X> createMerkle(Class<X> name) {
    throw new UnsupportedOperationException("IPFS based state does not support dynamic creation of Merkle structures");
  }


  @Override
  public Merkle<AddressEntry> getAssetBalances() {
    return assetBalanceList;
  }


  @Override
  public Merkle<ContractEntry> getContracts() {
    return contractsList;
  }


  @Override
  public Merkle<AddressEncumbrances> getEncumbrances() {
    return encumbrancesList;
  }


  @Override
  public Merkle<LockedAsset> getLockedAssets() {
    return lockedAssetsList;
  }


  @Override
  protected AbstractState getMutableCopy() {
    return isInitialised ? new IpfsBasedState(this) : this;
  }


  @Override
  public Merkle<NamespaceEntry> getNamespaces() {
    return namespaceList;
  }


  @Override
  public Merkle<PoaEntry> getPowerOfAttorneys() {
    return poaList;
  }


  @Override
  public Merkle<SignNodeEntry> getSignNodes() {
    return signNodeList;
  }


  /**
   * Initialise the by-name lookup of the fixed Merkles.
   */
  private void initMerkleNames() {
    merkles.put(AddressEntry.class, assetBalanceList);
    merkles.put(ContractEntry.class, contractsList);
    merkles.put(AddressEncumbrances.class, encumbrancesList);
    merkles.put(LockedAsset.class, lockedAssetsList);
    merkles.put(NamespaceEntry.class, namespaceList);
    merkles.put(PoaEntry.class, poaList);
  }


  @Override
  protected <V extends MEntry> void initialiseListFrom(Merkle<?> source, SnapshotMerkle<V> target) {
    // do nothing - the lists are created as copies
  }


  @Override
  protected void initialiseMerklesFrom(AbstractState source) {
    // Create copy lists. As the lists are copies, there is no need to invoke the super class to perform a copy.
    IpfsBasedState ipfs = (IpfsBasedState) source;
    namespaceList = ipfs.namespaceList.copy();
    assetBalanceList = ipfs.assetBalanceList.copy();
    contractsList = ipfs.contractsList.copy();
    encumbrancesList = ipfs.encumbrancesList.copy();
    poaList = ipfs.poaList.copy();
    lockedAssetsList = ipfs.lockedAssetsList.copy();
  }


  @Override
  protected void initialiseSignNodesFrom(AbstractState source) {
    IpfsBasedState ipfs = (IpfsBasedState) source;
    signNodeList = ipfs.signNodeList.copy();
  }


  private void setEncodedLockedAssets(MerkleStore<Object> store, MPWrappedMap<Object, Object> map) {
    LockedAssetsList tmpList = new LockedAssetsList();
    map.iterate((k, v) -> {
      String key = (String) k;

      // 'v' is standard encoding, so "index, key, datum, height" where datum is just the lock type
      Object[] val = (Object[]) v;
      LockedAsset lockedAsset;
      switch (val.length) {
        case 2: // [ type-code, null ]
          lockedAsset = new LockedAsset(key, LockedAsset.Type.forCode(((Number) val[0]).intValue()), 0);
          break;
        case 3: // [ type-code, null, height ] or [index,key,datum]
          if (val[1] == null) {
            lockedAsset = new LockedAsset(key, LockedAsset.Type.forCode(((Number) val[0]).intValue()), ((Number) val[2]).longValue());
          } else {
            lockedAsset = new LockedAsset(key, LockedAsset.Type.forCode(((Number) val[2]).intValue()), 0);
          }
          break;
        case 4: // Standard-encoding: [index, key, datum, height] where datum is type-code.
          lockedAsset = new LockedAsset(key, LockedAsset.Type.forCode(((Number) val[2]).intValue()), ((Number) val[3]).longValue());
          break;
        default:
          throw new IllegalArgumentException("Unknown encoding with length of " + val.length);
      }
      tmpList.update(key, lockedAsset);
    });
    Hash hash = tmpList.getHash();
    tmpList.iterate((h, d) -> {
      store.put(h, d);
    });
    lockedAssetsList = new LockedAssetIpfs(hash, store);
  }

}
