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
package io.setl.bc.pychain.state.hashtree;

import javax.annotation.Nullable;

public interface HashTree {

  byte[] getHashEntry(int level, long i);

  void setHash(int level, long entry, byte[] hash);
  
  void trimTree(long fromIndex);
  
  void removeHash(int level, long entry);
  
  /**
   * Return current master hash (not computed here).
   *
   * @return The master hash
   */
  @Nullable
  byte[] getTopHashEntry();
}
