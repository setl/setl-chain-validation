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
package io.setl.bc.pychain.state.entry;

import io.setl.bc.pychain.state.HashStore;

/**
 * Contract for named data in state.
 *
 * @author Simon Greatrix on 2019-12-20.
 */
public interface NamedDatum<X extends NamedDatum> {

  /**
   * Commit a snapshot datum into its parent datum. It is an error to invoke this on a datum in state.
   *
   * @throws UnsupportedOperationException if this datum is in state
   */
  default void commit() {
    throw new UnsupportedOperationException("Cannot commit state datum as it has no parent datum");
  }

  /**
   * Has this datum been changed?. An unchanged datum will not be written to storage.
   *
   * @return true if this has changed.
   */
  boolean isChanged();

  /**
   * Lock a datum in state against further modification. It is an error to invoke this on a datum in a snapshot. This is invoked even if the datum was
   * unchanged.
   *
   * @throws UnsupportedOperationException if this datum is in a snapshot
   */
  default void lock() {
    throw new UnsupportedOperationException("Cannot lock a datum in a mutable snapshot");
  }

  /**
   * Turn this named datum into a copy of the provided input. It should be noted that the copy may reside in a different hash store in which case referenced
   * information must also be copied.
   *
   * @param copy the input to copy
   *
   * @throws UnsupportedOperationException if this datum is in state
   */
  default void replaceWith(X copy) {
    throw new UnsupportedOperationException("Cannot reset a state datum");
  }

  /**
   * Reset a mutable child datum, removing all the pending changes.
   *
   * @throws UnsupportedOperationException if this datum is in state
   */
  default void reset() {
    throw new UnsupportedOperationException("Cannot reset a state datum");
  }


  /**
   * Create a mutable child datum of this datum.
   *
   * @return a child datum
   */
  X snapshot();

  /**
   * Create a new mutable state level datum which can be updated via a snapshot.
   *
   * @return new instance.
   */
  default X unlock() {
    throw new UnsupportedOperationException("Cannot unlock a datum in a mutable snapshot");
  }

  /** This datum with a layer of generics removed. */
  X unwrap();

}
