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
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.CreateDataNamespaceTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class CreateDataNamespace extends TxProcessor<CreateDataNamespaceTx> {

  private DataNamespace namespace;

  private MutableMerkle<DataNamespace> namespaceMerkle;

  private MutableMerkle<DocumentValidator> validatorMerkle;


  @Override
  protected Class<CreateDataNamespaceTx> fulfills() {
    return CreateDataNamespaceTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, CreateDataNamespaceTx txi) {
    namespace = txi.getNamespace();
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
    validatorMerkle = snapshot.getMerkle(DocumentValidator.class);
  }


  @Override
  protected ReturnTuple tryApply(CreateDataNamespaceTx txi, StateSnapshot snapshot, long updateTime) {
    // create addresses if necessary
    AddressRules.createDefaultAddresses(snapshot, namespace.getPrivileges().getAllAddresses());

    // Create the namespace
    namespace.setLastModified(updateTime);
    namespaceMerkle.add(namespace);

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(CreateDataNamespaceTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Namespace must have a valid ID
    if (namespace.getId().isEmpty()) {
      throw new TxFailedException("Data Namespace's ID must not be empty");
    }
    if (namespace.getId().equals(DataNamespace.INTERNAL_NAMESPACE)) {
      throw new TxFailedException("Cannot create a namespace with a reserved name");
    }

    // Verify that the namespace does not already exist
    MerkleRules.checkNotExists("Data Namespace", namespaceMerkle, namespace);

    // Verify any ACL exists
    SpaceId aclId = namespace.getDefaultAclId();
    if (!aclId.isEmpty()) {
      Rules.checkAcl(snapshot, aclId);
    }

    // Verify any validator exists
    SpaceId validId = namespace.getDefaultValidator();
    if (!validId.isEmpty() && DocumentValidator.findStandard(validId) == null) {
      MerkleRules.checkExists("Validator", validatorMerkle, validId.getFullId());
    }

    // verify all the addresses are OK
    AddressRules.checkAddresses(snapshot, namespace.getPrivileges().getAllAddresses());

    return null;
  }

}
