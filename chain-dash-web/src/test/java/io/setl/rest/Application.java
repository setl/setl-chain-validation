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
package io.setl.rest;

import io.setl.bc.pychain.event.TransactionListenerInternal;
import io.setl.bc.pychain.file.WalletLoader;
import io.setl.bc.pychain.node.StateManager;
import java.util.Properties;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@SpringBootApplication
@Configuration
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }


  /**
   * Create dummy properties for testing purposes.
   *
   * @return a configurer based on dummy properties
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer properties() {
    final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
    Properties properties = new Properties();

    properties.setProperty("basedir", "dummy");
    properties.setProperty("wallet", "dummy");

    pspc.setProperties(properties);
    return pspc;
  }


  @Bean
  public StateManager stateManager() {
    return Mockito.mock(StateManager.class);
  }


  @Bean
  public TransactionListenerInternal transactionListenerInternal() {
    return Mockito.mock(TransactionListenerInternal.class);
  }


  @Bean
  public WalletLoader walletLoader() {
    return Mockito.mock(WalletLoader.class);
  }
}
