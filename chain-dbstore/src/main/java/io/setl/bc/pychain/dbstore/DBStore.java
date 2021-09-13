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
package io.setl.bc.pychain.dbstore;

import io.setl.bc.exception.NotImplementedException;
import java.util.List;

/**
 * <p>Provide access to persisted blockchain status - current height, height to hash mappings etc.</p>
 *
 * <p>Both state and blocks start at 0, with block 0 applied to state 0 to give state 1. That is to say "state height" is always "last block"+1, and
 * state(N)=state(N-1)+block(N-1).</p>
 *
 * @author aanten
 */
public interface DBStore {

  /**
   * Archived block.
   *
   * @deprecated Not supported
   */
  @Deprecated
  default void archivedBlock(int height) {
    throw new NotImplementedException();
  }

  /**
   * Archived state.
   *
   * @deprecated Not supported
   */
  @Deprecated
  default void archivedState(int height) {
    throw new NotImplementedException();
  }

  /**
   * Close the store.
   */
  void close() throws DBStoreException;

  /**
   * Create the initial store. If the store already exists, deletes everything in the store.
   */
  void create() throws DBStoreException;

  /**
   * Get the block hash corresponding to a given chain height.
   *
   * @param height the height
   *
   * @return the hash
   */
  String getBlockHash(int height) throws DBStoreException;

  /**
   * Unknown purpose.
   *
   * @deprecated Not supported
   */
  @Deprecated
  default int getBlockHistory(int chainId) throws DBStoreException {
    throw new NotImplementedException();
  }

  /**
   * Returns chain id for this blockchain.
   *
   * @deprecated Not supported
   */
  @Deprecated
  default long getChainId() {
    throw new NotImplementedException();
  }

  /**
   * returns all expired blocks as list of triple (Height,Hash,date).
   *
   * @return List
   * @deprecated Not supported
   */
  @Deprecated
  default List<StoreEntry> getExiredBlock(long ageUTCseconds) {
    throw new NotImplementedException();
  }

  /**
   * returns all expired states as list of triple (Height,Hash,date).
   *
   * @return List
   * @deprecated Not supported
   */
  @Deprecated
  default List<StoreEntry> getExiredState(long ageUTCseconds) {
    throw new NotImplementedException();
  }

  /**
   * Get the current block chain height.
   *
   * @return the current height
   */
  int getHeight() throws DBStoreException;

  /**
   * Get the hash of the last block. The last block is the one at the height set by the last call to <code>setHeight</code>.
   *
   * @return the hash
   */
  default String getLastBlockHash() throws DBStoreException {
    return getBlockHash(getHeight());
  }

  /**
   * Get the hash of the last state. The last state is the one at the height set by the last call to <code>setHeight</code>.
   *
   * @return the hash
   */
  default String getLastStateHash() throws DBStoreException {
    return getStateHash(getHeight());
  }

  /**
   * Get the state hash corresponding to a given block chain height.
   *
   * @param height the height
   *
   * @return the hash
   */
  String getStateHash(int height) throws DBStoreException;

  default String getStateHashBefore(long utcTimeSeconds) throws DBStoreException {
    return getStateHash(getStateHeightBefore(utcTimeSeconds));
  }

  /**
   * <b>WARNING: Not currently supported.</b> This method may be implemented at some future date, or scheduled for removal.
   *
   * Get the state height at a specified time.
   *
   * @param utcTime the time
   *
   * @return the height
   */
  default int getStateHeightBefore(long utcTime) throws DBStoreException {
    throw new NotImplementedException();
  }

  /**
   * Unknown purpose.
   *
   * @param chainId the chain ID
   *
   * @return unknown
   * @deprecated Not supported
   */
  @Deprecated
  default int getStateHistory(int chainId) throws DBStoreException {
    throw new NotImplementedException();
  }

  /**
   * Set the block hash corresponding to a given chain height.
   *
   * @param height  the height
   * @param hash    the hash
   * @param utcTime the time the block was finalized
   */
  void setBlockHash(int height, String hash, long utcTime) throws DBStoreException;

  /**
   * Unknown purpose.
   *
   * @param chainId      the chain ID
   * @param blockHistory unknown purpose
   *
   * @deprecated Not supported
   */
  @Deprecated
  default void setBlockHistory(int chainId, int blockHistory) throws DBStoreException {
    throw new NotImplementedException();
  }

  /**
   * Set the current block chain height.
   *
   * @param height the new height
   */
  void setHeight(int height) throws DBStoreException;

  /**
   * Set the state hash corresponding to a given block chain height.
   *
   * @param height  the height
   * @param hash    the hash
   * @param utcTime the time the state was finalized
   */
  void setStateHash(int height, String hash, long utcTime) throws DBStoreException;

  /**
   * Unknown purpose.
   *
   * @param chainId      the chain ID
   * @param stateHistory unknown purpose
   *
   * @deprecated Not supported
   */
  @Deprecated
  default void setStateHistory(int chainId, int stateHistory) throws DBStoreException {
    throw new NotImplementedException();
  }
}

