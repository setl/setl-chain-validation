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
package io.setl.websocket.handlers;

import io.setl.util.ZipUtil;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class TextMessageQueueHandler implements MessageQueueHandler<TextMessage> {

  private static final Logger logger = LoggerFactory.getLogger(TextMessageQueueHandler.class);

  private Deque<TextMessage> queue = new ConcurrentLinkedDeque<>();


  public TextMessage getMessage() {

    return queue.getFirst();
  }


  public boolean isEmpty() {

    return queue.isEmpty();
  }


  /**
   * Send a text message to the web socket.
   *
   * @param message the message to send
   * @param session the session to send the message to
   *
   * @return true on success
   */
  @SuppressWarnings("squid:S2445") // 'Blocks should be synchronized on "private final" fields' : Use verified by SG. (NPP)
  public boolean sendMessage(byte[] message, WebSocketSession session) {

    try {
      String zippedMessage = "LZ_" + ZipUtil.zipB64(message);
      TextMessage msg = new TextMessage(zippedMessage);

      synchronized (session) {
        session.sendMessage(msg);
      }
    } catch (IOException e) {
      logger.warn("Exception caught", e);
      return false;
    }

    return true;
  }
}
