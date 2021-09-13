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

import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.common.Balance;
import io.setl.common.TypeSafeList;
import io.setl.common.TypeSafeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>Value in state is "assetBalances".</p>
 * <p>Type is a list of maps.</p>
 * <p>Map contains the keys: address, nonce, balances, highPriorityNonce, lowPriorityNonce.</p>
 * <p>Omitted nonces default to zero.</p>
 * <p>"balances" is a map of full asset ID to quantity</p>
 *
 * @author Simon Greatrix on 13/02/2018.
 */
public class YamlAssetBalances extends YamlBaseList<AddressEntry> {

  /**
   * Create an asset balance list from a state definition.
   *
   * @param state the state definition
   */
  public YamlAssetBalances(TypeSafeMap state) {
    TypeSafeList<TypeSafeMap> list = state.getList(TypeSafeList.F_MAP, "assetBalances");
    if (list == null) {
      return;
    }
    long index = 0;
    for (TypeSafeMap map : list) {
      AddressEntry entry = new AddressEntry(
          map.getString("address"),
          map.getLong("nonce", 0L),
          map.getLong("highPriorityNonce", 0L),
          map.getLong("lowPriorityNonce", 0L)
      );
      entry.setAddressMetadata(map.getString("metadata"));
      entry.setUpdateTime(map.getLong("updateTime"));
      entry.setBlockUpdateHeight(map.getInt("lastUpdateHeight",-1));
      Map<String, Balance> balances = entry.makeClassBalance();
      index++;

      TypeSafeMap values = map.getMap("balances");
      if (values != null) {
        for (String assetId : values.keySet()) {
          Long v = values.getLong(assetId);
          if (v != null) {
            balances.put(assetId, new Balance(v));
          }
        }
      }

      update(entry.getKey(), entry);
    }
  }


  public YamlAssetBalances() {
    // do nothing
  }


  @Override
  public Class<AddressEntry> getLeafType() {
    return AddressEntry.class;
  }


  /**
   * Save this.
   */
  public void save(Map<String, Object> state) {
    ArrayList<Map<String, Object>> list = new ArrayList<>();
    forEach(ae -> {
      TreeMap<String, Object> map = new TreeMap<>();
      list.add(map);
      map.put("address", ae.getAddress());
      map.put("nonce", ae.getNonce());
      if (ae.getHighPriorityNonce() != 0L) {
        map.put("highPriorityNonce", ae.getHighPriorityNonce());
      }
      if (ae.getLowPriorityNonce() != 0L) {
        map.put("lowPriorityNonce", ae.getLowPriorityNonce());
      }
      map.put("metadata", ae.getAddressMetadata());
      map.put("updateTime", ae.getUpdateTime());
      map.put("lastUpdateHeight", ae.getBlockUpdateHeight());

      Map<String, Balance> cb = ae.getClassBalance();
      TreeMap<String, Balance> balances = new TreeMap<>();
      if (cb != null) {
        balances.putAll(cb);
      }
      map.put("balances", balances);
    });

    state.put("assetBalances", list);
  }
}
