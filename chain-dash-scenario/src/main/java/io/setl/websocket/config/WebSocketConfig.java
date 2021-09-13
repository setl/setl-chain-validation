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
package io.setl.websocket.config;

import io.setl.util.LoggedThread;
import io.setl.websocket.handlers.UpdateSocketHandler;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Autowired
  UpdateSocketHandler updateSocketHandler;


  @Bean
  protected AsyncTaskExecutor demoTransactionTaskExecutor() {
    ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
    pool.setCorePoolSize(16);
    pool.setMaxPoolSize(16);
    pool.setQueueCapacity(128);
    pool.setRejectedExecutionHandler(new CallerRunsPolicy());
    pool.setWaitForTasksToCompleteOnShutdown(true);
    pool.setThreadPriority(Thread.MIN_PRIORITY);
    pool.setThreadFactory(LoggedThread.loggedThreadFactory("DemoTasks"));
    return pool;
  }


  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(updateSocketHandler, "/updateSocket").setAllowedOrigins("*");
  }

}