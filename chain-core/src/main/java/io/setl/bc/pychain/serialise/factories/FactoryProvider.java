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
package io.setl.bc.pychain.serialise.factories;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nonnull;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.MerkleLeafFactory;
import io.setl.bc.pychain.state.HashStore;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.util.CopyOnWriteMap;

/**
 * A provider of leaf factories and leaf stores.
 *
 * @author Simon Greatrix on 09/10/2019.
 */
@Service
@ConditionalOnBean(HashStore.class)
public class FactoryProvider {

  /** A global instance for where dependency injection is tricky. */
  private static FactoryProvider instance;


  public static FactoryProvider get() {
    return instance;
  }


  private static <X, Y> ContentFactory<Y> getLeafFactory0(Map<X, ContentFactory<?>> map, X key) {
    @SuppressWarnings("unchecked")
    ContentFactory<Y> factory = (ContentFactory<Y>) map.get(key);
    if (factory == null) {
      throw new IllegalArgumentException("No factory registered for the Merkle tree \"" + key + "\"");
    }
    return factory;
  }



  /**
   * Set the global instance.
   *
   * @param provider global provider
   */
  public static void set(FactoryProvider provider) {
    instance = provider;
  }


  private final CopyOnWriteMap<String, ContentFactory<?>> factoriesByName = new CopyOnWriteMap<>();

  private final CopyOnWriteMap<Class<?>, ContentFactory<?>> factoriesByType = new CopyOnWriteMap<>();




  /**
   * Add a factory implementation to those which can be provided by this.
   *
   * @param factory the factory
   */
  public void add(MerkleLeafFactory<?> factory) {
    ContentFactory<? extends MEntry> contentFactory = factory.getFactory();
    factoriesByName.put(contentFactory.getName(), contentFactory);
    factoriesByType.put(contentFactory.getType(), contentFactory);
  }


  /**
   * Get the leaf factory for a named Merkle.
   *
   * @param key the type of the Merkle
   *
   * @return the leaf factory
   */
  @Nonnull
  public <X> ContentFactory<X> getLeafFactory(Class<X> key) {
    return getLeafFactory0(factoriesByType, key);
  }


  /**
   * Get the leaf factory for a named Merkle.
   *
   * @param key the name of the Merkle
   *
   * @return the leaf factory
   */
  @Nonnull
  public <X> ContentFactory<X> getLeafFactory(String key) {
    return getLeafFactory0(factoriesByName, key);
  }







  /**
   * Update the known transaction handlers when the Spring context is refreshed.
   *
   * @param event details of the Spring context update
   */
  @EventListener
  public void handleContextRefresh(ContextRefreshedEvent event) {
    if (instance == null) {
      set(this);
    }

    // Create a new factory for the new context
    ApplicationContext context = event.getApplicationContext();
    loadFactories(context);
  }


  private void loadFactories(ApplicationContext context) {
    Map<String, MerkleLeafFactory> beans = context.getBeansOfType(MerkleLeafFactory.class);

    HashMap<String, ContentFactory<?>> mapName = new HashMap<>();
    HashMap<Class<?>, ContentFactory<?>> mapType = new HashMap<>();
    for (MerkleLeafFactory<?> f : beans.values()) {
      ContentFactory<? extends MEntry> contentFactory = f.getFactory();
      mapName.put(contentFactory.getName(), contentFactory);
      mapType.put(contentFactory.getType(), contentFactory);
    }
    factoriesByName.replace(mapName);
    factoriesByType.replace(mapType);
  }


}
