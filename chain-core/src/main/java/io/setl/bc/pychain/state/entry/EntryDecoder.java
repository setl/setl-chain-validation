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
package io.setl.bc.pychain.state.entry;

import io.setl.bc.pychain.msgpack.MPWrappedArray;

/**
 * A decoder which will return the given Entry.
 *
 * @param <V> The type of the Entry.
 */
public interface EntryDecoder<V extends MEntry> {

  /**
   * Decode a wrapped Object array into an entry.
   *
   * @param va The wrapped object array.
   *
   * @return The new Entry.
   */
  V decode(MPWrappedArray va);
}
