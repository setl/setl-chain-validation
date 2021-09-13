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
package io.setl.bc.pychain.state.tx;

import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.common.Hex;
import io.setl.crypto.SHA256;

/**
 * Created by nicholaspennington on 28/07/2017.
 */
public class Hash {

  /**
   * computeHash.
   *
   * @param thisTX :
   *
   * @return :
   */
  public static String computeHash(Txi thisTX) {
    return Hex.encode(SHA256.sha256(thisTX.buildHash(new JSONHashAccumulator()).getBytes()));
  }


  /**
   * computeHash.
   *
   * @param thisData :
   *
   * @return :
   */
  public static String computeHash(Object[] thisData) {
    JSONHashAccumulator hashList = new JSONHashAccumulator();
    hashList.addAll(thisData);
    return Hex.encode(SHA256.sha256(hashList.getBytes()));
  }
}
