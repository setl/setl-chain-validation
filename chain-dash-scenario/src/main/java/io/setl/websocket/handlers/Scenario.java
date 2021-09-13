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
package io.setl.websocket.handlers;

import java.util.HashMap;
import java.util.Map;

public enum Scenario {
  CHAPS(1), CREST(2), CLS(3), FPS(4), BACS(5), LINK(6);
  private final int id;
  private static Map<Integer, Scenario> map = new HashMap<>();

  static {
    for (Scenario s : Scenario.values()) {
      map.put(s.id, s);
    }
  }

  public static Scenario forId(int id) {
    return map.get(id);
  }

  public static int size() {
    return map.size();
  }

  public int getId() {
    return id;
  }

  private Scenario(int id) {
    this.id = id;
  }
}