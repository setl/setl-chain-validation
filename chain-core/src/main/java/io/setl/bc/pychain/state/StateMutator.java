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

import io.setl.bc.pychain.state.tx.Txi;
import java.io.IOException;

/**
 * Apply transactions to the associated state.
 * Created by aanten on 07/07/2017.
 * @deprecated mutators are no longer used
 */
@Deprecated
public interface StateMutator {
  
  /**
   * Apply the list of transactions to this mutators associated state.
   *
   * @param transactions The transactions to apply
   * @param onlyUpdated  Only apply updated transactions.
   * @return True if the operation succeded.
   * @throws IOException :
   */
  boolean processTransactions(Txi[] transactions, boolean onlyUpdated) throws IOException;
}
