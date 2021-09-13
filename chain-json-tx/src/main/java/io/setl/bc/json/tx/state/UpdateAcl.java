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

import java.util.Set;
import javax.json.JsonException;
import javax.json.JsonPatch;
import javax.json.JsonStructure;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.JsonConvert;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.data.acl.RoleUsage;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.UpdateAclTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * Update an ACL.
 *
 * @author Valerio Trigari
 */
public class UpdateAcl extends TxProcessor<UpdateAclTx> {

  /** The ID of the ACL to be updated. */
  private SpaceId aclId;

  /** Merkle for ACL. */
  private MutableMerkle<Acl> aclMerkle;

  /** Merkle for the namespace. */
  private MutableMerkle<DataNamespace> namespaceMerkle;

  /** The patch the TX hopes to apply. */
  private JsonPatch patch;

  /** The updated ACL. */
  private Acl updatedAcl;


  @Override
  protected Class<UpdateAclTx> fulfills() {
    return UpdateAclTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, UpdateAclTx txi) {
    aclId = txi.getAclId();
    patch = txi.getPatch();

    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
    aclMerkle = snapshot.getMerkle(Acl.class);
  }


  @Override
  protected ReturnTuple tryApply(UpdateAclTx txi, StateSnapshot snapshot, long updateTime) {
    Acl currentAcl = aclMerkle.findAndMarkUpdated(aclId.getFullId());

    currentAcl.setTitle(updatedAcl.getTitle());
    currentAcl.setMetadata(updatedAcl.getMetadata());
    currentAcl.setIncluded(updatedAcl.getIncluded());
    currentAcl.setRoleAddresses(updatedAcl.getRoleAddresses());
    currentAcl.setLastModified(updateTime);

    // Create any addresses that need creating
    AddressRules.createDefaultAddresses(snapshot, currentAcl.getAllAddresses());

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(UpdateAclTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // ACL must exist already
    MerkleRules.checkExists("ACL", aclMerkle, aclId.getFullId());

    Acl acl = aclMerkle.find(aclId.getFullId());
    String from = txi.getFromAddress();

    // From address must have privilege to write to the namespace
    Rules.checkNamespacePrivilege(namespaceMerkle, aclId, NamespacePrivilege.UPDATE_ACL, from);

    // Apply patch to original ACL
    try {
      JsonStructure result = patch.apply(JsonConvert.toJson(acl));
      updatedAcl = JsonConvert.toInstance(result, Acl.class);
    } catch (JsonException | JsonProcessingException e) {
      throw new TxFailedException("Invalid or illegal patch: " + logSafe(e.getMessage()), e);
    }

    // Verify the included ACLs exist
    for (SpaceId id : updatedAcl.getIncluded()) {
      MerkleRules.checkExists("ACL", aclMerkle, id.getFullId());
    }

    // Verify all the referenced roles exist
    for (RoleUsage ru : updatedAcl.getRoleAddresses()) {
      Rules.checkRole(snapshot, ru.getRoleId());
    }

    // All addresses must be OK
    Set<String> allAddresses = updatedAcl.getAllAddresses();
    allAddresses.remove(RoleUsage.THE_WORLD);
    allAddresses.remove(RoleUsage.THE_OWNER);
    AddressRules.checkAddresses(snapshot, acl.getAllAddresses());

    return null;
  }

}
