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

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.data.acl.CRUD;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.DeleteDataDocumentTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * Transaction to delete a data document.
 *
 * @author Valerio Trigari 13/02/2020
 */
public class DeleteDataDocument extends TxProcessor<DeleteDataDocumentTx> {

  private SpaceId documentId;

  private MutableMerkle<DataDocument> documentMerkle;


  @Override
  protected Class<DeleteDataDocumentTx> fulfills() {
    return DeleteDataDocumentTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, DeleteDataDocumentTx txi) {
    documentId = txi.getDocumentId();
    documentMerkle = snapshot.getMerkle(DataDocument.class);
  }


  @Override
  protected ReturnTuple tryApply(DeleteDataDocumentTx txi, StateSnapshot snapshot, long updateTime) {
    documentMerkle.delete(documentId.getFullId());

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(DeleteDataDocumentTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Document must exist already
    MerkleRules.checkExists("Data Document", documentMerkle, documentId.getFullId());

    DataDocument dataDocument = documentMerkle.find(documentId.getFullId());

    // From address must have privilege to write to the namespace.
    String from = txi.getFromAddress();

    Acl acl = Rules.getAcl(snapshot, dataDocument);
    boolean isOwner = dataDocument.getOwner() != null && dataDocument.getOwner().equals(from);
    if (acl != null && !acl.hasPermission(from, isOwner, "", CRUD.DELETE)) {
      throw fail("Address {0} does not have permission to delete the document.", logSafe(from));
    }

    return null;
  }

}
