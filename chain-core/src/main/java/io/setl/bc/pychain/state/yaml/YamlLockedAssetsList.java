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
package io.setl.bc.pychain.state.yaml;

import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.LockedAsset.Type;
import io.setl.common.TypeSafeMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author Simon Greatrix on 2019-02-25.
 */
public class YamlLockedAssetsList extends YamlBaseList<LockedAsset> {

  public YamlLockedAssetsList() {
    // do nothing
  }


  /**
   * New instance from a representation of state.
   *
   * @param myState the state representation
   */
  public YamlLockedAssetsList(TypeSafeMap myState) {
    TypeSafeMap assets = myState.getMap("lockedAssets");
    if (assets == null) {
      return;
    }
    Type[] types = Type.values();
    for (Entry<String, Object> e : assets.entrySet()) {
      long height = 0;
      String typeName;
      if (e.getValue() instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) e.getValue();
        typeName = String.valueOf(map.get("type"));
        Number n = (Number) (map.get("height"));
        height = (n != null) ? n.longValue() : 0L;
      } else {
        typeName = String.valueOf(e.getValue());
      }
      Type type = null;
      for (Type t : types) {
        if (t.name().equalsIgnoreCase(typeName)) {
          type = t;
          break;
        }
      }
      if (type == null) {
        throw new IllegalArgumentException("Unrecognised lock type: " + typeName);
      }
      update(e.getKey(), new LockedAsset(e.getKey(), type, height));
    }
  }


  @Override
  public Class<LockedAsset> getLeafType() {
    return LockedAsset.class;
  }


  /**
   * Save these locked assets.
   *
   * @param state the state representation to save this list to.
   */
  public void save(Map<String, Object> state) {
    TreeMap<String, String> map = new TreeMap<>();
    state.put("lockedAssets", map);
    forEach(la -> map.put(la.getKey(), la.getType().name()));
  }
}
