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
package io.setl.bc.serialise;

import io.setl.bc.pychain.msgpack.MsgPackable;
import java.io.IOException;
import org.msgpack.core.MessagePacker;

/**
 * An restricted version of MsgPackable that only throws IOExceptions when writing.
 *
 * @author Simon Greatrix on 2019-04-09.
 */
public interface SafeMsgPackable extends MsgPackable {

  @Override
  void pack(MessagePacker p) throws IOException;
}
