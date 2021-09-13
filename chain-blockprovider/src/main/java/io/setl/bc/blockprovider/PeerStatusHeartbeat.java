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
package io.setl.bc.blockprovider;

import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.StateRequest;
import io.setl.bc.pychain.peer.PeerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Request peer status, which allows us to received the finalized blocks, every 5 seconds.
 *
 * @author Simon Greatrix on 2019-02-08.
 */
@Component
public class PeerStatusHeartbeat {

  private static final Logger logger = LoggerFactory.getLogger(PeerStatusHeartbeat.class);

  private final MsgFactory msgFactory = new MsgFactory();

  private final PeerManager peerManager;

  @Value("${chainid}")
  private int chainId;


  @Autowired
  public PeerStatusHeartbeat(PeerManager peerManager) {
    this.peerManager = peerManager;
  }


  @Scheduled(cron = "4/5 * * * * ?")
  public void requestPeerStatus() {
    logger.debug("Requesting peer status");
    peerManager.broadcast(new StateRequest(chainId));
  }
}
