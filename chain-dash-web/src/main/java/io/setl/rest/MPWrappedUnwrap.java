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
package io.setl.rest;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;


// "squid:S1118" : SonarLint likes utility classes to have a private constructor
@SuppressWarnings("squid:S1118")
public class MPWrappedUnwrap {

  /**
   * Unwrap a message pack wrapped object into its natural Java form such as a Map or an Object array.
   *
   * @param in the object to convert
   *
   * @return the unwrapped equivalent
   */
  public static Object unwrap(Object in) {
    if (in == null) {
      return null;
    }
    if (in instanceof MPWrappedArray) {
      Object[] src = ((MPWrappedArray) in).unwrap();
      Object[] r = new Object[src.length];
      for (int i = 0; i < r.length; i++) {
        r[i] = unwrap(src[i]);
      }
      return r;
    }
    if (in instanceof MPWrappedMap) {
      NavigableMap<String, Object> src = ((MPWrappedMap<?,?>) in).toMap();
      Map<String, Object> r = new HashMap<>();
      src.forEach((k, v) -> r.put(k, unwrap(v)));
      return r;
    }
    return in;
  }

}
