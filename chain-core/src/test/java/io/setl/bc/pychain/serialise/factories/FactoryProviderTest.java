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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.serialise.MerkleLeafFactory;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.test.MemoryHashStore;

import java.util.Map;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author Simon Greatrix on 03/12/2019.
 */
public class FactoryProviderTest {

  @Test
  public void test() {
    AddressFactory addressFactory = new AddressFactory();
    ContractFactory contractFactory = new ContractFactory();
    FactoryProvider provider = new FactoryProvider();
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBeansOfType(eq(MerkleLeafFactory.class))).thenReturn(Map.of(
        "assetBalanceFactory", addressFactory,
        "contractFactory", contractFactory
    ));

    ContextRefreshedEvent event = new ContextRefreshedEvent(context);
    provider.handleContextRefresh(event);

    assertEquals(AddressEntry.class, provider.getLeafFactory(AddressEntry.class.getName()).getType());
    assertEquals(ContractEntry.class, provider.getLeafFactory(ContractEntry.class).getType());
  }


  @Test
  public void test2() {
    FactoryProvider provider = new FactoryProvider();
    provider.add(new NamespaceFactory());
    FactoryProvider.set(provider);
    assertSame(provider, FactoryProvider.get());
    assertEquals(NamespaceEntry.class, provider.getLeafFactory(NamespaceEntry.class).getType());
  }


  @Test(expected = IllegalArgumentException.class)
  public void test3() {
    FactoryProvider provider = new FactoryProvider();
    provider.getLeafFactory("wibble");
  }


  @Test(expected = IllegalArgumentException.class)
  public void test4() {
    FactoryProvider provider = new FactoryProvider();
    provider.getLeafFactory(Integer.class);
  }
}