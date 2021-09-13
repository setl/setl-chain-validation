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
package io.setl.bc.pychain.file;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.pychain.BlockWriter;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.msgpack.core.MessagePacker;

public class FileBlockWriter implements BlockWriter {

  public final String dirName;


  public FileBlockWriter(String dirName) {
    this.dirName = dirName;
  }


  public FileBlockWriter() {
    this(Defaults.get().getBlockFolder());
  }


  /**
   * writeBlock.
   *
   * @param block :
   * @param path  :
   *
   * @throws Exception :
   */
  public void writeBlock(Block block, Path path) throws Exception {
    try (
        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(path.toFile()))
    ) {
      try (MessagePacker packer = MsgPackUtil.newPacker(bos)) {
        MsgPackUtil.packAnything(packer, block.encode());
      }
    }
  }


  /**
   * writeBlock.
   *
   * @param block :
   * @param hash  :
   *
   * @throws Exception :
   */
  @Override
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public void writeBlock(Block block, Hash hash) throws Exception {
    Path path = Paths.get(dirName).resolve(hash.toHexString());
    writeBlock(block, path);
  }

}
