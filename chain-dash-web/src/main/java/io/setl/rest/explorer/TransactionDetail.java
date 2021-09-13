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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.create.BaseTransaction;
import io.setl.bc.pychain.tx.Views.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Get details of a transaction from a block.
 *
 * @author Simon Greatrix on 2019-03-04.
 */
@RestController
@RequestMapping("/explorer/transactionDetail")
public class TransactionDetail {

  private static final Logger logger = LoggerFactory.getLogger(TransactionDetail.class);

  private final BlockReader blockReader;

  private final ObjectMapper objectMapper;


  /**
   * New instance.
   *
   * @param blockReader a block reader to supply blocks
   */
  @Autowired
  public TransactionDetail(BlockReader blockReader) {
    this.blockReader = blockReader;

    ObjectMapper outObjectMapper = new ObjectMapper();
    outObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    outObjectMapper.setConfig(outObjectMapper.getSerializationConfig().withView(Output.class));
    objectMapper = outObjectMapper;
  }


  /**
   * Get a transaction from a block.
   *
   * @param blockHash the hash of the block
   * @param txHash    the hash of the transaction
   *
   * @return JSON representation of the transaction
   */
  @GetMapping
  public ResponseEntity<String> get(@RequestParam("blockHash") String blockHash, @RequestParam("txHash") String txHash) {
    Hash toMatch;

    logger.debug("Block transactions request for {} in {}", logSafe(txHash), logSafe(blockHash));
    Block block;
    try {
      toMatch = Util.getHash(logger, txHash);
      block = Util.getBlock(logger, blockReader, blockHash);
    } catch (ResponseException e) {
      logger.error("Block transaction request failed", e);
      return e.getResponse();
    }

    Txi[] transactions = block.getTransactions();
    Txi matched = null;
    for (Txi txi : transactions) {
      if (Hash.fromHex(txi.getHash()).equals(toMatch)) {
        matched = txi;
        break;
      }
    }

    if (matched == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("Transaction not found in block");
    }

    BaseTransaction baseTransaction = BaseTransaction.getRepresentation(matched);
    ObjectWriter objectWriter = objectMapper.writer();
    try {
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(objectWriter.writeValueAsString(baseTransaction));
    } catch (JsonProcessingException e) {
      logger.error("Failed to generate JSON version of TX", e);
      return Util.forException(e);
    }
  }
}
