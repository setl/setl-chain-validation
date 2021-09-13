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

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.CommonPy.P2PType;

/**
 * Thin wrapper around an message pack style BlockFinalized.
 * Created by aanten on 05/07/2017.
 */
public class BlockFinalized implements Message {

  private final MPWrappedArray message;

  private Block block = null;


  public BlockFinalized(MPWrappedArray message) {
    this.message = message;
  }


  /**
   * Create a new instance for the provided block.
   *
   * @param block the block that was finalized
   */
  public BlockFinalized(Block block) {
    this.block = block;
    message = new MPWrappedArrayImpl(new Object[]{
        block.getChainId(),
        P2PType.BLOCK_FINALIZED.getId(),
        MsgPackUtil.pack(block.encode())
    });
  }


  public Object[] encode() {
    return message.unwrap();
  }


  /**
   * Get the block contained in this message.
   *
   * @return the block
   */
  public Block getBlock() {
    if (block == null) {
      MPWrappedArray rawBlock = MsgPackUtil.unpackWrapped(getSerializedBlock(), false);
      block = new Block(rawBlock);
    }
    return block;
  }


  public int getChainId() {
    return message.asInt(0);
  }


  public byte[] getSerializedBlock() {
    return message.asByte(2);
  }


  public P2PType getType() {
    return P2PType.BLOCK_FINALIZED;
  }

}
