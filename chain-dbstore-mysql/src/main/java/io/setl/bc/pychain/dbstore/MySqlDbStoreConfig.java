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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Simon Greatrix on 21/11/2019.
 */
@Component
@ConditionalOnProperty(name = "db-store.type", havingValue = "mysql")
public class MySqlDbStoreConfig {

  private int chainId;

  private String dbStorePassword;

  private String dbStoreSchema;

  private String dbStoreUrl;

  private String dbStoreUser;


  public int getChainId() {
    return chainId;
  }


  public String getDbStorePassword() {
    return dbStorePassword;
  }


  public String getDbStoreSchema() {
    return dbStoreSchema;
  }


  public String getDbStoreUrl() {
    return dbStoreUrl;
  }


  public String getDbStoreUser() {
    return dbStoreUser;
  }


  public void setChainId(@Value("${chainid}") int chainId) {
    this.chainId = chainId;
  }


  public void setDbStorePassword(@Value("${db-store.password}") String dbStorePassword) {
    this.dbStorePassword = dbStorePassword;
  }


  public void setDbStoreSchema(@Value("${db-store.schema:null}") String dbStoreSchema) {
    this.dbStoreSchema = dbStoreSchema;
  }


  public void setDbStoreUrl(@Value("${db-store.url}") String dbStoreUrl) {
    this.dbStoreUrl = dbStoreUrl;
  }


  public void setDbStoreUser(@Value("${db-store.user}") String dbStoreUser) {
    this.dbStoreUser = dbStoreUser;
  }
}
