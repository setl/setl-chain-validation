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
package io.setl.bc.pychain.node;

import io.setl.bc.pychain.peer.PeerManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/status")
public class StatusRestController {

  @Autowired
  TransactionPool txPool;

  @Autowired
  PeerManager peerManager;
  @Autowired
  @Qualifier("prePoolTransactionTaskExecutor")
  AsyncTaskExecutor transactionVerifierExecutor;
  @Autowired
  private StateManager stateManager;

  @GetMapping()
  public List<String> status() {

    List<String> statusDetails = new ArrayList<>();
    int txc = txPool.getAvailableTransactionCount();
    List<Object> connections = peerManager.getActiveConnectionSnapshot();
    if (connections != null) {
      connections.forEach(i -> statusDetails.add(java.util.Arrays.toString((Object[]) i)));
    } else {
      statusDetails.add("Connection details not available");
    }
    statusDetails.add("txcount:" + txc);
    try {
      StateDetail csd = stateManager.getCurrentStateDetail();
      statusDetails.add(String.format("State:%d BlockHash:%s StateHash:%s", csd.getHeight(),
          csd.getBlockHash(), csd.getStateHash()));
    } catch (Exception ex) {
      statusDetails.add(String.format("State not available - check running state. Exception : %s", ex.getMessage()));
    }
    if (transactionVerifierExecutor instanceof ThreadPoolTaskExecutor) {
      statusDetails.add(
          String.format("Transaction processor queue:%d", ((ThreadPoolTaskExecutor) transactionVerifierExecutor).getThreadPoolExecutor().getQueue().size()));
    }
    return statusDetails;
  }

}
