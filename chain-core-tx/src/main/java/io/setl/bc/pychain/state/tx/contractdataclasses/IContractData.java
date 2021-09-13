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
package io.setl.bc.pychain.state.tx.contractdataclasses;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONObject;

public interface IContractData {

  Set<String> addresses();

  IContractData copy();

  MPWrappedMap<String, Object> encode();

  Map<String, Object> encodeToMap();

  Map<String, Object> encodeToMapForTxParameter();

  boolean equals(Object toCompare);

  String getContractType();

  String get__function();

  long setNextTimeEvent(long updateTime, boolean forceToUpdateTime);

  /**
   * A version of the contract data with human friendly values. For example timestamps may appear as ISO-8601 values, not seconds since the epoch. Friendly
   * JSON is semantically equivalent to the contract data, but may not be functionally equivalent.
   *
   * @return Human friendly JSON version of this contract data.
   */
  default JSONObject toFriendlyJSON() {
    return toJSON();
  }

  JSONObject toJSON();

}
