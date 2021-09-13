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
package io.setl.bc.pychain.state.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.bc.pychain.state.entry.XChainDetails.Store;
import io.setl.common.Balance;
import io.setl.common.Hex;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-03.
 */
public class XChainDetailsTest {

  XChainDetails details;


  @Test
  public void copy() {
    XChainDetails details2 = details.copy();
    assertEquals(details.toString(), details2.toString());
  }


  @Test
  public void getBlockHeight() {
    assertEquals(7, details.getBlockHeight());
  }


  @Test
  public void getChainId() {
    assertEquals(5, details.getChainId());
  }


  @Test
  public void getKey() {
    assertEquals(Long.valueOf(5), details.getKey());
  }


  @Test
  public void getParameters() {
    assertEquals(1000, details.getParameters());
  }


  @Test
  public void getStatus() {
    assertEquals(4000, details.getStatus());
  }


  @Before
  public void init() {
    SortedMap<String, Balance> map = new TreeMap<>();
    map.put("A", new Balance(123456));
    map.put("B", new Balance(new BigInteger("1234567890abcdef12345678", 16)));
    details = new XChainDetails(5, 7, map, 1000, 4000);
  }


  @Test
  public void serialise() throws IOException {
    ObjectWriter writer = MsgPack.writer();
    byte[] bytes = writer.writeValueAsBytes(details);
    assertEquals("950705cd03e882a141ce0001e240a142c70c011234567890abcdef12345678cd0fa0", Hex.encode(bytes));

    Store store = new Store(new HashMap<>());
    store.put(details.getKey(),details);
    bytes = writer.writeValueAsBytes(store);
    Store store2 = MsgPack.reader(Store.class).readValue(bytes);
    assertEquals(store,store2);
  }


  @Test
  public void setBlockHeight() {
    assertSame(details, details.setBlockHeight(-2));
    XChainDetails details2 = details.setBlockHeight(12);
    assertEquals(7, details.getBlockHeight());
    assertEquals(12, details2.getBlockHeight());
  }


  @Test
  public void setParameters() {
    XChainDetails details2 = details.setParameters(500);
    assertEquals(1000, details.getParameters());
    assertEquals(500, details2.getParameters());
  }


  @Test
  public void setSignNodes() {
    XChainDetails details2 = details.setSignNodes(Collections.emptyMap());
    assertTrue(details2.getSignNodes().isEmpty());

    XChainDetails details3 = details2.setSignNodes(details.getSignNodes());
    assertEquals(2, details3.getSignNodes().size());
  }


  @Test
  public void setStatus() {
    XChainDetails details2 = details.setStatus(12000);
    assertEquals(4000, details.getStatus());
    assertEquals(12000, details2.getStatus());
  }

  @Test
  public void encode() {
    Object[] encoded = details.encode();
    XChainDetails details2 = new XChainDetails(encoded);
    assertEquals(details.toString(),details2.toString());
  }
}