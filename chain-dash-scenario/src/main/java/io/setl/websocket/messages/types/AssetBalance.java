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
package io.setl.websocket.messages.types;

import static io.setl.common.Balance.BALANCE_ZERO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.setl.common.Balance;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetBalance {
  
  private static final Logger logger = LoggerFactory.getLogger(AssetBalance.class);
  private Map<String, Map<String, Map<String, Number>>> fullAssetBalanceMap;
  private ObjectMapper mapper;
  
  /**
   * AssetBalance Default Constructor.
   */
  public AssetBalance() {
    
    fullAssetBalanceMap = new HashMap<>();
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }
  
  /**
   * addBalance.
   *
   * @param address    :
   * @param namespace  :
   * @param instrument :
   * @param balance    :
   */
  public void addBalance(String address, String namespace, String instrument, Number balance) {
    
    Map<String, Map<String, Number>> namespaceInstrumentAmountMap = fullAssetBalanceMap.get(address);
    if (namespaceInstrumentAmountMap == null) {
      namespaceInstrumentAmountMap = new HashMap<>();
    }
    
    Map<String, Number> instrumentAmountMap = namespaceInstrumentAmountMap.get(namespace);
    if (instrumentAmountMap == null) {
      instrumentAmountMap = new HashMap<>();
    }
    
    Balance totalBalance = (new Balance(instrumentAmountMap.getOrDefault(instrument, BALANCE_ZERO))).add(balance);
    
    instrumentAmountMap.put(instrument, totalBalance.getValue());
    namespaceInstrumentAmountMap.put(namespace, instrumentAmountMap);
    fullAssetBalanceMap.put(address, namespaceInstrumentAmountMap);
  }
  
  public Map<String, Map<String, Map<String, Number>>> getFullAssetBalanceMap() {
    
    return fullAssetBalanceMap;
  }
  
  /**
   * toJSON.
   *
   * @return :
   */
  public String toJSON() {
    
    String json = "";
    
    try {
      json = mapper.writeValueAsString(this);
    } catch (IOException e) {
      logger.warn("IOException occurred: {}", e, e.getCause());
    }
    
    return json;
  }
}
