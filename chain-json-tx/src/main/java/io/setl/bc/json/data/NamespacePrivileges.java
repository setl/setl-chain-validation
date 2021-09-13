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

import static io.setl.bc.json.data.NamespacePrivilege.CREATE_ACL;
import static io.setl.bc.json.data.NamespacePrivilege.CREATE_DOCUMENT;
import static io.setl.bc.json.data.NamespacePrivilege.CREATE_ROLE;
import static io.setl.bc.json.data.NamespacePrivilege.CREATE_VALIDATOR;
import static io.setl.bc.json.data.NamespacePrivilege.DELETE_ACL;
import static io.setl.bc.json.data.NamespacePrivilege.DELETE_NAMESPACE;
import static io.setl.bc.json.data.NamespacePrivilege.DELETE_ROLE;
import static io.setl.bc.json.data.NamespacePrivilege.DELETE_VALIDATOR;
import static io.setl.bc.json.data.NamespacePrivilege.FORCE_DELETE;
import static io.setl.bc.json.data.NamespacePrivilege.READ_ACL;
import static io.setl.bc.json.data.NamespacePrivilege.READ_DESCRIPTION;
import static io.setl.bc.json.data.NamespacePrivilege.READ_NAMESPACE;
import static io.setl.bc.json.data.NamespacePrivilege.READ_ROLE;
import static io.setl.bc.json.data.NamespacePrivilege.READ_VALIDATOR;
import static io.setl.bc.json.data.NamespacePrivilege.UPDATE_ACL;
import static io.setl.bc.json.data.NamespacePrivilege.UPDATE_DESCRIPTION;
import static io.setl.bc.json.data.NamespacePrivilege.UPDATE_NAMESPACE;
import static io.setl.bc.json.data.NamespacePrivilege.UPDATE_ROLE;
import static io.setl.bc.json.data.acl.RoleUsage.THE_OWNER;
import static io.setl.bc.json.data.acl.RoleUsage.THE_WORLD;
import static io.setl.bc.json.data.acl.RoleUsage.WORLD_SET;
import static io.setl.common.StringUtils.notNull;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;

import io.setl.common.ObjectArrayReader;

/**
 * Addresses with the named privilege in a namespace.
 *
 * @author Simon Greatrix on 20/01/2020.
 */
public class NamespacePrivileges {

  /** All the privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL = EnumSet.allOf(NamespacePrivilege.class);

  /** All the ACL privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_ACL =
      EnumSet.of(CREATE_ACL, CREATE_ROLE, DELETE_ACL, DELETE_ROLE, READ_ACL, READ_ROLE, UPDATE_ACL, UPDATE_ROLE);

  /** All the create privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_CREATE = EnumSet.of(CREATE_ACL, CREATE_DOCUMENT, CREATE_ROLE, CREATE_VALIDATOR);

  /** All the delete privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_DELETE = EnumSet.of(DELETE_ACL, DELETE_ROLE, DELETE_NAMESPACE, DELETE_VALIDATOR, FORCE_DELETE);

  /** All the document privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_DOCUMENT = EnumSet.of(CREATE_DOCUMENT, READ_DESCRIPTION, UPDATE_DESCRIPTION);

  /** All the namespace privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_NAMESPACE = EnumSet.of(DELETE_NAMESPACE, READ_NAMESPACE, UPDATE_NAMESPACE);

  /** All the read privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_READ = EnumSet.of(READ_ACL, READ_DESCRIPTION, READ_NAMESPACE, READ_ROLE, READ_VALIDATOR);

  /** All the update privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_UPDATE = EnumSet.of(UPDATE_ACL, UPDATE_DESCRIPTION, UPDATE_ROLE, UPDATE_NAMESPACE);

  /** All the validator privileges. */
  private static final Set<NamespacePrivilege> PRIVILEGES_ALL_VALIDATOR = EnumSet.of(CREATE_VALIDATOR, DELETE_VALIDATOR, READ_VALIDATOR);


  private final EnumMap<NamespacePrivilege, SortedSet<String>> privileges = new EnumMap<>(NamespacePrivilege.class);


  public NamespacePrivileges() {
    // do nothing
  }


  /**
   * Copy constructor.
   *
   * @param copy the privileges to copy.
   */
  public NamespacePrivileges(NamespacePrivileges copy) {
    privileges.putAll(copy.privileges);
  }


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public NamespacePrivileges(ObjectArrayReader reader) {
    privileges.putAll(reader.getMap(
        () -> new EnumMap<>(NamespacePrivilege.class),
        o -> NamespacePrivilege.forLabel(String.valueOf(o)),
        ObjectArrayReader::convertToSortedStrings
    ));
  }


  /**
   * Encode this for persistent storage.
   *
   * @return the encoded representation
   */
  public Object[] encode() {
    synchronized (privileges) {
      return new Object[]{ObjectArrayReader.encode(privileges, NamespacePrivilege::getLabel, Collection::toArray)};
    }
  }


  /**
   * Get the addresses with the specified privilege.
   *
   * @param privilege the privilege
   *
   * @return the addresses
   */
  public SortedSet<String> get(NamespacePrivilege privilege) {
    synchronized (privileges) {
      return privileges.computeIfAbsent(privilege, p -> Collections.emptySortedSet());
    }
  }


  /**
   * Get all the addresses mentioned by any privilege in this set of privileges.
   *
   * @return all the addresses
   */
  @JsonIgnore
  @Hidden
  public Set<String> getAllAddresses() {
    synchronized (privileges) {
      HashSet<String> addresses = new HashSet<>();
      for (SortedSet<String> set : privileges.values()) {
        addresses.addAll(set);
      }
      addresses.remove(THE_WORLD);
      addresses.remove(THE_OWNER);
      return addresses;
    }
  }


  @Schema(description = "The addresses that can create Access Control Lists in the namespace.")
  public Set<String> getCreateAcl() {
    return get(CREATE_ACL);
  }


  @Schema(description = "The addresses that can create documents in the namespace.")
  public Set<String> getCreateDocument() {
    return get(CREATE_DOCUMENT);
  }


  @Schema(description = "The addresses that can create Access Control List Roles in the namespace.")
  public Set<String> getCreateRole() {
    return get(CREATE_ROLE);
  }


  @Schema(description = "The addresses that can create document validators in the namespace.")
  public Set<String> getCreateValidator() {
    return get(CREATE_VALIDATOR);
  }


  @Schema(description = "The addresses that can delete ACLs from the namespace.")
  public Set<String> getDeleteAcl() {
    return get(DELETE_ACL);
  }


  @Schema(description = "The addresses that can delete ACL Roles from the namespace.")
  public Set<String> getDeleteAclRole() {
    return get(DELETE_ROLE);
  }


  @Schema(description = "The addresses that can delete the namespace.")
  public Set<String> getDeleteNamespace() {
    return get(DELETE_NAMESPACE);
  }


  @Schema(description = "The addresses that can delete validators from the namespace.")
  public Set<String> getDeleteValidator() {
    return get(DELETE_VALIDATOR);
  }


  @Schema(description = "The addresses that can force deletes even if referential integrity checks suggest a datum is still in use.")
  public Set<String> getForceDelete() {
    return get(FORCE_DELETE);
  }


  @Hidden
  @JsonIgnore
  public Map<NamespacePrivilege, SortedSet<String>> getPrivileges() {
    return privileges;
  }


  @Schema(description = "The addresses that can read a definition of an Access Control List from the namespace.")
  public Set<String> getReadAcl() {
    return get(READ_ACL);
  }


  @Schema(description = "The addresses that can read a document's description from the namespace.")
  public Set<String> getReadDescription() {
    return get(READ_DESCRIPTION);
  }


  @Schema(description = "The addresses that can read the definition of the namespace.")
  public Set<String> getReadNamespace() {
    return get(READ_NAMESPACE);
  }


  @Schema(description = "The addresses that can read a definition of an Access Control List Role from the namespace.")
  public Set<String> getReadRole() {
    return get(READ_ROLE);
  }


  @Schema(description = "The addresses that can read a definition of a document validator from the namespace.")
  public Set<String> getReadValidator() {
    return get(READ_VALIDATOR);
  }


  @Schema(description = "The addresses that can update an Access Control List in the namespace.")
  public Set<String> getUpdateAcl() {
    return get(UPDATE_ACL);
  }


  @Schema(description = "The addresses that can update the description of a document in the namespace.")
  public Set<String> getUpdateDescription() {
    return get(UPDATE_DESCRIPTION);
  }


  @Schema(description = "The addresses that can update the namespace's definition.")
  public Set<String> getUpdateNamespace() {
    return get(UPDATE_NAMESPACE);
  }


  @Schema(description = "The addresses that can update an Access Control List Role in the namespace.")
  public Set<String> getUpdateRole() {
    return get(UPDATE_ROLE);
  }


  /**
   * Check if an address has a specified privilege.
   *
   * @param privilege the privilege
   * @param address   the address
   *
   * @return true if the address has the privilege.
   */
  public boolean isPrivileged(NamespacePrivilege privilege, String address) {
    SortedSet<String> namespacePrivilege = get(privilege);
    return namespacePrivilege.contains(address) || namespacePrivilege.contains(THE_WORLD);
  }


  /**
   * Set the addresses that are associated with a privilege.
   *
   * @param privilege the privilege
   * @param addresses the addresses
   */
  public void set(NamespacePrivilege privilege, Set<String> addresses) {
    SortedSet<String> set;
    if (addresses != null) {
      if (addresses.contains(THE_WORLD)) {
        set = WORLD_SET;
      } else {
        set = new TreeSet<>();
        for (String a : addresses) {
          set.add(notNull(a));
        }
      }
    } else {
      set = Collections.emptySortedSet();
    }
    synchronized (privileges) {
      privileges.put(privilege, set);
    }
  }


  /**
   * Set all the addresses that are granted all privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAll(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all the ACL related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all ACL privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllAcl(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_ACL) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all create related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all create privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllCreate(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_CREATE) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all delete related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all delete privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllDelete(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_DELETE) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all document related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all document privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllDocument(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_DOCUMENT) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all namespace privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllNamespace(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_NAMESPACE) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all read related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all read privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllRead(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_READ) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all update related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all update privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllUpdate(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_UPDATE) {
      set(np, addresses);
    }
  }


  /**
   * Set all the addresses that are granted all the validator related privileges.
   *
   * @param addresses the addresses to grant to
   */
  @Schema(description = "The addresses that are granted all validator privileges.", accessMode = AccessMode.WRITE_ONLY)
  @JsonProperty(access = Access.WRITE_ONLY)
  public void setAllValidator(Set<String> addresses) {
    for (NamespacePrivilege np : PRIVILEGES_ALL_VALIDATOR) {
      set(np, addresses);
    }
  }


  public void setCreateAcl(Set<String> addresses) {
    set(CREATE_ACL, addresses);
  }


  public void setCreateDocument(Set<String> addresses) {
    set(CREATE_DOCUMENT, addresses);
  }


  public void setCreateRole(Set<String> addresses) {
    set(CREATE_ROLE, addresses);
  }


  public void setCreateValidator(Set<String> addresses) {
    set(CREATE_VALIDATOR, addresses);
  }


  public void setDeleteAcl(Set<String> addresses) {
    set(DELETE_ACL, addresses);
  }


  public void setDeleteAclRole(Set<String> addresses) {
    set(DELETE_ROLE, addresses);
  }


  public void setDeleteNamespace(Set<String> addresses) {
    set(DELETE_NAMESPACE, addresses);
  }


  public void setDeleteValidator(Set<String> addresses) {
    set(DELETE_VALIDATOR, addresses);
  }


  public void setForceDelete(Set<String> addresses) {
    set(FORCE_DELETE, addresses);
  }


  /**
   * Set all the privileges. This replaces all existing privileges.
   *
   * @param newPrivileges the new privileges
   */
  public void setPrivileges(Map<NamespacePrivilege, SortedSet<String>> newPrivileges) {
    synchronized (privileges) {
      privileges.clear();
      privileges.putAll(newPrivileges);
    }
  }


  public void setReadAcl(Set<String> addresses) {
    set(READ_ACL, addresses);
  }


  public void setReadDescription(Set<String> addresses) {
    set(READ_DESCRIPTION, addresses);
  }


  public void setReadNamespace(Set<String> addresses) {
    set(READ_NAMESPACE, addresses);
  }


  public void setReadRole(Set<String> addresses) {
    set(READ_ROLE, addresses);
  }


  public void setReadValidator(Set<String> addresses) {
    set(READ_VALIDATOR, addresses);
  }


  public void setUpdateAcl(Set<String> addresses) {
    set(UPDATE_ACL, addresses);
  }


  public void setUpdateDescription(Set<String> addresses) {
    set(UPDATE_DESCRIPTION, addresses);
  }


  public void setUpdateNamespace(Set<String> addresses) {
    set(UPDATE_NAMESPACE, addresses);
  }


  public void setUpdateRole(Set<String> addresses) {
    set(UPDATE_ROLE, addresses);
  }


}
