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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.common.ObjectArrayReader;

/**
 * A data transfer object representing the triplet of namespace - role ID - addresses.
 *
 * @author Simon Greatrix on 24/01/2020.
 */
public class RoleUsage implements Comparable<RoleUsage> {

  /** Special address used to indicate that a role is available to the document's owner, if a document has a specified owner. */
  public static final String THE_OWNER = "OWNER";

  /** The set that contains the owner. */
  public static final SortedSet<String> OWNER_SET = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of(THE_OWNER)));

  /** Special address used to indicate that a role is available to everyone. */
  public static final String THE_WORLD = "WORLD";

  /** The set that contains the world. */
  public static final SortedSet<String> WORLD_SET = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of(THE_WORLD)));

  private SortedSet<String> addresses = Collections.emptySortedSet();

  private SpaceId roleId = SpaceId.EMPTY;


  public RoleUsage() {
    // do nothing
  }


  public RoleUsage(ObjectArrayReader reader) {
    roleId = new SpaceId(reader.getReader());
    addresses = ObjectArrayReader.convertToSortedStrings(reader.getArray());
  }


  /**
   * Add all the addresses specified to this role usage.
   *
   * @param toAdd the addresses to add
   */
  public void addAllAddresses(Collection<String> toAdd) {
    if (addresses.isEmpty()) {
      addresses = new TreeSet<>(toAdd);
    } else {
      addresses.addAll(toAdd);
    }
    if (addresses.contains(THE_WORLD)) {
      addresses = WORLD_SET;
    }
  }


  @Override
  public int compareTo(@NotNull RoleUsage o) {
    int c = roleId.compareTo(o.roleId);
    if (c != 0) {
      return c;
    }

    // Need to compare the addresses in dictionary order
    Iterator<String> me = addresses.iterator();
    Iterator<String> other = o.addresses.iterator();
    while (me.hasNext()) {
      if (!other.hasNext()) {
        // I'm longer than the other, so I sort after
        return 1;
      }

      // Compare current addresses
      String myAddress = me.next();
      String otherAddress = other.next();
      c = myAddress.compareTo(otherAddress);
      if (c != 0) {
        return c;
      }
    }

    // I'm either shorter or identical
    return other.hasNext() ? -1 : 0;
  }


  public Object[] encode() {
    return new Object[]{roleId.encode(), addresses.toArray()};
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RoleUsage)) {
      return false;
    }

    RoleUsage roleUsage = (RoleUsage) o;

    if (!addresses.equals(roleUsage.addresses)) {
      return false;
    }
    return roleId.equals(roleUsage.roleId);
  }


  /**
   * The addresses associated with this role usage.
   *
   * @return the addresses
   */
  @Schema(description = "The addresses associated with the role")
  public SortedSet<String> getAddresses() {
    return addresses != null ? addresses : Collections.emptySortedSet();
  }


  /**
   * The ID of the ACL Role.
   *
   * @return the role's ID
   */
  @Schema(description = "The identifier of the ACL role")
  public SpaceId getRoleId() {
    return roleId != null ? roleId : SpaceId.EMPTY;
  }


  @Override
  public int hashCode() {
    int result = addresses.hashCode();
    return 31 * result + roleId.hashCode();
  }


  /**
   * Set the addresses which can use the role.
   * @param addresses the addresses
   */
  public void setAddresses(SortedSet<String> addresses) {
    this.addresses = addresses != null ? new TreeSet<>(addresses) : Collections.emptySortedSet();
    if (this.addresses.contains(THE_WORLD)) {
      this.addresses = WORLD_SET;
    }
  }


  public void setRoleId(SpaceId roleId) {
    this.roleId = roleId;
  }

}
