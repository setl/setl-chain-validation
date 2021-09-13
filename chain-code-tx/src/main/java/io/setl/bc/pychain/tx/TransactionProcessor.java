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
package io.setl.bc.pychain.tx;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.util.PriorityExecutor;
import java.io.IOException;

/**
 * Created by aanten on 18/07/2017.
 */
public interface TransactionProcessor {


  boolean checkValidatedTransactionForPool(AbstractTx tx, State state);

  boolean postProcessTransactions(StateSnapshot stateSnapshot, Block block, long updateTime);

  boolean processTransactions(StateSnapshot stateSnapshot, Txi[] transactions, long updateTime, boolean validateMode)
      throws StateSnapshotCorruptedException;

  boolean processTransactions(int blockVersion, PriorityExecutor priorityExecutor, StateSnapshot stateSnapshot, ProposedTxList transactions, long updateTime)
      throws StateSnapshotCorruptedException;

  void removeProcessedTimeEvents(StateSnapshot stateSnapshot, Block block, long updateTime);
}
