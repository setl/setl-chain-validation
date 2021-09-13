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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.LockedAsset.Decoder;

/**
 * @author Simon Greatrix on 2019-02-25.
 */
public class LockedAssetIpfs extends IpfsList<LockedAsset> {

  public LockedAssetIpfs(Hash hash, MerkleStore<Object> ms) {
    super(hash, ms, new Decoder(), LockedAsset.class);
  }


  public LockedAssetIpfs(Hash hash, MerkleStore<Object> ms, KeyToHashIndex<String> theIndex) {
    super(hash, ms, new Decoder(), theIndex, LockedAsset.class);
  }
}
