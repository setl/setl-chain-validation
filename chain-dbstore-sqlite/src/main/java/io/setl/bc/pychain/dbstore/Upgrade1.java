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

import static io.setl.bc.pychain.dbstore.SQLiteDBStore.MainType.SCHEMA_VERSION;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Upgrade a SQLite DB Store to version 1.
 *
 * @author Simon Greatrix on 06/02/2018.
 */
@SuppressFBWarnings("SQL_INJECTION_JDBC")
public class Upgrade1 {

  private static final String[] SQL = new String[]{
      "DROP TABLE IF EXISTS bv_temp",

      "CREATE TABLE bv_temp (\n"
          + "    k TEXT,\n"
          + "    v BLOB,\n"
          + "    location TEXT DEFAULT Null,\n"
          + "    \"DateEntered\" INTEGER DEFAULT CURRENT_TIMESTAMP,\n"
          + "    PRIMARY KEY(k)\n"
          + ")",

      "CREATE INDEX I_BV_DATE_ENTERED ON bv_temp ( \"DateEntered\" )",

      "INSERT INTO bv_temp (k,v,location,DateEntered)\n"
          + "SELECT k,v,location,cast(strftime('%s','20' || DateEntered) as integer)\n"
          + "FROM bv\n"
          + "WHERE length(DateEntered)=17",

      "INSERT INTO bv_temp (k,v,location,DateEntered)\n"
          + "SELECT k,v,location,cast(strftime('%s',DateEntered) as integer)\n"
          + "FROM bv\n"
          + "WHERE length(DateEntered)=19",

      "DROP TABLE IF EXISTS blocks_temp",

      "CREATE TABLE blocks_temp (\n"
          + "    k TEXT,\n"
          + "    v BLOB,\n"
          + "    location TEXT DEFAULT Null,\n"
          + "    \"DateEntered\" INTEGER DEFAULT CURRENT_TIMESTAMP,\n"
          + "    PRIMARY KEY(k)\n"
          + ")",

      "CREATE INDEX I_BLOCKS_DATE_ENTERED ON blocks_temp ( \"DateEntered\" )",

      "INSERT INTO blocks_temp (k,v,location,DateEntered)\n"
          + "SELECT k,v,location,cast(strftime('%s','20' || DateEntered) as integer)\n"
          + "FROM blocks\n"
          + "WHERE length(DateEntered)=17",

      "INSERT INTO blocks_temp (k,v,location,DateEntered)\n"
          + "SELECT k,v,location,cast(strftime('%s',DateEntered) as integer)\n"
          + "FROM blocks\n"
          + "WHERE length(DateEntered)=19",

      "ALTER TABLE bv RENAME TO bv_old",

      "ALTER TABLE blocks RENAME TO blocks_old",

      "ALTER TABLE bv_temp RENAME TO bv",

      "ALTER TABLE blocks_temp RENAME TO blocks",

      "DROP TABLE bv_old",

      "DROP TABLE blocks_old"
  };


  private static final String SQL_COUNT_VERSIONS = "SELECT COUNT(*) FROM main WHERE k='" + SCHEMA_VERSION.getKey() + "'";

  private static final String SQL_VERSION_INSERT = "INSERT INTO main(k) VALUES ( '" + SCHEMA_VERSION.getKey() + "')";


  /**
   * Perform the upgrade of the SQLite DB Store.
   *
   * @param dbStore    the store to upgrade
   * @param connection the connection to the store
   */
  public static void doUpgrade(SQLiteDBStore dbStore, Connection connection) throws SQLException, DBStoreException {
    try (Statement stmt = connection.createStatement()) {
      // Ensure the schema version is properly recorded.
      int count;
      try (ResultSet rs = stmt.executeQuery(SQL_COUNT_VERSIONS)) {
        rs.next();
        count = rs.getInt(1);
      }
      if (count == 0) {
        // Ensure there is a schema version entry
        stmt.execute(SQL_VERSION_INSERT);
        dbStore.setLast(SCHEMA_VERSION, 0);
      }

      // Double check upgrade is needed
      int version = dbStore.getLast(SCHEMA_VERSION);
      if (version >= 1) {
        // No upgrade needed
        return;
      }

      // Run through the upgrade
      for (final String sql : SQL) {
        stmt.executeUpdate(sql);
      }

      dbStore.setLast(SCHEMA_VERSION, 1);
    }
  }
}
