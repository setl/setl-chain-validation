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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <p>Pychain compatible DBStore.</p> <p>Data are stored in the following SQL tables:</p> <ol> <li>"main": contains a key-value mapping for a single value,
 * 'maxheight', which gives the block chain's current height.</li> <li>"bv": contains a timestamped key-value mapping for chain height to state hash. The height
 * is stored as text for backwards compatibility. The state hash is Message Packed.</li> <li>"blocks": contains a timestamped key-value mapping for chain height
 * to block hash. The height is stored as text for backwards compatibility. The state hash is Message Packed.</li> </ol>
 *
 * @author andy
 */
// S2445 - can synchronized on method parameters if you know what you are doing.
// S1192 - don't need constants for repeated SQL blocks.
@SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
@SuppressWarnings({"squid:S2445", "squid:S1192"})
@Component
@ConditionalOnProperty(name = "db-store.type", havingValue = "sqlite")
public class SQLiteDBStore implements DBStore {

  /**
   * SQL to create the storage tables.
   */
  private static final String[] SQL_CREATE_TABLES = new String[]{
      "DROP TABLE IF EXISTS blocks",
      "CREATE TABLE blocks (\n"
          + "    k TEXT,\n"
          + "    v BLOB,\n"
          + "    location TEXT DEFAULT Null,\n"
          + "    \"DateEntered\" INTEGER DEFAULT CURRENT_TIMESTAMP,\n"
          + "    PRIMARY KEY(k)\n"
          + ")",

      "DROP TABLE IF EXISTS bv",
      "CREATE TABLE bv (\n"
          + "    k TEXT,\n"
          + "    v BLOB,\n"
          + "    location TEXT DEFAULT Null,\n"
          + "    \"DateEntered\" INTEGER DEFAULT CURRENT_TIMESTAMP,\n"
          + "    PRIMARY KEY(k)\n"
          + ")",

      "DROP TABLE IF EXISTS main",
      "CREATE TABLE main (\n"
          + "    k TEXT,\n"
          + "    v BLOB DEFAULT Null,\n"
          + "    PRIMARY KEY(k)\n"
          + ")"
  };

  /**
   * Logger instance.
   */
  private static final Logger logger = LoggerFactory.getLogger(SQLiteDBStore.class);



  /**
   * Things that can be stored in the "main" table. Only "maxheight" is used.
   */
  public enum MainType {
    /**
     * The current chain height.
     */
    MAXHEIGHT("maxheight"),

    /**
     * Purpose unknown. Possibly the first block stored?.
     */
    BLOCKHISTORY("blockhistory"),

    /**
     * Purpose unknown. Possibly the first state stored?.
     */
    STATEHISTORY("statehistory"),

    /**
     * Current schema version.
     */
    SCHEMA_VERSION("schemaversion");

    private final String key;


    MainType(String key) {
      this.key = key;
    }


    public String getKey() {
      return key;
    }
  }



  /**
   * The connection. Connections are multi-threaded and we do not use any transactions, so one connection suffices for everything.
   */
  private final Connection connection;

  private final PreparedStatement sqlDeleteBlock;

  private final PreparedStatement sqlDeleteState;

  private final PreparedStatement sqlGetBlockHash;

  private final PreparedStatement sqlGetHeightAt;

  private final PreparedStatement sqlGetLast;

  private final PreparedStatement sqlGetStateHash;

  private final PreparedStatement sqlInsertBlock;

  private final PreparedStatement sqlInsertState;

  private final PreparedStatement sqlUpdateMain;


  /**
   * Create a SQLite DB Store in the default location.
   *
   * @throws DBStoreException some kind of storage exception
   */
  public SQLiteDBStore() throws DBStoreException {
    this(Defaults.get().getNodeId(), false);
  }


  /**
   * Create a SQLite DB Store in the default location.
   *
   * @throws DBStoreException some kind of storage exception
   */
  @Autowired
  public SQLiteDBStore(Defaults defaults) throws DBStoreException {
    this(String.format("jdbc:sqlite:%sstate%d.db", defaults.getBaseFolder(), defaults.getNodeId()), false);
  }


  /**
   * Create a SQLite DB Store in the default location.
   *
   * @param doCreate if true, delete any existing store and create a new one
   *
   * @throws DBStoreException some kind of storage exception
   */
  public SQLiteDBStore(boolean doCreate) throws DBStoreException {
    this(Defaults.get().getNodeId(), doCreate);
  }


  /**
   * Create a SQLite DB Store for a node in the default location.
   *
   * @param id the node's ID
   *
   * @throws DBStoreException some kind of storage exception
   */
  public SQLiteDBStore(int id) throws DBStoreException {
    this(String.format("jdbc:sqlite:%sstate%d.db", Defaults.get().getBaseFolder(), id), false);
  }


  /**
   * Create a SQLite DB Store in the default location.
   *
   * @param id       the node's ID
   * @param doCreate if true, delete any existing store and create a new one
   *
   * @throws DBStoreException some kind of storage exception
   */
  public SQLiteDBStore(int id, boolean doCreate) throws DBStoreException {
    this(String.format("jdbc:sqlite:%sstate%d.db", Defaults.get().getBaseFolder(), id), doCreate);
  }


  private SQLiteDBStore(String url, boolean doCreate) throws DBStoreException {
    logger.info("JDBC Connection URL:{}", url);
    try {
      connection = DriverManager.getConnection(url);

      if (doCreate) {
        create();
      }

      sqlGetBlockHash = connection.prepareStatement("select v from blocks where k=?");
      sqlGetStateHash = connection.prepareStatement("select v from bv where k=?");
      sqlGetLast = connection.prepareStatement("select v from main where k=?");

      sqlDeleteBlock = connection.prepareStatement("delete from blocks where k=?");
      sqlInsertBlock = connection.prepareStatement("insert into blocks(k, v,DateEntered) values(?, ?, ?)");

      sqlDeleteState = connection.prepareStatement("delete from bv where k=?");
      sqlInsertState = connection.prepareStatement("insert into bv(k, v,DateEntered) values(?, ?, ?)");

      sqlUpdateMain = connection.prepareStatement("update main set v=? where k=?");

      sqlGetHeightAt = connection.prepareStatement("select k from bv where DateEntered = ( select max(DateEntered) from bv where DateEntered <= ? )");

      if (doCreate) {
        // Set schema version. We have to do this when creating a DB. Can't do it without the prepared statements, and can't make the prepared statements
        // without the tables.
        setLast(MainType.SCHEMA_VERSION, 1);
      }

      checkVersion();
    } catch (SQLException e) {
      throw new DBStoreException("Unable to open SQLite DB Store", e);
    }

  }


  private void checkVersion() throws SQLException, DBStoreException {
    int version = getLast(MainType.SCHEMA_VERSION);
    logger.info("SQLite DB Store schema is at version {}.", version);
    if (version < 1) {
      logger.warn("Upgrading SQLite DB Store schema to version 1.");
      Upgrade1.doUpgrade(this, connection);
      logger.warn("Upgrade of SQLite DB Store schema to version 1 was successful.");
    }
  }


  @Override
  public void close() throws DBStoreException {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new DBStoreException("Failed to close database", e);
    }
  }


  /**
   * Create and populate 'main' table.
   *
   * @throws DBStoreException some kind of storage exception
   */
  @SuppressFBWarnings("SQL_INJECTION_JDBC") // No injection as the SQL is created from hard-coded values
  public final void create() throws DBStoreException {
    try (Statement stmt = connection.createStatement()) {
      // Create the tables
      for (String sql : SQL_CREATE_TABLES) {
        stmt.addBatch(sql);
      }
      stmt.executeBatch();

      // Insert initial null values
      try (PreparedStatement insert = connection.prepareStatement("insert into main (k) values (?)")) {
        for (MainType mt : MainType.values()) {
          insert.setString(1, mt.getKey());
          insert.addBatch();
        }
        insert.executeBatch();
      }
    } catch (SQLException e) {
      throw new DBStoreException("Unable to create SQLite DB Store", e);
    }
  }


  @Override
  public String getBlockHash(int height) throws DBStoreException {
    return getHash(sqlGetBlockHash, height);
  }


  @Override
  public int getBlockHistory(int chainId) {
    throw new NotImplementedException();
  }


  private String getHash(PreparedStatement statement, int height) throws DBStoreException {
    synchronized (statement) {
      try {
        statement.setInt(1, height);
        try (ResultSet rs = statement.executeQuery()) {
          if (rs.next()) {
            byte[] bb = rs.getBytes(1);
            return unpackHash(bb);
          }
        }
      } catch (SQLException e) {
        throw new DBStoreException(e);
      }
    }
    return null;
  }


  @Override
  public int getHeight() throws DBStoreException {
    return getLast(MainType.MAXHEIGHT);
  }


  /**
   * Get Last value for MainType from 'main' table.
   *
   * @param mt MainType
   *
   * @return value
   *
   * @throws DBStoreException some kind of storage exception
   */
  public int getLast(MainType mt) throws DBStoreException {
    synchronized (sqlGetLast) {
      try {
        sqlGetLast.setString(1, mt.getKey());
        try (ResultSet r = sqlGetLast.executeQuery()) {
          if (r.next()) {
            byte[] bb = r.getBytes(1);
            try (MessageUnpacker unpacker2 = MsgPackUtil.newUnpacker(bb)) {
              return unpacker2.unpackValue().asIntegerValue().asInt();
            }
          }
          return -1;
        }
      } catch (SQLException | IOException e) {
        throw new DBStoreException(e);
      }
    }
  }


  /**
   * Get hash of last processed block.
   *
   * @return The block hash.
   *
   * @throws DBStoreException some kind of storage exception
   */
  @Override
  public String getLastBlockHash() throws DBStoreException {
    int maxHeight = getLast(MainType.MAXHEIGHT);
    return getBlockHash(maxHeight - 1);
  }


  /**
   * Get hash of last state.
   *
   * @return The state hash.
   *
   * @throws DBStoreException some kind of storage exception
   */
  @Override
  public String getLastStateHash() throws DBStoreException {
    int maxHeight = getLast(MainType.MAXHEIGHT);
    return getStateHash(maxHeight);
  }


  /**
   * Get State from hash.
   *
   * @param hash State hash
   *
   * @return state as MPWrappedArray
   *
   * @throws DBStoreException some kind of storage exception
   * @deprecated deprecated method
   */
  @Deprecated
  public MPWrappedArray getState(String hash) throws DBStoreException {
    synchronized (sqlGetStateHash) {
      try {
        sqlGetStateHash.setString(1, hash);
        try (ResultSet r = sqlGetStateHash.executeQuery()) {
          if (r.next()) {
            byte[] bb = r.getBytes(1);
            MessageUnpacker unpacker2 = MsgPackUtil.newUnpacker(bb);
            return MsgPackUtil.unpackWrapped(unpacker2, true);
          }
        }
      } catch (SQLException | IOException e) {
        throw new DBStoreException(e);
      }
    }
    return null;
  }


  /**
   * Get state as byte array.
   *
   * @param key String value
   *
   * @return byte array representing the State
   *
   * @throws DBStoreException some kind of storage exception
   * @deprecated deprecated method
   */
  @Deprecated
  public byte[] getStateAsBytes(String key) throws DBStoreException {
    synchronized (sqlGetStateHash) {
      try {
        sqlGetStateHash.setString(1, key);
        try (ResultSet r = sqlGetStateHash.executeQuery()) {
          if (r.next()) {
            return r.getBytes(1);
          }
        }
      } catch (SQLException e) {
        throw new DBStoreException(e);
      }
    }
    return null;
  }


  @Override
  public String getStateHash(int height) throws DBStoreException {
    return getHash(sqlGetStateHash, height);
  }


  @Override
  public int getStateHeightBefore(long utcTime) throws DBStoreException {
    synchronized (sqlGetHeightAt) {
      try {
        sqlGetHeightAt.setLong(1, utcTime);
        try (ResultSet rs = sqlGetHeightAt.executeQuery()) {
          if (rs.next()) {
            String key = rs.getString(1);
            return Integer.parseInt(key);
          }
        }
      } catch (SQLException e) {
        throw new DBStoreException(e);
      }
    }
    return -1;
  }


  @Override
  public int getStateHistory(int chainId) {
    throw new NotImplementedException();
  }


  @Override
  public void setBlockHash(int height, String hash, long utcTime) throws DBStoreException {
    setHash(sqlDeleteBlock, sqlInsertBlock, height, hash, utcTime);
  }


  @Override
  public void setBlockHistory(int chainId, int blockHistory) {
    throw new NotImplementedException();
  }


  private void setHash(PreparedStatement delete, PreparedStatement insert, int height, String hash, long utcTime)
      throws DBStoreException {
    byte[] packed = MsgPackUtil.pack(new Object[]{hash});

    try {
      synchronized (delete) {
        delete.setInt(1, height);
        delete.executeUpdate();
      }

      synchronized (insert) {
        insert.setInt(1, height);
        insert.setBytes(2, packed);
        insert.setLong(3, utcTime);
        insert.executeUpdate();
      }

    } catch (SQLException e) {
      throw new DBStoreException(e);
    }
  }


  /**
   * Set the block chain height.
   *
   * @param height The new height.
   *
   * @throws DBStoreException some kind of storage exception
   */
  public void setHeight(int height) throws DBStoreException {
    setLast(MainType.MAXHEIGHT, height);
  }


  final void setLast(MainType mt, int height) throws DBStoreException {
    byte[] packed;
    try (MessageBufferPacker p = MsgPackUtil.newBufferPacker()) {
      p.packInt(height);
      packed = p.toByteArray();
    } catch (IOException e) {
      throw new DBStoreException(e);
    }

    synchronized (sqlUpdateMain) {
      try {
        sqlUpdateMain.setBytes(1, packed);
        sqlUpdateMain.setString(2, mt.getKey());
        int c = sqlUpdateMain.executeUpdate();
        if (c != 1) {
          throw new DBStoreException("Expected 1 row, got " + c, null);
        }
      } catch (SQLException e) {
        throw new DBStoreException(e);
      }
    }
  }


  @Override
  public void setStateHash(int height, String hash, long utcTime) throws DBStoreException {
    setHash(sqlDeleteState, sqlInsertState, height, hash, utcTime);
  }


  @Override
  public void setStateHistory(int chainId, int stateHistory) {
    throw new NotImplementedException();
  }


  private String unpackHash(byte[] bb) throws DBStoreException {
    try (MessageUnpacker unpacker2 = MsgPackUtil.newUnpacker(bb)) {
      ImmutableValue v = unpacker2.unpackValue();
      return v.asArrayValue().get(0).asStringValue().asString();
    } catch (IOException e) {
      throw new DBStoreException(e);
    }
  }

}
