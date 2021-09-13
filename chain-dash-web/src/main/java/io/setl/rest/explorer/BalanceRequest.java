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

import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.json.simple.JSONObject;
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
 * Look-up a balance in state using a POA authorisation. If the attorney does not have permission to execute an asset transfer for the issuing address, then
 * the request is forbidden. Error messages returned from this call are intended for developers, not customers.
 *
 * @author Simon Greatrix on 2019-04-03.
 */
@RestController
@RequestMapping(path = "/explorer/balance", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class BalanceRequest {

  private static final Logger logger = LoggerFactory.getLogger(BalanceRequest.class);

  private final StateManager stateManager;


  @Autowired
  public BalanceRequest(StateManager stateManager) {
    this.stateManager = stateManager;
  }


  /**
   * Get the balance available to a POA.
   *
   * @param address   the address whose balance is to be queried
   * @param authority the POA authority querying the balance
   * @param reference the POA reference being used
   *
   * @return the balance details
   */
  @GetMapping
  public ResponseEntity<String> get(@RequestParam("address") String address, @RequestParam("authority") String authority,
      @RequestParam("reference") String reference) {
    try {
      State state = stateManager.getState();
      Merkle<AddressEntry> addressEntries = state.getAssetBalances();
      AddressEntry addressEntry = addressEntries.find(address);
      if (addressEntry == null) {
        JSONObject error = new JSONObject();
        error.put("error", "Unknown issuing address");
        error.put("address", address);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }
      TreeMap<String, Balance> balances = new TreeMap<>(addressEntry.getClassBalance());

      // Check the POA reference is OK
      Merkle<PoaEntry> poaEntries = state.getPowerOfAttorneys();
      PoaEntry addressPOA = poaEntries.find(address);
      if (addressPOA == null) {
        JSONObject error = new JSONObject();
        error.put("error", "No POA entries for address");
        error.put("address", address);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }
      PoaHeader thisPOA = addressPOA.getReference(reference);
      if (thisPOA == null) {
        JSONObject error = new JSONObject();
        error.put("error", "POA reference is not recognised for address");
        error.put("address", address);
        error.put("reference", reference);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }

      long now = Instant.now().getEpochSecond();
      if (thisPOA.startDate > now || thisPOA.expiryDate < now) {
        JSONObject error = new JSONObject();
        error.put("error", "POA is not currently valid");
        error.put("address", address);
        error.put("reference", reference);
        error.put("start", thisPOA.startDate);
        error.put("startDate", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(thisPOA.startDate)));
        error.put("expiry", thisPOA.expiryDate);
        error.put("expiryDate", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(thisPOA.expiryDate)));
        error.put("now", now);
        error.put("nowDate", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(now)));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }

      PoaEntry detailPOA = poaEntries.find(addressPOA.getFullReference(reference));
      if (detailPOA == null) {
        JSONObject error = new JSONObject();
        error.put("error", "No details on record for authorisation");
        error.put("address", address);
        error.put("reference", reference);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }
      PoaDetail details = detailPOA.getPoaDetail();
      if (!authority.equals(details.getAttorneyAddress())) {
        JSONObject error = new JSONObject();
        error.put("error", "POA is for a different authority");
        error.put("address", address);
        error.put("actualAuthority", authority);
        error.put("reference", reference);
        error.put("expectedAuthority", details.getAttorneyAddress());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }
      List<PoaItem> itemList = details.getItem(TxType.TRANSFER_ASSET_X_CHAIN);
      if (itemList == null || itemList.isEmpty()) {
        JSONObject error = new JSONObject();
        error.put("error", "POA does not have asset transfer permission");
        error.put("address", address);
        error.put("authority", authority);
        error.put("reference", reference);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.toJSONString());
      }

      Iterator<String> assetIterator = balances.keySet().iterator();
      while (assetIterator.hasNext()) {
        String assetId = assetIterator.next();
        boolean matched = false;
        for (PoaItem item : itemList) {
          if (item.matchAsset(assetId)) {
            matched = true;
            break;
          }
        }
        if (!matched) {
          // Do not have permission to see this
          assetIterator.remove();
        }
      }

      JSONObject response = new JSONObject();
      response.put("balances", balances);

      Long lastChange = addressEntry.getUpdateTime();
      if (lastChange != null && lastChange.longValue() != 0) {
        response.put("lastchange", lastChange);
      }

      return ResponseEntity.ok(response.toJSONString());
    } catch (RuntimeException e) {
      logger.error("Error in /explorer/balance", e);
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(sw.toString());
    }
  }
}
