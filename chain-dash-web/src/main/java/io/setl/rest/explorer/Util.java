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
package io.setl.rest.explorer;

import static io.setl.common.StringUtils.logSafe;

import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Utility methods for operations performed by several endpoints.
 *
 * @author Simon Greatrix on 2019-03-04.
 */
public class Util {

  /**
   * Convert an Exception instance into a suitable HTTP 500 response.
   *
   * @param e the exception
   *
   * @return the HTTP response
   */
  public static ResponseEntity<String> forException(Exception e) {
    // In contravention of good security, I am outputting the error message. If this is ever moved to production code, this should be removed.
    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    printWriter.println(e.getMessage());
    e.printStackTrace(printWriter);
    printWriter.flush();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body(writer.toString());
  }


  /**
   * Get a block from the chain.
   *
   * @param logger      logger in case of failures
   * @param blockReader the block reader
   * @param blockHash   the hash of the desired block
   *
   * @return the block
   * @throws ResponseException if the block is unavailable
   */
  public static Block getBlock(Logger logger, BlockReader blockReader, String blockHash) throws ResponseException {
    Hash hash = getHash(logger, blockHash);
    Block block;
    try {
      block = blockReader.readBlock(hash);
    } catch (FileNotFoundException e) {
      logger.warn("Invalid hash specified: {}", hash);
      throw new ResponseException(HttpStatus.NOT_FOUND, "Unrecognised block hash");
    } catch (Exception e) {
      logger.error("Internal error retrieving block: {}", hash, e);
      throw new ResponseException(e);
    }
    return block;
  }


  /**
   * Convert user supplied text into a Hash.
   *
   * @param logger logger in case of failures
   * @param hash   the alleged hash
   *
   * @return the hash
   * @throws ResponseException if the user supplied input is not a hexadecimal encoded hash
   */
  public static Hash getHash(Logger logger, String hash) throws ResponseException {
    if (hash == null || hash.length() != 64) {
      throw new ResponseException(HttpStatus.BAD_REQUEST, "Invalid hash - must be 64 hexadecimal digits");
    }
    logger.debug("Block transactions request for {}", logSafe(hash));
    try {
      return Hash.fromHex(hash);
    } catch (IllegalArgumentException e) {
      logger.error("Invalid hash specified: {}", logSafe(hash));
      throw new ResponseException(HttpStatus.BAD_REQUEST, "Invalid hash - must be 64 hexadecimal digits");
    }
  }
}
