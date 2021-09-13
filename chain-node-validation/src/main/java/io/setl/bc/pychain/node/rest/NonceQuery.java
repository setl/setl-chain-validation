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
package io.setl.bc.pychain.node.rest;


import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.common.AddressUtil;
import io.setl.common.TypeSafeMap;
import io.setl.passwd.vault.SharedSecret;
import io.setl.passwd.vault.VaultAccessor;
import io.setl.utils.jwt.JwsHolder;
import io.setl.utils.jwt.Jwt;
import io.setl.utils.jwt.Jwt.Signer;
import io.setl.utils.spring.jwt.BadJwsRequest;
import io.setl.utils.spring.jwt.RequestIdTracker;
import io.setl.utils.spring.jwt.Validator;
import java.security.GeneralSecurityException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inform a client of what its nonce currently is.
 *
 * @author Simon Greatrix on 10/07/2018.
 */
@RestController
@RequestMapping("/nonce")
public class NonceQuery {

  private static final Logger logger = LoggerFactory.getLogger(NonceQuery.class);


  /** The shared secret that allows access to this query. */
  private final String secret;

  private final StateManager stateManager;

  private final TransactionPool transactionPool;

  private final Validator validator;


  /**
   * New instance.
   *
   * @param requestIdTracker the ID tracker
   * @param transactionPool  the transaction pool
   * @param stateManager     the state manager
   */
  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters") // Our default password cannot be represented any other way
  @Autowired
  public NonceQuery(RequestIdTracker requestIdTracker, TransactionPool transactionPool, StateManager stateManager) throws GeneralSecurityException {
    this.transactionPool = transactionPool;
    this.stateManager = stateManager;

    VaultAccessor vaultAccessor = VaultAccessor.getInstance();
    SharedSecret sharedSecret = vaultAccessor.get("nonce-query-jwt-secret");
    if (sharedSecret == null) {
      // The unicode U+EDDD is a private-use character, and U+FDDD is a non-character. The pair U+D83E, U+DD21 is the "clown face" emoji. The Braille is the
      // number "1337". Regrettably this password does not contain a capital letter, digit, nor symbol - so is not very secure ;-).
      secret = "\ueddd\u007f\ufddd\u007f\ud83e\udd21 ⓣⓗⓘⓢ ⓘⓢ мы sǝɔɹǝʇ ραssωοяδ ⠼⠁⠼⠉⠼⠉⠼⠛\0\n";
    } else {
      secret = new String(vaultAccessor.get("nonce-query-jwt-secret").password());
    }

    validator = new Validator(requestIdTracker, (j, u) -> j.isValid(secret));
    validator.setToken(RequestIdTracker.getDisambiguator());
  }


  /**
   * Handle the request.
   *
   * @param data the JWS input
   *
   * @return JWS output
   */
  @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
  public ResponseEntity<JwsHolder> doIt(@RequestBody JwsHolder data) {
    logger.trace("Request received");
    try {
      TypeSafeMap map = new TypeSafeMap(validator.validate(data));

      String address = map.getString("address");
      if (address == null || !AddressUtil.verifyAddress(address)) {
        logger.debug("Request was rejected due to address: {}", address);
        throw new BadJwsRequest(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
      }

      TypeSafeMap output = new TypeSafeMap();

      State state = stateManager.getState();
      output.put("chainid", state.getChainId());
      output.put("height", state.getHeight());
      output.put("timestamp", state.getTimestamp());

      // Get the nonce from state
      int stateNonce = -1;
      Merkle<AddressEntry> balances = state.getAssetBalances();
      AddressEntry addressEntry = balances.find(address);
      output.put("isinstate", addressEntry != null);
      if (addressEntry != null) {
        Long val = addressEntry.getNonce();
        if (val == null) {
          output.put("isnonceset", false);
          stateNonce = -2;
        } else {
          output.put("isnonceset", true);
          stateNonce = val.intValue();
        }
      }
      output.put("nonce", stateNonce);

      // Count in-flight transactions
      List<Txi> txiList = transactionPool.getAllTx(address);
      int inFlight = txiList.size();
      output.put("inflight", inFlight);
      if (inFlight > 0) {
        long minNonce = txiList.get(0).getNonce();
        output.put("mintxnonce", minNonce);

        // If the minimum known nonce is not the next one due, then we have a gap. Which is probably a huge problem.
        if (minNonce != stateNonce + 1) {
          logger.info("Query may have detected problem futures for address {}", address);
          output.put("hasfutures", true);
        }
      }

      Jwt jwt = new Jwt(Signer.HS256, output);
      JwsHolder out = new JwsHolder();
      out.setJws(jwt.token(secret));

      logger.trace("Request complete");
      return ResponseEntity.ok(out);
    } catch (BadJwsRequest badRequest) {
      return badRequest.getResponse();
    }
  }

}
