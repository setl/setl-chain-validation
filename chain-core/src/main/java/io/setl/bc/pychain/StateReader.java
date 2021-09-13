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
package io.setl.bc.pychain;

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.StateChangeListener;

/**
 * Interface for loading state from persistent storage.
 */
public interface StateReader {

  /**
   * Read a state.
   *
   * @param hash The hash of the state to be read encoded in hexadecimal.
   *
   * @return The state.
   *
   * @throws NoStateFoundException Thrown when state could not be found.
   * @throws DBStoreException      Thrown when the location of the state cannot be identified
   */
  AbstractState readState(String hash) throws DBStoreException, NoStateFoundException;

  /**
   * Set the change listener which will be notified of any changes during block processing of states loaded by this reader.
   *
   * @param listener the listener
   */
  void setChangeListener(StateChangeListener listener);

}
