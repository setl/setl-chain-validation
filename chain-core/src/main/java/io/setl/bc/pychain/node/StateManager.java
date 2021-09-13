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
package io.setl.bc.pychain.node;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.State;
import io.setl.common.Balance;

/**
 * Provide state management and transactional updates of state.
 *
 * @author aanten
 */
public interface StateManager extends Supplier<State> {


  default State get() {
    return getState();
  }

  Hash getBlockHash(int height) throws IOException, SQLException, DBStoreException;

  StateDetail getCurrentStateDetail();

  List<String> getSortedSignerKeys();

  State getState();

  @Nonnull
  default State getState(int height) throws DBStoreException, NoStateFoundException {
    throw new UnsupportedOperationException();
  }

  default Balance getTotalRequiredVotingPower() {
    return getTotalVotingPower().divideBy(2).add(1L);
  }

  Balance getTotalVotingPower();

  Balance getVotingPower(String pubkey);

  void init(String stateHash);

  void reset();

  void setState(State newState);

}
