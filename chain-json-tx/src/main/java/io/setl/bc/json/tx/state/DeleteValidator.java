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

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.DocumentValidator;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.DeleteValidatorTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 24/01/2020.
 */
public class DeleteValidator extends TxProcessor<DeleteValidatorTx> {

  private SpaceId validatorId;

  private MutableMerkle<DocumentValidator> validatorMerkle;

  private MutableMerkle<DataNamespace> namespaceMerkle;


  @Override
  protected Class<DeleteValidatorTx> fulfills() {
    return DeleteValidatorTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, DeleteValidatorTx txi) {
    validatorId = txi.getValidatorId();
    validatorMerkle = snapshot.getMerkle(DocumentValidator.class);
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(DeleteValidatorTx txi, StateSnapshot snapshot, long updateTime) {
    validatorMerkle.delete(validatorId.getFullId());
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(DeleteValidatorTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Check the ACL has exists.
    MerkleRules.checkExists("Validator", validatorMerkle, validatorId.getFullId());

    // From address must have privilege to delete ACLs to the namespace
    Rules.checkNamespacePrivilege(namespaceMerkle, validatorId, NamespacePrivilege.DELETE_VALIDATOR, txi.getFromAddress());

    if (txi.isForceDelete()) {
      Rules.checkNamespacePrivilege(namespaceMerkle, validatorId, NamespacePrivilege.FORCE_DELETE, txi.getFromAddress());
    } else {
      // We do not have a way of testing referential integrity, so we require the delete to be forced
      throw new TxFailedException("Referential integrity cannot be tested. A forced delete is required.");
    }

    // all checks done
    return null;
  }

}
