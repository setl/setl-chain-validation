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
package io.setl.bc.store;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.StateWriter;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.AbstractEncodedState;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.NoOpChangeListener;
import io.setl.bc.pychain.state.StateChangeListener;
import io.setl.bc.pychain.state.ipfs.IpfsBasedState;
import io.setl.bc.pychain.state.ipfs.MerkleStore;
import io.setl.utils.ByteUtil;

/**
 * State reader, writer and general purpose MerkleStore appropriate to working with IPFS based state.
 *
 * @author Simon Greatrix on 2019-07-05.
 */
public class IpfsStore implements StateReader, StateWriter, MerkleStore<Object> {

  private final RawStore rawStore;

  private final MerkleStore<Object> store;

  private StateChangeListener stateChangeListener = NoOpChangeListener.INSTANCE;


  /**
   * New store with the specified raw store as the persistence engine.
   *
   * @param rawStore the store
   */
  public IpfsStore(RawStore rawStore) {
    store = new MerkleStoreWrapper(rawStore);
    this.rawStore = rawStore;
  }


  @Override
  public void flush() {
    store.flush();
  }


  @Override
  public Object get(Hash hash) {
    return store.get(hash);
  }


  @Override
  public void put(Hash hash, Object data) {
    store.put(hash, data);
  }


  @Override
  public IpfsBasedState readState(String hexHash) throws NoStateFoundException {
    Hash hash = new Hash(ByteUtil.hexToBytes(hexHash));
    Object[] array = (Object[]) store.get(hash);
    if (array == null) {
      throw new NoStateFoundException("Hash:" + hash);
    }
    MPWrappedArray wrapped = new MPWrappedArrayImpl(array);
    IpfsBasedState state = IpfsBasedState.decodeRootFromIPFsStorage(wrapped, store);
    state.setStateChangeListener(stateChangeListener);
    return state;
  }


  @Override
  @Autowired(required = false)
  public void setChangeListener(StateChangeListener listener) {
    stateChangeListener = (listener != null) ? listener : NoOpChangeListener.INSTANCE;
  }


  @Override
  public void shutdown(AbstractState state) throws DBStoreException {
    store.flush();
    try {
      rawStore.shutdown();
    } catch (IOException e) {
      throw new DBStoreException(e);
    }
  }


  @Override
  public void writeState(AbstractState state) {
    AbstractEncodedState encodedState = (AbstractEncodedState) state;
    Hash stateHash = HashSerialisation.getInstance().hash(encodedState);

    // Create outer wrapper, which also contains root hash of each merkle state
    Object[] ipfsWrappedState = IpfsBasedState.encodeRootForIPFsStorage(encodedState);

    store.put(stateHash, ipfsWrappedState);
    store.put(encodedState.getConfigHash(), encodedState.getEncodedConfig());
    store.flush();
  }

}

