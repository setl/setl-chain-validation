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
package io.setl.bc.json.data.acl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.data.StandardDatum;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.state.KeyedList;
import io.setl.bc.pychain.state.StateBase;
import io.setl.common.ObjectArrayReader;
import io.setl.common.TypeSafeMap;

/**
 * An Access Control List Role. The role defines the paths and permissions a user assigned that role would have access to.
 *
 * <p>The external representation of the role is a map of create, read, update and delete privileges to paths in the document.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 *   "/sales/southwest" : [ 'READ' ],
 *   "/sales/south" : [ 'Read' ],
 *   "/sales/southeast" : [ 'r' ],
 *   "/sales/southwest/actual" : [ 'CREATE', 'UPDATE' ],
 *   "/sales/south/actual" : [ 'c', 'u' ],
 *   "/sales/southeast/actual" : [ 'Up', 'Create' ],
 *   "/staff/sales/review" : [ 'r', 'create', 'DELETE' ],
 *   "/sales/trash" : [ 'r', 'd' ]
 * </pre>
 *
 * <p>No privilege implies another. For example, being able to create data does not imply the same data can be read.</p>
 *
 * <p>A privilege applies to all descendants of a specified node.</p>
 *
 * <p>The path is in the form of a JSON pointer. The special '-' pointer token may be used to indicate any element of an array.</p>
 *
 * @author Simon Greatrix on 20/01/2020.
 */
public class AclRole extends StandardDatum {

  public static final AclRole EMPTY = new AclRole();


  /**
   * Load an ACL Role by its ID, allowing for the standard ACL Roles.
   *
   * @param state  the state where ACLs are stored
   * @param roleId the role ID to retrieve
   *
   * @return the role, or null
   */
  @Nullable
  public static AclRole load(StateBase state, SpaceId roleId) {
    KeyedList<String, AclRole> merkle = state.getMerkle(AclRole.class);
    AclRole role = merkle.find(roleId.getFullId());
    if (role != null) {
      return role;
    }
    return StandardAcls.getRole(roleId);
  }


  private SortedMap<String, CrudSet> permissions;


  public AclRole() {
    permissions = Collections.emptySortedMap();
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public AclRole(
      @JsonProperty(value = "id", required = true) SpaceId id
  ) {
    super(id);
    permissions = Collections.emptySortedMap();
  }


  public AclRole(AclRole copy) {
    super(copy);
    permissions = copy.permissions;
  }


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public AclRole(ObjectArrayReader reader) {
    super(reader);
    SortedMap<String, CrudSet> map = reader.getMap(TreeMap::new, String::valueOf, o -> CrudSet.forMask(TypeSafeMap.asInt(o)));
    permissions = Collections.unmodifiableSortedMap(map);
  }


  @Override
  public AclRole copy() {
    return new AclRole(this);
  }


  @Override
  protected void encode(List<Object> list) {
    super.encode(list);

    // The encoded form specifies each pointer once, with a CRUD list for what can be done with that pointer.
    list.add(ObjectArrayReader.encode(permissions, o -> o, CrudSet::getMask));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AclRole)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    AclRole aclRole = (AclRole) o;
    return permissions.equals(aclRole.permissions);
  }


  /**
   * Get the permissions in this role. The map key is the path. The map values are the CRUD permissions.
   *
   * @return a map of the permissions
   */
  public Map<String, CrudSet> getPermissions() {
    return permissions;
  }


  @Override
  public int hashCode() {
    int result = super.hashCode();
    return 31 * result + permissions.hashCode();
  }


  /**
   * Set the permissions of this role.
   *
   * @param map the new permissions
   */
  @Schema(description = "The permissions provided by this role, as JSON document pointer to permissions.")
  public void setPermissions(Map<String, CrudSet> map) {
    TreeMap<String, CrudSet> newPermissions = new TreeMap<>(map);
    permissions = Collections.unmodifiableSortedMap(newPermissions);
  }


  @Override
  public String toString() {
    return String.format(
        "AclRole(%s)",
        this.permissions
    );
  }

}
