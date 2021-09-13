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
package io.setl.bc.pychain.state.entry;

import java.util.Map;

/**
 * This class exists purely to support test cases and for some deprecated functionality.
 * DO NOT USE for production functionality !
 * (Modifications to the base dictionary will be ignored if the 'IContractData contractData' has been created.
 */
public class ContractEntryTestClass {
  
  ContractEntry baseClass;
  
  private ContractEntryTestClass() {
  
  }
  
  public ContractEntryTestClass(ContractEntry baseClass) {
    this.baseClass = baseClass;
  }
  
  /**
   * getContractDictionary().
   * <p>
   * Basic object dictionary accessor.
   * Values are Object arrays as read from state.
   * DO NOT USE for production functionality !
   * (Modifications to the base dictionary will be ignored if the 'IContractData contractData' has been created.
   * </p>
   *
   * @return : Map[String, Object]dictionary.
   */
  public Map<String, Object> getDictionary() {
    
    return this.baseClass.dictionary;
  }
}
