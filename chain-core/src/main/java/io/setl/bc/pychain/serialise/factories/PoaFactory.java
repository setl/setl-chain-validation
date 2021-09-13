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
package io.setl.bc.pychain.serialise.factories;

import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.MerkleLeafFactory;
import io.setl.bc.pychain.serialise.ObjectArrayFactory;
import io.setl.bc.pychain.state.entry.PoaEntry;
import org.springframework.stereotype.Component;

/**
 * Factory for the leaves of the "power of attorney" Merkle.
 *
 * @author Simon Greatrix on 09/10/2019.
 */
@Component
public class PoaFactory implements MerkleLeafFactory<PoaEntry> {

  @Override
  public ContentFactory<PoaEntry> getFactory() {
    return new ObjectArrayFactory<>(PoaEntry.class, new PoaEntry.Decoder());
  }
}
