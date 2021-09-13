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

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.common.TypeSafeMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Simon Greatrix on 14/02/2018.
 */
public class YamlContractsList extends YamlBaseList<ContractEntry> {

  /**
   * Create a contracts list from a state definition.
   *
   * @param myState the state definition
   */
  public YamlContractsList(TypeSafeMap myState) {
    TypeSafeMap contracts = myState.getMap("contracts");
    if (contracts == null) {
      return;
    }

    int index = 0;
    for (String address : contracts.keySet()) {
      TypeSafeMap contract = contracts.getMap(address);
      if (contract != null) {
        update(address, new ContractEntry((long) index, address, new MPWrappedMap<>(contract)));
      }
      index++;
    }
  }


  public YamlContractsList() {
    // do nothing
  }


  @Override
  public Class<ContractEntry> getLeafType() {
    return ContractEntry.class;
  }


  /**
   * Save this contracts list into a map definition of state.
   *
   * @param state the state to save into
   */
  public void save(Map<String, Object> state) {
    TreeMap<String, Map<String, Object>> contracts = new TreeMap<>();
    forEach(ce -> {
      TreeMap<String, Object> data = new TreeMap<>();
      contracts.put(ce.getContractAddress(), data);

      IContractData iContractData = ce.getContractData();
      data.putAll(iContractData.encodeToMap());
    });

    state.put("contracts", contracts);
  }
}
