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

import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.common.TypeSafeList;
import io.setl.common.TypeSafeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Simon Greatrix on 14/02/2018.
 */
public class YamlSignodeList extends YamlBaseList<SignNodeEntry> {

  /**
   * Create a Sig Node list from a map based state definition.
   *
   * @param myState the state definition
   */
  public YamlSignodeList(TypeSafeMap myState) {
    TypeSafeList<TypeSafeMap> nodes = myState.getList(TypeSafeList.F_MAP, "signodes");
    if (nodes == null) {
      return;
    }
    for (TypeSafeMap map : nodes) {
      SignNodeEntry entry = new SignNodeEntry(
          map.getString("publicKey"),
          map.getString("returnAddress", ""),
          map.getInt("balance", 1),
          map.getInt("nonce", 0),
          0,
          0
      );
      update(entry.getKey(), entry);
    }
  }


  public YamlSignodeList() {
    // do nothing
  }


  @Override
  public Class<SignNodeEntry> getLeafType() {
    return SignNodeEntry.class;
  }


  /**
   * Save this Sig Node list into a map based state definition.
   *
   * @param state the state definition
   */
  public void save(Map<String, Object> state) {
    ArrayList<Map<String, Object>> signodes = new ArrayList<>();
    state.put("signodes", signodes);

    forEach(sne -> {
      TreeMap<String, Object> map = new TreeMap<>();
      map.put("publicKey", sne.getHexPublicKey());
      map.put("returnAddress", sne.getReturnAddress());
      map.put("balance", sne.getBalance().getValue());
      map.put("nonce", sne.getNonce());
      signodes.add(map);
    });
  }
}
