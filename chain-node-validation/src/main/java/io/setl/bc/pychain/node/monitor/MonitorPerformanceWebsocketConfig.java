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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "experimental.monitoring.ws.enabled", havingValue = "true")
public class MonitorPerformanceWebsocketConfig implements WebSocketConfigurer {

  private final MonitorPerformanceWebsocket monitorPerformanceWebsocket;


  public MonitorPerformanceWebsocketConfig(MonitorPerformanceWebsocket monitorPerformanceWebsocket) {
    this.monitorPerformanceWebsocket = monitorPerformanceWebsocket;
  }


  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(monitorPerformanceWebsocket, "/ws/performance")
        .setAllowedOrigins("*");
  }

}
