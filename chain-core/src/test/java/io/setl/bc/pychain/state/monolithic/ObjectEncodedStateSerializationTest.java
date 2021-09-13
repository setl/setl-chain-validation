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
package io.setl.bc.pychain.state.monolithic;

import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Created by aanten on 06/07/2017.
 */
public class ObjectEncodedStateSerializationTest {
  
  private static final Path PATH = Paths.get("src/test/resources/test-serialization");
  private static final String STATE1 = "31da94af2b7636ee117ef86844d90c1a32f5cd78effba91a3eb539c1cbbc8a63";
  private DefaultHashableHashComputer dhc = new DefaultHashableHashComputer();
  
  @Test
  public void deserializeFromFileTest() throws Exception {
    
    MPWrappedArray encodedState = MsgPackUtil.unpackWrapped(PATH.resolve(STATE1), true);
    ObjectEncodedState oes = ObjectEncodedState.decode(encodedState.asWrapped(1));
    String hash1 = dhc.computeHashAsHex(oes);
    assertEquals("Hash does not match expected", STATE1, hash1);
  }
  
  @Test
  public void deserializeSerializeDeserializeTest() throws Exception {
    
    MPWrappedArray encodedState = MsgPackUtil.unpackWrapped(PATH.resolve(STATE1), true);
    ObjectEncodedState oes = ObjectEncodedState.decode(encodedState.asWrapped(1));
    String hash1 = dhc.computeHashAsHex(oes);
    
    Object[] fileEncodedState = {oes.getChainId(), oes.encode()};
    byte[] serializedState = MsgPackUtil.pack(fileEncodedState);
    MPWrappedArray deserializedEncodedState = MsgPackUtil.unpackWrapped(serializedState, true);
    ObjectEncodedState deserializedState = ObjectEncodedState.decode(deserializedEncodedState.asWrapped(1));
    String hash2 = dhc.computeHashAsHex(deserializedState);
    
    assertEquals("Reserialized state hash does not match original", hash1, hash2);
    
  }
  
}
