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
package io.setl.bc.pychain;

import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.common.Sha256Hash;
import java.security.MessageDigest;

public class DefaultHashableHashComputer {

  /**
   * Compute the hash of a hashable object array.
   *
   * @param hashableObjectArray the input array
   *
   * @return the hash
   */
  public Hash computeHash(HashableObjectArray hashableObjectArray) {
    Object[] objectList = hashableObjectArray.getHashableObject();
    byte[] bytes = HashSerialisation.getInstance().serialise(objectList);
    MessageDigest digest = Sha256Hash.newDigest();
    byte[] hash = digest.digest(bytes);
    return new Hash(hash);
  }


  /**
   * computeHashAsHex.
   *
   * @param hashableObjectArray :
   *
   * @return : Hash string
   */
  public String computeHashAsHex(HashableObjectArray hashableObjectArray) {
    return computeHash(hashableObjectArray).toHexString();
  }
}
