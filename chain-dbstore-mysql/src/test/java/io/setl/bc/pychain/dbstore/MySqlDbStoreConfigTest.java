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
package io.setl.bc.pychain.dbstore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-12-04.
 */
public class MySqlDbStoreConfigTest {

  MySqlDbStoreConfig instance = new MySqlDbStoreConfig();


  @Test
  public void getChainId() {
    instance.setChainId(4);
    assertEquals(4, instance.getChainId());
  }


  @Test
  public void getDbStorePassword() {
    instance.setDbStorePassword("secret");
    assertEquals("secret", instance.getDbStorePassword());
  }


  @Test
  public void getDbStoreSchema() {
    instance.setDbStoreSchema("schema");
    assertEquals("schema", instance.getDbStoreSchema());
  }


  @Test
  public void getDbStoreUrl() {
    instance.setDbStoreUrl("jdbc://something");
    assertEquals("jdbc://something", instance.getDbStoreUrl());
  }


  @Test
  public void getDbStoreUser() {
    instance.setDbStoreUser("bob");
    assertEquals("bob", instance.getDbStoreUser());
  }
}