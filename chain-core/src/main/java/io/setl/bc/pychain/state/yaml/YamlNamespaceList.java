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

import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.common.TypeSafeList;
import io.setl.common.TypeSafeMap;
import java.util.ArrayList;
import java.util.Map;

/**
 * <p>Value in state is "namespaces".</p>
 * <p>Format is a list of maps.</p>
 * <p>Each map contains the entries: name, address, classes, metadata.</p>
 * <p>"classes" is a map of asset-id to metadata.</p>
 *
 * @author Simon Greatrix on 13/02/2018.
 */
public class YamlNamespaceList extends YamlBaseList<NamespaceEntry> {

  /**
   * Create a namespace list from a state definition.
   *
   * @param state the state definition
   */
  public YamlNamespaceList(TypeSafeMap state) {
    TypeSafeList<TypeSafeMap> namespaces = state.getList(TypeSafeList.F_MAP, "namespaces");
    if (namespaces == null) {
      return;
    }
    for (TypeSafeMap map : namespaces) {
      YamlNamespaceEntry yne = new YamlNamespaceEntry(map);
      update(yne.getKey(), yne);
    }
  }


  public YamlNamespaceList() {
    // do nothing
  }


  @Override
  public Class<NamespaceEntry> getLeafType() {
    return NamespaceEntry.class;
  }


  /**
   * Save this namespace list into a map based state definition.
   *
   * @param state the state definition
   */
  public void save(Map<String, Object> state) {
    ArrayList<Map<String, Object>> namespaces = new ArrayList<>();
    state.put("namespaces", namespaces);
    forEach(nse -> namespaces.add(((YamlNamespaceEntry) nse).save()));
  }


  @Override
  public void update(String key, NamespaceEntry nse) {
    if (nse instanceof YamlNamespaceEntry) {
      super.update(nse.getKey(), nse);
      return;
    }

    YamlNamespaceEntry ynse = new YamlNamespaceEntry(nse);
    super.update(ynse.getKey(), ynse);
  }
}
