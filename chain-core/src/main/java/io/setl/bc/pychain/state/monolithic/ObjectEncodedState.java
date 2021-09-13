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
package io.setl.bc.pychain.state.monolithic;

import static io.setl.common.CommonPy.VersionConstants.VERSION_LOCKED_ASSET_AS_MERKLE;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.AbstractEncodedState;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.SnapshotMerkle;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;

public class ObjectEncodedState extends AbstractEncodedState {

  private static final Logger logger = LoggerFactory.getLogger(ObjectEncodedState.class);


  /**
   * Decode a state that has been previously encoded as a message packed array.
   *
   * @param v the message packed array
   *
   * @return the state
   */
  public static ObjectEncodedState decode(MPWrappedArray v) {
    ObjectEncodedState stateObject = new ObjectEncodedState(v.asInt(0), v.asInt(1), v.asInt(2), Hash.fromHex(v.asString(3)), Hash.fromHex(v.asString(4)),
        v.asLong(5)
    );

    logger.trace("Loaded state hash:{}", stateObject.getLoadedHash());
    // loadedHash = ""; // untrusted

    stateObject.setEncodedConfig(v.asWrappedMap(6));

    if (stateObject.getVersion() <= 2) {
      stateObject.signNodeList = new SignNodeList(v.asWrapped(7), stateObject.getVersion());

      stateObject.namespaceList = new NamespaceList(v.asWrapped(8), stateObject.getVersion());
      stateObject.assetBalanceList = new AssetBalanceList(v.asWrapped(9), stateObject.getVersion());

      stateObject.contractsList = new ContractsList(v.asWrapped(10), stateObject.getVersion());

      /* contractTimeEvents not supported in V2. */

      // Item 12 was a "transaction count". The field is now unused.
      // Item 13 was a "cross chain length". The field is now unused.

      // Note : Do not support v2 xChain data

      // No encumbrances
      stateObject.encumbrancesList =
          new EncumbrancesList(new MPWrappedArrayImpl(new Object[]{new MPWrappedArrayImpl(new Object[0])}), stateObject.getVersion());

      // No POA
      stateObject.poaList =
          new PowerOfAttorney(new MPWrappedArrayImpl(new Object[]{new MPWrappedArrayImpl(new Object[0])}), stateObject.getVersion());

      // No locked assets
      stateObject.lockedAssetsList = new LockedAssetsList();

      stateObject.setLoadedHash(Hash.fromHex(v.asString(3)));
      stateObject.isInitialised = true;
      stateObject.initMerkleNames();
      return stateObject;
    }

    MPWrappedArray merks = v.asWrapped(7);
    stateObject.signNodeList = new SignNodeList(merks.asWrapped(0), stateObject.getVersion());
    stateObject.namespaceList = new NamespaceList(merks.asWrapped(1), stateObject.getVersion());
    stateObject.assetBalanceList = new AssetBalanceList(merks.asWrapped(2), stateObject.getVersion());
    stateObject.contractsList = new ContractsList(merks.asWrapped(3), stateObject.getVersion());
    stateObject.encumbrancesList = new EncumbrancesList(merks.asWrapped(4), stateObject.getVersion());

    if (merks.size() > 5) {
      stateObject.poaList = new PowerOfAttorney(merks.asWrapped(5), stateObject.getVersion());
      stateObject.setEncodedPrivilegedKeys(merks.asWrappedMap(6));
      if (stateObject.getVersion() < VERSION_LOCKED_ASSET_AS_MERKLE) {
        stateObject.setEncodedLockedAssets(merks.asWrappedMap(7));
      } else {
        stateObject.lockedAssetsList = new LockedAssetsList(merks.asWrapped(7), stateObject.getVersion());
      }
    } else {
      stateObject.lockedAssetsList = new LockedAssetsList();
    }

    if (v.get(8) != null) {
      if (v.get(8) instanceof MPWrappedArray) {
        stateObject.setEncodedContractTimeEvents(v.asWrapped(8));
      } else if (v.get(8) instanceof Object[]) {
        stateObject.setEncodedContractTimeEvents(new MPWrappedArrayImpl((Object[]) v.get(8)));
      } else if (v.get(8) instanceof List) {
        stateObject.setEncodedContractTimeEvents(new MPWrappedArrayImpl(((List) v.get(8)).toArray()));
      } else {
        stateObject.setEncodedContractTimeEvents(new MPWrappedArrayImpl(new Object[]{}));
      }

    } else {
      stateObject.setEncodedContractTimeEvents(new MPWrappedArrayImpl(new Object[]{}));
    }

    // Item 9 was a "transaction count" field. It is now unused.
    // Item 10 was a "cross chain length" field". It is now unused.

    if ((v.size() > 11) && (v.get(11) instanceof MPWrappedMap) && v.asWrappedMap(11).size() > 0) {

      stateObject.setEncodedXChainSignodes(v.asWrappedMap(11));
    } else {
      stateObject.setEncodedXChainSignodes(new MPWrappedMap<>(new HashMap<>()));
    }

    stateObject.setLoadedHash(Hash.fromHex(v.asString(3)));
    stateObject.isInitialised = true;
    stateObject.initMerkleNames();
    return stateObject;
  }


  private AssetBalanceList assetBalanceList;

  private ContractsList contractsList;

  private EncumbrancesList encumbrancesList;

  private LockedAssetsList lockedAssetsList;

  private NamespaceList namespaceList;

  private PowerOfAttorney poaList;

  private SignNodeList signNodeList;


  private ObjectEncodedState(int chainId, int version, int height, Hash loadedHash, Hash blockHash, long timestamp) {
    super(chainId, version, height, loadedHash, blockHash, timestamp);
    initMerkleNames();
  }


  /**
   * Create a new empty state on the specified chain.
   *
   * @param chainId the chain
   */
  public ObjectEncodedState(int chainId, long timestamp) {
    super(chainId, VERSION_LOCKED_ASSET_AS_MERKLE, -1, Hash.NULL_HASH, Hash.NULL_HASH, timestamp);
    assetBalanceList = new AssetBalanceList(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    contractsList = new ContractsList(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    encumbrancesList = new EncumbrancesList(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    lockedAssetsList = new LockedAssetsList(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    namespaceList = new NamespaceList(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    poaList = new PowerOfAttorney(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    signNodeList = new SignNodeList(null, VERSION_LOCKED_ASSET_AS_MERKLE);
    initMerkleNames();
  }


  public ObjectEncodedState(AbstractState source) {
    super(source);
    initMerkleNames();
  }


  @Override
  protected <X extends MEntry> SnapshotMerkle<X> createMerkle(Class<X> name) {
    throw new UnsupportedOperationException("Object Encoded State does not support Merkle creation.");
  }


  /**
   * Encode state as an array of arrays of primitives, which can later be decoded by the <code>decode()</code>.
   *
   * @return encoded state
   */
  public Object[] encode() {
    Object[] merks = new Object[]{
        signNodeList.encode(),
        namespaceList.encode(),
        assetBalanceList.encode(),
        contractsList.encode(),
        encumbrancesList.encode(),
        poaList.encode(),
        getEncodedPrivilegedKeys(),
        getVersion() < VERSION_LOCKED_ASSET_AS_MERKLE ? getEncodedLockedAssets() : lockedAssetsList.encode()
    };

    return new Object[]{
        getChainId(),
        getVersion(),
        getHeight(),
        getLoadedHash().toHexString(),
        getBlockHash().toHexString(),
        getTimestamp(),
        getEncodedConfig(),
        merks,
        contractTimeEvents.encode(),
        //removed unused variables txcount and xchainlen, and replace them with -1
        -1 /*txcount*/, /*txCount*/
        -1 /*xchainlen*/,
        getEncodedXChainSignNodes()
    };
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
    return isInitialised ? new ObjectEncodedState(this) : this;
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
  protected void initialiseMerklesFrom(AbstractState source) {
    // Initialise the Merkles. We have to create empty structures appropriate for the state's version
    int v = source.getVersion();
    MPWrappedArray empty;
    if (v <= 2) {
      empty = new MPWrappedArrayImpl(new Object[]{new Object[0]});
    } else {
      empty = new MPWrappedArrayImpl(new Object[]{0, 0, 0, 0, new Object[0]});
    }
    namespaceList = new NamespaceList(empty, v);
    assetBalanceList = new AssetBalanceList(empty, v);
    contractsList = new ContractsList(empty, v);
    encumbrancesList = new EncumbrancesList(empty, v);
    poaList = new PowerOfAttorney(empty, v);
    lockedAssetsList = new LockedAssetsList(empty, v);
    initMerkleNames();

    // invoke the super class to perform a copy
    super.initialiseMerklesFrom(source);
  }


  @Override
  protected void initialiseSignNodesFrom(AbstractState source) {
    int v = source.getVersion();
    MPWrappedArray empty;
    if (v <= 2) {
      empty = new MPWrappedArrayImpl(new Object[]{new Object[0]});
    } else {
      empty = new MPWrappedArrayImpl(new Object[]{0, 0, 0, 0, new Object[0]});
    }
    signNodeList = new SignNodeList(empty, v);

    // invoke the super class to perform a copy
    super.initialiseSignNodesFrom(source);
  }


  private void setEncodedLockedAssets(MPWrappedMap<Object, Object> map) {
    lockedAssetsList = new LockedAssetsList();
    map.iterate((k, v) -> {
      String key = (String) k;
      Object[] val = (Object[]) v;
      lockedAssetsList.update(key, new LockedAsset(key, LockedAsset.Type.forCode(((Number) val[0]).intValue()), getHeight()));
    });
    lockedAssetsList.getHash();
  }

}
