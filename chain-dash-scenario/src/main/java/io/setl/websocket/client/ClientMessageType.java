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
package io.setl.websocket.client;

public enum ClientMessageType {
  CLASS_LIST(0x10),
  BALANCE_LIST(0x11),
  CONTRACT_DETAILS(0x13),
  INSTRUMENT_HOLDERS(0x1D),
  SIGNODE_LIST(0x83),
  CONTRACT_TX(0x85),
  TX_POOL(0x86),
  WEBSOCKET_REQUEST(0xA0),
  API_REQUEST(0xA1),
  SELECTIVE_UPDATE(0xA2),
  ADMIN(0xA3);

  private int value;

  ClientMessageType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
