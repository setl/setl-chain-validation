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

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/**
 * Implementation of DBStore for MySQL.
 *
 * @author Valerio Trigari, 24/01/2018
 */
@Component
@ConditionalOnProperty(name = "db-store.type", havingValue = "mysql")
public class MySqlDBStore implements DBStore {


  private static final String SP_BLOCKS_DELETE = "CALL sp_blocks_delete(?)";

  private static final String SP_BLOCKS_GET_HASH = "CALL sp_blocks_getHash(?, ?)";

  private static final String SP_BLOCKS_INSERT = "CALL sp_blocks_insert(?, ?, ?)";

  private static final String SP_BLOCK_ARCHIVED = "CALL sp_blocks_archived(?)";

  private static final String SP_BLOCK_GET_EXPIRED = "CALL sp_blocks_getExpired(?)";

  private static final String SP_MAIN_GET_BLOCK_HISTORY = "CALL sp_main_getBlockHistory(?, ?)";

  private static final String SP_MAIN_GET_CHAINID = "CALL sp_main_getChainId()";

  private static final String SP_MAIN_GET_MAX_HEIGHT = "CALL sp_main_getMaxHeight(?, ?)";

  private static final String SP_MAIN_GET_STATE_HISTORY = "CALL sp_main_getStateHistory(?, ?)";

  private static final String SP_MAIN_INIT = "CALL sp_main_init(?)";

  private static final String SP_MAIN_UPDATE_BLOCK_HISTORY = "CALL sp_main_updateBlockHistory(?, ?)";

  private static final String SP_MAIN_UPDATE_MAX_HEIGHT = "CALL sp_main_updateMaxHeight(?, ?)";

  private static final String SP_MAIN_UPDATE_STATE_HISTORY = "CALL sp_main_updateStateHistory(?, ?)";

  private static final String SP_STATE_ARCHIVED = "CALL sp_state_archived(?)";

  private static final String SP_STATE_DELETE = "CALL sp_state_delete(?)";

  private static final String SP_STATE_GET_EXPIRED = "CALL sp_state_getExpired(?)";

  private static final String SP_STATE_GET_HASH = "CALL sp_state_getHash(?, ?)";

  private static final String SP_STATE_INSERT = "CALL sp_state_insert(?, ?, ?)";



  private enum MainValue {
    BLOCK(
        c -> c.prepareCall(SP_MAIN_GET_BLOCK_HISTORY),
        c -> c.prepareCall(SP_MAIN_UPDATE_BLOCK_HISTORY)
    ),
    STATE(
        c -> c.prepareCall(SP_MAIN_GET_STATE_HISTORY),
        c -> c.prepareCall(SP_MAIN_UPDATE_STATE_HISTORY)
    ),
    HEIGHT(
        c -> c.prepareCall(SP_MAIN_GET_MAX_HEIGHT),
        c -> c.prepareCall(SP_MAIN_UPDATE_MAX_HEIGHT)
    );

    private final CallableStatementCreator getter;

    private final CallableStatementCreator setter;


    MainValue(CallableStatementCreator g, CallableStatementCreator s) {
      getter = g;
      setter = s;
    }


    /**
     * Get a value from the main table.
     *
     * @param chainId the chain ID
     *
     * @return CSC
     */
    CallableStatementCreator getMainValueCsc(int chainId) {
      return connection -> {
        CallableStatement callableStatement = getter.createCallableStatement(connection);
        callableStatement.setInt(1, chainId);
        callableStatement.registerOutParameter(2, Types.INTEGER);
        return callableStatement;
      };
    }


    /**
     * Set a value from the main table.
     *
     * @param chainId the chain ID
     *
     * @return CSC
     */
    CallableStatementCreator setMainValueCsc(int chainId, int value) {
      return connection -> {
        CallableStatement callableStatement = setter.createCallableStatement(connection);
        callableStatement.setInt(1, chainId);
        callableStatement.setInt(2, value);
        return callableStatement;
      };
    }

  }



  private int chainId;

  private JdbcTemplate jdbcTemplate;


  /**
   * Constructor.
   *
   * @param dataSource MySQL data source
   * @param chainId    Chain ID
   * @param initMain   initialize 'main' table?
   *
   * @throws DBStoreException thrown of failed execution of stored procedure
   */
  public MySqlDBStore(DataSource dataSource, @Value("${chainid}") int chainId, @Value("${initmain:false}") boolean initMain) throws DBStoreException {
    jdbcTemplate = new JdbcTemplate(dataSource);
    this.chainId = chainId;

    if (initMain) {
      initMainTable();
    }
  }


  /**
   * Constructor.
   *
   * @param config   configuration
   * @param initMain initialize 'main' table?
   *
   * @throws DBStoreException thrown of failed execution of stored procedure
   */
  @Autowired
  public MySqlDBStore(MySqlDbStoreConfig config, @Value("${initmain:false}") boolean initMain) throws DBStoreException {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(config.getDbStoreUrl() + config.getDbStoreSchema());
    dataSource.setUsername(config.getDbStoreUser());
    dataSource.setPassword(config.getDbStorePassword());
    jdbcTemplate = new JdbcTemplate(dataSource);
    this.chainId = config.getChainId();

    if (initMain) {
      initMainTable();
    }
  }


  @Override
  public void archivedBlock(int height) {

    CallableStatementCreator callable = con -> {
      CallableStatement callableStatement = con.prepareCall(SP_BLOCK_ARCHIVED);
      callableStatement.setInt(1, height);
      return callableStatement;
    };

    List<SqlParameter> parameters = Arrays.asList(
        new SqlParameter(Types.INTEGER)
    );

    jdbcTemplate.call(callable, parameters);
  }


  @Override
  public void archivedState(final int height) {

    CallableStatementCreator callable = con -> {
      CallableStatement callableStatement = con.prepareCall(SP_STATE_ARCHIVED);
      callableStatement.setInt(1, height);
      return callableStatement;
    };

    List<SqlParameter> parameters = Arrays.asList(
        new SqlParameter(Types.INTEGER)
    );

    jdbcTemplate.call(callable, parameters);
    //TODO check
  }


  private void checkUpdate(Map<String, Object> result) throws SQLException {
    String key = (String) result.keySet().toArray()[0];
    if ((int) result.get(key) == 0) {
      throw new SQLException();
    }
  }


  @Override
  public void close() {
    // Do nothing
  }


  private List<StoreEntry> convertResult(Map<String, Object> result) {
    Object object = result.get("#result-set-1");
    if (object instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> results = (List) object;
      return results.stream().map(o ->
          new StoreEntry(
              o.get("hash").toString(),
              ((Number) o.get("height")).longValue(),
              ((java.sql.Date) o.get("dateEntered")).getTime()
          )).collect(Collectors.toList());
    } else {
      return null;
    }


  }


  @Override
  public void create() {
    // Do nothing
  }


  private CallableStatementCreator deleteHeightCsc(int height, boolean isForState) {
    return connection -> {
      CallableStatement callableStatement = connection.prepareCall(isForState ? SP_STATE_DELETE : SP_BLOCKS_DELETE);
      callableStatement.setInt(1, height);
      return callableStatement;
    };
  }


  @Override
  public String getBlockHash(int height) {
    return getHash(height, false);
  }


  @Override
  public int getBlockHistory(int chainId) {
    return getMainValue("blockHistory", chainId, MainValue.BLOCK);
  }


  @Override
  public long getChainId() {
    CallableStatementCreator statementCreator = connection -> {
      CallableStatement callableStatement = connection.prepareCall(SP_MAIN_GET_CHAINID);
      callableStatement.registerOutParameter(1, Types.INTEGER);
      return callableStatement;
    };

    List<SqlParameter> parameters = Arrays.asList(

        new SqlOutParameter("chainId", Types.INTEGER)
    );
    Map<String, Object> result = jdbcTemplate.call(statementCreator, parameters);
    result.putIfAbsent("chainId", -1);

    return (int) result.get("chainId");

  }


  @Override
  public List<StoreEntry> getExiredBlock(long ageUTCseconds) {
    return getExiredData(false, ageUTCseconds);
  }


  /**
   * Identify entries in the database which have expired.
   *
   * @param isForState    state or blocks?
   * @param ageUTCseconds the cut off for entry expiry
   *
   * @return the list of store entries which have expired
   */
  private List<StoreEntry> getExiredData(final boolean isForState, long ageUTCseconds) {

    CallableStatementCreator callable = con -> {
      CallableStatement callableStatement = con.prepareCall(isForState ? SP_STATE_GET_EXPIRED : SP_BLOCK_GET_EXPIRED);
      callableStatement.setDate(1, new Date(ageUTCseconds));
      return callableStatement;
    };

    List<SqlParameter> parameters = Arrays.asList(
        new SqlParameter(Types.DATE),
        new SqlOutParameter("hash", Types.VARCHAR),
        new SqlOutParameter("height", Types.INTEGER),
        new SqlOutParameter("dateEntered", Types.DATE)
    );
    Map<String, Object> result = jdbcTemplate.call(callable, parameters);
    return convertResult(result);
  }


  @Override
  public List<StoreEntry> getExiredState(long ageUTCseconds) {
    return getExiredData(true, ageUTCseconds);
  }


  private String getHash(int height, boolean isForState) {
    CallableStatementCreator cscGetStateHash = getHashCsc(height, isForState);
    List<SqlParameter> parameters = Arrays.asList(
        new SqlParameter(Types.INTEGER),
        new SqlOutParameter("hash", Types.VARCHAR)
    );
    Map<String, Object> result = jdbcTemplate.call(cscGetStateHash, parameters);

    return (String) result.get("hash");
  }


  private CallableStatementCreator getHashCsc(int height, boolean isForState) {
    return connection -> {
      CallableStatement callableStatement = connection.prepareCall(isForState ? SP_STATE_GET_HASH : SP_BLOCKS_GET_HASH);
      callableStatement.setInt(1, height);
      callableStatement.registerOutParameter(2, Types.VARCHAR);
      return callableStatement;
    };
  }


  @Override
  public int getHeight() {
    return getMainValue("maxHeight", chainId, MainValue.HEIGHT);
  }


  @Override
  public String getLastBlockHash() {
    int maxHeight = getHeight();
    return getBlockHash(maxHeight - 1);
  }


  @Override
  public String getLastStateHash() {
    int maxHeight = getHeight();
    return getStateHash(maxHeight);
  }


  private int getMainValue(String column, int chainId, MainValue type) {
    CallableStatementCreator cscGetHeight = type.getMainValueCsc(chainId);
    List<SqlParameter> parameters = Arrays.asList(
        new SqlParameter(Types.INTEGER),
        new SqlOutParameter(column, Types.INTEGER)
    );
    Map<String, Object> result = jdbcTemplate.call(cscGetHeight, parameters);
    result.putIfAbsent(column, -1);

    return (int) result.get(column);
  }


  @Override
  public String getStateHash(int height) {
    return getHash(height, true);
  }


  @Override
  public int getStateHistory(int chainId) {
    return getMainValue("stateHistory", chainId, MainValue.STATE);
  }


  private void initMainTable() throws DBStoreException {
    try {
      setMainInitValue(chainId);
    } catch (SQLException e) {
      throw new DBStoreException("Initialization of table 'main' failed.", e);
    }
  }


  @Override
  public void setBlockHash(int height, String hash, long utcTime) throws DBStoreException {
    try {
      setHash(height, hash, utcTime, false);
    } catch (SQLException e) {
      throw new DBStoreException("Insert of Block Hash failed.", e);
    }
  }


  @Override
  public void setBlockHistory(int chainId, int blockHistory) throws DBStoreException {
    try {
      setMainValue(chainId, blockHistory, MainValue.BLOCK);
    } catch (SQLException e) {
      throw new DBStoreException("Insert/Update of 'blockHistory' in 'main' table failed.", e);
    }
  }


  private void setHash(int height, String hash, long utcTime, boolean isForState) throws SQLException {
    CallableStatementCreator cscDelete = deleteHeightCsc(height, isForState);
    List<SqlParameter> deleteParameters = new ArrayList<>();
    deleteParameters.add(new SqlParameter(Types.INTEGER));

    jdbcTemplate.call(cscDelete, deleteParameters);

    CallableStatementCreator cscInsert = setHeightHashDateCsc(height, hash, utcTime, isForState);
    List<SqlParameter> insertParameters = Arrays.asList(
        new SqlParameter(Types.INTEGER),
        new SqlParameter(Types.VARCHAR),
        new SqlParameter(Types.TIMESTAMP)
    );

    Map<String, Object> insertResult = jdbcTemplate.call(cscInsert, insertParameters);
    checkUpdate(insertResult);
  }


  @Override
  public void setHeight(int height) throws DBStoreException {
    try {
      setMainValue(chainId, height, MainValue.HEIGHT);
    } catch (SQLException e) {
      throw new DBStoreException("Insert/Update of 'maxHeight' in 'main' table failed.", e);
    }
  }


  private CallableStatementCreator setHeightHashDateCsc(int height, String hash, long utcTime, boolean isForState) {
    return connection -> {
      CallableStatement callableStatement = connection.prepareCall(isForState ? SP_STATE_INSERT : SP_BLOCKS_INSERT);
      callableStatement.setInt(1, height);
      callableStatement.setString(2, hash);
      callableStatement.setTimestamp(3, unixTimeToTimestamp(utcTime));
      return callableStatement;
    };
  }


  private void setMainInitValue(int chaindId) throws SQLException {
    CallableStatementCreator cscSetMainInitValue = setMainInitValueCsc(chaindId);
    List<SqlParameter> parameters = new ArrayList<>();
    parameters.add(new SqlParameter(Types.INTEGER));

    Map<String, Object> result = jdbcTemplate.call(cscSetMainInitValue, parameters);
    checkUpdate(result);
  }


  private CallableStatementCreator setMainInitValueCsc(int chaindId) {
    return connection -> {
      CallableStatement callableStatement = connection.prepareCall(SP_MAIN_INIT);
      callableStatement.setInt(1, chaindId);
      return callableStatement;
    };
  }


  private void setMainValue(int chainId, int value, MainValue type) throws SQLException {
    CallableStatementCreator setValueCsc = type.setMainValueCsc(chainId, value);
    List<SqlParameter> parameters = Arrays.asList(
        new SqlParameter(Types.INTEGER),
        new SqlParameter(Types.INTEGER)
    );

    Map<String, Object> result = jdbcTemplate.call(setValueCsc, parameters);
    checkUpdate(result);
  }


  @Override
  public void setStateHash(int height, String hash, long utcTime) throws DBStoreException {
    try {
      setHash(height, hash, utcTime, true);
    } catch (SQLException e) {
      throw new DBStoreException("Insert of State Hash failed.", e);
    }
  }


  @Override
  public void setStateHistory(int chainId, int stateHistory) throws DBStoreException {
    try {
      setMainValue(chainId, stateHistory, MainValue.STATE);
    } catch (SQLException e) {
      throw new DBStoreException("Insert/Update of 'stateHistory' in 'main' table failed.", e);
    }
  }


  private Timestamp unixTimeToTimestamp(long uTime) {
    return new Timestamp(uTime * 1000L);
  }
}
