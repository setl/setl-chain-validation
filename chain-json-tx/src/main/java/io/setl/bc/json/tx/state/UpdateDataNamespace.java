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
import io.setl.bc.json.tx.internal.UpdateDataNamespaceTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * Update the specification of a data namespace.
 *
 * @author Valerio Trigari
 */
public class UpdateDataNamespace extends TxProcessor<UpdateDataNamespaceTx> {

  DataNamespace updatedNamespace;
  private String namespaceId;
  private MutableMerkle<DataNamespace> namespaceMerkle;
  private JsonPatch patch;


  @Override
  protected Class<UpdateDataNamespaceTx> fulfills() {
    return UpdateDataNamespaceTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, UpdateDataNamespaceTx txi) {
    namespaceId = txi.getNamespaceId();
    patch = txi.getPatch();
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(UpdateDataNamespaceTx txi, StateSnapshot snapshot, long updateTime) {
    DataNamespace currentNamespace = namespaceMerkle.findAndMarkUpdated(namespaceId);

    currentNamespace.setTitle(updatedNamespace.getTitle());
    currentNamespace.setMetadata(updatedNamespace.getMetadata());
    currentNamespace.setDefaultAclId(updatedNamespace.getDefaultAclId());
    currentNamespace.setDefaultValidator(updatedNamespace.getDefaultValidator());
    currentNamespace.setPrivileges(updatedNamespace.getPrivileges());
    currentNamespace.setLastModified(updateTime);

    // create addresses if necessary
    AddressRules.createDefaultAddresses(snapshot, updatedNamespace.getPrivileges().getAllAddresses());

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(UpdateDataNamespaceTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    String from = txi.getFromAddress();
    DataNamespace namespace = MerkleRules.find("Data Namespace", namespaceMerkle, namespaceId);
    Rules.checkNamespacePrivilege(namespaceMerkle, namespaceId, NamespacePrivilege.UPDATE_NAMESPACE, from);

    try {
      JsonStructure result = patch.apply(JsonConvert.toJson(namespace));
      updatedNamespace = JsonConvert.toInstance(result, DataNamespace.class);
    } catch (JsonException | JsonProcessingException e) {
      throw new TxFailedException("Invalid or illegal patch: " + logSafe(e.getMessage()), e);
    }

    // verify all the addresses are OK
    AddressRules.checkAddresses(snapshot, updatedNamespace.getPrivileges().getAllAddresses());
    return null;
  }

}
