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
package io.setl.bc.json.data.acl;

import java.util.List;

/**
 * @author Simon Greatrix on 13/01/2020.
 */
public enum CRUD {
  CREATE(1),
  READ(2),
  UPDATE(4),
  DELETE(8);

  /** A list of all the possible values. */
  static final List<CRUD> VALUES = List.of(CREATE, READ, UPDATE, DELETE);

  /** Bit mask used in CRUD lists. */
  private final int bitMask;


  CRUD(int mask) {
    bitMask = mask;
  }


  public int getBitMask() {
    return bitMask;
  }


  public String getLabel() {
    return name();
  }
}
