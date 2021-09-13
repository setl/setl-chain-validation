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
package io.setl.bc.json.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon Greatrix on 20/01/2020.
 */
public enum NamespacePrivilege {
  /**
   * Create an ACL in this namespace.
   */
  @Schema(description = "The address may create Access Control Lists within this namespace.")
  CREATE_ACL("createAcl"),

  /** Create a document in this namespace. */
  @Schema(description = "The address may create documents in this namespace.")
  CREATE_DOCUMENT("createDocument"),

  /**
   * Create an ACL role in this namespace.
   */
  @Schema(description = "The address may create Access Control Roles within this namespace.")
  CREATE_ROLE("createRole"),

  /**
   * Create a document validator in this namespace.
   */
  @Schema(description = "The address may create document validators within this namespace.")
  CREATE_VALIDATOR("createValidator"),

  /**
   * Delete an ACL from this namespace.
   */
  @Schema(description = "The address may delete Access Control Lists from this namespace.")
  DELETE_ACL("deleteAcl"),

  /**
   * Delete an ACL Role from this namespace.
   */
  @Schema(description = "The address may delete Access Control List Roles from this namespace.")
  DELETE_ROLE("deleteAclRole"),

  /**
   * Delete this namespace.
   */
  @Schema(description = "The address may delete this namespace.")
  DELETE_NAMESPACE("deleteNamespace"),

  /**
   * Delete a validator from this namespace.
   */
  @Schema(description = "The address may delete a document validator from this namespace.")
  DELETE_VALIDATOR("deleteValidator"),

  /**
   * Force delete of this namespace, or any item in this namespace.
   */
  @Schema(description = "The address may force deletes even if data integrity checks suggest a datum should not be deleted.")
  FORCE_DELETE("forceDelete"),

  /**
   * Read the contents of an ACL.
   */
  @Schema(description = "The address may read the details of an Access Control List.")
  READ_ACL("readAcl"),

  /**
   * Read the description of a document.
   */
  @Schema(description = "The address may read the metadata for a document.")
  READ_DESCRIPTION("readDescription"),

  /**
   * Read the specification of this namespace.
   */
  @Schema(description = "The address may read the definition of a namespace.")
  READ_NAMESPACE("readNamespace"),

  /**
   * Read the specification of an ACL role.
   */
  @Schema(description = "The address may read the definition of an ACL role.")
  READ_ROLE("readRole"),

  /**
   * Read the specification of a document validator.
   */
  @Schema(description = "The address may read the definition of a document validator.")
  READ_VALIDATOR("readValidator"),

  /**
   * Update an ACL in this namespace.
   */
  @Schema(description = "The address may update an Access Control List.")
  UPDATE_ACL("updateAcl"),

  /**
   * Update this namespace.
   */
  @Schema(description = "The address may update this namespace.")
  UPDATE_NAMESPACE("updateNamespace"),

  /**
   * Update ACL roles in this namespace.
   */
  @Schema(description = "The address may update the specification of an ACL role.")
  UPDATE_ROLE("updateRole"),

  /**
   * Update the document description.
   */
  @Schema(description = "The address may update the description of a document.")
  UPDATE_DESCRIPTION("updateDescription");

  private static final Map<String, NamespacePrivilege> FOR_LABELS;


  /**
   * Get the privilege instance associated with a privilege's label.
   *
   * @param label the label
   *
   * @return the privilege
   *
   * @throws IllegalArgumentException if no privilege matches the label
   */
  @JsonCreator
  public static NamespacePrivilege forLabel(String label) {
    NamespacePrivilege np = FOR_LABELS.get(label);
    if (np != null) {
      return np;
    }
    throw new IllegalArgumentException("Unknown Namespace Privilege: " + label);
  }


  static {
    HashMap<String, NamespacePrivilege> map = new HashMap<>();
    for (NamespacePrivilege np : values()) {
      if (map.put(np.label, np) != null) {
        throw new InternalError("Two privileges map to label: " + np.label);
      }
    }
    FOR_LABELS = Collections.unmodifiableMap(map);
  }

  private final String label;


  NamespacePrivilege(String label) {
    this.label = label;
  }


  @JsonValue
  public String getLabel() {
    return label;
  }
}
