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
package io.setl.bc.pychain.state.entry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.setl.util.CopyOnWriteMap;

/**
 * Central repository for data names. Data Names can be automatically registered on creation, and are also registered via Spring.
 *
 * @author Simon Greatrix on 10/11/2020.
 */
@Service
public class DataNameManager {

  private static final CopyOnWriteMap<String, DataName<? extends NamedDatum<?>>> NAMES = new CopyOnWriteMap<>();


  /**
   * Get all the known names.
   *
   * @return the known names
   */
  @SuppressWarnings("java:S1452") // Allow use of wildcard generics here
  public static Collection<DataName<? extends NamedDatum<?>>> getAllNames() {
    return NAMES.values();
  }


  /**
   * Get a known named instance. To be known, the instance must have been explicitly created previously.
   *
   * @param name the name
   *
   * @return the instance
   */
  @SuppressWarnings("java:S1452") // Allow use of wildcard generics here
  public static DataName<? extends NamedDatum<?>> getByName(String name) {
    return NAMES.get(name);
  }


  public static void register(DataName<?> instance) {
    NAMES.put(instance.getName(), instance);
  }


  /**
   * Update the known transaction handlers when the Spring context is refreshed.
   *
   * @param event details of the Spring context update
   */
  @EventListener
  @SuppressWarnings({"rawtypes"})
  public void handleContextRefresh(ContextRefreshedEvent event) {
    ApplicationContext context = event.getApplicationContext();
    Map<String, DataName> beans = context.getBeansOfType(DataName.class);
    HashMap<String, DataName<? extends NamedDatum<?>>> newNames = new HashMap<>();
    for (DataName dataName : beans.values()) {
      newNames.put(dataName.getName(), (DataName<? extends NamedDatum<?>>) dataName);
    }
    NAMES.putAll(newNames);
  }

}
