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
package io.setl.bc.pychain.tx.updatestate.rules;

import static io.setl.common.StringUtils.logSafe;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;

/**
 * @author Simon Greatrix on 30/01/2020.
 */
public class MerkleRules {

  public static <T extends MEntry> void checkExists(String typeName, MutableMerkle<T> merkle, T entry) throws TxFailedException {
    if (entry == null) {
      throw new TxFailedException(String.format("%s is unspecified.", typeName));
    }
    if (!merkle.itemExists(entry.getKey())) {
      throw new TxFailedException(String.format("%s \"%s\" does not exist.", typeName, logSafe(entry.getKey())));
    }
  }


  public static void checkExists(String typeName, MutableMerkle<?> merkle, String key) throws TxFailedException {
    if (key == null || key.isEmpty()) {
      throw new TxFailedException(String.format("Key for %s is unspecified.", typeName));
    }
    if (!merkle.itemExists(key)) {
      throw new TxFailedException(String.format("%s \"%s\" does not exist.", typeName, logSafe(key)));
    }
  }


  public static <T extends MEntry> void checkNotExists(String typeName, MutableMerkle<T> merkle, T entry) throws TxFailedException {
    if (entry == null) {
      throw new TxFailedException(String.format("%s is unspecified.", typeName));
    }
    if (merkle.itemExists(entry.getKey())) {
      throw new TxFailedException(String.format("%s \"%s\" already exists.", typeName, logSafe(entry.getKey())));
    }
  }


  public static void checkNotExists(String typeName, MutableMerkle<?> merkle, String key) throws TxFailedException {
    if (key == null || key.isEmpty()) {
      throw new TxFailedException(String.format("Key for %s is unspecified.", typeName));
    }
    if (merkle.itemExists(key)) {
      throw new TxFailedException(String.format("%s \"%s\" already exists.", typeName, logSafe(key)));
    }
  }


  public static <T extends MEntry> T find(String typeName, MutableMerkle<T> merkle, String key) throws TxFailedException {
    if (key == null || key.isEmpty()) {
      throw new TxFailedException(String.format("Key for %s is unspecified.", typeName));
    }

    T item = merkle.find(key);
    if (item == null) {
      throw new TxFailedException(String.format("%s \"%s\" does not exist.", typeName, logSafe(key)));
    }

    return item;
  }


  public static <T extends MEntry> T findAndMarkUpdated(String typeName, MutableMerkle<T> merkle, String key) throws TxFailedException {
    if (key == null || key.isEmpty()) {
      throw new TxFailedException(String.format("Key for %s is unspecified.", typeName));
    }

    T item = merkle.findAndMarkUpdated(key);
    if (item == null) {
      throw new TxFailedException(String.format("%s \"%s\" does not exist.", typeName, logSafe(key)));
    }

    return item;
  }

}
