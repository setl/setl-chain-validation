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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.common.TypeSafeMap;

/**
 * @author Simon Greatrix on 13/02/2018.
 */
public class YamlNamespaceEntry extends NamespaceEntry {

  private static Map<String, Asset> getClasses(TypeSafeMap classes) {
    TreeMap<String, Asset> map = new TreeMap<>();
    if (classes != null) {
      for (Entry<String, Object> e : classes.entrySet()) {
        map.put(e.getKey(), new Asset(e.getKey(), (String) e.getValue()));
      }
    }
    return map;
  }


  /**
   * Create a namespace entry from its map representation.
   *
   * @param map the representation
   */
  public YamlNamespaceEntry(TypeSafeMap map) {
    super(
        map.getString("name"),
        map.getString("address"),
        Collections.emptyMap(),
        map.getString("metadata")
    );
    for (var asset : getClasses(map.getMap("classes")).values()) {
      setAsset(asset);
    }
  }


  /**
   * Create a YAML namespace entry by copying another entry.
   *
   * @param nse the entry to copy
   */
  public YamlNamespaceEntry(NamespaceEntry nse) {
    super(nse);
  }


  /**
   * Save this entry as a map representation.
   *
   * @return this entry as a map
   */
  public Map<String, Object> save() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("name", getKey());
    map.put("address", getAddress());

    TreeMap<String, String> classes = new TreeMap<>();
    map.put("classes", classes);
    for (String name : getAllAssetNames()) {
      classes.put(name, getClassMetadata(name));
    }
    map.put("metadata", getMetadata());

    return map;
  }

}
