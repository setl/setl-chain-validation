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

import java.util.Set;

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.data.acl.RoleUsage;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.CreateAclTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 24/01/2020.
 */
public class CreateAcl extends TxProcessor<CreateAclTx> {

  private Acl acl;

  private MutableMerkle<Acl> aclMerkle;

  private MutableMerkle<DataNamespace> namespaceMerkle;


  @Override
  protected Class<CreateAclTx> fulfills() {
    return CreateAclTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, CreateAclTx txi) {
    acl = txi.getAcl();
    aclMerkle = snapshot.getMerkle(Acl.class);
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(CreateAclTx txi, StateSnapshot snapshot, long updateTime) {
    // create any required addresses
    AddressRules.createDefaultAddresses(snapshot, acl.getAllAddresses());

    // Create the ACL
    acl.setLastModified(updateTime);
    aclMerkle.add(acl);
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(CreateAclTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Check the ACL has not already been created.
    MerkleRules.checkNotExists("ACL", aclMerkle, acl);

    // From address must have privilege to write to the namespace
    Rules.checkNamespacePrivilege(namespaceMerkle, acl.getId(), NamespacePrivilege.CREATE_ACL, txi.getFromAddress());

    // Verify the included ACLs exist
    for (SpaceId id : acl.getIncluded()) {
      Rules.checkAcl(snapshot,id);
    }

    // Verify all the referenced roles exist
    for (RoleUsage ru : acl.getRoleAddresses()) {
      Rules.checkRole(snapshot,ru.getRoleId());
    }

    // verify all the addresses are OK
    Set<String> allAddresses = acl.getAllAddresses();
    allAddresses.remove(RoleUsage.THE_WORLD);
    allAddresses.remove(RoleUsage.THE_OWNER);
    AddressRules.checkAddresses(snapshot, allAddresses);

    // all checks done
    return null;
  }

}
