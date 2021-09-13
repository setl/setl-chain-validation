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
package io.setl.bc.pychain.state.test;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileBlockLoader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;

/**
 * @author Simon Greatrix on 09/01/2020.
 */
public class TestBlockSimulatorTest {

  @Test
  public void load() throws IOException {
    BlockBuilder bb = new BlockBuilder(loadBlock());
    Block b1 = bb.build();

    ObjectMapper mapper = new ObjectMapper();
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    String json = writer.writeValueAsString(bb);

    BlockBuilder bb2 = TestBlockSimulator.load(new StringReader(json));
    Block b2 = bb2.build();
    assertEquals(b1.getBlockHash(), b2.getBlockHash());
  }


  private Block loadBlock() {
    FileBlockLoader blockLoader = new FileBlockLoader();
    try {
      return blockLoader.loadBlockFromFile(
          "src/test/resources/test-transitions/mono/task/5-6/47cc7420bda671f748b8b61bcf06ef275ebbb6236bf868ddab00efe9fff40bfb");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load test block", e);
    }
  }
}