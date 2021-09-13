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

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.LockedAsset.Decoder;

/**
 * @author Simon Greatrix on 2019-02-25.
 */
public class LockedAssetsList extends KeyedIndexedEntryList<LockedAsset> {

  public LockedAssetsList(MPWrappedArray v, int version) {
    super(v, version, new Decoder(), LockedAsset.class);
  }


  public LockedAssetsList() {
    super(new Decoder(), LockedAsset.class);
  }
}
