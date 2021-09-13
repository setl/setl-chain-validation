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

import static io.setl.common.Balance.BALANCE_ZERO;

import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.StateWriter;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.common.Balance;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default implementation of StateManager.
 */
@Component
public class DefaultStateManager implements StateManager {

  private static final Logger logger = LoggerFactory.getLogger(DefaultStateManager.class);

  private final ApplicationEventPublisher eventPublisher;

  private State currentState;

  private StateDetail currentStateDetail = null;

  private DBStore dbStore;

  private StateReader stateLoader;

  private StateWriter stateWriter;

  @Autowired
  public DefaultStateManager(ApplicationEventPublisher eventPublisher, DBStore dbStore, StateReader stateLoader, StateWriter stateWriter) {
    this.eventPublisher = eventPublisher;
    this.dbStore = dbStore;
    this.stateLoader = stateLoader;
    this.stateWriter = stateWriter;
  }


  @Override
  public Hash getBlockHash(int height) throws DBStoreException {
    return Hash.fromHex(dbStore.getBlockHash(height));
  }


  @Override
  public StateDetail getCurrentStateDetail() {

    if (currentStateDetail == null) {

      int signingNodes = getSortedSignerKeys().size();

      currentStateDetail = new StateDetail(
          currentState.getLoadedHash(),
          currentState.getBlockHash(),
          currentState.getChainId(),
          currentState.getHeight(),
          currentState.getTimestamp(),
          signingNodes
      );
    }

    return currentStateDetail;
  }


  @Override
  public List<String> getSortedSignerKeys() {
    SortedSet<String> sn = new TreeSet<>();

    if (currentState != null) {
      for (SignNodeEntry signNodeEntry : currentState.getSignNodes()) {
        sn.add(signNodeEntry.getHexPublicKey());
      }
    }

    return new ArrayList<>(sn);
  }


  @Override
  public State getState() {
    return currentState;
  }


  @Override
  public Balance getTotalVotingPower() {

    // Taken from the wallet node :

    final Balance[] power = {new Balance(0L)};

    currentState.getSignNodes().forEach(n -> power[0] = power[0].add(n.getBalance()));

    return power[0];
  }


  @Override
  public Balance getVotingPower(String pubkey) {
    SignNodeEntry nodeEntry = currentState.getSignNodes().find(pubkey);

    if (nodeEntry != null) {
      return nodeEntry.getBalance();
    }
    logger.error("Sign node not found");

    return BALANCE_ZERO;
  }


  @Override
  public void init(String stateHash) {

    logger.info("init:{}", stateHash);
    try {
      currentState = stateLoader.readState(stateHash);
      try {
        // TODO: This is required otherwise MerkleMutable.hashTree is not populated
        ((AbstractState) currentState).verifyAll();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (DBStoreException | NoStateFoundException e) {
      throw new RuntimeException(e);
    }

    // Announce to the application that the state has been initialised.
    eventPublisher.publishEvent(new StateInitializedEvent(currentState));
  }


  @Override
  public void reset() {
    // TODO: This is wrong
    try {
      stateWriter.writeState((AbstractState) currentState);
    } catch (DBStoreException e) {
      throw new RuntimeException(e);
    }

    try {
      int newHeight = currentState.getHeight();
      dbStore.setStateHash(newHeight, currentState.getLoadedHash().toHexString(), currentState.getTimestamp());
      dbStore.setHeight(newHeight);
    } catch (DBStoreException e) {
      throw new RuntimeException(e);
    }

    currentStateDetail = null;
  }


  @Override
  public void setState(State newState) {
    currentState = newState;
    currentStateDetail = null;
  }

}
