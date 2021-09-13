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
package io.setl.bc.pychain.key2index.jdbc;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.KeyToIndex;
import io.setl.bc.pychain.state.ipfs.IpfsWalker;
import io.setl.bc.pychain.state.ipfs.MerkleStoreReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * H2 Bdb (MVStore) based implementation. Uses two separate maps, - map - used only to store the root hash of the current mapI - mapI - the map of key to index
 *
 * <p></p>Upon construction, the current hash is checked, if not that requested, the entire index is rebuilt.
 *
 * @author aanten
 */
public class JDBCKeyToIndex<K>
    implements KeyToIndex<K> {

  private static final Logger logger = LoggerFactory.getLogger(JDBCKeyToIndex.class);

  private final String url;
  private Connection conn;
  private PreparedStatement psInsert;
  private PreparedStatement psDelete;
  private PreparedStatement psFind;
  private int count = 0;


  /**
   * Construct and rebuild as necessary, the key to index mapping for the given name and hash.
   *
   * @param name : The unique name of this map. Usually classname of List, eg "AssetBalanceIpfs"
   * @param hash : The required root hash
   * @param ms : The source store used to populate if necessary.
   */
  public JDBCKeyToIndex(String url,
      String name,
      Hash hash,
      MerkleStoreReader<Object> ms)
      throws SQLException {

    this.url = url;

    //  establish connection, set default commit behaviour
    //
    conn = DriverManager.getConnection(this.url);
    conn.setAutoCommit(false);

    //  pre-load object variables with sql boilerplate code
    //
    psInsert = conn.prepareStatement(
        "insert into ktoi( k,v) (select ?, ? where not exists ( select 1 from ktoi where k=?))");
    psDelete = conn.prepareStatement(
        "delete from ktoi where k=?");
    psFind = conn.prepareStatement("select v from ktoi where k=?");

    //  construct object
    //
    create(name, hash, ms);
    logger.info("Constructed JDBCKeyToIndex for {}", name);
  }


  /**
   * Constructor.
   *
   * @param name : The name to be assocaited with the store
   * @param hash : The hash to be associated with the store
   * @param ms : The Merkle store
   * @throws SQLException : Raises
   */
  private void create(String name,
      Hash hash,
      MerkleStoreReader<Object> ms)
      throws SQLException {

    //  start by clearing down the database table
    //
    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate("truncate table ktoi");
    }

    if (!hash.isNull()) {

      //  add a key+index pair...
      //
      IpfsWalker.walk(hash, ms, new Consumer<Object>() {
        long index = 0;

        @Override
        public void accept(Object obj) {

          //  tally the pair
          //
          long i = index++;

          //  form the key
          //
          K key = (K) ((Object[]) obj)[1];

          //  add to the table, via batching
          //
          put(key, i);
        }
      });

      //  run the batch of statements which has formed, ie
      //  insert everything into the database
      //
      flush();

    } else {
      try (Statement stmt = conn.createStatement()) {
        stmt.executeUpdate("truncate table ktoi");
      }
      conn.commit();
    }

    // Record the hash of active data
    // map.put(ACTIVEHASH, hash)
    // Flush changes
    // store.commit()
  }


  /**
   * Add an "insert" statement for a key+value associated pair to the current batch.
   *
   * @param key : The key
   * @param value : The value, as long
   */
  public void put(K key,
      long value) {
    try {
      //  replace the sql boilerplate placeholders with real data
      //
      psInsert.setString(1, (String) key);
      psInsert.setLong(2, value);
      psInsert.setString(3, (String) key);

      //  add the completed statement to the batch
      //
      psInsert.addBatch();

      //  tally it
      //
      count++;

    } catch (SQLException e) {
      SQLException ee = e.getNextException();
      throw new RuntimeException(ee);
    }

  }


  /**
   * Run the current batch of statements against the database.
   */
  @Override
  public void flush() {
    if (count > 0) {
      try {
        //  run and commit
        //
        psInsert.executeBatch();
        conn.commit();

        //  reset batch tally
        //
        count = 0;

      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }


  /**
   * Return the index (as long) which is assocaited with key.
   *
   * @param key : The key for which the associated index is sought
   * @return : The index (long) associated withe the key
   */
  @Override
  public long find(K key) {
    long retVal = -1L;

    try {
      //  replace sql boilerplate placeholders with actual data
      psFind.setString(1, (String) key);

      //  run the fully formed sql statement
      try (ResultSet rs = psFind.executeQuery()) {

        //  The first result in the first column is the one we want.
        if (rs.next()) {
          retVal = rs.getLong(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return retVal;
  }

  @Override
  public void remove(K key) {
    try {
      //  replace the sql boilerplate placeholders with real data
      //
      psDelete.setString(1, (String) key);

      //  add the completed statement to the batch
      //
      psDelete.addBatch();

      //  tally it
      //
      count--;

    } catch (SQLException e) {
      SQLException ee = e.getNextException();
      throw new RuntimeException(ee);
    }

  }


  /**
   * Report the number of entries as a long (currently 0).
   *
   * @return : The number of entries, as a long
   */
  @Override
  public long getEntryCount() {

    return 0;
  }

}
