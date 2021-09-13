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
package io.setl.bc.pychain.accumulator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.setl.json.CJArray;
import io.setl.json.CJObject;
import io.setl.json.Canonical;

/**
 * Implementation: Accumulate individual hash components, to provide a hashable byte array
 * *
 * Created by aanten on 13/06/2017.
 */
public class JSONHashAccumulator implements HashAccumulator {

  /**
   * The hash list to be constructed. All data will be converted by canonical JSON to provide a deterministic hash.
   */
  private final CJArray hashList = new CJArray();


  @Override
  public void add(int obj) {
    hashList.add(obj);
  }


  @Override
  public void add(boolean obj) {
    hashList.add(obj);
  }


  @Override
  public void add(String obj) {
    hashList.add(obj);
  }


  @Override
  public void add(long obj) {
    hashList.add(obj);
  }


  /**
   * Add a Number type object to the accumulated hash.
   *
   * @param obj : Long type object to be added
   */
  @Override
  public void add(Number obj) {
    hashList.add(obj);
  }


  @Override
  public void add(List<?> value) {
    if (value == null) {
      hashList.addNull();
      return;
    }

    CJArray newArray = new CJArray(value.size());
    value.forEach(o -> newArray.add(Canonical.cast(o)));
    hashList.add(newArray);
  }


  @Override
  public void add(Object[] objArray) {
    add(Arrays.asList(objArray));
  }


  @Override
  public void add(Map<String, ?> objMap) {
    if (objMap == null) {
      hashList.addNull();
      return;
    }

    CJObject newObject = new CJObject();
    objMap.forEach((k, v) -> newObject.put(k, Canonical.cast(v)));
    hashList.add(newObject);
  }


  @Override
  public void addAll(Object[] objArray) {
    if (objArray != null) {
      addAll(Arrays.asList(objArray));
    } else {
      hashList.addNull();
    }
  }


  @Override
  public void addAll(List<?> objList) {
    if (objList != null) {
      objList.forEach(o -> hashList.add(Canonical.cast(o)));
    } else {
      hashList.addNull();
    }
  }


  /**
   * Report the byte array.
   *
   * @return : The hash as a byte array
   */
  @Override
  public byte[] getBytes() {
    String json = hashList.toCanonicalString();
    return json.getBytes(StandardCharsets.UTF_8);
  }


  @Override
  public String toString() {
    return hashList.toCanonicalString();
  }

}
