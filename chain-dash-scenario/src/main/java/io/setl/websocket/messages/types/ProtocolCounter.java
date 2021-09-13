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
package io.setl.websocket.messages.types;

import io.setl.bc.pychain.state.tx.AssetTransferTx;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolCounter {

  private static final String BACS = "BACS";

  private static final String CHAPS = "CHAPS";

  private static final String CLS = "CLS";

  private static final String CONTRACT = "CONTRACT";

  private static final String CREST = "CREST";

  private static final String FPS = "FPS";

  private static final String HEARTBEAT = "HEARTBEAT";

  private static final String LINK = "LINK";

  private static final Logger logger = LoggerFactory.getLogger(ProtocolCounter.class);

  private static Map<String, Integer> counter;


  /**
   * Get the count of how many times each protocol has been used.
   *
   * @param transactions the transactions to analyze
   *
   * @return the counts for each protocol
   */
  public static Map<String, Integer> getCounter(Txi[] transactions) {
    initCounter();

    for (Txi transaction : transactions) {
      if (transaction instanceof AssetTransferTx) {
        AssetTransferTx assetTransferTx = (AssetTransferTx) transaction;

        String protocol = assetTransferTx.getProtocol();
        switch (protocol) {
          case LINK:
          case CONTRACT:
          case CHAPS:
          case BACS:
          case FPS:
          case HEARTBEAT:
          case CREST:
          case CLS:
            counter.put(protocol, counter.get(protocol) + 1);
            break;
          default:
            logger.warn("Invalid protocol value: {}. It will be skipped", protocol);
            break;
        }
      }
    }

    return counter;
  }


  private static void initCounter() {
    counter = new LinkedHashMap<>();
    counter.put(LINK, 0);
    counter.put(CONTRACT, 0);
    counter.put(CHAPS, 0);
    counter.put(BACS, 0);
    counter.put(FPS, 0);
    counter.put(HEARTBEAT, 0);
    counter.put(CREST, 0);
    counter.put(CLS, 0);
  }


  private ProtocolCounter() {
  }

}
