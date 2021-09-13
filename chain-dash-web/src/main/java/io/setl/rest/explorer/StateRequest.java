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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Parameter;
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

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.State;
import io.setl.json.Canonical;

/**
 * Get details of a state either by the hash of the block that produced it, or the hash of the state.
 *
 * @author Simon Greatrix on 2019-03-04.
 */
@RestController
@RequestMapping("/explorer/state")
public class StateRequest {

  private static final Logger logger = LoggerFactory.getLogger(StateRequest.class);

  private final BlockReader blockReader;

  private final DBStore dbStore;

  private final StateReader stateReader;


  /**
   * New instance.
   *
   * @param stateReader a state reader
   * @param blockReader a block reader
   * @param dbStore     a DBStore linking hashes to blocks or states
   */
  @Autowired
  public StateRequest(StateReader stateReader, BlockReader blockReader, DBStore dbStore) {
    this.stateReader = stateReader;
    this.blockReader = blockReader;
    this.dbStore = dbStore;
  }


  /**
   * Get the details of a state. If a state hash is supplied, that is used to identify the state. Otherwise, if a block hash is supplied, that is used to
   * identify the state. If neither is supplied, the current state is returned.
   *
   * @param blockHash the block hash
   * @param stateHash the state hash
   *
   * @return the state
   */
  @GetMapping
  public ResponseEntity<String> get(
      @Parameter(hidden = true) HttpServletRequest httpRequest,
      @RequestParam(name = "blockHash", required = false) String blockHash,
      @RequestParam(name = "stateHash", required = false) String stateHash,
      @RequestParam(name = "height", required = false) Integer height
  ) {
    if (stateHash != null) {
      return getByState(httpRequest, stateHash);
    }
    if (blockHash != null) {
      return getByBlock(httpRequest, blockHash);
    }
    try {
      if (height != null) {
        return getByState(httpRequest, dbStore.getStateHash(height));
      }

      return getByState(httpRequest, dbStore.getLastStateHash());
    } catch (DBStoreException e) {
      return Util.forException(e);
    }
  }


  private ResponseEntity<String> getByBlock(HttpServletRequest httpRequest, String blockHash) {
    logger.debug("Requested state by block hash: {}", logSafe(blockHash));
    Block block;
    try {
      block = Util.getBlock(logger, blockReader, blockHash);
    } catch (ResponseException e) {
      return e.getResponse();
    }
    int height = block.getHeight();
    String nextStateHash;
    try {
      nextStateHash = dbStore.getStateHash(height + 1);
    } catch (DBStoreException e) {
      logger.error("Failed to get state hash at height {}", height + 1, e);
      return Util.forException(e);
    }
    if (nextStateHash == null) {
      logger.debug("No state at height {}", height + 1);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("No state found at height " + (height + 1));
    }
    return getByState(httpRequest, nextStateHash);
  }


  private ResponseEntity<String> getByState(HttpServletRequest httpRequest, String stateHash) {
    final String urlBase = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort() + httpRequest.getContextPath()
        + "/explorer/";

    logger.debug("Requested state by state hash: {}", logSafe(stateHash));
    Hash hash;
    try {
      hash = Util.getHash(logger, stateHash);
    } catch (ResponseException e) {
      return e.getResponse();
    }
    State state;
    try {
      state = stateReader.readState(hash.toHexString());
    } catch (DBStoreException e) {
      logger.error("Failed to get state for hash {}", hash, e);
      return Util.forException(e);
    } catch (NoStateFoundException e) {
      logger.debug("State {} not found", hash);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body("No state found with hash " + hash);
    }
    JsonObject stateRepresentation = null;
    JsonObjectBuilder builder = Json.createObjectBuilder();
    builder.add("state", stateRepresentation);

    int height = state.getHeight();
    builder.add("blockTimelineUrl", urlBase + "blockTimeline?start=" + (height - 2) + "&end=" + (height + 2));
    if (state.getBlockHash() != null) {
      builder.add("blockUrl", urlBase + "blockTransactions?blockHash=" + state.getBlockHash().toHexString());
    }
    if (height > 0) {
      builder.add("previousStateUrl", urlBase + "state?height=" + (height - 1));
    }
    try {
      if (height < dbStore.getHeight()) {
        builder.add("nextStateUrl", urlBase + "state?height=" + (height + 1));
      }
    } catch (DBStoreException e) {
      logger.debug("Failed to get current height", e);
    }
    JsonObject result = builder.build();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Canonical.cast(result).toPrettyString());
  }

}
