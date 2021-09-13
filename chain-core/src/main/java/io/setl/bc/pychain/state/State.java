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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.ContractTimeEvents;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import java.util.Map;
import java.util.SortedSet;

public interface State extends StateBase {

  /**
   * Get the effective time of an event. An event's time is rounded UP to the next 5 second interval, which ensures it is always executed after its appointed
   * time.
   *
   * @param eventTime the desired event time
   *
   * @return the effective time of the event
   */
  static Long getEventTimeKey(Long eventTime) {
    long mod = (eventTime % 5L);
    if (mod == 0) {
      // return same Long instance to prevent needless object creation.
      return eventTime;
    }
    // Create new Long instance (implicitly via boxing)
    return eventTime.longValue() + 5L - mod;
  }

  boolean anyPendingEventTime(long eventTime);

  Merkle<AddressEntry> getAssetBalances();

  ContractTimeEvents getContractTimeEvents();

  Merkle<ContractEntry> getContracts();

  Merkle<AddressEncumbrances> getEncumbrances();

  Hash getLoadedHash();

  Merkle<LockedAsset> getLockedAssets();

  /**
   * Get a typed Merkle, if it already exists in state.
   *
   * @param leafType the type of the leaf of this Merkle
   * @param <X>      the leaf type
   *
   * @return the Merkle, or null if it does not exist
   */
  <X extends MEntry> Merkle<X> getMerkle(Class<X> leafType);

  Merkle<NamespaceEntry> getNamespaces();

  Merkle<PoaEntry> getPowerOfAttorneys();

  Map<String, PrivilegedKey> getPrivilegedKeys();

  Merkle<SignNodeEntry> getSignNodes();

  Map<Long, XChainDetails> getXChainSignNodes();

  default XChainDetails getXChainSignNodesValue(Number keyName) {
    return getXChainSignNodes().get(keyName.longValue());
  }

  long nextPendingEventTime();

  SortedSet<String> pendingEventTimeAddresses(long eventTime);

  /**
   * Verify all the top level hashes of a state are correct. Does not verify the internal hashes of the Merkle structures.
   *
   * @return true if the state is correct
   */
  boolean verifyAll();
}
