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
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.DeleteAclTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 24/01/2020.
 */
public class DeleteAcl extends TxProcessor<DeleteAclTx> {

  private SpaceId aclId;

  private MutableMerkle<Acl> aclMerkle;
  private MutableMerkle<DataNamespace> namespaceMerkle;


  @Override
  protected Class<DeleteAclTx> fulfills() {
    return DeleteAclTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, DeleteAclTx txi) {
    aclId = txi.getAclId();
    aclMerkle = snapshot.getMerkle(Acl.class);
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(DeleteAclTx txi, StateSnapshot snapshot, long updateTime) {
    aclMerkle.delete(aclId.getFullId());
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(DeleteAclTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Check the ACL has exists.
    MerkleRules.checkExists("ACL", aclMerkle, aclId.getFullId());

    // From address must have privilege to delete ACLs to the namespace
    Rules.checkNamespacePrivilege(namespaceMerkle, aclId, NamespacePrivilege.DELETE_ACL, txi.getFromAddress());

    if (txi.isForceDelete()) {
      Rules.checkNamespacePrivilege(namespaceMerkle, aclId, NamespacePrivilege.FORCE_DELETE, txi.getFromAddress());
    } else {
      // We do not have a way of testing referential integrity, so we require the delete to be forced
      throw new TxFailedException("Referential integrity cannot be tested. A forced delete is required.");
    }

    // all checks done
    return null;
  }

}
