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
package io.setl.bc.pychain.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.AbstractEncodedState;
import io.setl.bc.pychain.state.NoOpChangeListener;
import io.setl.bc.pychain.state.StateChangeListener;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.util.MsgPackUtil;

/**
 * Load monolithic state from a file.
 */
@Component
@ConditionalOnProperty(value = "statemode", havingValue = "mono", matchIfMissing = true)
public class FileStateLoader implements StateReader {

  private static final Logger logger = LoggerFactory.getLogger(FileStateLoader.class);


  public static void main(String[] args) throws Exception {
    new FileStateLoader().stateReader();
  }


  private final String dirName;

  private DBStore dbStore = null;

  private StateChangeListener stateChangeListener = NoOpChangeListener.INSTANCE;


  public FileStateLoader(String dirName) {
    this.dirName = dirName;
  }


  public FileStateLoader() {
    this(Defaults.get().getBalanceFolder());
  }


  /**
   * Load an object encoded state from the given file.
   *
   * @param fname The full pathname of the file.
   *
   * @return The object encoded state.
   *
   * @throws IOException io error.
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public ObjectEncodedState loadStateFromFile(String fname) throws IOException {
    MPWrappedArray v = MsgPackUtil.unpackWrapped(Paths.get(fname), true);

    return ObjectEncodedState.decode(v.asWrapped(1));
  }


  /**
   * Read the current state.
   *
   * @return The state.
   */
  public ObjectEncodedState readLastState(DBStore dbStore) throws DBStoreException {
    String h = dbStore.getLastStateHash();
    logger.info("Reading state:{}", h);
    return readState(h);
  }


  public ObjectEncodedState readState(DBStore dbStore, int height) throws DBStoreException {
    String h = dbStore.getStateHash(height);
    return readState(h);
  }


  @Override
  public ObjectEncodedState readState(String h) throws DBStoreException {
    logger.info("Reading state:{}", h);
    ObjectEncodedState state;

    try {
      state = loadStateFromFile(dirName + h);
    } catch (IOException e) {
      throw new DBStoreException("Failed to read stored state: " + dirName + h, e);
    }
    state.setStateChangeListener(stateChangeListener);
    return state;
  }


  @Override
  @Autowired(required = false)
  public void setChangeListener(StateChangeListener listener) {
    stateChangeListener = (listener != null) ? listener : NoOpChangeListener.INSTANCE;
  }


  @SuppressFBWarnings({"PATH_TRAVERSAL_IN", "UNSAFE_HASH_EQUALS"})
  void stateReader() throws Exception {
    File dir = new File(dirName);
    String filterPattern = ".*";
    File[] listFiles = dir.listFiles(new PatternFilenameFilter(filterPattern));
    if (listFiles == null || listFiles.length == 0) {
      throw new RuntimeException("files not found!");
    }
    logger.info("Processing {} files", listFiles.length);
    DefaultHashableHashComputer computer = new DefaultHashableHashComputer();
    for (int blockIt = 0; blockIt < listFiles.length; blockIt++) {
      File fname = listFiles[blockIt];
      AbstractEncodedState state = loadStateFromFile(fname.getAbsolutePath());
      String hash = computer.computeHashAsHex(state);
      if (!fname.getName().equals(hash)) {
        throw new RuntimeException("Hash compare failed");
      }
      logger.trace("Hash compare ok:{}", hash);
    }
    logger.info("Processed {} files", listFiles.length);
  }

}
