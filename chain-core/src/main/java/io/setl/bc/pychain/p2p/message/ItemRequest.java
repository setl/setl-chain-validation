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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.ItemType;
import io.setl.common.CommonPy.P2PType;

/**
 * @author Simon Greatrix on 2019-03-26.
 */
public class ItemRequest implements Message {

  private final MPWrappedArray message;


  public ItemRequest(MPWrappedArray message) {
    this.message = message;
  }


  public ItemRequest(int chainId, ItemType item, Object itemID) {
    message = new MPWrappedArrayImpl(new Object[]{chainId, P2PType.ITEM_REQUEST.getId(), item.getId(), itemID});
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  @Override
  public int getChainId() {
    return message.asInt(0);
  }


  public Object getId() {
    return message.get(3);
  }


  public ItemType getItemType() {
    return ItemType.byId(message.asString(2));
  }

  @Override
  public P2PType getType() {
    return P2PType.ITEM_REQUEST;
  }
}
