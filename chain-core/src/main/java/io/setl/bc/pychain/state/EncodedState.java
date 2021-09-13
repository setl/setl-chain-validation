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

import io.setl.bc.pychain.msgpack.MPWrappedMap;

/**
 * @author Simon Greatrix on 30/12/2018.
 */
public interface EncodedState {

  MPWrappedMap<String, Object> getEncodedConfig();

  /**
   * Get the encoded form of the locked assets. This is only applicable to state before version 5.
   *
   * @return encoded form of the locked assets
   */
  MPWrappedMap<String, Object[]> getEncodedLockedAssets();

  MPWrappedMap<String, Object[]> getEncodedPrivilegedKeys();

  MPWrappedMap<Long, Object[]> getEncodedXChainSignNodes();
}
