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

import org.springframework.stereotype.Component;

import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.serialise.MerkleLeafFactory;
import io.setl.bc.pychain.serialise.MsgPackableFactory;
import io.setl.bc.pychain.state.entry.NamespaceEntry;

/**
 * Factory for the leaves of the "namespace" Merkle tree.
 *
 * @author Simon Greatrix on 09/10/2019.
 */
@Component
public class NamespaceFactory extends MsgPackableFactory<NamespaceEntry> implements MerkleLeafFactory<NamespaceEntry> {

  public NamespaceFactory() {
    super(NamespaceEntry.class);
  }

  @Override
  public ContentFactory<NamespaceEntry> getFactory() {
    return this;
  }

}
