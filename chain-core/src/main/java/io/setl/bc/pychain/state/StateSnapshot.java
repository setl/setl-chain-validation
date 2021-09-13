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

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.common.CommonPy.TxType;

/**
 * A mutable wrapper around a complete state. Each state list getter returns a mutable wrapper
 * aronund the individual state list. Commit forces all changes to be applied to the wrapped state.
 *
 * @author aanten
 */
public interface StateSnapshot extends StateBase {

  boolean addContractEvent(String contractAddress, String contractFunction, String eventName, String eventData);

  boolean addContractEventTime(String address, long eventTime);

  boolean addEffectiveTX(Txi txData);

  /**
   * Add a contract life cycle event to this snapshot.
   *
   * @param event           the type of event
   * @param contractAddress the contract that experienced it.
   * @param affectedUsers   addresses of users affected by this event
   */
  void addLifeCycleEvent(ContractLifeCycle event, String contractAddress, Set<String> affectedUsers);

  /**
   * Commit this snapshot onto its parent.
   */
  void commit() throws StateSnapshotCorruptedException;

  /**
   * Commit this snapshot onto its parent only if this snapshot is not corrupt.
   *
   * @return null on success, nature of corruption on failure
   */
  String commitIfNotCorrupt();

  /**
   * Finalize the snapshot and its parent state. No further modification will be allowed after this. This method can only be invoked on a snapshot created
   * directly from a state.
   *
   * @param block the block that has been processed.
   */
  State finalizeBlock(Block block) throws StateSnapshotCorruptedException;

  long getAddressPermissions(String address);

  default MutableMerkle<AddressEntry> getAssetBalances() {
    return getMerkle(AddressEntry.class);
  }

  Collection<EventData> getContractEvents();

  Object[] getContractEventsEncoded();

  /**
   * Map of life cycle event type to the contract addresses which experienced that event.
   *
   * @return map of event types to the contracts which experienced it.
   */
  EnumMap<ContractLifeCycle, SortedSet<String>> getContractLifeCycleEvents();

  /**
   * Map of contract address to the set of user addresses affected by that contract.
   *
   * @return map of contract address to set of user addresses.
   */
  SortedMap<String, SortedSet<String>> getContractUsers();

  default MutableMerkle<ContractEntry> getContracts() {
    return getMerkle(ContractEntry.class);
  }

  List<Txi> getEffectiveTXList();

  Object[] getEffectiveTxListEncoded();

  default MutableMerkle<AddressEncumbrances> getEncumbrances() {
    return getMerkle(AddressEncumbrances.class);
  }

  default MutableMerkle<LockedAsset> getLockedAssets() {
    return getMerkle(LockedAsset.class);
  }

  <X extends MEntry> MutableMerkle<X> getMerkle(Class<X> leafType);

  default MutableMerkle<NamespaceEntry> getNamespaces() {
    return getMerkle(NamespaceEntry.class);
  }

  default MutableMerkle<PoaEntry> getPowerOfAttorneys() {
    return getMerkle(PoaEntry.class);
  }

  MutableMerkle<SignNodeEntry> getSignNodes();

  boolean canUseTx(String address, TxType txID);

  boolean isCorrupted();

  void removeAssetLockValue(String keyName);

  void removeContractEventTime(String address, long eventTime);

  void removePendingEventTimeAddresses(long updateTime, Set<String> addressSet);

  void removeXChainSignNodesValue(Number keyName);

  void setAssetLockValue(String keyName, LockedAsset.Type keyValue);

  default <Q> void setConfigValue(Setting<Q> keyName, Q keyValue) {
    setConfigValue(keyName.getLabel(), keyValue);
  }

  void setConfigValue(String keyName, Object keyValue);

  void setCorrupted(boolean corrupted);

  void setCorrupted(boolean corrupted, String message);

  void setPrivilegedKey(String keyName, PrivilegedKey key);

  void setXChainSignNodesValue(Number keyName, XChainDetails keyValue);

}

