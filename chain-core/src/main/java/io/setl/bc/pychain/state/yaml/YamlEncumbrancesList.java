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

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.AssetEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEncumbrances.EncumbranceEntry;
import io.setl.common.TypeSafeList;
import io.setl.common.TypeSafeMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.Nonnull;

/**
 * <p>Value in state is "encumbrances".</p>
 * <p>Format is a map of address to address encumbrances.</p>
 * <p>An address encumbrance consists of a map of full asset IDs to asset encumbrances.</p>
 * <p>Asset encumbrances consist of a list of encumbrance entries.</p>
 * <p>Encumbrance entries have a reference, amount, and lists of administrators and beneficiaries.</p>
 * <p>Administrators and beneficiaries have an address and optional start and end dates.</p>
 *
 * <pre>
 *   encumbrances :
 *     address1 :                    # Address encumbrance
 *       "n1|asset1" :               # Asset encumbrance
 *         -
 *           reference: myReference  # Encumbrance entry
 *           amount: 1000
 *           administrators:
 *             -
 *               address: adminAddress
 *               startTime : 2018-01-01T00:00:00
 *               endTime : 2018-12-31T23:59:59
 *           beneficiaries:
 *             -
 *               address: beneficiary1
 *         -
 *           reference: otherReference
 *           amount: 50
 *       ]
 *       "n1|asset2" : [
 *       ]
 *     address2 :
 *       "n1|asset1" : [
 *          ...
 *       ]
 *
 * </pre>
 *
 * @author Simon Greatrix on 13/02/2018.
 */
public class YamlEncumbrancesList extends YamlBaseList<AddressEncumbrances> {

  private static AssetEncumbrances createEncumbrances(TypeSafeList<TypeSafeMap> assetEncumbrance) {

    ArrayList<EncumbranceEntry> entries = new ArrayList<>();

    for (TypeSafeMap map : assetEncumbrance) {

      TypeSafeList<TypeSafeMap> list = getList(map, "beneficiaries");

      ArrayList<EncumbranceDetail> beneficiaries = new ArrayList<>();

      for (TypeSafeMap m : list) {
        EncumbranceDetail ed = new EncumbranceDetail(m.getString("address"), YamlState.getDate(m.getString("startTime")),
            YamlState.getDate(m.getString("endTime"))
        );
        beneficiaries.add(ed);
      }

      list = getList(map, "administrators");

      ArrayList<EncumbranceDetail> administrators = new ArrayList<>();

      for (TypeSafeMap m : list) {
        EncumbranceDetail ed = new EncumbranceDetail(m.getString("address"), YamlState.getDate(m.getString("startTime")),
            YamlState.getDate(m.getString("endTime"))
        );
        administrators.add(ed);
      }

      EncumbranceEntry entry = new EncumbranceEntry(
          map.getString("reference", ""),
          map.getLong("amount", 0L),
          beneficiaries,
          administrators
      );

      entries.add(entry);
    }

    AssetEncumbrances assetEncumbrances = new AssetEncumbrances(entries);
    assetEncumbrances.recalculateTotal();

    return assetEncumbrances;
  }


  private static TreeMap<String, Object> detailToMap(EncumbranceDetail ed) {

    TreeMap<String, Object> map = new TreeMap<>();
    map.put("address", ed.address);
    if (ed.startTime == 0) {
      map.put("startTime", null);
    } else {
      map.put("startTime", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(ed.startTime)));
    }

    if (ed.endTime == 0) {
      map.put("endTime", null);
    } else {
      map.put("endTime", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(ed.endTime)));
    }

    return map;
  }


  @Nonnull
  private static TypeSafeList<TypeSafeMap> getList(TypeSafeMap map, String key) {

    TypeSafeList<TypeSafeMap> list = map.getList(TypeSafeList.F_MAP, key);
    return (list != null) ? list : new TypeSafeList<>(TypeSafeList.F_MAP);
  }


  /**
   * Create an Encumbrances List from the YAML state definition.
   *
   * @param state the state definition including the encumbrances
   */
  public YamlEncumbrancesList(TypeSafeMap state) {

    TypeSafeMap encumbrances = state.getMap("encumbrances");
    if (encumbrances == null) {
      return;
    }

    int index = 0;
    for (String address : encumbrances.keySet()) {
      TypeSafeMap addressEncumbrance = encumbrances.getMap(address);
      if (addressEncumbrance == null) {
        continue;
      }

      AddressEncumbrances addressEncumbrances = new AddressEncumbrances(address);
      for (String assetId : addressEncumbrance.keySet()) {
        TypeSafeList<TypeSafeMap> assetEncumbrance = getList(addressEncumbrance, assetId);
        addressEncumbrances.getEncumbranceList().put(assetId, createEncumbrances(assetEncumbrance));
      }

      update(addressEncumbrances.getKey(), addressEncumbrances);
      index++;
    }
  }


  public YamlEncumbrancesList() {
    // do nothing
  }


  @Override
  public Class<AddressEncumbrances> getLeafType() {
    return AddressEncumbrances.class;
  }


  /**
   * Save this into a map.
   *
   * @param state the state map
   */
  public void save(Map<String, Object> state) {

    TreeMap<String, Object> encumbrances = new TreeMap<>();
    state.put("encumbrances", encumbrances);
    forEach(addressEncumbrances -> {
      TreeMap<String, Object> forAddress = new TreeMap<>();
      encumbrances.put(addressEncumbrances.getAddress(), forAddress);

      Map<String, AssetEncumbrances> assetEncumbrances = addressEncumbrances.getEncumbranceList();
      for (Entry<String, AssetEncumbrances> ae : assetEncumbrances.entrySet()) {
        ArrayList<TreeMap<String, Object>> forAsset = new ArrayList<>();
        forAddress.put(ae.getKey(), forAsset);

        ae.getValue().forEach(ee -> {
          TreeMap<String, Object> entry = new TreeMap<>();
          forAsset.add(entry);
          entry.put("reference", ee.reference);
          entry.put("amount", ee.amount.getValue());
          final ArrayList<TreeMap<String, Object>> admins = new ArrayList<>();
          ee.getAdministrators().forEach(ed -> admins.add(detailToMap(ed)));
          entry.put("administrators", admins);

          final ArrayList<TreeMap<String, Object>> bens = new ArrayList<>();
          ee.getBeneficiaries().forEach(ed -> bens.add(detailToMap(ed)));
          entry.put("beneficiaries", bens);
        });
      }
    });
  }
}
