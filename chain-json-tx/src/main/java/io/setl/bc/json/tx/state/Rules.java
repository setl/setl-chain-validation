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

import static io.setl.bc.pychain.tx.updatestate.TxProcessor.fail;
import static io.setl.common.StringUtils.logSafe;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.json.JsonStructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.DocumentValidator;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.data.NamespacePrivileges;
import io.setl.bc.json.data.ValidationFailure;
import io.setl.bc.json.data.acl.Acl;
import io.setl.bc.json.data.acl.AclRole;
import io.setl.bc.json.data.acl.StandardAcls;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.state.KeyedList;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 19/02/2020.
 */
public class Rules {

  private static final Logger logger = LoggerFactory.getLogger(Rules.class);


  /**
   * Check if an ACL exists, allowing for standard ACLs.
   *
   * @param snapshot the snapshot
   * @param id       the ACL id
   */
  public static void checkAcl(StateSnapshot snapshot, SpaceId id) throws TxFailedException {
    MutableMerkle<Acl> merkle = snapshot.getMerkle(Acl.class);
    if (merkle.itemExists(id.getFullId())) {
      return;
    }
    if (StandardAcls.getAcl(id) != null) {
      return;
    }
    throw new TxFailedException(String.format("ACL \"%s\" does not exist.", logSafe(id.toString())));
  }


  /**
   * Check an address has a namespace privilege.
   *
   * @param namespaceMerkle the merkle storing namespaces
   * @param id              the ID of the namespace
   * @param privilege       the privilege required
   * @param address         the address seeking the privilege
   */
  public static void checkNamespacePrivilege(MutableMerkle<DataNamespace> namespaceMerkle, String id, NamespacePrivilege privilege, String address)
      throws TxFailedException {
    DataNamespace dataNamespace = MerkleRules.find("Data Namespace", namespaceMerkle, id);
    NamespacePrivileges privileges = dataNamespace.getPrivileges();
    if (!privileges.isPrivileged(privilege, address)) {
      throw fail(
          "Address {0} does not have the \"{1}\" privilege in the \"{2}\" namespace.",
          logSafe(address),
          privilege.getLabel(),
          logSafe(id)
      );
    }
  }


  /**
   * Check an address has a namespace privilege.
   *
   * @param namespaceMerkle the merkle storing namespaces
   * @param id              an ID including a namespace
   * @param privilege       the privilege required
   * @param address         the address seeking the privilege
   */
  public static void checkNamespacePrivilege(MutableMerkle<DataNamespace> namespaceMerkle, SpaceId id, NamespacePrivilege privilege, String address)
      throws TxFailedException {
    checkNamespacePrivilege(namespaceMerkle, id.getNamespace(), privilege, address);
  }


  /**
   * Check if an ACL Role exists, allowing for standard ACLs.
   *
   * @param snapshot the snapshot
   * @param id       the ACL id
   */
  public static void checkRole(StateSnapshot snapshot, SpaceId id) throws TxFailedException {
    MutableMerkle<AclRole> merkle = snapshot.getMerkle(AclRole.class);
    if (merkle.itemExists(id.getFullId())) {
      return;
    }
    if (StandardAcls.getRole(id) != null) {
      return;
    }
    throw new TxFailedException(String.format("ACL Role \"%s\" does not exist.", logSafe(id.toString())));
  }


  /**
   * Get the ACL associated with a document.
   *
   * @param state    the state
   * @param document the document
   *
   * @return the ACL, or null for none
   *
   * @throws TxFailedException if the ACL does not exist
   */
  @Nullable
  public static Acl getAcl(StateSnapshot state, DataDocument document) throws TxFailedException {
    SpaceId aclId = document.getAclId();
    if (aclId == null || aclId.isEmpty()) {
      // No ACL on the document so check the namespace
      KeyedList<String, DataNamespace> namespaces = state.getMerkle(DataNamespace.class);
      DataNamespace dataNamespace = namespaces.find(document.getId().getNamespace());
      if (dataNamespace == null) {
        // namespace has gone missing
        throw fail("Namespace {0} does not exist", logSafe(document.getId().getNamespace()));
      }
      aclId = dataNamespace.getDefaultAclId();
      if (aclId == null || aclId.isEmpty()) {
        // neither document nor namespace specify an ACL
        return null;
      }
    }

    Acl acl = Acl.load(state, aclId);
    if (acl == null) {
      // The ACL was specified but didn't exist.
      throw fail("ACL {0} does not exist.", logSafe(aclId.toString()));
    }
    acl.resolveRoles(state);
    return acl;
  }


  /**
   * Validate a document update.
   *
   * @param state       the state
   * @param documentId  the document's ID
   * @param validatorId the validator associated with the document
   * @param newValue    the new value of the document
   *
   * @throws TxFailedException if the document does not validate
   */
  public static void validateDocument(StateSnapshot state, SpaceId documentId, SpaceId validatorId, JsonStructure newValue) throws TxFailedException {
    if (validatorId == null || validatorId.isEmpty()) {
      // try for a default validator in the namespace
      String nameId = documentId.getNamespace();
      KeyedList<String, DataNamespace> namespaces = state.getMerkle(DataNamespace.class);
      DataNamespace dataNamespace = namespaces.find(nameId);
      if (dataNamespace == null) {
        // namespace has gone missing
        throw fail("Namespace {0} does not exist", logSafe(nameId));
      }
      validatorId = dataNamespace.getDefaultValidator();
      if (validatorId == null || validatorId.isEmpty()) {
        // no validator
        return;
      }
    }

    DocumentValidator documentValidator = DocumentValidator.findStandard(validatorId);
    if (documentValidator == null) {
      MutableMerkle<DocumentValidator> validatorMerkle = state.getMerkle(DocumentValidator.class);
      documentValidator = MerkleRules.find("Document Validator", validatorMerkle, validatorId.getFullId());
    }
    List<ValidationFailure> results = documentValidator.validate(newValue);
    if (results.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "New document {} successfully validated with {}",
            logSafe(documentId.toString()),
            logSafe(validatorId.toString())
        );
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "New document {} did not validated with {}:\n{}",
            logSafe(documentId.toString()),
            logSafe(validatorId.toString()),
            logSafe(results.stream().map(ValidationFailure::toString).collect(Collectors.joining("\n")))
        );
      }
      throw fail("Document does not validate");
    }

  }

}
