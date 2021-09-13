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

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.tx.internal.CreateDataDocumentTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 29/01/2020.
 */
public class CreateDataDocument extends TxProcessor<CreateDataDocumentTx> {

  private DataDocument document;

  private MutableMerkle<DataDocument> documentMerkle;

  private MutableMerkle<DataNamespace> namespaceMerkle;


  @Override
  protected Class<CreateDataDocumentTx> fulfills() {
    return CreateDataDocumentTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, CreateDataDocumentTx txi) {
    document = txi.getDataDocument();
    documentMerkle = snapshot.getMerkle(DataDocument.class);
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
  }


  @Override
  protected ReturnTuple tryApply(CreateDataDocumentTx txi, StateSnapshot snapshot, long updateTime) {
    document.setLastModified(updateTime);
    documentMerkle.add(document);
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(CreateDataDocumentTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Document must not exist already
    MerkleRules.checkNotExists("Data Document", documentMerkle, document.getId().getFullId());

    Rules.checkNamespacePrivilege(namespaceMerkle, txi.getDataDocument().getId(), NamespacePrivilege.CREATE_DOCUMENT, txi.getFromAddress());

    // ACL, if specified, must exist and allow the document's creation.
    String from = txi.getFromAddress();
    Acl acl = Rules.getAcl(snapshot, document);
    boolean isOwner = document.getOwner() != null && document.getOwner().equals(from);
    if (acl != null && !acl.checkCanCreate(from, isOwner, document.getDocument())) {
      throw fail("ACL {0} does not provide sufficient authority to create document", acl.getId());
    }

    // Document must validate if a validator is specified.
    Rules.validateDocument(snapshot, document.getId(), document.getValidatorId(), document.getDocument());

    return null;
  }

}
