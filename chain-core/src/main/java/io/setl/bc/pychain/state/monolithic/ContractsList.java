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
package io.setl.bc.pychain.state.monolithic;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.entry.ContractEntry;
import java.util.List;
import java.util.function.Consumer;

public class ContractsList extends KeyedIndexedEntryList<ContractEntry> {

  public ContractsList(MPWrappedArray v, int version) {
    super(v, version, new ContractEntry.Decoder(), ContractEntry.class);
  }


  /**
   * Visit all the contracts in this list.
   *
   * @param cb visitor to the contract entries
   */
  public void forEachContract(Consumer<ContractEntry> cb) {
    List<ContractEntry> e = this.entriesList;
    for (ContractEntry a : e) {
      cb.accept(a);
    }
  }

}
