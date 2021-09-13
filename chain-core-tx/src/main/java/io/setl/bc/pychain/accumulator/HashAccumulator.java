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

import java.util.List;
import java.util.Map;

/**
 * Accumulate individual hash components, to provide a hashable byte array.
 * Created by aanten on 13/06/2017.
 */
public interface HashAccumulator {

  /**
   * Add an integer typed value to the accumulated hash.
   *
   * @param value value to add
   */
  void add(int value);

  /**
   * Add an String typed value to the accumulated hash.
   *
   * @param value value to add
   */
  void add(String value);

  /**
   * Add an long typed value to the accumulated hash.
   *
   * @param value value to add
   */
  void add(long value);

  /**
   * Add a numeric value to the accumulated hash.
   *
   * @param value value to add
   */
  void add(Number value);

  /**
   * Add a list of values to the accumulated hash.
   *
   * @param value value to add
   */
  void add(List<?> value);

  /**
   * Add a list of values to the accumulated hash.
   *
   * @param value value to add
   */
  void add(Object[] value);

  /**
   * Add a map to the accumulated hash.
   *
   * @param value value to add
   */
  void add(Map<String, ?> value);

  /**
   * Add a boolean value to the accumulated hash.
   *
   * @param value value to add
   */
  void add(boolean value);

  /**
   * Add a list of values to the accumulated hash one value at a time.
   *
   * @param values values to add
   */
  void addAll(Object[] values);

  /**
   * Add a list of values to the accumulated hash one value at a time.
   *
   * @param values values to add
   */
  void addAll(List<?> values);

  /**
   * Get the bytes of the resulting hash.
   *
   * @return the bytes of the hash
   */
  byte[] getBytes();

}
