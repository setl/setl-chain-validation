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

import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;

/**
 * Interface for receiving update as to how state is changing during the application of a block.
 *
 * @author Simon Greatrix on 14/04/2021.
 */
public interface StateChangeListener {

  /**
   * A new entry is inserted into a Merkle structure in state.
   *
   * @param entry the new entry.
   */
  <T extends MEntry> void add(T entry);

  /**
   * A new state has been created.
   *
   * @param newState the new state
   */
  void complete(State newState);

  /**
   * The update to state has failed for some reason and should be abandoned.
   *
   * @param e the cause of the failure, if known
   */
  void fail(StateSnapshotCorruptedException e);


  /**
   * Initialise the change listener from a specific state that changes will be made to.
   *
   * @param state the starting state
   */
  void initialise(State state);

  /**
   * An entry is being removed from a Merkle structure.
   *
   * @param key  the entry's key
   * @param type the entry's type
   */
  <T extends MEntry> void remove(String key, Class<T> type);

  /**
   * Start the finalization of a new state.
   */
  void start(int version, int height);

  /**
   * Update an entry in a Merkle structure.
   *
   * @param entry the entry's new value
   */
  <T extends MEntry> void update(T entry);

}
