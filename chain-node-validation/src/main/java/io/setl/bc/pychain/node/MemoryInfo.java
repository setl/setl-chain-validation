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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * @author Simon Greatrix on 2019-03-04.
 */
@Component
public class MemoryInfo implements InfoContributor {

  private static Map<String, Object> convert(MemoryUsage usage) {
    TreeMap<String, Object> map = new TreeMap<>();
    map.put("initial", usage.getInit());
    map.put("committed", usage.getCommitted());
    map.put("maximum", usage.getMax());
    map.put("used", usage.getUsed());
    return map;
  }


  @Override
  public void contribute(Builder builder) {
    TreeMap<String, Object> root = new TreeMap<>();

    TreeMap<String, Object> memory = new TreeMap<>();
    root.put("memory", memory);

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    memory.put("heap", convert(memoryMXBean.getHeapMemoryUsage()));
    memory.put("nonHeap", convert(memoryMXBean.getNonHeapMemoryUsage()));
    memory.put("finalizeCount", memoryMXBean.getObjectPendingFinalizationCount());

    TreeMap<String, Object> pools = new TreeMap<>();
    memory.put("pools", pools);
    for (MemoryPoolMXBean poolMXBean : ManagementFactory.getMemoryPoolMXBeans()) {
      pools.put(poolMXBean.getName(), convert(poolMXBean.getUsage()));
    }

    builder.withDetail("memory", root);
  }
}
