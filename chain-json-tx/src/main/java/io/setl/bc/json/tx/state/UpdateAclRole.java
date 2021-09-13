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

import static io.setl.common.StringUtils.logSafe;

import javax.json.JsonException;
import javax.json.JsonPatch;
import javax.json.JsonStructure;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.JsonConvert;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.acl.AclRole;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.UpdateAclRoleTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * Update an ACL role.
 *
 * @author Valerio Trigari
 */
public class UpdateAclRole extends TxProcessor<UpdateAclRoleTx> {

  /** Merkle for ACL roles. */
  private MutableMerkle<AclRole> aclRoleMerkle;

  /** Merkle for the namespace. */
  private MutableMerkle<DataNamespace> namespaceMerkle;

  /** The patch the TX hopes to apply. */
  private JsonPatch patch;

  /** The ID of the ACL role to be updated. */
  private SpaceId roleId;

  /** The result of applying the patch. */
  private AclRole updatedAclRole;


  @Override
  protected Class<UpdateAclRoleTx> fulfills() {
    return UpdateAclRoleTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, UpdateAclRoleTx txi) {
    roleId = txi.getRoleId();
    patch = txi.getPatch();

    aclRoleMerkle = snapshot.getMerkle(AclRole.class);
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(UpdateAclRoleTx txi, StateSnapshot snapshot, long updateTime) {
    AclRole currentAclRole = aclRoleMerkle.findAndMarkUpdated(roleId.getFullId());

    currentAclRole.setTitle(updatedAclRole.getTitle());
    currentAclRole.setMetadata(updatedAclRole.getMetadata());
    currentAclRole.setPermissions(updatedAclRole.getPermissions());
    currentAclRole.setLastModified(updateTime);

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(UpdateAclRoleTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // ACL Role must exist already
    AclRole aclRole = MerkleRules.find("ACL Role", aclRoleMerkle, roleId.getFullId());

    String from = txi.getFromAddress();

    // From address must have privilege to update the ACL role in the namespace
    Rules.checkNamespacePrivilege(namespaceMerkle, roleId, NamespacePrivilege.UPDATE_ROLE, from);

    // Apply patch to original ACL role
    try {
      JsonStructure result = patch.apply(JsonConvert.toJson(aclRole));
      updatedAclRole = JsonConvert.toInstance(result, AclRole.class);
    } catch (JsonException | JsonProcessingException e) {
      throw new TxFailedException("Invalid or illegal patch: " + logSafe(e.getMessage()), e);
    }

    return null;
  }

}
