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

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.DataName;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamedDatum;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import java.util.Set;

/**
 * @author Simon Greatrix on 19/12/2018.
 */
public interface StateBase {

  /**
   * Create a new snapshot as a child of this.
   *
   * @return new snapshot
   */
  StateSnapshot createSnapshot();


  KeyedList<String, AddressEntry> getAssetBalances();

  /**
   * Get the value of the asset's lock. If no level has been specified, assumes NO_LOCK.
   *
   * @param keyName the asset's lock key name
   *
   * @return the asset lock level
   */
  default LockedAsset.Type getAssetLockValue(String keyName) {
    return getAssetLockValue(keyName, LockedAsset.Type.NO_LOCK);
  }

  /**
   * Get the value of the asset's lock.
   *
   * @param keyName      the asset's lock key name
   * @param defaultValue the lock level to assume if no explicit level
   *
   * @return the asset lock level
   */
  default LockedAsset.Type getAssetLockValue(String keyName, LockedAsset.Type defaultValue) {
    if (keyName == null) {
      return defaultValue;
    }
    LockedAsset lockedAsset = getLockedAssets().find(keyName);
    return lockedAsset != null ? lockedAsset.getType() : defaultValue;
  }

  Hash getBlockHash();

  int getChainId();

  <Q> Q getConfigValue(Setting<Q> keyName);

  default <Q> Q getConfigValue(Setting<Q> keyName, Q defaultValue) {
    Q rVal = getConfigValue(keyName);
    return (rVal == null ? defaultValue : rVal);
  }

  KeyedList<String, ContractEntry> getContracts();

  <N extends NamedDatum<N>> N getDatum(DataName<N> name);

  KeyedList<String, AddressEncumbrances> getEncumbrances();

  int getHeight();

  KeyedList<String, LockedAsset> getLockedAssets();

  <X extends MEntry> KeyedList<String, X> getMerkle(Class<X> leafType);

  Set<Class<? extends MEntry>> getMerkleTypes();

  KeyedList<String, NamespaceEntry> getNamespaces();

  KeyedList<String, PoaEntry> getPowerOfAttorneys();

  PrivilegedKey getPrivilegedKey(String keyName);

  KeyedList<String, SignNodeEntry> getSignNodes();

  StateConfig getStateConfig();

  long getTimestamp();

  int getVersion();

  XChainDetails getXChainSignNodesValue(Number keyName);

  boolean isAssetLocked(String assetName);
}
