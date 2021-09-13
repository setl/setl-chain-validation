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

import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.utils.Base64;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockVerifier extends DefaultHashableHashComputer {

  private static final Logger logger = LoggerFactory.getLogger(BlockVerifier.class);


  /**
   * verifyBlockSignatures.
   * Verify signatures attached to the given block.
   * Any verification failure will fail the block.
   *
   * @param block : Block to verify
   *
   * @return : Success
   * @throws Exception :
   */
  public boolean verifyBlockSignatures(Block block) throws Exception {
    String blockHash = computeHashAsHex(block);
    boolean verifyStatus = true;

    for (int i = 0, l = block.getSigList().size(); i < l; i++) {

      MPWrappedArray txa = block.getSigList().asWrapped(i);
      byte[] signature = Base64.decode(txa.asString(0));
      String fromPubKey = txa.asString(1);
      //int chainId = txa.asInt(2)

      boolean verified = false;

      verified = MessageVerifierFactory.get().verifySignature(blockHash.getBytes(StandardCharsets.UTF_8), fromPubKey, signature);

      if (!verified) {
        verifyStatus = false;
      }

      logger.info("verifyBlockSignature pubkey:{} hash:{} verfied:{}", fromPubKey, blockHash,
          verified);
    }
    return verifyStatus;
  }
}
