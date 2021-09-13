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

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@Endpoint(id = "bluescreen")
@ConditionalOnProperty(name = "experimental.monitoring.rest.enabled", havingValue = "true")
public class BlueScreenEndpoint {

  private final MeterRegistry meterRegistry;


  public BlueScreenEndpoint(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }


  @ReadOperation
  public BlueScreenDetails details() {
    Map<String, Object> detailsMap = new LinkedHashMap<>();
    detailsMap.put("calledTimestamp", System.currentTimeMillis());
    detailsMap.put("transactionPoolSize", this.meterRegistry.get("transactionPool_size").gauge().measure().iterator().next().getValue());

    BlueScreenDetails blueScreenDetails = new BlueScreenDetails();
    blueScreenDetails.setBluescreenDetail(detailsMap);

    return blueScreenDetails;
  }


  @ReadOperation
  public String blueScreenEndpointByName(@Selector String name) {
    return "bluescreen-endpoint";
  }

}
