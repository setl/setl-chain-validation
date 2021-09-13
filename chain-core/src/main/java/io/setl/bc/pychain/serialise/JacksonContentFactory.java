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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.setl.bc.pychain.Digest;
import java.io.IOException;

/**
 * @author Simon Greatrix on 2019-05-31.
 */
public class JacksonContentFactory<T> implements ContentFactory<T> {

  private final Class<T> type;


  public JacksonContentFactory(Class<T> type) {
    this.type = type;
  }


  @Override
  public Content asContent(int digestType, T value) {
    byte[] data;
    try {
      data = MsgPack.writer().writeValueAsBytes(value);
    } catch (JsonProcessingException e) {
      throw new ContentException(e);
    }
    return new Content(Digest.create(digestType), data);
  }


  @Override
  public T asValue(byte[] data) {
    if (data == null) {
      return null;
    }
    Object o = null;
    try {
      o = MsgPack.reader(type).readValue(data);
    } catch (IOException e) {
      throw new ContentException(e);
    }
    return type.cast(o);
  }


  @Override
  public Class<T> getType() {
    return type;
  }
}
