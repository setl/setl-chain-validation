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

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.tx.internal.UpdateDataDocumentTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * Transaction processor to update a document in state.
 *
 * @author Valerio Trigari
 * @author Simon Greatrix
 */
public class UpdateDataDocument extends TxProcessor<UpdateDataDocumentTx> {

  /** The ID of the document to be updated. */
  private SpaceId documentId;

  /** Merkle for data documents. */
  private MutableMerkle<DataDocument> documentMerkle;

  /** The patch the TX hopes to apply. */
  private JsonPatch patch;

  /** The result of applying the patch. */
  private JsonStructure result;


  @Override
  protected Class<UpdateDataDocumentTx> fulfills() {
    return UpdateDataDocumentTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, UpdateDataDocumentTx txi) {
    documentId = txi.getDocumentId();
    patch = txi.getPatch();
    documentMerkle = snapshot.getMerkle(DataDocument.class);
  }


  @Override
  protected ReturnTuple tryApply(UpdateDataDocumentTx txi, StateSnapshot snapshot, long updateTime) {
    DataDocument dataDocument = documentMerkle.findAndMarkUpdated(documentId.getFullId());
    dataDocument.setDocument(result);
    dataDocument.setLastModified(updateTime);

    return null;
  }


  @Override
  protected ReturnTuple tryChecks(UpdateDataDocumentTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // Document must exist already
    DataDocument dataDocument = MerkleRules.find("Data Document", documentMerkle, documentId.getFullId());

    // ACL, if specified, must exist
    String from = txi.getFromAddress();
    Acl acl = Rules.getAcl(snapshot, dataDocument);
    if (acl != null) {
      boolean isOwner = dataDocument.getOwner() != null && dataDocument.getOwner().equals(from);
      patch = acl.verifyPatch(from, isOwner, patch);
      if (patch == null) {
        throw fail("Patch does not conform to user's access privileges");
      }
    }

    // Apply patch to original document
    try {
      result = patch.apply(dataDocument.getDocument());
    } catch (JsonException e) {
      throw new TxFailedException("Invalid or illegal patch: " + logSafe(e.getMessage()), e);
    }

    // Document must validate if a validator is specified.
    Rules.validateDocument(snapshot, dataDocument.getId(), dataDocument.getValidatorId(), result);

    return null;
  }

}
