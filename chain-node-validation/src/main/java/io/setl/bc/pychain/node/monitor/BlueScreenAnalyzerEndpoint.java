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

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@Endpoint(id = "bluescreenanalyzer")
@ConditionalOnProperty(name = "experimental.monitoring.rest.enabled", havingValue = "true")
public class BlueScreenAnalyzerEndpoint {

  private final MeterRegistry meterRegistry;

  private final Object lock = new Object();

  private final CircularFifoQueue<BlueScreenTransactionPoolSize> buffer = new CircularFifoQueue<>(100);


  public BlueScreenAnalyzerEndpoint(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }


  @Scheduled(fixedRate = 100)
  public void addToBuffer() {
    BlueScreenTransactionPoolSize blueScreenTransactionPoolSize = new BlueScreenTransactionPoolSize(
        System.currentTimeMillis(),
        this.meterRegistry.get("transactionPool_size").gauge().measure().iterator().next().getValue()
    );
    synchronized (lock) {
      buffer.add(blueScreenTransactionPoolSize);
    }
  }


  @ReadOperation
  public List<BlueScreenTransactionPoolSize> getBufferedPoolSize(int lastIndex) {
    List<BlueScreenTransactionPoolSize> poolSizeList = new ArrayList<>();

    synchronized (lock) {
      buffer.stream()
          .filter(b -> (b.getIndex() > lastIndex))
          .forEach(poolSizeList::add);
    }

    return poolSizeList;
  }

}
