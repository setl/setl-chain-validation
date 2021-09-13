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
package io.setl.bc.pychain.serialise;

import io.setl.bc.pychain.state.entry.MEntry;

/**
 * A factory for initialising the leaves of a Merkle Tree.
 *
 * @author Simon Greatrix on 09/10/2019.
 */
public interface MerkleLeafFactory<X extends MEntry> {

  /**
   * The ContentFactory instance which encodes, decodes, and verifies the leaves.
   *
   * @return the factory
   */
  ContentFactory<X> getFactory();
}
