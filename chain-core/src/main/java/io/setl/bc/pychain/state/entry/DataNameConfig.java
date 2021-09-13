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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Register the standard data names so that state can discover how to interpret them.
 *
 * @author Simon Greatrix on 10/11/2020.
 */
@Configuration
public class DataNameConfig {

  /**
   * Data Name for Contract Time Events.
   *
   * @return the data name
   */
  @Bean
  public DataName<ContractTimeEvents> contractTimeEventsDataName() {
    return ContractTimeEvents.NAME;
  }

}
