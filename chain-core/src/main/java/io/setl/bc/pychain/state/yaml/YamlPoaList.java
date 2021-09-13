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

import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.common.CommonPy.TxType;
import io.setl.common.TypeSafeList;
import io.setl.common.TypeSafeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The PoA list is indexed by the PoA entry's key. Each entry contains multiple headers and a detail.
 *
 * <pre>
 *   poa:
 *     address1:
 *       reference1:
 *         attorney: address
 *         startTime: time
 *         endTime: time
 *         items:
 * txType1:
 *             amount: value
 *             assets: [ asset1, asset2 ]
 * </pre>
 *
 * @author Simon Greatrix on 14/02/2018.
 */
public class YamlPoaList extends YamlBaseList<PoaEntry> {

  /**
   * Create a Power Of Attorney (POA) list from the state definition.
   *
   * @param myState the state definition
   */
  public YamlPoaList(TypeSafeMap myState) {
    TypeSafeMap poa = myState.getMap("poa");
    if (poa == null) {
      return;
    }

    // for each issuing address
    for (String issuerAddress : poa.keySet()) {
      PoaEntry header = new PoaEntry(0, issuerAddress);
      update(header.getKey(), header);

      TypeSafeMap byReference = poa.getMap(issuerAddress);
      if (byReference == null) {
        continue;
      }

      // for each reference for the issuing address
      for (String reference : byReference.keySet()) {
        TypeSafeMap entry = byReference.getMap(reference);
        if (entry == null) {
          continue;
        }

        // start and end times
        Long startTime = YamlState.getDate(entry.getString("startTime"));
        if (startTime == null) {
          startTime = 0L;
        }
        Long endTime = YamlState.getDate(entry.getString("endTime"));
        if (endTime == null) {
          endTime = Long.MAX_VALUE;
        }

        // Parse the items
        TypeSafeMap items = entry.getMap("items");
        ArrayList<PoaItem> itemsList = new ArrayList<>();
        if (items != null) {
          for (String txName : items.keySet()) {
            TxType txType = TxType.get(txName);
            if (txType == null) {
              throw new IllegalArgumentException("Unknown TX type:" + txName);
            }
            TypeSafeMap itemDef = items.getMap(txName);
            if (itemDef == null) {
              throw new IllegalArgumentException("PoA Item Definition must be a map entry");
            }
            Long amount = itemDef.getLong("amount");
            TypeSafeList<String> assets = itemDef.getList(TypeSafeList.F_STRING, "assets");
            if (assets != null) {
              PoaItem poaItem = new PoaItem(txType, amount, assets);
              itemsList.add(poaItem);
            }
          }
        }

        // Add reference to header
        header.setReference(reference, startTime, endTime);

        // Create new detail entry for the attorney
        String attorneyAddress = entry.getString("attorney");
        PoaEntry detail = new PoaEntry(0, header.getFullReference(reference));
        detail.setDetail(reference, issuerAddress, attorneyAddress, startTime, endTime, itemsList);
        update(detail.getKey(), detail);
      }
    }
  }


  public YamlPoaList() {
    // do nothing
  }


  @Override
  public Class<PoaEntry> getLeafType() {
    return PoaEntry.class;
  }


  /**
   * Save this POA list into a Map based state definition.
   *
   * @param state the state definition
   */
  public void save(Map<String, Object> state) {
    TreeMap<String, TreeMap<String, TreeMap<String, Object>>> poa = new TreeMap<>();
    state.put("poa", poa);

    forEach(poaEntry -> {
      PoaDetail poaDetail = poaEntry.getPoaDetail();
      if (poaDetail != null) {
        // This is a detail entry
        TreeMap<String, TreeMap<String, Object>> forIssuer = poa.computeIfAbsent(poaDetail.getIssuerAddress(), k -> new TreeMap<>());
        TreeMap<String, Object> entry = forIssuer.computeIfAbsent(poaDetail.getReference(), k -> new TreeMap<>());
        entry.put("attorney", poaDetail.getAttorneyAddress());
        entry.put("startTime", (poaDetail.getStartTime() == 0) ? null : poaDetail.getStartTime());
        entry.put("endTime", (poaDetail.getEndTime() == Long.MAX_VALUE) ? null : poaDetail.getEndTime());
        TreeMap<String, Object> itemDefs = new TreeMap<>();
        entry.put("items", itemDefs);

        // PoaItem does not expose its contents, so have to try each TX type.
        for (TxType tx : TxType.values()) {
          List<PoaItem> itemList = poaDetail.getItem(tx);
          if ((itemList != null) && (!itemList.isEmpty())) {
            ArrayList<TreeMap<String,Object>> items = new ArrayList<>();
            itemDefs.put(tx.getExternalName(), items);

            for (PoaItem item : itemList) {
              TreeMap<String, Object> itemDef = new TreeMap<>();
              itemDef.put("amount", item.getAmount().getValue());
              itemDef.put("assets", item.getAssets());
              items.add(itemDef);
            }
          }
        }

      } else {
        // This is a header entry. Just ensure the entry exists.
        poa.computeIfAbsent(poaEntry.getKey(), k -> new TreeMap<>());
      }
    });
  }
}
