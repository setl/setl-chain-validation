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
public class MonitorPerformanceWebsocket extends TextWebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(MonitorPerformanceWebsocket.class);

  private final MeterRegistry meterRegistry;

  private final Map<String, AtomicBoolean> runningMap;


  public MonitorPerformanceWebsocket(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.runningMap = new LinkedHashMap<>();
  }


  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    AtomicBoolean running = runningMap.get(session.getId());
    running.set(false);
    logger.debug("Connection closed with status {}.\n", status);
  }


  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    logger.debug("Connection established.\n");
    String sessionId = session.getId();
    final AtomicBoolean keepRunning = new AtomicBoolean(true);

    MonitorPerformanceMetrics monitorPerformanceMetrics = new MonitorPerformanceMetrics();
    ObjectMapper objectMapper = new ObjectMapper();

    Runnable runnable = () -> {
      while (keepRunning.get()) {
        try {
          monitorPerformanceMetrics.setTime(System.currentTimeMillis());
          monitorPerformanceMetrics.setProposalInterval(getGaugeReading("proposal_interval"));
          monitorPerformanceMetrics.setBlockSize(getGaugeReading("block_size"));
          monitorPerformanceMetrics.setBlockElapsed(getGaugeReading("block_elapsed"));

          String json = objectMapper.writeValueAsString(monitorPerformanceMetrics);
          session.sendMessage(new TextMessage(json));

          Thread.sleep(5000);
        } catch (Exception ex) {
          logger.error("Error sending a message ", ex);
        }
      }
    };
    Thread aThread = new Thread(runnable, "Monitor performance metrics websocket");
    aThread.start();
    runningMap.put(sessionId, keepRunning);
  }


  private Double getGaugeReading(String nameOfGauge) {
    return this.meterRegistry.get(nameOfGauge).gauge().measure().iterator().next().getValue();
  }


  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {

  }


  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    logger.error("Error in session {}: {}", session.getId(), exception.getMessage());
  }


  public static class MonitorPerformanceMetrics {

    private Long time;

    private Double proposalInterval;

    private Double blockSize;

    private Double blockElapsed;


    public void setTime(Long time) {
      this.time = time;
    }


    public void setProposalInterval(Double proposalInterval) {
      this.proposalInterval = proposalInterval;
    }


    public void setBlockSize(Double blockSize) {
      this.blockSize = blockSize;
    }


    public void setBlockElapsed(Double blockElapsed) {
      this.blockElapsed = blockElapsed;
    }


    public Long getTime() {
      return time;
    }


    public Double getProposalInterval() {
      return proposalInterval;
    }


    public Double getBlockSize() {
      return blockSize;
    }


    public Double getBlockElapsed() {
      return blockElapsed;
    }

  }

}
