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

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.NamespacePrivileges;
import io.setl.bc.json.tx.internal.DeleteDataNamespaceTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * Transaction to delete a namespace from space.
 *
 * @author Valerio Trigari
 */
public class DeleteDataNamespace extends TxProcessor<DeleteDataNamespaceTx> {

  private String namespaceId;
  private MutableMerkle<DataNamespace> namespaceMerkle;


  @Override
  protected Class<DeleteDataNamespaceTx> fulfills() {
    return DeleteDataNamespaceTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, DeleteDataNamespaceTx txi) {
    namespaceId = txi.getNamespaceId();
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(DeleteDataNamespaceTx txi, StateSnapshot snapshot, long updateTime) {
    // Delete namespace
    namespaceMerkle.delete(namespaceId);

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(DeleteDataNamespaceTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    MerkleRules.checkExists("Data namespace", namespaceMerkle, namespaceId);

    DataNamespace dataNamespace = namespaceMerkle.find(namespaceId);

    String from = txi.getFromAddress();
    NamespacePrivileges privileges = dataNamespace.getPrivileges();

    if (!privileges.isPrivileged(NamespacePrivilege.DELETE_NAMESPACE, from)) {
      throw fail("Address {0} does not have the \"delete namespace\" privilege in the \"{1}\" namespace.", logSafe(from), logSafe(dataNamespace.getKey()));
    }

    if (txi.isForceDelete()) {
      Rules.checkNamespacePrivilege(namespaceMerkle, namespaceId, NamespacePrivilege.FORCE_DELETE, txi.getFromAddress());
    } else {
      // We do not have a way of testing referential integrity, so we require the delete to be forced
      throw new TxFailedException("Referential integrity cannot be tested. A forced delete is required.");
    }

    return null;
  }

}
