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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.state.StateBase;
import io.setl.json.pointer.PointerFactory;
import io.setl.json.pointer.JsonExtendedPointer;
import io.setl.json.pointer.tree.PointerTree;
import io.setl.json.pointer.tree.PointerTreeBuilder;

/**
 * Helper class to do the complicated process of resolving an access control list.
 *
 * @author Simon Greatrix on 18/02/2020.
 */
class AclResolver {

  private static final Logger logger = LoggerFactory.getLogger(AclResolver.class);

  /** Map of address to the ID of the mix bits, which indicates which roles apply to the address. */
  private final HashMap<String, Integer> addressToMixBits = new HashMap<>();

  /** Pointer tree builders for each unique role mixture. */
  private final ArrayList<EnumMap<CRUD, PointerTreeBuilder>> builders = new ArrayList<>();

  /** The resolved list of ACLs after processing all inclusions. */
  private final ArrayList<Acl> includedLists = new ArrayList<>();

  /** The master ACL that everything starts from. */
  private final Acl master;

  /** All the unique role mixtures. */
  private final ArrayList<BitSet> mixes = new ArrayList<>();

  /** Map of role ID to a mixture index. */
  private final HashMap<SpaceId, Integer> rolesIdToIndex = new HashMap<>();

  /** Map of role ID to role. */
  private final HashMap<SpaceId, AclRole> rolesIdToRole = new HashMap<>();

  /** Current state. */
  private final StateBase state;


  /**
   * New instance.
   *
   * @param state the current state
   * @param acl   the starting ACL
   */
  AclResolver(StateBase state, Acl acl) {
    this.state = state;
    master = acl;
  }


  /**
   * Build the pointer trees for each role combination.
   */
  private void buildTrees() {
    int size = builders.size();
    // for every role
    for (Entry<SpaceId, AclRole> roleEntry : rolesIdToRole.entrySet()) {
      // Get the role details
      EnumMap<CRUD, HashSet<JsonExtendedPointer>> flipped = flipRole(roleEntry.getValue());
      int roleIndex = rolesIdToIndex.get(roleEntry.getKey());

      // for every mixture
      for (int i = 0; i < size; i++) {
        // if the mixture includes the role
        if (mixes.get(i).get(roleIndex)) {
          // add the details to the builders
          EnumMap<CRUD, PointerTreeBuilder> builder = builders.get(i);
          for (Entry<CRUD, HashSet<JsonExtendedPointer>> flipEntry : flipped.entrySet()) {
            PointerTreeBuilder b = builder.get(flipEntry.getKey());
            flipEntry.getValue().forEach(b::add);
          }
        }
      }
    }
  }


  private Map<String, Map<CRUD, PointerTree>> finish() {
    // finish building the trees
    ArrayList<EnumMap<CRUD, PointerTree>> trees = new ArrayList<>(builders.size());
    for (EnumMap<CRUD, PointerTreeBuilder> b : builders) {
      EnumMap<CRUD, PointerTree> m = new EnumMap<>(CRUD.class);
      b.forEach((k, v) -> m.put(k, v.build()));
      trees.add(m);
    }

    HashMap<String, Map<CRUD, PointerTree>> result = new HashMap<>();
    addressToMixBits.forEach((k, v) -> result.put(k, trees.get(v)));
    return result;
  }


  /**
   * Flip the path -> CRUD set mapping of a role into a CRUD -> pointers mapping.
   *
   * @param role the role
   *
   * @return the flipped mapping
   */
  private EnumMap<CRUD, HashSet<JsonExtendedPointer>> flipRole(AclRole role) {
    EnumMap<CRUD, HashSet<JsonExtendedPointer>> flipped = new EnumMap<>(CRUD.class);
    for (CRUD c : CRUD.values()) {
      flipped.put(c, new HashSet<>());
    }

    Map<String, CrudSet> permissions = role.getPermissions();
    for (Entry<String, CrudSet> entry : permissions.entrySet()) {
      JsonExtendedPointer pointer = PointerFactory.create(entry.getKey());
      for (CRUD c : entry.getValue()) {
        flipped.get(c).add(pointer);
      }
    }
    return flipped;
  }


  /**
   * Create the pointer tree builders.
   */
  private void initialiseBuilders() {
    CRUD[] crudValues = CRUD.values();
    for (int i = 0; i < mixes.size(); i++) {
      EnumMap<CRUD, PointerTreeBuilder> builder = new EnumMap<>(CRUD.class);
      for (CRUD c : crudValues) {
        builder.put(c, new PointerTreeBuilder());
      }
      builders.add(builder);
    }
  }


  /**
   * Load all roles for all the ACLs from state.
   */
  private void loadRoles() {
    for (Acl acl : includedLists) {
      for (RoleUsage roleUsage : acl.getRoleAddresses()) {
        SpaceId roleId = roleUsage.getRoleId();

        // roles may be in multiple ACLs, so only load once
        if (!rolesIdToRole.containsKey(roleId)) {
          AclRole role = AclRole.load(state, roleId);

          if (role != null) {
            rolesIdToRole.put(roleId, role);
            rolesIdToIndex.put(roleId, rolesIdToIndex.size());
          } else {
            // If the role doesn't exist warn that we can't do it.
            logger.warn("ACL role {} was not found in state", roleId);
          }
        }
      }
    }
  }


  /**
   * For every address in the ACLs, map it to a BitSet indicating the applied roles.
   */
  private void mapAddressesToMixes() {
    // Map each address to an appropriate BitSet
    int roleCount = rolesIdToIndex.size();
    HashMap<String, BitSet> addressToBits = new HashMap<>();
    for (Acl acl : includedLists) {
      for (RoleUsage roleUsage : acl.getRoleAddresses()) {
        int index = rolesIdToIndex.get(roleUsage.getRoleId());
        for (String address : roleUsage.getAddresses()) {
          BitSet bitSet = addressToBits.computeIfAbsent(address, a -> new BitSet(roleCount));
          bitSet.set(index);
        }
      }
    }

    // Anything granted to the world is available to all addresses
    BitSet worldSet = addressToBits.get(RoleUsage.THE_WORLD);
    if (worldSet != null) {
      for (BitSet bitSet : addressToBits.values()) {
        bitSet.or(worldSet);
      }
    }

    // Map each BitSet to a unique identifier
    HashMap<BitSet, Integer> mixToIndex = new HashMap<>();
    for (Entry<String, BitSet> entry : addressToBits.entrySet()) {
      Integer index = mixToIndex.computeIfAbsent(entry.getValue(), bitSet -> {
        Integer id = mixes.size();
        mixes.add(bitSet);
        return id;
      });
      addressToMixBits.put(entry.getKey(), index);
    }
  }


  Map<String, Map<CRUD, PointerTree>> resolve() {
    resolveIncludes(master, new HashSet<>());
    loadRoles();
    mapAddressesToMixes();
    initialiseBuilders();
    buildTrees();
    return finish();
  }


  /**
   * Identify all the ACLs involved in this resolution by scanning all the includes.
   *
   * @param acl   the ACL to get includes from
   * @param names the names of ACLs already included
   */
  private void resolveIncludes(Acl acl, HashSet<String> names) {
    if (!names.add(acl.getKey())) {
      // already processed
      return;
    }

    // Add it to the list
    includedLists.add(acl);

    // Find and include all this ACLs included lists
    for (SpaceId other : acl.getIncluded()) {
      Acl nextAcl = Acl.load(state, other);
      if (nextAcl == null) {
        logger.warn("ACL list {} which is included from {} was not found.", other, acl.getKey());
        continue;
      }
      resolveIncludes(nextAcl, names);
    }
  }

}
