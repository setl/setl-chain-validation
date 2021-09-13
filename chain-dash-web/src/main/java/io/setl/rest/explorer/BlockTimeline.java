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

import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Optional;
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
 * Get summaries of the block in the chain.
 *
 * @author Simon Greatrix on 2019-03-04.
 */
@RestController
@RequestMapping("/explorer/blockTimeline")
public class BlockTimeline {

  private static final int DEFAULT_RANGE = 10;

  private static final Logger logger = LoggerFactory.getLogger(BlockTimeline.class);

  private final BlockReader blockReader;

  private final DBStore dbStore;


  @Autowired
  public BlockTimeline(DBStore dbStore, BlockReader blockReader) {
    this.dbStore = dbStore;
    this.blockReader = blockReader;
  }


  /**
   * Get summaries of blocks in the chain.
   *
   * @param start      the height of the starting block
   * @param end        the height of the final block
   * @param isDetailed if true, provide extra details (requires loading the block)
   *
   * @return the block summaries
   */
  @GetMapping
  public ResponseEntity<String> get(
      @Parameter(hidden = true) HttpServletRequest httpRequest,
      @RequestParam(name = "start", required = false) Integer start,
      @RequestParam(name = "end", required = false) Integer end,
      @RequestParam(name = "details", required = false) Optional<Boolean> isDetailed
  ) {
    String urlBase = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort() + httpRequest.getContextPath()
        + "/explorer/";
    logger.debug("Block timeline request {} to {}", start, end);
    try {
      int currentHeight = dbStore.getHeight();
      int endValue = Math.min(Math.max(0, end != null ? end.intValue() : currentHeight), currentHeight);
      int startValue = Math.min(Math.max(0, start != null ? start.intValue() : (endValue - DEFAULT_RANGE)), currentHeight);
      logger.debug("Block timeline processing {} to {}", start, end);

      JSONArray timeline = new JSONArray();
      for (int height = startValue; height <= endValue; height++) {
        JSONObject details = new JSONObject(true);
        details.put("height", height);
        String blockHash = dbStore.getBlockHash(height);
        details.put("blockHash", blockHash);
        details.put("blockUrl", urlBase + "blockTransactions?blockHash=" + blockHash);
        String stateHash = dbStore.getStateHash(height);
        details.put("stateHash", stateHash);
        details.put("stateUrl", urlBase + "state?stateHash=" + stateHash);

        if (blockHash != null && isDetailed.orElse(Boolean.FALSE)) {
          Block block;
          try {
            block = Util.getBlock(logger, blockReader, blockHash);
          } catch (ResponseException e) {
            logger.error("Block transaction request failed", e);
            return e.getResponse();
          }
          details.put("size", block.getTransactionCount());
          details.put("timestamp", block.getTimeStamp());
          details.put("node", block.getNodeName());
        }

        timeline.add(details);
      }

      logger.debug("Block timeline done");

      JSONObject wrapper = new JSONObject(true);
      if (endValue < currentHeight) {
        wrapper.put("nextBlocksUrl", String.format("%sblockTimeline?start=%d&end=%d&details=%s",
            urlBase, endValue, Math.min(currentHeight, endValue + DEFAULT_RANGE), isDetailed.orElse(Boolean.FALSE)
        ));
      }
      if (startValue > 0) {
        wrapper.put("previousBlocksUrl", String.format("%sblockTimeline?start=%d&end=%d&details=%s",
            urlBase, Math.max(0, startValue - DEFAULT_RANGE), startValue, isDetailed.orElse(Boolean.FALSE)
        ));
      }
      wrapper.put("timeline", timeline);
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(wrapper.toJSONString());
    } catch (DBStoreException e) {
      logger.error("Block timeline request failed", e);
      return Util.forException(e);
    }
  }
}
