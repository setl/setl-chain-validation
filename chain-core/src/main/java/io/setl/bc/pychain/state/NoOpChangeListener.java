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
package io.setl.bc.pychain.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;

/**
 * A no-op state change listener that only logs what happens.
 *
 * @author Simon Greatrix on 15/04/2021.
 */
public class NoOpChangeListener implements StateChangeListener {

  public static final StateChangeListener INSTANCE = new NoOpChangeListener();

  private static final Logger logger = LoggerFactory.getLogger(NoOpChangeListener.class);


  @Override
  public void add(MEntry entry) {
    logger.debug("Entry {} of type {} added to state.", entry.getKey(), entry.getClass());
  }


  @Override
  public void complete(State state) {
    logger.debug("State {} is complete with hash {}", state.getHeight(), state.getLoadedHash());
  }


  @Override
  public void fail(StateSnapshotCorruptedException e) {
    if (e != null) {
      logger.debug("Update to state failed.", e);
    } else {
      logger.debug("Update to state failed for reasons unknown.");
    }
  }


  @Override
  public void initialise(State state) {
    logger.debug("Initialising from state {}", state.getHeight());
  }


  @Override
  public <T extends MEntry> void remove(String key, Class<T> type) {
    logger.debug("Entry {} of type {} removed from state.", key, type);
  }


  @Override
  public void start(int version, int height) {
    logger.debug("Creation of version {} state {} commenced", version, height);
  }


  @Override
  public void update(MEntry entry) {
    logger.debug("Entry {} of type {} updated.", entry.getKey(), entry.getClass());
  }

}
