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
import io.setl.bc.json.data.acl.AclRole;
import io.setl.bc.json.tx.internal.CreateAclRoleTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 24/01/2020.
 */
public class CreateAclRole extends TxProcessor<CreateAclRoleTx> {

  private AclRole aclRole;

  private MutableMerkle<DataNamespace> namespaceMerkle;

  private MutableMerkle<AclRole> roleMerkle;


  @Override
  protected Class<CreateAclRoleTx> fulfills() {
    return CreateAclRoleTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, CreateAclRoleTx txi) {
    aclRole = txi.getAclRole();
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
    roleMerkle = snapshot.getMerkle(AclRole.class);
  }


  @Override
  protected ReturnTuple tryApply(CreateAclRoleTx txi, StateSnapshot snapshot, long updateTime) {
    aclRole.setLastModified(updateTime);
    roleMerkle.add(aclRole);
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(CreateAclRoleTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // check the ACL Role has not already been created.
    MerkleRules.checkNotExists("ACL Role", roleMerkle, aclRole);

    // From address must have privilege to write to the namespace
    Rules.checkNamespacePrivilege(namespaceMerkle, aclRole.getId(), NamespacePrivilege.CREATE_ROLE, txi.getFromAddress());

    return null;
  }

}
