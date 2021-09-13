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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonPatch;
import javax.json.JsonStructure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.data.StandardDatum;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.state.KeyedList;
import io.setl.bc.pychain.state.StateBase;
import io.setl.common.ObjectArrayReader;
import io.setl.json.patch.Patch;
import io.setl.json.patch.PatchOperation;
import io.setl.json.patch.ops.Copy;
import io.setl.json.patch.ops.Move;
import io.setl.json.patch.ops.Test;
import io.setl.json.pointer.PointerFactory;
import io.setl.json.pointer.JsonExtendedPointer;
import io.setl.json.pointer.JsonExtendedPointer.ResultOfAdd;
import io.setl.json.pointer.tree.PointerEmptyTree;
import io.setl.json.pointer.tree.PointerTree;
import io.setl.json.pointer.tree.PointerTreeBuilder;

/**
 * An Access Control List (ACL) for modifying or accessing a JSON document. ACLs are strictly additive: a privilege granted by one list cannot be revoked by
 * another.
 *
 * @author Simon Greatrix on 13/01/2020.
 */
public class Acl extends StandardDatum {

  public static final Acl EMPTY = new Acl();


  /**
   * Load an ACL by its ID, allowing for the standard ACLs.
   *
   * @param state the state where ACLs are stored
   * @param aclId the acl ID to retrieve
   *
   * @return the ACL, or null
   */
  @Nullable
  public static Acl load(StateBase state, SpaceId aclId) {
    KeyedList<String, Acl> merkle = state.getMerkle(Acl.class);
    Acl acl = merkle.find(aclId.getFullId());
    if (acl != null) {
      return acl;
    }
    return StandardAcls.getAcl(aclId);
  }


  /**
   * Test if a user has the permission on a path.
   *
   * @param treeMap the user's permissions
   * @param crud    the permission required
   * @param pointer the path
   *
   * @return true if the user DOES NOT have the permission
   */
  private static boolean test(Map<CRUD, PointerTree> treeMap, CRUD crud, JsonExtendedPointer pointer) {
    PointerTree tree = treeMap.get(crud);
    return tree == null || !tree.isParentOf(pointer);
  }


  /**
   * Verify an "ADD" operation in a patch. An "add" may be a "create" or an "update" and if the user does not have both privileges an additional custom test
   * must be added to the patch to ensure it only succeeds if the correct permission is available.
   */
  private static boolean verifyPatchAdd(ListIterator<PatchOperation> iterator, Map<CRUD, PointerTree> treeMap, PatchOperation op) {
    // A JsonPatch "add" always creates a new entry in an array, but will update objects if a value is present.
    JsonExtendedPointer pointer = op.getPathPointer();
    boolean cantCreate = test(treeMap, CRUD.CREATE, pointer);
    boolean cantUpdate = test(treeMap, CRUD.UPDATE, pointer);
    ResultOfAdd result = null;
    if (cantCreate) {
      if (cantUpdate) {
        // Can't create and can't update, so forbidden to add
        return true;
      } else {
        // we can do this operation provided it updates
        result = ResultOfAdd.UPDATE;
      }
    } else {
      if (cantUpdate) {
        // we can do this operation provided it creates.
        result = ResultOfAdd.CREATE;
      }
    }

    if (result != null) {
      // Add a test to ensure the add will do an allowed thing.
      iterator.previous();
      iterator.add(new Test(pointer, result));
      iterator.next();
    }
    return false;
  }


  private static boolean verifyPatchCopy(ListIterator<PatchOperation> iterator, Map<CRUD, PointerTree> treeMap, PatchOperation op) {
    // Must be able to add at the target location
    if (verifyPatchAdd(iterator, treeMap, op)) {
      return true;
    }

    // Must be able to read the value we are copying from
    JsonExtendedPointer pointer = ((Copy) op).getFromPointer();
    return test(treeMap, CRUD.READ, pointer);
  }


  private static boolean verifyPatchMove(ListIterator<PatchOperation> iterator, Map<CRUD, PointerTree> treeMap, PatchOperation op) {
    // Must be able to add at the target location
    if (verifyPatchAdd(iterator, treeMap, op)) {
      return true;
    }

    // Must be able to read and delete the value we are copying from
    JsonExtendedPointer pointer = ((Move) op).getFromPointer();
    return test(treeMap, CRUD.READ, pointer) || test(treeMap, CRUD.DELETE, pointer);
  }


  /**
   * List of included ACLs.
   */
  private SortedSet<SpaceId> included;

  /**
   * The effective permissions. Only available after the roles are resolved.
   */
  private Map<String, Map<CRUD, PointerTree>> permissions;

  /**
   * Chain height that the role definitions are correct for.
   */
  private int resolvedHeight = -1;

  /**
   * Collection of role usages.
   */
  private SortedSet<RoleUsage> roleUsages;


  /**
   * New empty instance.
   */
  public Acl() {
    roleUsages = Collections.emptySortedSet();
    included = Collections.emptySortedSet();
    permissions = null;
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public Acl(
      @JsonProperty(value = "id", required = true) SpaceId id
  ) {
    super(id);
    roleUsages = Collections.emptySortedSet();
    included = Collections.emptySortedSet();
    permissions = null;
  }


  /**
   * New instance from encoded representation.
   *
   * @param reader the representation reader
   */
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public Acl(ObjectArrayReader reader) {
    super(reader);
    roleUsages = ObjectArrayReader.convertToSortedSet(reader.getArray(), RoleUsage::new);
    included = ObjectArrayReader.convertToSortedSet(reader.getArray(), SpaceId::new);
    permissions = null;
  }


  /**
   * New instance as copy of another.
   *
   * @param acl the ACL to copy
   */
  public Acl(Acl acl) {
    super(acl);
    permissions = acl.permissions;
    resolvedHeight = acl.resolvedHeight;
    roleUsages = acl.roleUsages;
    included = acl.included;
  }


  /**
   * Check if a user can create the proposed structure.
   *
   * @param address  the user's address
   * @param isOwner  is the user the registered document owner?
   * @param proposed the proposed structure
   *
   * @return true if creation is allowed
   */
  public boolean checkCanCreate(String address, boolean isOwner, JsonStructure proposed) {
    Map<CRUD, PointerTree> treeMap = getPermissions(address, isOwner);
    PointerTree tree = treeMap.get(CRUD.CREATE);
    if (tree == null) {
      return false;
    }
    return tree.containsAll(proposed);
  }


  @Override
  public Acl copy() {
    return new Acl(this);
  }


  /**
   * Create a copy of all parts of a structure that a user has read access to.
   *
   * @param address the user's address
   * @param isOwner is the user the registered document owner?
   * @param source  the source structure
   * @param <T>     the structure type
   *
   * @return the readable parts of the structure
   */
  public <T extends JsonStructure> T createReadableCopy(String address, boolean isOwner, T source) {
    Map<CRUD, PointerTree> treeMap = getPermissions(address, isOwner);
    PointerTree tree = treeMap.getOrDefault(CRUD.READ, PointerEmptyTree.INSTANCE);
    return tree.copy(source);
  }


  @Override
  protected void encode(List<Object> list) {
    synchronized (this) {
      super.encode(list);
      list.add(roleUsages.stream().map(RoleUsage::encode).toArray());
      list.add(included.stream().map(SpaceId::encode).toArray());
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Acl)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    Acl acl = (Acl) o;

    if (!getIncluded().equals(acl.getIncluded())) {
      return false;
    }
    return getRoleAddresses().equals(acl.getRoleAddresses());
  }


  /**
   * Get all the addresses mentioned in this ACL.
   *
   * @return the addresses
   */
  @JsonIgnore
  @Hidden
  public Set<String> getAllAddresses() {
    HashSet<String> set = new HashSet<>();
    synchronized (this) {
      for (RoleUsage ru : roleUsages) {
        set.addAll(ru.getAddresses());
      }
    }
    return set;
  }


  /**
   * Get the IDs of all ACLs included in this ACL.
   *
   * @return the included IDs
   */
  public SortedSet<SpaceId> getIncluded() {
    synchronized (this) {
      return included;
    }
  }


  @Nonnull
  private Map<CRUD, PointerTree> getPermissions(String address, boolean isOwner) {
    Map<CRUD, PointerTree> treeMap = permissions.get(address);

    if (isOwner) {
      Map<CRUD, PointerTree> ownerMap = permissions.get(RoleUsage.THE_OWNER);
      if (ownerMap != null) {
        if (treeMap != null) {
          // Merge owner and address specific permissions
          EnumMap<CRUD, PointerTree> merged = new EnumMap<>(CRUD.class);
          for (CRUD crud : CRUD.values()) {
            PointerTree tree1 = treeMap.get(crud);
            PointerTree tree2 = ownerMap.get(crud);
            if (tree1 != null) {
              if (tree2 != null) {
                // Merge the two trees
                PointerTreeBuilder builder = new PointerTreeBuilder();
                tree1.getPointers().forEach(builder::add);
                tree2.getPointers().forEach(builder::add);
                merged.put(crud, builder.build());
              } else {
                merged.put(crud, tree1);
              }
            } else if (tree2 != null) {
              merged.put(crud, tree2);
            }
            treeMap = merged;
          }
        } else {
          // treeMap was null, but ownerMap is not null, so just use ownerMap.
          treeMap = ownerMap;
        }
      }
    }

    // If no permissions so far, then the permissions will be just what is granted to the world.
    if (treeMap == null) {
      treeMap = permissions.getOrDefault(RoleUsage.THE_WORLD, Collections.emptyMap());
    }

    return treeMap;
  }


  /**
   * Get the access list as role names to addresses with that role.
   *
   * @return the access list
   */
  public Collection<RoleUsage> getRoleAddresses() {
    synchronized (this) {
      return roleUsages;
    }
  }


  /**
   * Does this ACL list allow the sought permission?.
   *
   * @param address    the address seeking permission
   * @param isOwner    is the user the registered document owner?
   * @param path       the path where the permission is sought
   * @param permission the permission being sought
   *
   * @return true if the permission is allowed
   */
  public boolean hasPermission(String address, boolean isOwner, String path, CRUD permission) {
    Map<CRUD, PointerTree> treeMap = getPermissions(address, isOwner);
    PointerTree tree = treeMap.getOrDefault(permission, PointerEmptyTree.INSTANCE);
    JsonExtendedPointer pointer = PointerFactory.create(path);
    return tree.isParentOf(pointer);
  }


  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getIncluded().hashCode();
    return 31 * result + getRoleAddresses().hashCode();
  }


  /**
   * Retrieve all parts of this ACL from state and resolve the effective permissions for each address.
   *
   * @param state representation of state
   */
  public void resolveRoles(StateBase state) {
    synchronized (this) {
      if (resolvedHeight == state.getHeight()) {
        // already resolved at this height
        return;
      }

      AclResolver resolver = new AclResolver(state, this);
      permissions = resolver.resolve();
      resolvedHeight = state.getHeight();
    }
  }


  /**
   * Other ACLs included in this ACL.
   *
   * @param included the included ACLs
   */
  @Schema(description = "Identifiers of other ACLs which are included in this ACL")
  public void setIncluded(SortedSet<SpaceId> included) {
    synchronized (this) {
      this.included = Collections.unmodifiableSortedSet(new TreeSet<>(included));
      permissions = null;
      resolvedHeight = -1;
    }
  }


  /**
   * Set the access list as roles to addresses with that role.
   *
   * @param roleAddresses the new access list
   */
  @Schema(description = "Mappings of ACL Roles to the addresses that are permitted to use them.")
  public void setRoleAddresses(Collection<RoleUsage> roleAddresses) {
    synchronized (this) {
      if (roleAddresses == null) {
        roleUsages = Collections.emptySortedSet();
        return;
      }

      // We consolidate the roles
      TreeMap<SpaceId, RoleUsage> map = new TreeMap<>();
      for (RoleUsage ru : roleAddresses) {
        if (ru.getAddresses().isEmpty()) {
          continue;
        }
        SpaceId roleId = ru.getRoleId();
        RoleUsage current = map.get(roleId);
        if (current == null) {
          map.put(roleId, ru);
        } else {
          current.addAllAddresses(ru.getAddresses());
        }
      }

      roleUsages = Collections.unmodifiableSortedSet(new TreeSet<>(map.values()));
      permissions = null;
      resolvedHeight = -1;
    }
  }


  @Override
  public String toString() {
    return String.format(
        "Acl(included=%s, resolvedHeight=%s, permissions=%s, roleUsages=%s, key='%s', lastModified=%s, metadata='%s', title='%s', updateHeight=%s)",
        this.included,
        this.resolvedHeight,
        this.permissions,
        this.roleUsages,
        this.getId(),
        this.lastModified,
        this.metadata,
        this.title,
        this.updateHeight
    );
  }


  /**
   * Create an approved copy of a patch. Approving the patch may require adding additional test operations.
   *
   * @param address the address enacting the patch
   * @param patch   the patch
   *
   * @return a copy of the patch, or null if it is not approved at all.
   */
  @Nullable
  public JsonPatch verifyPatch(String address, boolean isOwner, JsonPatch patch) {
    Map<CRUD, PointerTree> treeMap = getPermissions(address, isOwner);
    if (treeMap.isEmpty()) {
      return null;
    }

    // Create a copy
    Patch myPatch;
    if (patch instanceof Patch) {
      myPatch = new Patch(((Patch) patch).getOperations());
    } else {
      myPatch = new Patch(patch.toJsonArray());
    }

    List<PatchOperation> operations = myPatch.getOperations();
    ListIterator<PatchOperation> iterator = operations.listIterator();
    while (iterator.hasNext()) {
      PatchOperation op = iterator.next();
      boolean checkResult;
      switch (op.getOperation()) {
        case ADD:
          checkResult = verifyPatchAdd(iterator, treeMap, op);
          break;

        case COPY:
          checkResult = verifyPatchCopy(iterator, treeMap, op);
          break;

        case MOVE:
          checkResult = verifyPatchMove(iterator, treeMap, op);
          break;

        case REMOVE:
          // Must be able to delete
          checkResult = test(treeMap, CRUD.DELETE, op.getPathPointer());
          break;

        case REPLACE:
          // Must be able to update
          checkResult = test(treeMap, CRUD.UPDATE, op.getPathPointer());
          break;

        case TEST:
          // Must be able to read
          checkResult = test(treeMap, CRUD.READ, op.getPathPointer());
          break;

        default:
          // Should be unreachable
          throw new IllegalArgumentException("Unknown patch operation: " + op.getOperation());
      }
      if (checkResult) {
        return null;
      }
    }

    // All steps of the patch are OK
    return myPatch;
  }

}
