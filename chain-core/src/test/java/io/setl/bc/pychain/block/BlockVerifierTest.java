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
package io.setl.bc.pychain.block;

import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.file.FileBlockLoader;
import java.io.File;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wojciechcichon on 16/06/2017.
 */
public class BlockVerifierTest {

  private static final String PATH = "./src/test/resources/test-blocks/misc/260417/";


  @Test
  public void verifyBlockSignatures() throws Exception {
    BlockReader reader = new FileBlockLoader(PATH);
    File f = new File(PATH);
    File[] allFiles = f.listFiles();
    if (allFiles == null) {
      return;
    }
    for (File file : allFiles) {
      if (!file.getName().matches("^[0-9a-fA-F]*$")) {
        continue;
      }
      Block block = reader.readBlock(Hash.fromHex(file.getName()));

      BlockVerifier verifier = new BlockVerifier();
      boolean verifiedBlock = verifier.verifyBlockSignatures(block);
      Assert.assertTrue(verifiedBlock);
    }
  }

}
