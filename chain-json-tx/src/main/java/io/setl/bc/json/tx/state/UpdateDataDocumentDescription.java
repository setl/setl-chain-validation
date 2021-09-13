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

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.DocumentValidator;
import io.setl.bc.json.data.JsonConvert;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.NamespacePrivileges;
import io.setl.bc.json.data.acl.RoleUsage;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.UpdateDataDocumentDescriptionTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Valerio Trigari, 19/02/2020.
 */
public class UpdateDataDocumentDescription extends TxProcessor<UpdateDataDocumentDescriptionTx> {

  private SpaceId documentId;

  private MutableMerkle<DataDocument> documentMerkle;

  private MutableMerkle<DataNamespace> namespaceMerkle;

  private JsonPatch patch;

  private DataDocument updatedDataDocument;

  private MutableMerkle<DocumentValidator> validatorMerkle;


  @Override
  protected Class<UpdateDataDocumentDescriptionTx> fulfills() {
    return UpdateDataDocumentDescriptionTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, UpdateDataDocumentDescriptionTx txi) {
    documentId = txi.getDocumentId();
    patch = txi.getPatch();

    documentMerkle = snapshot.getMerkle(DataDocument.class);
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
    validatorMerkle = snapshot.getMerkle(DocumentValidator.class);
  }


  @Override
  protected ReturnTuple tryApply(UpdateDataDocumentDescriptionTx txi, StateSnapshot snapshot, long updateTime) {
    DataDocument currentDataDocument = documentMerkle.findAndMarkUpdated(documentId.getFullId());

    currentDataDocument.setTitle(updatedDataDocument.getTitle());
    currentDataDocument.setMetadata(updatedDataDocument.getMetadata());
    currentDataDocument.setAclId(updatedDataDocument.getAclId());
    currentDataDocument.setValidatorId(updatedDataDocument.getValidatorId());
    currentDataDocument.setOwner(updatedDataDocument.getOwner());
    currentDataDocument.setLastModified(updateTime);

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(UpdateDataDocumentDescriptionTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Document must exist already
    MerkleRules.checkExists("Data Document", documentMerkle, documentId.getFullId());

    DataDocument dataDocument = documentMerkle.find(documentId.getFullId());

    // From address must have privilege to write to the namespace
    String from = txi.getFromAddress();
    DataNamespace dataNamespace = MerkleRules.find("Data Namespace", namespaceMerkle, dataDocument.getId().getNamespace());
    NamespacePrivileges privileges = dataNamespace.getPrivileges();
    if (!(privileges.isPrivileged(NamespacePrivilege.UPDATE_DESCRIPTION, from)
        || (from.equals(dataDocument.getOwner()) && privileges.isPrivileged(NamespacePrivilege.UPDATE_DESCRIPTION, RoleUsage.THE_OWNER))
    )) {
      throw fail("Address {0} does not have the \"update description\" privilege in the \"{1}\" namespace.", logSafe(from), logSafe(dataNamespace.getKey()));
    }

    try {
      JsonStructure result = patch.apply(JsonConvert.toJson(dataDocument));
      updatedDataDocument = JsonConvert.toInstance(result, DataDocument.class);
    } catch (JsonException | JsonProcessingException e) {
      throw new TxFailedException("Invalid or illegal patch: " + logSafe(e.getMessage()), e);
    }

    // New ACL must exist
    SpaceId id = updatedDataDocument.getAclId();
    if (!id.isEmpty()) {
      Rules.checkAcl(snapshot, id);
    }

    // New validator must exist
    id = updatedDataDocument.getValidatorId();
    if (!id.isEmpty() && DocumentValidator.findStandard(id) == null) {
      MerkleRules.checkExists("Validator", validatorMerkle, id.getFullId());
      // If validator changed, the document must validate
      if (!id.equals(dataDocument.getValidatorId())) {
        Rules.validateDocument(snapshot, dataDocument.getId(), id, dataDocument.getDocument());
      }
    }

    return null;
  }

}
