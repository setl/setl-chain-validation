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

import static io.setl.bc.json.data.acl.RoleUsage.THE_OWNER;
import static io.setl.bc.json.data.acl.RoleUsage.THE_WORLD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.util.CopyOnWriteMap;

/**
 * A collection of built-in ACLs. The built-in ACLs are held in the implicit "$INTERNAL$" namespace and their names are composed of what the document owner
 * and what other users can do at the document root. The name "CRUD/" allows the owner to create, read, update and delete, but global users are granted no
 * special access. "/CRUD" allows universal access to all operations. "C/R" allows the user to create and everybody to read.
 *
 * @author Simon Greatrix on 09/07/2020.
 */
public class StandardAcls {

  private static final Pattern ACL_MATCH = Pattern.compile("(?i)[CRUD]{0,4}/[CRUD]{0,4}");

  private static final CopyOnWriteMap<String, Acl> NAME_TO_ACL = new CopyOnWriteMap<>();

  private static final CopyOnWriteMap<String, AclRole> NAME_TO_ROLE = new CopyOnWriteMap<>();

  private static final Pattern ROLE_MATCH = Pattern.compile("(?i)[CRUD]{1,4}");


  /**
   * Get a standard ACL by its ID.
   *
   * @param id the ID
   *
   * @return the ACL, or null if it is not a standard one
   */
  @Nullable
  public static Acl getAcl(SpaceId id) {
    if (!(
        id.getNamespace().equals(DataNamespace.INTERNAL_NAMESPACE)
            && ACL_MATCH.matcher(id.getId()).matches()
            && id.getId().length() > 1)) {
      return null;
    }

    String name = id.getId().toUpperCase();
    Acl acl = NAME_TO_ACL.get(name);
    if (acl != null) {
      return acl;
    }

    // Create the ACL
    int dividerPosition = name.indexOf('/');
    String ownerRole = name.substring(0, dividerPosition);
    String worldRole = name.substring(dividerPosition + 1);

    List<RoleUsage> usage = new ArrayList<>(2);
    if (!ownerRole.isEmpty()) {
      RoleUsage r = new RoleUsage();
      r.setRoleId(new SpaceId(DataNamespace.INTERNAL_NAMESPACE, ownerRole));
      r.addAllAddresses(List.of(THE_OWNER));
      usage.add(r);
    }
    if (!worldRole.isEmpty()) {
      RoleUsage r = new RoleUsage();
      r.setRoleId(new SpaceId(DataNamespace.INTERNAL_NAMESPACE, worldRole));
      r.addAllAddresses(List.of(THE_WORLD));
      usage.add(r);
    }

    acl = new Acl();
    acl.setId(new SpaceId(DataNamespace.INTERNAL_NAMESPACE, name));
    acl.setRoleAddresses(usage);
    NAME_TO_ACL.put(name, acl);

    return acl;
  }


  /**
   * Get the ID for one of the internal ACLs according to its nature. The nature
   * @param nature the nature specification.
   * @return the ID
   */
  public static SpaceId getAclId(String nature) {
    if (nature == null || nature.length() < 2 || !ACL_MATCH.matcher(nature).matches()) {
      throw new IllegalArgumentException("Nature \"" + nature + "\" is not a valid internal ACL.");
    }
    return new SpaceId(DataNamespace.INTERNAL_NAMESPACE, nature);
  }


  private static Iterable<String> getOps(String spec) {
    String[] ops = new String[spec.length()];
    for (int i = 0; i < spec.length(); i++) {
      ops[i] = Character.toString(spec.charAt(i));
    }
    return Arrays.asList(ops);
  }


  /**
   * Get a standard ACL Role by its ID.
   *
   * @param id the role's ID
   *
   * @return the role, or null if it is not a standard one
   */
  @Nullable
  public static AclRole getRole(SpaceId id) {
    String name = id.getId();
    if (!(id.getNamespace().equals(DataNamespace.INTERNAL_NAMESPACE) && ROLE_MATCH.matcher(name).matches())) {
      return null;
    }

    name = name.toUpperCase();
    AclRole role = NAME_TO_ROLE.get(name);
    if (role != null) {
      return role;
    }

    // Create the role
    CrudSet crudSet = CrudSet.forOperations(getOps(name));
    role = new AclRole();
    role.setId(new SpaceId(DataNamespace.INTERNAL_NAMESPACE, name.toUpperCase()));
    role.setPermissions(Map.of("", crudSet));
    NAME_TO_ROLE.put(name, role);
    return role;
  }

}
