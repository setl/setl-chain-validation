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
package io.setl.bc.pychain.node.monitor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@ConditionalOnProperty(name = "experimental.monitoring.ws.enabled", havingValue = "true")
public class BlueScreenWebsocket extends TextWebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(BlueScreenWebsocket.class);

  private final MeterRegistry meterRegistry;

  private final Map<String, AtomicBoolean> runningMap;


  public BlueScreenWebsocket(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.runningMap = new LinkedHashMap<>();
  }


  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    AtomicBoolean running = runningMap.get(session.getId());
    running.set(false);
    logger.info("Connection closed with status {}.\n", status);
  }


  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    logger.info("Connection established.\n");
    String sessionId = session.getId();
    final AtomicBoolean keepRunning = new AtomicBoolean(true);
    Runnable runnable = () -> {
      while (keepRunning.get()) {
        try {
          Double txPoolSize = this.meterRegistry.get("transactionPool_size").gauge().measure().iterator().next().getValue();
          BlueScreenTransactionPoolSize poolSizeObject = new BlueScreenTransactionPoolSize(System.currentTimeMillis(), txPoolSize);
          ObjectMapper objectMapper = new ObjectMapper();
          String json = objectMapper.writeValueAsString(poolSizeObject);
          session.sendMessage(new TextMessage(json));
          Thread.sleep(200);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          logger.warn("Interrupted ", ie);
          return;
        } catch (Exception ex) {
          logger.warn("Error sending a message ", ex);
        }
      }
    };
    Thread aThread = new Thread(runnable, "Blue Screen Large Transaction Pool websocket");
    aThread.start();
    runningMap.put(sessionId, keepRunning);
  }


  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {

  }


  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    logger.warn("Error in session {}: {}", session.getId(), exception.getMessage());
  }


  public static class BlueScreenTransactionPoolSize {

    private final Long time;

    private final Double value;


    public BlueScreenTransactionPoolSize(Long time, Double value) {
      this.time = time;
      this.value = value;
    }


    public Long getTime() {
      return time;
    }


    public Double getValue() {
      return value;
    }

  }

}
