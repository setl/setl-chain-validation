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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class JsonTypeSafeList<E> extends TypeSafeList<E> implements JSONStreamAware {

  public JsonTypeSafeList(Function<Object, E> convertor) {
    super(convertor);
  }


  public JsonTypeSafeList(Class<E> cls) {
    super(cls);
  }


  public JsonTypeSafeList(List<E> c, Function<Object, E> convertor) {
    super(c, convertor);
  }


  @Override
  public void writeJSONString(Writer out) throws IOException {
    JSONArray.writeJSONString(this, out);
  }
}
