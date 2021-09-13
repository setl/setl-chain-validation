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
package io.setl.bc.pychain;

import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.common.TypeSafeMap;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class ConfigMapTest {

  private static final Setting<Boolean> BAR = new Setting("bar", TypeSafeMap::asBoolean);

  private static final Setting<Integer> FOO = new Setting("foo", TypeSafeMap::asInt);

  ConfigMap configMap = new ConfigMap();


  @Before
  public void before() {
    configMap.putAll(Collections.singletonMap(FOO.getLabel(), "5"));
    configMap.putAll(Collections.singletonMap(BAR.getLabel(), 0));
  }


  @Test
  public void jackson() throws IOException {
    byte[] data = MsgPack.writer().writeValueAsBytes(configMap);
    ConfigMap other = MsgPack.reader(ConfigMap.class).readValue(data);
    assertEquals(configMap.asMap(), other.asMap());
    assertEquals(configMap.getHash(Digest.TYPE_SHA_256), other.getHash(Digest.TYPE_SHA_256));
  }


  @Test
  public void testGet() {
    assertEquals(Integer.valueOf(5), configMap.get(FOO));
    configMap.putAll(Collections.singletonMap(FOO.getLabel(), 6));
    assertEquals(Integer.valueOf(6), configMap.get(FOO));
  }
}