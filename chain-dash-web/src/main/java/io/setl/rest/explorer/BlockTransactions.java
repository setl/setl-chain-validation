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
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.tx.Txi;
import io.swagger.v3.oas.annotations.Parameter;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Get transactions from a specified block.
 *
 * @author Simon Greatrix on 2019-03-04.
 */
@RestController
@RequestMapping("/explorer/blockTransactions")
public class BlockTransactions {

  private static final Logger logger = LoggerFactory.getLogger(BlockTransactions.class);

  private final BlockReader blockReader;

  private final DBStore dbStore;


  @Autowired
  public BlockTransactions(DBStore dbStore, BlockReader blockReader) {
    this.dbStore = dbStore;
    this.blockReader = blockReader;
  }


  /**
   * Get transactions from a specified block.
   *
   * @param blockHash the block
   *
   * @return the transactions
   */
  @GetMapping
  public ResponseEntity<String> get(
      @Parameter(hidden = true) HttpServletRequest httpRequest,
      @RequestParam("blockHash") String blockHash
  ) {
    String urlBase = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort() + httpRequest.getContextPath()
        + "/explorer/";

    logger.debug("Block transactions request for {}", logSafe(blockHash));
    Block block;
    try {
      block = Util.getBlock(logger, blockReader, blockHash);
    } catch (ResponseException e) {
      logger.error("Block transaction request failed", e);
      return e.getResponse();
    }

    Txi[] transactions = block.getTransactions();
    JSONArray output = new JSONArray();
    for (Txi tx : transactions) {
      JSONObject map = new JSONObject(true);
      map.put("nonceAddress", tx.getNonceAddress());
      map.put("nonce", tx.getNonce());
      map.put("hash", tx.getHash());
      map.put("timestamp", tx.getTimestamp());
      map.put("addresses", new JSONArray(tx.addresses()));
      map.put("txType", tx.getTxType().getExternalName());
      map.put("isGood", tx.isGood());
      map.put("fromAddress", tx.getFromAddress());
      map.put("fromPublicKey", tx.getFromPublicKey());

      output.add(map);
    }

    JSONObject wrapper = new JSONObject(true);

    // Is there a next block, or a previous block
    try {
      String hash = dbStore.getBlockHash(block.getHeight() - 1);
      if (hash != null) {
        wrapper.put("previousBlockHash", hash);
        wrapper.put("previousBlockUrl", urlBase + "blockTransactions?blockHash=" + hash);
      }
      hash = dbStore.getBlockHash(block.getHeight() + 1);
      if (hash != null) {
        wrapper.put("nextBlockHash", hash);
        wrapper.put("nextBlockUrl", urlBase + "blockTransactions?blockHash=" + hash);
      }
    } catch (DBStoreException e) {
      logger.error("Block timeline request failed", e);
      return Util.forException(e);
    }

    wrapper.put("baseStateHash", block.getBaseStateHash().toHexString());
    wrapper.put("baseStateUrl", urlBase + "state?stateHash=" + block.getBaseStateHash().toHexString());
    wrapper.put("nextStateUrl", urlBase + "state?blockHash=" + blockHash);

    wrapper.put("height", block.getHeight());
    wrapper.put("timelineUrl", urlBase + "blockTimeline?start=" + (block.getHeight() - 2) + "&end=" + (block.getHeight() + 2));
    wrapper.put("transactions", output);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(wrapper.toJSONString());
  }
}
