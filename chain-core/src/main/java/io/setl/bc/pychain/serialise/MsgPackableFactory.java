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
package io.setl.bc.pychain.serialise;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.msgpack.core.MessageUnpacker;

import io.setl.bc.serialise.SafeMsgPackable;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.SafeMsgPackable;

/**
 * A content factory for values that are message packable. The value must implement the SafeMsgPackable interface and have a public constructor that accepts
 * a MessageUnpacker as its only argument.
 *
 * @author Simon Greatrix on 2019-05-31.
 */
public class MsgPackableFactory<Q extends SafeMsgPackable> extends BaseMsgPackableFactory<Q> {

  /**
   * The constructor for creating values from a MessageUnpacker instance.
   */
  private final Constructor<Q> constructor;

  private final Class<Q> type;


  /**
   * New instance.
   *
   * @param type the class
   */
  public MsgPackableFactory(Class<Q> type) {
    this.type = type;
    try {
      constructor = type.getConstructor(MessageUnpacker.class);
    } catch (NoSuchMethodException e) {
      throw new LinkageError("The specified class " + type + " must support a suitable constructor");
    }
  }


  @Override
  public Class<Q> getType() {
    return type;
  }


  @Override
  protected Q newInstance(MessageUnpacker unpacker) {
    try {
      return constructor.newInstance(unpacker);
    } catch (IllegalAccessException | InstantiationException e) {
      throw new ContentException("Constructor failed in unexpected way", e);
    } catch (InvocationTargetException e) {
      Throwable thrown = e.getTargetException();
      if (thrown instanceof RuntimeException) {
        throw (RuntimeException) thrown;
      }
      if (thrown instanceof Error) {
        throw (Error) thrown;
      }
      throw new ContentException("Constructor threw a checked exception", e);
    }
  }

}
