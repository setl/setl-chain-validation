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

import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.CommonPy.P2PType;
import java.io.IOException;
import org.msgpack.core.MessagePacker;

/**
 * Common features of a message.
 *
 * @author Simon Greatrix on 2019-03-26.
 */
public interface Message extends MsgPackable, Encodable {

  int getChainId();

  P2PType getType();

  default void pack(MessagePacker p) throws IOException {
    MsgPackUtil.packAnything(p, encode());
  }

}
