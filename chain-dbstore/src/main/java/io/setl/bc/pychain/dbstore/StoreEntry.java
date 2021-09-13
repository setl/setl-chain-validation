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
package io.setl.bc.pychain.dbstore;

public class StoreEntry {

  private final long date;

  private final String hash;

  private final long height;


  /**
   * Create a new instance.
   *
   * @param hash   the state or block hash
   * @param height the chain height
   * @param date   the time stamp
   */
  public StoreEntry(String hash, long height, long date) {

    this.hash = hash;
    this.height = height;
    this.date = date;
  }


  public long getDate() {

    return date;
  }


  public String getHash() {

    return hash;
  }


  public long getHeight() {

    return height;
  }
}
