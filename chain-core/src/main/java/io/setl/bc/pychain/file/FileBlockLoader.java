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
import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.BlockVerifier;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class FileBlockLoader implements BlockReader {

  private static final Logger logger = LoggerFactory.getLogger(FileBlockLoader.class);

  public final String dirName;


  public FileBlockLoader(String dirName) {
    this.dirName = dirName;
  }


  public FileBlockLoader() {
    this(String.format("%s", Defaults.get().getBlockFolder()));
  }


  @SuppressFBWarnings("UNSAFE_HASH_EQUALS")
  void blockReader() throws Exception {
    File dir = new File(dirName);
    String filterPattern = ".*";

    File[] listFiles = dir.listFiles(new PatternFilenameFilter(filterPattern));
    if (listFiles == null || listFiles.length == 0) {
      throw new RuntimeException("blocks not found!");
    }
    logger.info("Processing {} files", listFiles.length);
    BlockVerifier blockVerifier = new BlockVerifier();
    for (int blockIt = 0; blockIt < listFiles.length; blockIt++) {
      File fname = listFiles[blockIt];
      Block block = loadBlockFromFile(fname.getAbsolutePath());
      String hash = blockVerifier.computeHashAsHex(block);
      if (!fname.getName().equals(hash)) {
        throw new RuntimeException("Hash compare failed");
      }
      logger.trace("Hash compare ok:{}", hash);
    }
    logger.info("Processed {} files", listFiles.length);
  }


  /**
   * loadBlockFromFile().
   * Load block from the given file, return Block object.
   *
   * @param fileName :
   *
   * @return : Block
   * @throws IOException :
   */
  public Block loadBlockFromFile(String fileName) throws IOException {
    logger.trace("Read block {}", fileName);
    // self.inithashstr = sha256(msgpacksetl.spackb([self.chainid,
    // self.version, int(self.timestamp), self.base_height,
    // self.base_hash, hashlist, self.xcheights, self.timeevents,
    // self.contractevents])).hexdigest()

    BufferedInputStream bin = new BufferedInputStream(new FileInputStream(fileName), 1024 * 1024 * 2);

    MessageUnpacker unpacker = MsgPackUtil.newUnpacker(
        bin);

    MPWrappedArray blockList = MsgPackUtil.unpackWrapped(unpacker);
    Block block = new Block(blockList);
    bin.close();

    logger.trace("Block Level={}", block.getHeight());

    return block;
  }


  /**
   * readBlock().
   *
   * @param hash : String, Hash relating to block to load.
   *
   * @return : Block
   */
  @Override
  public Block readBlock(Hash hash) throws IOException, DBStoreException {
    logger.info("Reading block:{}", hash);
    return loadBlockFromFile(dirName + hash.toHexString());
  }
}
