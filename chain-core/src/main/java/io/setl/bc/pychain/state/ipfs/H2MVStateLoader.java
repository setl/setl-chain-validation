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

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.springframework.beans.factory.annotation.Autowired;

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.NoOpChangeListener;
import io.setl.bc.pychain.state.StateChangeListener;
import io.setl.utils.ByteUtil;

public class H2MVStateLoader implements StateReader {

  private final String fileName;

  private StateChangeListener stateChangeListener = NoOpChangeListener.INSTANCE;

  private MVStore store;


  public H2MVStateLoader(String fileName) {
    this.fileName = fileName;
    open();
  }


  private void open() {
    store = MVStore.open(fileName);
  }


  @Override
  public IpfsBasedState readState(String hash)
      throws NoStateFoundException {
    MVMap<Hash, Object> map2 = store.openMap(IpfsConstants.IPFSMAPNAME);
    Object[] s2d = (Object[]) map2.get(new Hash(ByteUtil.hexToBytes(hash)));
    if (s2d == null) {
      throw new NoStateFoundException("Hash:" + hash);
    }

    IpfsBasedState state = IpfsBasedState.decodeRootFromIPFsStorage(
        new MPWrappedArrayImpl(s2d),
        new MapBackedMerkleStore<>(map2)
    );
    state.setStateChangeListener(stateChangeListener);
    return state;
  }


  @Override
  @Autowired(required = false)
  public void setChangeListener(StateChangeListener listener) {
    stateChangeListener = (listener != null) ? listener : NoOpChangeListener.INSTANCE;
  }

}
