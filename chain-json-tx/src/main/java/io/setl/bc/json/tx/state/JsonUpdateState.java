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
package io.setl.bc.json.tx.state;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.UpdateState.StateUpdater;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class JsonUpdateState implements StateUpdater {

  private static final EnumSet<TxType> HANDLES = EnumSet.of(
      TxType.JSON_CREATE_DATA_NAMESPACE,
      TxType.JSON_DELETE_DATA_NAMESPACE,
      TxType.JSON_UPDATE_DATA_NAMESPACE,
      TxType.JSON_CREATE_DATA_DOCUMENT,
      TxType.JSON_UPDATE_DATA_DOCUMENT,
      TxType.JSON_DELETE_DATA_DOCUMENT,
      TxType.JSON_UPDATE_DESCRIPTION,
      TxType.JSON_CREATE_VALIDATOR,
      TxType.JSON_DELETE_VALIDATOR,
      TxType.JSON_CREATE_ALGORITHM,
      TxType.JSON_CREATE_ACL,
      TxType.JSON_DELETE_ACL,
      TxType.JSON_UPDATE_ACL,
      TxType.JSON_CREATE_ACL_ROLE,
      TxType.JSON_DELETE_ACL_ROLE,
      TxType.JSON_UPDATE_ACL_ROLE
  );


  @Override
  public Set<TxType> handles() {
    return Collections.unmodifiableSet(HANDLES);
  }


  @Override
  public ReturnTuple update(
      Txi tx, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly
  ) {
    // Some rules apply to all JSON transactions, so we process them here.
    // If it is not for this chain, or not for the current priority, we just skip it.
    if (((AbstractTx) tx).getChainId() != stateSnapshot.getChainId() || priority != tx.getPriority()) {
      return new ReturnTuple(SuccessType.PASS, "");
    }

    // If we are using permission-by-address, we always check that now
    if (stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
      String attorneyAddress = ((AbstractTx) tx).getPoaAddress();

      // Check the address is limited by transaction type but does not have permission for this transaction
      if (!stateSnapshot.canUseTx(attorneyAddress, tx.getTxType())) {
        return new ReturnTuple(checkOnly ? SuccessType.WARNING : SuccessType.FAIL, "Insufficient address permissions");
      }
    }

    switch (tx.getTxType()) {
      case JSON_CREATE_DATA_NAMESPACE:
        return new CreateDataNamespace().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_DELETE_DATA_NAMESPACE:
        return new DeleteDataNamespace().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_UPDATE_DATA_NAMESPACE:
        return new UpdateDataNamespace().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_CREATE_ACL:
        return new CreateAcl().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_DELETE_ACL:
        return new DeleteAcl().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_UPDATE_ACL:
        return new UpdateAcl().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_CREATE_ACL_ROLE:
        return new CreateAclRole().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_DELETE_ACL_ROLE:
        return new DeleteAclRole().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_UPDATE_ACL_ROLE:
        return new UpdateAclRole().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_CREATE_DATA_DOCUMENT:
        return new CreateDataDocument().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_DELETE_DATA_DOCUMENT:
        return new DeleteDataDocument().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_UPDATE_DATA_DOCUMENT:
        return new UpdateDataDocument().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_UPDATE_DESCRIPTION:
        return new UpdateDataDocumentDescription().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_CREATE_VALIDATOR:
        return new CreateValidator().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_DELETE_VALIDATOR:
        return new DeleteValidator().update(tx, stateSnapshot, updateTime, priority, checkOnly);
      case JSON_CREATE_ALGORITHM:
        // We will not be implementing create algorithm at this time
        throw new NotImplementedException();
      default:
        throw new NotImplementedException();
    }
  }

}
