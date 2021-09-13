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
package io.setl.bc.pychain.state.index;

import io.setl.bc.pychain.state.KeyToIndex;
import java.util.HashMap;
import java.util.Map;

/**
 * Map based implementation of Key to Index.
 *
 * @param <T> :
 */
public class DefaultKeyToIndex<T> implements KeyToIndex<T> {

  protected Map<T, Long> map = new HashMap<>();


  public DefaultKeyToIndex() {
    // Default constructor.
  }


  @Override
  public long find(T key) {
    Long r = map.get(key);
    if (r == null) {
      return -1L;
    }
    return r;
  }


  @Override
  public long getEntryCount() {
    return map.size();
  }


  @Override
  public void put(T key, long index) {
    map.put(key, index);
  }


  @Override
  public void remove(T key) {
    map.remove(key);
  }

}
