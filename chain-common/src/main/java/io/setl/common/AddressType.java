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
package io.setl.common;


/**
 * Enumeration of possible block chain address types.
 */
public enum AddressType {

  NORMAL(0x00),
  PRIVILEGED(0x01),
  CONTRACT(0x05),
  INVALID(0xff);


  /**
   * Get the AddressType instance associated with a type id.
   *
   * @param mType the type ID
   *
   * @return the AddressType instance, or null
   */
  public static AddressType get(int mType) {
    for (AddressType type : values()) {
      if (type.id == mType) {
        return type;
      }
    }
    return INVALID;
  }


  private final int id;


  /**
   * Constructor taking integer value.
   *
   * @param id :
   */
  AddressType(int id) {
    this.id = id;
  }


  public int getId() {
    return id;
  }
}
